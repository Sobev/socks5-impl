import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @description:
 * @author: luojx
 * @create 2022-04-22  10:41
 */
public class SSLHttpsTest {
    public static void main(String[] args) {
        testSocketSSL();
    }

    public static void testSocketSSL(){
        try {
            SSLSocketFactory sslSocketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
            SSLSocket socket = (SSLSocket) sslSocketFactory.createSocket("httpbin.org", 443);
            String[] supportedCipherSuites = socket.getSupportedCipherSuites();
            socket.setEnabledCipherSuites(supportedCipherSuites);
            OutputStream os = socket.getOutputStream();
            InputStream is = socket.getInputStream();
            StringBuilder builder = new StringBuilder();
            builder.append("GET " + "/get" + " HTTP/1.1\r\n");
            builder.append("Host: httpbin.org:443\r\n");
            builder.append("User-Agent: Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1; .NET CLR 2.0.50727; TheWorld)\r\n");
            builder.append("Accept: text/html,application/xhtml+xml,application/xml.application/json;q=0.9,*/*;q=0.8\r\n");
            builder.append("Accept-Language: en-US;q=0.7,en;q=0.3\r\n");
            builder.append("Connection: close\r\n\r\n");
            byte[] data = builder.toString().getBytes();
            os.write(data);
            byte[] recv = new byte[1024];
            int len;
            while ((len = is.read(recv)) != -1){
                String s = new String(recv, 0, len);
                System.out.println("s = " + s);
            }
        } catch (IOException e) {
            e.printStackTrace();
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
