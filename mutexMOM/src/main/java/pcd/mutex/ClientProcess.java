package pcd.mutex;

import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

public class ClientProcess {

    public static void main(String[] argv) throws Exception {
        String name = argv.length > 0 ? argv[0] : "P";
        String cs = "resource-A";
        int rounds = 50;

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(MutexProtocol.HOST);
        Connection connection = factory.newConnection();
        DistributedMutex mutex = new DistributedMutex(connection, cs);

        for (int i = 0; i < rounds; i++) {
            System.out.println("[" + name + "] need CS...");
            mutex.acquire();
            System.out.println("[" + name + "] >>> IN");
            Thread.sleep(1000);                  // cs
            System.out.println("[" + name + "] <<< OUT");
            mutex.release();
            Thread.sleep(500);                   // ncs
        }

        connection.close();
    }
}
