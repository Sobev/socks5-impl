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
            Socks5Pipe c2s = new Socks5Pipe(client, socket, "c2s");
            Socks5Pipe s2c = new Socks5Pipe(socket, client, "s2c");

            c2s.relay();
            s2c.relay();
        } catch (IOException e) {
            System.err.println("relay1: " + e.getMessage());
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
            thread.setName(src.getInetAddress() + "-> " + target.getInetAddress());
            System.out.println(thread.getName());
            thread.start();
        }

        @Override
        public void run() {
            try {
                InputStream is = src.getInputStream();
                OutputStream os = target.getOutputStream();
                byte[] recv = new byte[8192];
                int len = 0;
                while((len = is.read(recv)) > 0){
//                    String s = new String(recv, 0, len);
//                    System.out.println("s = " + s);
                    os.write(recv, 0, len);
                }
                close();
            } catch (IOException e) {
                close();
                System.err.println("relay2: " + e.getMessage());
            }
        }

        private void close(){
                try {
                    if(!src.isInputShutdown())
                        src.shutdownInput();
                    if(!target.isOutputShutdown()){
                        target.shutdownOutput();
                    }
                } catch (IOException e) {
                    System.err.println("relay :close socket error" + e.getMessage());
                }
        }
    }
}
