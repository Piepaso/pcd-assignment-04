package pcd.mutex;

import com.rabbitmq.client.*;

public class DistributedMutex {

    private final Channel channel;
    private final String csName;
    private final String replyQueue;

    private final Object semaphore = new Object();
    private boolean granted = false;

    public DistributedMutex(Connection connection, String csName) throws Exception {
        this.channel = connection.createChannel();
        this.csName = csName;

        this.replyQueue = channel.queueDeclare().getQueue();

        DeliverCallback onGrant = (consumerTag, delivery) -> {
            synchronized (semaphore) {
                granted = true;
                semaphore.notify();
            }
        };
        channel.basicConsume(replyQueue, true, onGrant, tag -> {});
    }

    public void acquire() throws Exception {
        String msg = MutexProtocol.REQUEST + " " + csName + " " + replyQueue;
        channel.basicPublish(MutexProtocol.DEFAULT_EXCHANGE, MutexProtocol.COORDINATOR_QUEUE, null, msg.getBytes("UTF-8"));

        synchronized (semaphore) {
            while (!granted) {
                semaphore.wait();
            }
            granted = false;
        }
    }

    public void release() throws Exception {
        String msg = MutexProtocol.RELEASE + " " + csName + " " + replyQueue;
        channel.basicPublish(MutexProtocol.DEFAULT_EXCHANGE, MutexProtocol.COORDINATOR_QUEUE, null, msg.getBytes("UTF-8"));
    }
}
