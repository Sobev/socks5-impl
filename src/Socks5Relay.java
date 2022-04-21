import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
 * @description:
 * @author: sobev
 * @create 2022-04-19  08:40
 */
public class Socks5Relay {

    public void doRelay(Socket client, String addr, int port){
        Socket socket = null;
        try {
            socket = new Socket(addr, port);
            socket.setSoTimeout(10*1000);

            Socks5Pipe c2s = new Socks5Pipe(client, socket, "c2s");
            Socks5Pipe s2c = new Socks5Pipe(socket, client, "s2c");

            c2s.relay();
            s2c.relay();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static class Socks5Pipe implements Runnable{
        private String id;
        private Socket src;
        private Socket target;

        public Socks5Pipe(Socket src, Socket target, String id) {
            this.src = src;
            this.target = target;
            this.id = id;
        }

        public void relay(){
            Thread thread = new Thread(this);
            thread.setName("Thread-" + src.getInetAddress() + "-> " + target.getInetAddress());
            System.out.println(thread.getName() + " started");
            thread.start();
        }

        @Override
        public void run() {
            try {
                InputStream is = src.getInputStream();
                OutputStream os = target.getOutputStream();
                byte[] recv = new byte[1024];
                int len = 0;
                while((len = is.read(recv)) > 0){
                    String s = new String(recv, 0, len);
                    System.out.println("received bytes =\n " + s);
                    os.write(recv, 0, len);
                }
                close();
            } catch (IOException e) {
                e.printStackTrace();
                close();
            }
        }

        private void close(){
                try {
                    System.out.println(id + " close");
                    if(!src.isClosed())
                        src.close();
                    if(!target.isInputShutdown())
                        target.shutdownInput();
                } catch (IOException e) {
                    System.out.println("close socket error");
                    e.printStackTrace();
                }
        }
    }
}
