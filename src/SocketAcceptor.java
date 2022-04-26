import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;

/**
 * @description:
 * @author: sobev
 * @create 2022-04-18  15:58
 * @des 接收socket连接
 */
public class SocketAcceptor implements Runnable{
    private int port;
    private BlockingQueue<Socket> queue;

    public SocketAcceptor(int port, BlockingQueue<Socket> queue) {
        this.port = port;
        this.queue = queue;
    }


    @Override
    public void run() {
        try {
            ServerSocket serverSocket = new ServerSocket(port);
            while (true){
                Socket socket = serverSocket.accept();
                queue.put(socket);
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

    }
}
