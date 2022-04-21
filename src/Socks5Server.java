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
        Socks5Acceptor acceptor = new Socks5Acceptor(25866, queue);
        Socks5Processor processor = new Socks5Processor(queue);

        new Thread(acceptor).start();
        new Thread(processor).start();

        Thread.currentThread().join();
    }
}
