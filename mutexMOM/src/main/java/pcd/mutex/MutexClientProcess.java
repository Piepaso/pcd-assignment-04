package pcd.mutex;

import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

/**
 * Processo client AUTONOMO (JVM separata), da lanciare in piu' terminali per
 * osservare la mutua esclusione "dal vivo".
 *
 *   java pcd.lab13.rabbitmq.MutexClientProcess <nome> [csName] [iterazioni]
 *
 * Ogni processo entra/esce ripetutamente dalla sezione critica stampando
 * IN/OUT con un timestamp: lanciandone piu' istanze sulla stessa CS non si
 * vedranno mai due processi "IN" contemporaneamente, e l'ordine dei GRANT
 * rispettera' l'ordine di arrivo delle richieste al coordinatore.
 */
public class MutexClientProcess {

    public static void main(String[] args) throws Exception {
        String name = args.length > 0 ? args[0] : "P" + (int) (Math.random() * 1000);
        String cs = args.length > 1 ? args[1] : "resource-A";
        int rounds = 3;

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(MutexProtocol.HOST);

        try (Connection connection = factory.newConnection();
             DistributedMutex mutex = new DistributedMutex(connection, cs)) {

            for (int i = 0; i < rounds; i++) {
                System.out.println(ts() + " [" + name + "] asks for CS '" + cs + "'...");
                mutex.acquire();
                System.out.println(ts() + " [" + name + "] >>> IN  (round " + i + ")");

                Thread.sleep(1000 + (long) (Math.random() * 1000));  // lavoro nella CS

                System.out.println(ts() + " [" + name + "] <<< OUT (round " + i + ")");
                mutex.release();

                Thread.sleep((long) (Math.random() * 500));          // pausa fuori dalla CS
            }
        }
        System.out.println("[" + name + "] finito.");
    }

    private static String ts() {
        return String.format("%tT", new java.util.Date());
    }
}
