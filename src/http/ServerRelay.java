package http;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * @description:
 * @author: luojx
 * @create 2022-04-25  10:01
 */
public class ServerRelay implements Runnable {


    public static void main(String[] args) {
        ServerRelay relay = new ServerRelay();
        relay.run();
    }

    @Override
    public void run() {
        try {
            ServerSocket serverSocket = new ServerSocket(25866);
            while (true) {
                Socket client = serverSocket.accept();
                (new Handler(client)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static class Handler extends Thread {

        private Socket clientSocket;

        public Handler(Socket clientSocket) {
            System.out.println("Initialize Handler ...");
            this.clientSocket = clientSocket;
        }

        @Override
        public void run() {
            String[] target = getTarget(clientSocket);
            System.out.println(target[0] + " " + target[1]);
            Socket forwardSocket;
            try {
                forwardSocket = new Socket(target[0], Integer.parseInt(target[1]));
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
                    clientSocket.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private String[] getTarget(Socket client) {
            String addr;
            String port;
            try {
                InputStream inputStream = client.getInputStream();
                try {
                    byte[] buffer = new byte[1024];
                    int len = inputStream.read(buffer);
                    if (len != -1) {
                        int addrLen = buffer[0];
                        addr = new String(buffer, 1, addrLen);
                        byte[] portByte = new byte[]{buffer[1 + addrLen], buffer[2 + addrLen]};
                        int p = (portByte[0] << 8) | (portByte[1] & 0xff);
                        port = String.valueOf(p);
                        return new String[]{addr, port};
                    }
                } finally {
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
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
                        if (!outputSocket.isOutputShutdown()) {
                            outputSocket.shutdownOutput();
                        }
                    }
                } finally {
                    if (!inputSocket.isInputShutdown()) {
                        inputSocket.shutdownInput();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();  // TODO: implement catch
            }
        }
    }
}
