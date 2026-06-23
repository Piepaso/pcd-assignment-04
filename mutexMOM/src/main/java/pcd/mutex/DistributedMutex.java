package pcd.mutex;

import com.rabbitmq.client.*;

import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class DistributedMutex {

    private final Channel channel;
    private final String csName;
    private final String replyQueue;                       // coda privata: qui arrivano i GRANT
    private final BlockingQueue<String> grants = new ArrayBlockingQueue<>(1);
    private volatile boolean held = false;

    public DistributedMutex(Connection connection, String csName) throws IOException {
        this.channel = connection.createChannel();
        this.csName = csName;

        // Coda privata, generata dal server, esclusiva e auto-delete (come Test2).
        // E' l'unico "indirizzo" con cui mi presento al coordinatore.
        this.replyQueue = channel.queueDeclare().getQueue();

        // Consumer sulla coda privata: ogni GRANT sblocca un acquire() in attesa.
        DeliverCallback onGrant = (consumerTag, delivery) -> {
            String body = new String(delivery.getBody(), "UTF-8");   // "GRANT <cs>"
            grants.offer(body);
        };
        channel.basicConsume(replyQueue, true, onGrant, tag -> { });
    }

    public void acquire() throws IOException, InterruptedException {
        String body = MutexProtocol.CMD_REQUEST + " " + csName;
        AMQP.BasicProperties props = new AMQP.BasicProperties.Builder()
                .replyTo(replyQueue)          // dico al coordinatore dove rispondermi
                .build();
        channel.basicPublish("", MutexProtocol.COORDINATOR_QUEUE, props, body.getBytes("UTF-8"));

        grants.take();                        // attesa BLOCCANTE del GRANT
        held = true;
    }

    public void release() throws IOException {
        if (!held) {
            return;
        }
        String body = MutexProtocol.CMD_RELEASE + " " + csName;
        AMQP.BasicProperties props = new AMQP.BasicProperties.Builder()
                .replyTo(replyQueue)
                .build();
        channel.basicPublish("", MutexProtocol.COORDINATOR_QUEUE, props, body.getBytes("UTF-8"));
        held = false;
    }
}
