import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * @description:
 * @author: sobev
 * @create 2022-04-19  08:58
 */
public class Socks5Server {
    public static void main(String[] args) throws InterruptedException {
        BlockingQueue<Socket> queue = new LinkedBlockingDeque<>();
        SocketAcceptor acceptor = new SocketAcceptor(25866, queue);
        Socks5Dispatcher processor = new Socks5Dispatcher(queue);

        new Thread(acceptor).start();
        new Thread(processor).start();

        Thread.currentThread().join();
    }
}
