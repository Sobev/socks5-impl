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
//            testSocketSSL(null);
            testPlainHttp();
        } catch (IOException e) {
            e.printStackTrace();
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
    public static void testPlainHttp() throws IOException {
        Socket socket = new Socket("httpbin.org", 80);
        OutputStream os = socket.getOutputStream();
        InputStream is = socket.getInputStream();
        StringBuilder builder = new StringBuilder();
        builder.append("GET http://httpbin.org/get HTTP/1.1\r\n");
        builder.append("Host: httpbin.org\r\n");
        builder.append("Connection: close\r\n");
        builder.append("Cache-Control: max-age=0\r\n");
        builder.append("Upgrade-Insecure-Requests: 1\r\n");
        builder.append("User-Agent: Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/100.0.4896.127 Safari/537.36\r\n");
        builder.append("Accept: text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9\r\n");
        builder.append("Accept-Encoding: gzip, deflate\r\n");
        builder.append("Accept-Language: zh-CN,zh;q=0.9\r\n\r\n");
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
