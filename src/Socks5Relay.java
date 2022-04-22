import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
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
//            socket = getSocket(addr, port);
//            Socks5Pipe c2s = new Socks5Pipe(client, socket, "c2s");
//            Socks5Pipe s2c = new Socks5Pipe(socket, client, "s2c");
//
//            c2s.relay();
//            s2c.relay();
            byPass(client, addr, port);
//                SSLHttpsTest.testSocketSSL(client);
        } catch (IOException e) {
            System.err.println("relay1: " + e.getMessage());
        }
    }

    /**
    * @Description: 分流
    * @param: 区分http和https流量
    * @return:
    */
    private void byPass(Socket client, String addr, int port) throws IOException{
        if(port == 443){
            SSLSocket sslSocket = getSSLSocket(addr, port);
            SSLSocks5Pipe c2s = new SSLSocks5Pipe(client, sslSocket, "c2sSSL", 1);
            SSLSocks5Pipe s2c = new SSLSocks5Pipe(client, sslSocket, "c2sSSL", 2);
            c2s.relay();
            s2c.relay();
        }else {
            Socket socket = new Socket(addr, port);
            Socks5Pipe c2s = new Socks5Pipe(client, socket, "c2s");
            Socks5Pipe s2c = new Socks5Pipe(socket, client, "s2c");

            c2s.relay();
            s2c.relay();
        }
    }

    private SSLSocket getSSLSocket(String addr, int port) throws IOException {
                SSLSocketFactory sslSocketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
                SSLSocket sslSocket = (SSLSocket) sslSocketFactory.createSocket(addr, port);
                sslSocket.setEnabledCipherSuites(sslSocket.getSupportedCipherSuites());
                return sslSocket;
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
                byte[] recv = new byte[8192];
                int len = 0;
                while((len = is.read(recv)) > 0){
                    os.write(recv, 0, len);
                }
//                close();
            } catch (IOException e) {
                close();
                System.err.println("relay2: " + e.getMessage());
            }
        }

        private void close(){
                try {
                    if(!src.isClosed())
                        src.close();
                    if(!target.isClosed()){
                        target.close();
                    }
                } catch (IOException e) {
                    System.err.println("relay :close socket error" + e.getMessage());
                }
        }
    }
}
