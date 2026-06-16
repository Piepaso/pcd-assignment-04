package tictactoe;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class ActiveController extends Thread {
    private final BlockingQueue<Runnable> queue = new LinkedBlockingQueue<>();

    public ActiveController() {
        super("active-controller");
    }

    public void submit(Runnable task) {
        queue.add(task);
    }

    @Override
    public void run() {
        while (true) {
            try {
                Runnable task = queue.take();
                task.run();
            } catch (InterruptedException e) {
                break;
            } catch (RuntimeException e) {
                e.printStackTrace();
            }
        }
    }
}
