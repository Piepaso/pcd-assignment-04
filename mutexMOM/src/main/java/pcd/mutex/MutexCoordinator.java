package pcd.mutex;

import com.rabbitmq.client.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

/**
 * COORDINATORE (P0) dell'algoritmo CENTRALIZZATO di mutua esclusione.
 *
 * Per ogni sezione critica mantiene:
 *   - holder  : la coda privata del processo che detiene il "token"
 *               (null = sezione libera)                     -> stato del token
 *   - waiting : coda FIFO delle richieste pendenti          -> reqlist
 *
 * Politica: la sezione viene sempre concessa alla TESTA della coda di attesa
 * (la richiesta "eligible"). Questo realizza:
 *   - SAFETY    : un solo holder per volta, quindi mai due processi nella CS;
 *   - LIVENESS  : finche' i processi rilasciano, ogni richiesta viene servita;
 *   - FAIRNESS  : le richieste sono concesse nell'ordine in cui arrivano al
 *                 coordinatore (FIFO). [Qui si innesterebbe il vector clock
 *                 delle slide per una fairness in ordine causale.]
 *
 * Vantaggio rispetto al "token-in-coda": NON serve il bootstrap del token.
 * Il coordinatore E' l'autorita': all'avvio ogni sezione e' semplicemente
 * libera (holder == null), quindi non esiste il rischio di token duplicati.
 */
public class MutexCoordinator {

    /** Stato per singola sezione critica. */
    private static final class CSState {
        String holder = null;                              // null = libera
        final Queue<String> waiting = new LinkedList<>();  // reqlist (FIFO)
    }

    private final Map<String, CSState> sections = new HashMap<>();
    private Channel channel;

    private CSState state(String cs) {
        return sections.computeIfAbsent(cs, k -> new CSState());
    }

    public void start() throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(MutexProtocol.HOST);
        Connection connection = factory.newConnection();
        channel = connection.createChannel();

        channel.queueDeclare(MutexProtocol.COORDINATOR_QUEUE, false, false, false, null);
        channel.basicQos(1);   // una richiesta alla volta: elaborazione serializzata
        System.out.println("[coordinator] in ascolto su '" + MutexProtocol.COORDINATOR_QUEUE + "'");

        DeliverCallback onRequest = (consumerTag, delivery) -> {
            String body = new String(delivery.getBody(), "UTF-8");
            String replyTo = delivery.getProperties().getReplyTo();
            handle(body, replyTo);
            channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
        };
        channel.basicConsume(MutexProtocol.COORDINATOR_QUEUE, false, onRequest, tag -> {});
    }


    private synchronized void handle(String body, String replyTo) throws IOException {
        String[] parts = body.split(" ", 2);
        String cmd = parts[0];
        String cs = parts.length > 1 ? parts[1] : "";
        CSState st = state(cs);

        switch (cmd) {
            case MutexProtocol.CMD_REQUEST:
                st.waiting.add(replyTo);                 // append a reqlist
                System.out.println("[coordinator] REQUEST  cs=" + cs + " da " + shortId(replyTo)
                        + "  (in attesa: " + st.waiting.size() + ")");
                tryGrant(cs, st);
                break;

            case MutexProtocol.CMD_RELEASE:
                System.out.println("[coordinator] RELEASE  cs=" + cs + " da " + shortId(replyTo));
                if (replyTo.equals(st.holder)) {
                    st.holder = null;                    // token tornato libero
                }
                tryGrant(cs, st);
                break;

            default:
                System.err.println("[coordinator] comando ignoto: " + body);
        }
    }

    /** Se la sezione e' libera e c'e' qualcuno in attesa, concede il token. */
    private void tryGrant(String cs, CSState st) throws IOException {
        if (st.holder == null && !st.waiting.isEmpty()) {
            String next = st.waiting.poll();             // richiesta eligible = testa FIFO
            st.holder = next;
            String grant = MutexProtocol.CMD_GRANT + " " + cs;
            channel.basicPublish("", next, null, grant.getBytes("UTF-8"));
            System.out.println("[coordinator] GRANT    cs=" + cs + " a  " + shortId(next));
        }
    }

    /** Ultimi caratteri del nome coda anonima, per log leggibili. */
    private static String shortId(String q) {
        return q == null ? "?" : q.substring(Math.max(0, q.length() - 6));
    }

    public static void main(String[] args) throws Exception {
        new MutexCoordinator().start();
        System.out.println("[coordinator] avviato. Premi CTRL+C per terminare.");
    }
}
