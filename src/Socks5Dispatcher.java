import java.net.Socket;
import java.util.concurrent.BlockingQueue;

/**
 * @description:
 * @author: sobev
 * @create 2022-04-18  16:03
 */
public class Socks5Dispatcher implements Runnable{

    private BlockingQueue<Socket> queue;

    private Socks5Handler handler;

    public Socks5Dispatcher(BlockingQueue<Socket> queue) {
        this.queue = queue;
        handler = new Socks5Handler();
    }

    @Override
    public void run() {
        while(true){
            try {
                Socket socket = queue.take();
                handler.handle(socket, false);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
