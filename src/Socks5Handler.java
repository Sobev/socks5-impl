import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.*;

/**
 * @description:
 * @author: sobev
 * @create 2022-04-18  16:07
 */
public class Socks5Handler {

    private ExecutorService pool = new ThreadPoolExecutor(4, 10, 20, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(100), new ThreadPoolExecutor.CallerRunsPolicy());

    private Socks5Relay relay = new Socks5Relay();

    public void handle(Socket socket, boolean allowNonAuth) {
        pool.execute(() -> {
            connect(socket, allowNonAuth);
        });
    }

    private void connect(Socket client, boolean allowNonAuth){
        try {
            InputStream is = client.getInputStream();
            OutputStream os = client.getOutputStream();
            /*
             *  +----+----------+----------+
             *  |VER | NMETHODS | METHODS  |
             *  +----+----------+----------+
             *  | 1  |    1     | 1 to 255 |
             *  +----+----------+----------+
             */

            byte[] recv = new byte[257];
            int len = is.read(recv);
            byte ver = recv[0];
            if(ver != 0x05){
                // 0xFF -1
                os.write(new byte[]{5, -1});
                return;
            }
            if(allowNonAuth){
                os.write(new byte[]{5, 0});
                waitingRequests(client);
                return;
            }
            if(len <= 1){
                os.write(new byte[]{5, -1});
                return;
            }
            byte nMethods = recv[1];
            for (int i = 0; i < nMethods; i++) {
                byte method = recv[i + 2];
                if(method == 0x02) {
                    os.write(new byte[]{5, 2});
                    //do authentication
                    if(doAuthentication(client)){
                        //waiting requests
                        waitingRequests(client);
                    }

                }
            }


        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }


    /**
       * VER 01
      +----+------+----------+------+----------+
      |VER | ULEN |  UNAME   | PLEN |  PASSWD  |
      +----+------+----------+------+----------+
      | 1  |  1   | 1 to 255 |  1   | 1 to 255 |
      +----+------+----------+------+----------+

      +----+--------+
      |VER | STATUS |
      +----+--------+
      | 1  |   1    |
      +----+--------+
       A STATUS field of X'00' indicates success. If the server returns a
       `failure' (STATUS value other than X'00') status, it MUST close the
       connection.
      */
    public boolean doAuthentication(Socket client){
        try {
            InputStream is = client.getInputStream();
            OutputStream os = client.getOutputStream();
            byte[] recv = new byte[513];
            int len = is.read(recv);
            if(len <= 0){
                os.write(new byte[]{1, 1});
                return false;
            }
            byte ver = recv[0];
            if(ver != 0x01){
                os.write(new byte[]{1, 1});
                return false;
            }
            byte uLen = recv[1];
            byte[] uName = new byte[uLen];
            System.arraycopy(recv, 2, uName, 0, uLen);
            String parsedUsername = new String(uName);
            byte pLen = recv[2+uLen];
            byte[] password = new byte[pLen];
            System.arraycopy(recv, 3+uLen, password, 0, pLen);
            String parsedPassword = new String(password);
            if(!parsedUsername.equals("sobev") || !parsedPassword.equals("123456")){
                os.write(new byte[]{1, 1});
                return false;
            }
            os.write(new byte[]{1, 0});
            return true;

        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
        return false;
    }

    /**
     *  Once the method-dependent subnegotiation has completed, the client
     *  sends the request details.  If the negotiated method includes
     *  encapsulation for purposes of integrity checking and/or
     *  confidentiality, these requests MUST be encapsulated in the method-
     *  dependent encapsulation.
     *
     *  The SOCKS request is formed as follows:
     *
     *   +----+-----+-------+------+----------+----------+
     *   |VER | CMD |  RSV  | ATYP | DST.ADDR | DST.PORT |
     *   +----+-----+-------+------+----------+----------+
     *   | 1  |  1  | X'00' |  1   | Variable |    2     |
     *   +----+-----+-------+------+----------+----------+
     *
     *   Where:
     *
     *     o  VER    protocol version: X'05'
     *     o  CMD
     *         o  CONNECT X'01'
     *         o  BIND X'02'
     *         o  UDP ASSOCIATE X'03'
     *     o  RSV    RESERVED
     *     o  ATYP   address type of following address
     *         o  IP V4 address: X'01'
     *         o  DOMAINNAME: X'03'
     *         o  IP V6 address: X'04'
     *     o  DST.ADDR       desired destination address
     *     o  DST.PORT desired destination port in network octet order
     *
     *    The SOCKS server will typically evaluate the request based on source
     *    and destination addresses, and return one or more reply messages, as
     *    appropriate for the request type.
     *
     *    +----+-----+-------+------+----------+----------+
     *    |VER | REP |  RSV  | ATYP | BND.ADDR | BND.PORT |
     *    +----+-----+-------+------+----------+----------+
     *    | 1  |  1  | X'00' |  1   | Variable |    2     |
     *    +----+-----+-------+------+----------+----------+
     *    X’00’ succeeded
     *    X’01’ general SOCKS server failure
     *    X’02’ connection not allowed by ruleset
     *    X’03’ Network unreachable
     *    X’04’ Host unreachable
     *    X’05’ Connection refused
     *    X’06’ TTL expired
     *    X’07’ Command not supported
     *    X’08’ Address type not supported
     *    X’09’ to X’FF’ unassigned
     */
    private void waitingRequests(Socket client) {
        try {
            InputStream is = client.getInputStream();
            OutputStream os = client.getOutputStream();
            byte[] recv = new byte[512];
            int len = is.read(recv);
            if(len <= 0){
                client.close();
                return;
            }
            byte ver = recv[0];
            if(ver != 0x05){
                os.write(new byte[]{5, 1, 0, 1, 0, 0, 0, 0, 0});
                return;
            }
            byte cmd = recv[1];
            if(cmd != 0x01){
                os.write(new byte[]{5, 1, 0, 1, 0, 0, 0, 0, 0});
                return;
            }
            byte addType = recv[3];
            byte[] addr;
            if(addType == 0x01){
                addr = new byte[4];
                System.arraycopy(recv, 4, addr, 0, addr.length);
            }else if(addType == 0x03){
                //domain 前有domain的长度
                addr = new byte[recv[4]];
                System.arraycopy(recv, 5, addr, 0, addr.length);
            }else{
                os.write(new byte[]{5, 1, 0, 1, 0, 0, 0, 0, 0});
                return;
            }
            String target = new String(addr);
            byte[] port = new byte[]{recv[5 + recv[4]], recv[6 + recv[4]]};
            int targetPort = (port[0] & 0xff) << 8 | (port[1] & 0xff);
            System.out.println("target = " + target + ":" + targetPort);
            os.write(new byte[]{5, 0, 0, addType, 0, 0, 0, 0, 0, 0,});

            relay.doRelay(client, target, targetPort);

        } catch (IOException e) {
            System.err.println("handler_1: " + e.getMessage());
        }
    }
}
