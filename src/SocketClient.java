import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * @description:
 * @author: sobev
 * @create 2022-04-20  14:34
 * TODO: []加密请求
 * TODO: []支持https
 */
public class SocketClient{

    //SOCKS5 server
    private String serverAddr;

    //SOCKS5 server port
    private int port;

    //authentication
    private String username;

    private String pwd;

    public SocketClient(String serverAddr, int port, String username, String pwd) {
        this.serverAddr = serverAddr;
        this.port = port;
        this.username = username;
        this.pwd = pwd;
    }

    public static void main(String[] args) throws InterruptedException {
        SocketClient client = new SocketClient("127.0.0.1", 25866, "sobev", "123456");
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
            //parse byte to port
            int parsedPort = (p1 & 0xff) << 8 | (p2 & 0xff);
        }

        @Override
        public void run() {
            InputStream cis = null;
            OutputStream cos = null;
            Socket ss = null;
            try {
                cis = socket.getInputStream();
                cos = socket.getOutputStream();
                ss = new Socket(client.serverAddr, client.port);
                InputStream sis = ss.getInputStream();
                OutputStream sos = ss.getOutputStream();
                //step 1
                byte[] clienttMethod = new byte[]{5, 1, 2};
                sos.write(clienttMethod);
                byte[] serverMethod = new byte[2];
                int len = sis.read(serverMethod);
                if(len <= 0){
                    System.out.println("No Data Recv");
                    return;
                }
                if(serverMethod[1] == 0x02){
                    //System.out.println("Stay tuned for pwd authentication");
                    boolean authRes = doAuthentication(sis, sos);
                    if(!authRes)
                        return;
                }
                byte[] cbuf = new byte[2048];
                len = cis.read(cbuf);
                if(len <= 1){
                    System.out.println("request format error");
                    return;
                }
                //find host
                String httpreq = new String(cbuf, 0, len);
                HttpParser parse = HttpParser.parse(httpreq);
                String[] addr_port = parse.getAddrPort();
                if(addr_port == null){
                    System.out.println("parsed host error");
                    return;
                }
                String addr = addr_port[0];
                int port = Integer.parseInt(addr_port[1]);

                System.out.println("accept -> " + addr + ":" + port);

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
                sos.write(data);

                byte[] res = new byte[10];
                len = sis.read(res);

                if(len <= 0)
                    return;
                if(res[1] != 0x00){
                    System.out.println("地址解析失败 \n 支持IPV4, DOMAIN");
                    return;
                }

                //send http request
                //return 404? try build my own http request :) test success
                String headers = parse.generateRequestHeader().toString();
//                System.out.println("headers = " + headers);
                byte[] buildreq = headers.getBytes();
                sos.write(buildreq);

                byte[] httpres = new byte[8192];

                len = 0;
                while((len = sis.read(httpres)) != -1){
                    cos.write(httpres, 0, len);
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

        private boolean doAuthentication(InputStream sis, OutputStream sos){
            int uLen = client.username.getBytes().length;
            int pLen = client.pwd.getBytes().length;
            byte[] auth = new byte[3 + uLen + pLen];
            auth[0] = 0x01;
            auth[1] = (byte) uLen;
            System.arraycopy(client.username.getBytes(), 0, auth, 2, uLen);
            auth[2 + uLen] = (byte) pLen;
            System.arraycopy(client.pwd.getBytes(), 0, auth, 3 + uLen, pLen);
            try {
                sos.write(auth);
                byte[] authRes = new byte[2];
                int len = sis.read(authRes);
                if(len <= 1){
                    System.out.println("Auth Failed Check Parameters");
                    return false;
                }
                if(authRes[1] != 0x00){
                    System.out.println("Username Or Password Error");
                    return false;
                }
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
            return true;
        }
    }
}
