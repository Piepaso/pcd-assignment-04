package pcd.mutex;

import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * TEST AUTOMATICO della mutua esclusione distribuita.
 *
 * Avvia N "processi" (thread, ciascuno con la PROPRIA connessione RabbitMQ:
 * comunicano solo via broker, come fossero JVM distinte) che competono per la
 * stessa sezione critica. Verifica due proprieta':
 *
 *   1) ORACOLO DI SICUREZZA: dentro la CS deve esserci sempre UN solo processo.
 *      Un contatore atomico "insiders" che superi 1 segnala una violazione.
 *
 *   2) RISORSA CONDIVISA: un contatore incrementato con una sequenza
 *      lettura-attesa-scrittura NON protetta da alcun lock locale. Senza
 *      mutua esclusione si avrebbero "lost update" e il totale finale sarebbe
 *      inferiore all'atteso; con il mutex distribuito il totale e' esatto.
 *
 * PREREQUISITI: RabbitMQ in esecuzione e MutexCoordinator gia' avviato.
 */
public class TestMutex {

    static final String CS = "shared-counter";
    static final int N_PROCESSES = 4;
    static final int INCREMENTS = 10;

    // Oracolo di sicurezza: quanti processi sono dentro la CS in questo istante.
    static final AtomicInteger insiders = new AtomicInteger(0);
    static volatile boolean violation = false;

    // Risorsa condivisa, protetta SOLO dal mutex distribuito.
    static long sharedCounter = 0;

    public static void main(String[] args) throws Exception {
        System.out.println("Avvio " + N_PROCESSES + " processi, "
                + INCREMENTS + " incrementi ciascuno...\n");

        Thread[] threads = new Thread[N_PROCESSES];
        for (int p = 0; p < N_PROCESSES; p++) {
            final String name = "proc-" + p;
            threads[p] = new Thread(() -> runProcess(name));
            threads[p].start();
        }
        for (Thread t : threads) {
            t.join();
        }

        long expected = (long) N_PROCESSES * INCREMENTS;
        System.out.println("\n========== RISULTATO ==========");
        System.out.println("contatore atteso : " + expected);
        System.out.println("contatore finale : " + sharedCounter);
        System.out.println("violazioni ME    : " + (violation ? "SI'" : "nessuna"));
        boolean ok = !violation && sharedCounter == expected;
        System.out.println(ok ? ">>> TEST SUPERATO" : ">>> TEST FALLITO");
        System.exit(ok ? 0 : 1);
    }

    static void runProcess(String name) {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(MutexProtocol.HOST);
        try (Connection connection = factory.newConnection();
             DistributedMutex mutex = new DistributedMutex(connection, CS)) {

            for (int i = 0; i < INCREMENTS; i++) {
                mutex.acquire();
                // ---------------- inizio sezione critica ----------------
                int n = insiders.incrementAndGet();
                if (n != 1) {
                    violation = true;
                    System.out.println("!!! VIOLAZIONE: " + n + " processi nella CS !!!");
                }

                long tmp = sharedCounter;     // lettura
                Thread.sleep(5);              // allarga la finestra di race
                sharedCounter = tmp + 1;      // scrittura (lost update se non protetto)

                insiders.decrementAndGet();
                // ----------------- fine sezione critica -----------------
                mutex.release();
            }
            System.out.println("[" + name + "] completato");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
