package pcd.mutex;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DeliverCallback;
import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class DistributedMutex {

	private final Channel channel;
	private final String lockQueue;
	private final BlockingQueue<Long> granted = new ArrayBlockingQueue<>(1);
	private String consumerTag;
	private long heldTag;

	public DistributedMutex(Channel channel, String csName) throws IOException {
		this.channel = channel;
		this.lockQueue = "mutex." + csName;          // una coda per sezione critica
		channel.queueDeclare(lockQueue, false, false, false, null);
		channel.basicQos(1);                          // al più un token non-ack alla volta
	}

	/** Si blocca finché non ottengo il token = entro nella sezione critica. */
	public void acquire() throws IOException, InterruptedException {
		DeliverCallback onToken = (tag, delivery) ->
				granted.add(delivery.getEnvelope().getDeliveryTag());
		consumerTag = channel.basicConsume(lockQueue, false, onToken, t -> {});
		heldTag = granted.take();          // attesa BLOCCANTE del token
		channel.basicCancel(consumerTag);  // il token è mio: smetto di consumare
	}

	/** Rilascio: restituisco il token agli altri processi. */
	public void release() throws IOException {
		channel.basicAck(heldTag, false);                          // tolgo "il mio" token
		channel.basicPublish("", lockQueue, null, "token".getBytes()); // lo rimetto in coda
	}
}