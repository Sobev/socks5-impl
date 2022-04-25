package http;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created for http://stackoverflow.com/q/16351413/1266906.
 * readLine对于http有问题
 */
public class HttpClient extends Thread {

    public static void main(String[] args) {
        (new HttpClient()).run();
    }

    public HttpClient() {
        super("Server Thread");
    }

    @Override
    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(25800)) {
            Socket socket;
            try {
                while ((socket = serverSocket.accept()) != null) {
                    (new Handler(socket)).start();
                }
            } catch (IOException e) {
                e.printStackTrace();  // TODO: implement catch
            }
        } catch (IOException e) {
            e.printStackTrace();  // TODO: implement catch
            return;
        }
    }

    public static class Handler extends Thread {
        public static final Pattern CONNECT_PATTERN = Pattern.compile("CONNECT (.+):(.+) HTTP/(1\\.[01])",
                Pattern.CASE_INSENSITIVE);
        public static final Pattern HOST_PATTERN = Pattern.compile("Host: (.+):?([0-9]+)?");
        private final Socket clientSocket;
        private boolean previousWasR = false;

        public Handler(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        @Override
        public void run() {
            try {
                String request = readLine(clientSocket);
                Matcher matcher = CONNECT_PATTERN.matcher(request);
                if (matcher.matches()) {
                    System.out.println(request);
                    String header;
                    do {
                        header = readLine(clientSocket);
                    } while (!"".equals(header));
                    OutputStreamWriter outputStreamWriter = new OutputStreamWriter(clientSocket.getOutputStream(),
                            "ISO-8859-1");

                    final Socket forwardSocket;
                    try {
                        forwardSocket = new Socket("127.0.0.1", 25866);
                        OutputStream outputStream = forwardSocket.getOutputStream();
                        String host = matcher.group(1);
                        int port = Integer.parseInt(matcher.group(2));
                        byte[] data = new byte[host.length() + 3];
                        data[0] = (byte) host.length();
                        System.arraycopy(host.getBytes(), 0, data, 1, host.length());
                        data[host.length() + 1] = (byte) (port >> 8 & 0xff);
                        data[host.length() + 2] = (byte) (port & 0xff);
                        outputStream.write(data);
                        System.out.println(forwardSocket);
                    } catch (IOException | NumberFormatException e) {
                        e.printStackTrace();  // TODO: implement catch
                        outputStreamWriter.write("HTTP/" + matcher.group(3) + " 502 Bad Gateway\r\n");
                        outputStreamWriter.write("Proxy-agent: Simple/0.1\r\n");
                        outputStreamWriter.write("\r\n");
                        outputStreamWriter.flush();
                        return;
                    }
                    try {
                        outputStreamWriter.write("HTTP/" + matcher.group(3) + " 200 Connection established\r\n");
                        outputStreamWriter.write("Proxy-agent: Simple/0.1\r\n");
                        outputStreamWriter.write("\r\n");
                        outputStreamWriter.flush();


                        Thread remoteToClient = new Thread() {
                            @Override
                            public void run() {
                                forwardData(forwardSocket, clientSocket);
                            }
                        };
                        remoteToClient.start();
                        try {
                            if (previousWasR) {
                                int read = clientSocket.getInputStream().read();
                                if (read != -1) {
                                    if (read != '\n') {
                                        forwardSocket.getOutputStream().write(read);
                                    }
                                    forwardData(clientSocket, forwardSocket);
                                } else {
                                    if (!forwardSocket.isOutputShutdown()) {
                                        forwardSocket.shutdownOutput();
                                    }
                                    if (!clientSocket.isInputShutdown()) {
                                        clientSocket.shutdownInput();
                                    }
                                }
                            } else {
                                forwardData(clientSocket, forwardSocket);
                            }
                        } finally {
                            try {
                                remoteToClient.join();
                            } catch (InterruptedException e) {
                                e.printStackTrace();  // TODO: implement catch
                            }
                        }
                    } finally {
                        forwardSocket.close();
                    }
                } else {
                    try {
                        System.out.println(request);
                        InputStream inputStream = clientSocket.getInputStream();
                        byte[] buffer = new byte[1024];
                        int len;
                        StringBuilder rawHeader = new StringBuilder();
                        request += "\r";
                        rawHeader.append(request);
                        while ((len = inputStream.read(buffer)) != -1) {
                            rawHeader.append(new String(buffer, 0, len));
                        }
                        HttpParser parseRes = HttpParser.parse(rawHeader.toString());
                        String[] addrPort = parseRes.getAddrPort();
                        buffer = new byte[1024];
                        buffer[0] = (byte) addrPort[0].getBytes().length;
                        System.arraycopy(addrPort[0].getBytes(), 0, buffer, 1, buffer[0]);
                        int port = Integer.parseInt(addrPort[1]);
                        byte portByte1 = (byte) (port >> 8 & 0xff);
                        byte portByte2 = (byte) (port & 0xff);
                        buffer[1 + buffer[0]] = portByte1;
                        buffer[2 + buffer[0]] = portByte2;
                        final Socket forwardSocket;
                        try {
                            forwardSocket = new Socket("127.0.0.1", 25867);
                            OutputStream outputStream = forwardSocket.getOutputStream();
                            outputStream.write(buffer);
                            Thread remoteToClient = new Thread() {
                                @Override
                                public void run() {
                                    forwardData(forwardSocket, clientSocket);
                                }
                            };
                            remoteToClient.start();
                            forwardHttpData(forwardSocket, rawHeader.toString());
                            try {
                                remoteToClient.join();
                            } finally {
                                forwardSocket.close();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();  // TODO: implement catch
            } finally {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();  // TODO: implement catch
                }
            }
        }

        private void forwardHttpData(Socket outputSocket, String headers) {
            try {
                OutputStream outputStream = outputSocket.getOutputStream();
                try {
                    outputStream.write(headers.getBytes());
                } finally {
                    if (!outputSocket.isOutputShutdown()) {
                        outputSocket.shutdownOutput();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();  // TODO: implement catch
            }
        }

        private static void forwardData(Socket inputSocket, Socket outputSocket) {
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

        private String readLine(Socket socket) throws IOException {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            int next;
            readerLoop:
            while ((next = socket.getInputStream().read()) != -1) {
                if (previousWasR && next == '\n') {
                    previousWasR = false;
                    continue;
                }
                previousWasR = false;
                switch (next) {
                    case '\r':
                        previousWasR = true;
                        break readerLoop;
                    case '\n':
                        break readerLoop;
                    default:
                        byteArrayOutputStream.write(next);
                        break;
                }
            }
            return byteArrayOutputStream.toString("ISO-8859-1");
        }
    }
}
