package pcd.mutex;

import com.rabbitmq.client.*;
import java.util.*;

public class MutexCoordinator {

    static class CriticalSection {
        String holder = null;
        Queue<String> waiting = new LinkedList<>();
    }

    private final Map<String, CriticalSection> sections = new HashMap<>();
    private Channel channel;

    public static void main(String[] argv) throws Exception {
        new MutexCoordinator().start();
    }

    public void start() throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(MutexProtocol.HOST);
        Connection connection = factory.newConnection();
        channel = connection.createChannel();

        channel.queueDeclare(MutexProtocol.COORDINATOR_QUEUE, false, false, false, null);
        System.out.println("[coordinator] listening on '" + MutexProtocol.COORDINATOR_QUEUE + "'");

        DeliverCallback onRequest = (consumerTag, delivery) -> {
            String msg = new String(delivery.getBody(), "UTF-8");
	        try {
		        handle(msg);
	        } catch (Exception e) {
		        throw new RuntimeException(e);
	        }
        };
        channel.basicConsume(MutexProtocol.COORDINATOR_QUEUE, true, onRequest, tag -> {});
    }

    private void handle(String msg) throws Exception {
        String[] p = msg.split(" ");
        String cmd = p[0];
        String cs = p[1];
        String client = p[2];

        CriticalSection section = sections.get(cs);
        if (section == null) {
            section = new CriticalSection();
            sections.put(cs, section);
        }

        if (cmd.equals(MutexProtocol.REQUEST)) {
            section.waiting.add(client);
            System.out.println("[coordinator] REQUEST cs=" + cs + " (in attesa: " + section.waiting.size() + ")");
        } else if (cmd.equals(MutexProtocol.RELEASE)) {
            section.holder = null;
            System.out.println("[coordinator] RELEASE cs=" + cs);
        }

        if (section.holder == null && !section.waiting.isEmpty()) {
            String next = section.waiting.remove();
            section.holder = next;
            String grantMessage = MutexProtocol.GRANT + " " + cs;
            channel.basicPublish(MutexProtocol.DEFAULT_EXCHANGE, next, null, grantMessage.getBytes("UTF-8"));
            System.out.println("[coordinator] GRANT cs=" + cs);
        }
    }
}
