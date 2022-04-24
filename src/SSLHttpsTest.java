import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
 * @description:
 * @author: luojx
 * @create 2022-04-22  10:41
 */
public class SSLHttpsTest {
    public static void main(String[] args) {
        try {
            testSocketSSL(null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void httpsSocketByte(byte[] data) throws IOException {
        SSLSocketFactory sslSocketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
        SSLSocket socket = (SSLSocket) sslSocketFactory.createSocket("www.baidu.com", 443);
        socket.setEnabledCipherSuites(socket.getSupportedCipherSuites());
        OutputStream os = socket.getOutputStream();
        InputStream is = socket.getInputStream();
        System.out.println(new String(data));
        os.write(data);
        byte[] recv = new byte[1024];
        int len;
        while ((len = is.read(recv)) != -1){
            String s = new String(recv, 0, len);
            System.out.println("httpsSocketByte = " + s);
        }
    }

    public static void testSocketSSL(Socket client) throws IOException {
            SSLSocketFactory sslSocketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
            SSLSocket socket = (SSLSocket) sslSocketFactory.createSocket("www.baidu.com", 443);
            String[] supportedCipherSuites = socket.getSupportedCipherSuites();
            socket.setEnabledCipherSuites(supportedCipherSuites);
            OutputStream os = socket.getOutputStream();
            InputStream is = socket.getInputStream();
            StringBuilder builder = new StringBuilder();
            builder.append("GET " + "/" + " HTTP/1.1\r\n");
            builder.append("Host: www.baidu.com:443\r\n");
            builder.append("User-Agent: Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/100.0.4896.127 Safari/537.36\r\n");
            builder.append("Accept: */*\r\n");
            builder.append("Accept-Language: en-US;q=0.7,en;q=0.3\r\n");
            builder.append("Connection: close\r\n\r\n");
            byte[] data = builder.toString().getBytes();
//            OutputStream cos = client.getOutputStream();
            os.write(data);
            byte[] recv = new byte[1024];
            int len;
            while ((len = is.read(recv)) != -1){
                String s = new String(recv, 0, len);
                System.out.println("s = " + s);
//                cos.write(recv, 0, len);
            }
    }

    public static void testServerSocket(){
        SSLServerSocketFactory factory = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
        SSLServerSocket serverSocket = null;
        try {
            serverSocket = (SSLServerSocket) factory.createServerSocket(8080);
            String[] suites = serverSocket.getSupportedCipherSuites();
            for (int i = 0; i < suites.length; i++) {
                System.out.println(suites[i]);
            }
            System.out.println();
            serverSocket.setEnabledCipherSuites(suites);
            String[] protocols = serverSocket.getSupportedProtocols();
            for (int i = 0; i < protocols.length; i++) {
                System.out.println(protocols[i]);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}