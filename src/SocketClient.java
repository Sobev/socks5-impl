import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @description:
 * @author: sobev
 * @create 2022-04-20  14:34
 */
public class SocketClient{

    private String serverAddr;

    private int port;

    private String pwd;

    public SocketClient(String serverAddr, int port, String pwd) {
        this.serverAddr = serverAddr;
        this.port = port;
        this.pwd = pwd;
    }

    public static void main(String[] args) throws InterruptedException {
        SocketClient client = new SocketClient("127.0.0.1", 25866, "123456");
        client.accept();
    }

    public void accept() {
        try {
            ServerSocket serverSocket = new ServerSocket(25800);
            while (true){
                Socket socket = serverSocket.accept();
                new ClientSocketHandler(socket, this);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static class ClientSocketHandler implements Runnable{

        private Socket socket;

        private SocketClient client;

        public ClientSocketHandler(Socket socket, SocketClient client) {
            this.socket = socket;
            this.client = client;
            new Thread(this).start();
        }

        public static void main(String[] args) {
            int port = 8080;
            byte p1 = (byte) (port >> 8 & 0xff);
            byte p2 = (byte) (port & 0xff);
            System.out.println("p1 = " + p1);
            System.out.println("p2 = " + p2);

            //parse byte to port
            int parsedPort = (p1 & 0xff) << 8 | (p2 & 0xff);
            System.out.println("parsedPort = " + parsedPort);
        }

        @Override
        public void run() {
            InputStream cis = null;
            Socket ss = null;
            try {
                cis = socket.getInputStream();
                ss = new Socket(client.serverAddr, client.port);
                InputStream sis = ss.getInputStream();
                OutputStream sos = ss.getOutputStream();
                //step 1
                byte[] step1 = new byte[]{5, 1, 0};
                sos.write(step1);
                byte[] resp1 = new byte[2];
                int len = sis.read(resp1);
                if(len <= 0){
                    System.out.println("No Data Recv");
                    return;
                }
                if(resp1[1] == 0x02){
                    System.out.println("Stay tuned for pwd authentication");
                    return;
                }
                //no pwd
                byte[] cbuf = new byte[1024];
                len = cis.read(cbuf);
                //find host
                String httpreq = new String(cbuf, 0, len);
                String[] split = httpreq.split("\\n");
                List<String> host_ = Arrays.stream(split).filter((item) -> item.startsWith("Host:")).collect(Collectors.toList());
                if(host_ ==null || host_.size() == 0){
                    System.out.println("No Host Found");
                    return;
                }
                String host = host_.get(0).split(" ")[1];
                System.out.println("HOST: " + host);
                String[] path = host.split(":");
                String addr = path[0];
                int port;
                if(path.length <= 1)
                    port = 80;
                else
                    port = Integer.parseInt(path[1].trim());
                System.out.println("port = " + port);
                //send data
                byte[] data = new byte[512];
                data[0] = 0x05;
                data[1] = 0x01;
                data[2] = 0x00;
                data[3] = 0x03;
                data[4] = (byte) addr.getBytes().length;
                System.arraycopy(addr.getBytes(), 0, data, 5, addr.getBytes().length);
                //parse port into byte
                byte p1 = (byte) (port >> 8 & 0xff);
                byte p2 = (byte) (port & 0xff);
                data[5 + data[4]] = p1;
                data[5 + data[4] + 1] = p2;
                for (int i = 0; i < data.length; i++) {
                    System.out.print(data[i] + " ");
                }
                sos.write(data);

                byte[] res = new byte[10];
                len = sis.read(res);

                if(len <= 0)
                    return;
                if(res[1] != 0x00){
                    System.out.println("地址解析失败 支持IPV4, DOMAIN, IPV6");
                    return;
                }

                //send http request
                sos.write(cbuf);

                byte[] httpres = new byte[1024];

                len = 0;
                while((len = sis.read(httpres)) != 0){
                    String responseText = new String(httpres, 0, len);
                    System.out.println(responseText);
                }




            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    ss.close();
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    socket = null;
                }
            }
        }
    }
}
