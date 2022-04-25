package http;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.regex.Pattern;

/**
 * @description:
 * @author: luojx
 * @create 2022-04-25  14:35
 */
public class HttpRelay implements Runnable{

    public static void main(String[] args) {
        HttpRelay httpRelay = new HttpRelay();
        httpRelay.run();
    }

    @Override
    public void run() {
        try {
            ServerSocket serverSocket = new ServerSocket(25867);
            while (true) {
                Socket client = serverSocket.accept();
                (new HttpRelay.HttpHandler(client)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static class HttpHandler extends Thread{

        public static final Pattern HOST_PATTERN = Pattern.compile("Host: (.+):?([0-9]+)?");

        private Socket clientSocket;

        public HttpHandler(Socket client){
            System.out.println("Initialize HttpHandler ...");
            this.clientSocket = client;
        }

        @Override
        public void run() {
            Socket forwardSocket;
            try {
                InputStream inputStream = clientSocket.getInputStream();
                byte[] buffer = new byte[1024];
                int len = inputStream.read(buffer);
                int hostLen = buffer[0];
                String host = new String(buffer, 1, hostLen);
                byte portByte1 = buffer[1 + buffer[0]];
                byte portByte2 = buffer[2 + buffer[0]];

                int port = (portByte1 << 8 & 0xff) | (portByte2 & 0xff);
                System.out.println(host + ":" + port);

                forwardSocket = new Socket(host, port);
                try {
                    Thread thread = new Thread(() -> {
                        forwardData(clientSocket, forwardSocket);
                    });
                    thread.start();
                    Thread thread1 = new Thread(() -> {
                        forwardData(forwardSocket, clientSocket);
                    });
                    thread1.start();
                    try {
                        thread.join();
                        thread1.join();
                    } finally {
                        forwardSocket.close();
                    }
                }finally {
                    clientSocket.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void forwardData(Socket inputSocket, Socket outputSocket) {
            try {
                InputStream inputStream = inputSocket.getInputStream();
                try {
                    OutputStream outputStream = outputSocket.getOutputStream();
                    try {
                        byte[] buffer = new byte[4096];
                        int read;
                        do {
                            read = inputStream.read(buffer);
                            if (read > 0) {
                                outputStream.write(buffer, 0, read);
                                if (inputStream.available() < 1) {
                                    outputStream.flush();
                                }
                            }
                        } while (read >= 0);
                    } finally {
                    }
                } finally {
                }
            } catch (IOException e) {
                e.printStackTrace();  // TODO: implement catch
            }
        }
    }
}
