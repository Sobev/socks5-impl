import http.HttpParser;

import java.io.*;
import java.net.ConnectException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @description:
 * @author: sobev
 * @create 2022-04-20  14:34
 * TODO: []加密请求
 * TODO: []支持https
 */
public class SocketClient {

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
            while (true) {
                Socket socket = serverSocket.accept();
                new ClientSocketHandler(socket, this);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static class ClientSocketHandler implements Runnable {

        public static final Pattern CONNECT_PATTERN = Pattern.compile("CONNECT (.+):(.+) HTTP/(1\\.[01])",
                Pattern.CASE_INSENSITIVE);

        private Socket socket;

        private SocketClient client;

        private boolean abortHttps = false;

        private boolean isSecuredConnection = false;

        public ClientSocketHandler(Socket socket, SocketClient client) {
            this.socket = socket;
            this.client = client;
//            this.abortHttps = true;
            new Thread(this).start();
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
                if (getAuthMethod(sis, sos) == 0x02) {
                    boolean authRes = doAuthentication(sis, sos);
                    if (!authRes)
                        return;
                }
                byte[] buf = new byte[2048];
                int len;
                len = cis.read(buf);
                //find host
                String httpreq = new String(buf, 0, len);
                Matcher matcher = CONNECT_PATTERN.matcher(httpreq);
                if(matcher.matches()){
                    String header;
                    do {
                        header = readLine(socket);
                    } while (!"".equals(header));
                }
                HttpParser parse = HttpParser.parse(httpreq);
                String addr = parse.getAddrPort()[0];
                int port = Integer.parseInt(parse.getAddrPort()[1]);
//                String addr = matcher.group(1);
//                int port = Integer.parseInt(matcher.group(2));
                this.isSecuredConnection = (port == 443);

                if(isSecuredConnection && abortHttps){
                    System.err.println("abort -> " + addr + ":" + port);
                    return;
                }
                System.out.println("accept -> " + addr + ":" + port);
//                OutputStreamWriter outputStreamWriter = new OutputStreamWriter(socket.getOutputStream(),
//                        StandardCharsets.ISO_8859_1);
//                if(matcher.matches()){
//                    outputStreamWriter.write("HTTP/" + matcher.group(3) + " 200 Connection established\r\n");
//                    outputStreamWriter.write("Proxy-agent: Simple/0.1\r\n");
//                    outputStreamWriter.write("\r\n");
//                    outputStreamWriter.flush();
//                }
                //forward request or confirm https tunnel
                byte[] res = forwardAddress(sis, sos, addr, port);
                if (res[1] != 0x00) {
                    System.out.println("地址解析失败 ... 支持IPV4, DOMAIN");
                    return;
                }
                //如果为Connect那么已经返回连接成功 并重新获取请求头
                byte[] headers;
                if (parse.getMethod().equals("CONNECT")) {
                    headers = getHttpsEncryptedData(sis, sos, cis, cos, parse);
                } else {
                    headers = parse.generateRequestHeader().getBytes();
                }
                sos.write(headers);

                byte[] resp = new byte[8192];

                len = 0;
                while ((len = sis.read(resp)) != -1) {
                    cos.write(resp, 0, len);
//                    String responseText = new String(resp, 0, len);
//                    System.out.println(responseText);
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

        private byte getAuthMethod(InputStream sis, OutputStream sos) {
            byte[] clientMethod = new byte[]{5, 1, 2};
            byte[] serverMethod = new byte[2];
            try {
                sos.write(clientMethod);
                int len = sis.read(serverMethod);
                if (len <= 0) {
                    throw new ConnectException("No Data Recv");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return serverMethod[1];
        }

        private boolean doAuthentication(InputStream sis, OutputStream sos) {
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
                if (len <= 1) {
                    System.out.println("Auth Failed Check Parameters");
                    return false;
                }
                if (authRes[1] != 0x00) {
                    System.out.println("Username Or Password Error");
                    return false;
                }
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
            return true;
        }

        private byte[] forwardAddress(InputStream sis, OutputStream sos, String addr, Integer port) {
            byte[] res = new byte[10];
            int len;
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
            try {
                sos.write(data);
                len = sis.read(res);
                if (len <= 0)
                    throw new ConnectException("REP 0 received failed");
            } catch (
                    IOException e) {
                e.printStackTrace();
            }
            return res;
        }

        private byte[] getHttpsEncryptedData(InputStream sis, OutputStream sos, InputStream cis, OutputStream cos, HttpParser parse) {
            StringBuilder builder = new StringBuilder();
            try {
                cos.write((parse.getHttpVersion() + " 200 Connection established\r\nProxy-agent: ProxyServer/1.0\r\n\r\n").getBytes());
                int len = 0;
                byte[] buf = new byte[2048];
                while ((len = cis.read(buf)) != -1) {
                    String s = new String(buf, 0, len);
                    builder.append(s);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return builder.toString().getBytes();
        }

        private String readLine(Socket socket) throws IOException{
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            int next;
            while((next = socket.getInputStream().read()) != -1){
                if(next == '\r' || next == '\n'){
                    break;
                }else{
                    byteArrayOutputStream.write(next);
                }
            }
            return byteArrayOutputStream.toString("ISO-8859-1");
        }

        public static void main(String[] args) {
            int port = 8080;
            byte p1 = (byte) (port >> 8 & 0xff);
            byte p2 = (byte) (port & 0xff);
            //parse byte to port
            int parsedPort = (p1 & 0xff) << 8 | (p2 & 0xff);
        }
    }
}
