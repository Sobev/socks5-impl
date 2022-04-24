import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
 * @description:
 * @author: luojx
 * @create 2022-04-22  14:15
 *
 */
public class SSLSocks5Pipe implements Runnable{
    private String id;
    private Socket socket;
    private SSLSocket sslSocket;
    // flag == 1 则socket作为数据输入端
    // flag == 2 则sslSocket作为数据输入端
    private int flag;

    public SSLSocks5Pipe(Socket socket, SSLSocket sslSocket, String id, int flag) {
        this.socket = socket;
        this.sslSocket = sslSocket;
        this.id = id;
        this.flag = flag;
    }

    public void relay(){
        Thread thread = new Thread(this);
        if(flag == FlowDirection.CLIENT2SERVER)
            thread.setName("Thread-" + socket.getInetAddress() + "-> " + sslSocket.getInetAddress());
        else
            thread.setName("Thread-" + sslSocket.getInetAddress() + "-> " + socket.getInetAddress());
        System.out.println(thread.getName() + " started");
        thread.start();
    }

    @Override
    public void run() {
        try {
            InputStream is = null;
            OutputStream os = null;
            if(flag == 1){
                is = socket.getInputStream();
                os = sslSocket.getOutputStream();
            }else{
                is = sslSocket.getInputStream();
                os = socket.getOutputStream();
            }
            byte[] recv = new byte[8192];
            int len = 0;
            while((len = is.read(recv)) > 0){
                String data = new String(recv, 0, len);
                System.out.println(this.id + "\n" + data);
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
            if(!socket.isClosed())
                socket.close();
            if(!sslSocket.isClosed()){
                sslSocket.close();
            }
        } catch (IOException e) {
            System.err.println("relay :close socket error" + e.getMessage());
        }
    }
}
