package monitoring.lifeservice;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Liveness testing TCP server
 * <p/>
 * User: Petr Holub (hopet@ics.muni.cz)
 * Date: 23.10.2007
 * Time: 16:46:01
 */
public class LifeServerTCP {

    final static int readBufferSize = 65636; // buffer size in bytes
    final static int socketTimeout = 500;    // in milliseconds

    final AtomicBoolean terminate;
    Thread serverThread;
    ThreadPoolExecutor threadPoolExecutor;

    public LifeServerTCP() {
        terminate = new AtomicBoolean(false);
        threadPoolExecutor = new ThreadPoolExecutor(2, 20, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
    }

    public void start() {
        serverThread = new Thread() {
            public void run() {
                try {
                    // TODO: does this cover all the local interfaces?
                    ServerSocket serverSocket = new ServerSocket(LifeSetup.LIFE_PORT_NUMBER);
                    serverSocket.setSoTimeout(socketTimeout);
                    while (!terminate.get()) {
                        final Socket clientSocket;
                        try {
                            clientSocket = serverSocket.accept();
                            threadPoolExecutor.execute(new Runnable() {
                                public void run() {
                                    byte[] readBuffer = new byte[readBufferSize];
                                    int readBytes;

                                    DataInputStream input;
                                    try {
                                        input = new DataInputStream(clientSocket.getInputStream());
                                        PrintStream output = new PrintStream(clientSocket.getOutputStream());
                                        while ((readBytes = input.read(readBuffer, 0, readBuffer.length)) != -1) {
                                            output.write(readBuffer, 0, readBytes);
                                        }
                                        input.close();
                                        output.close();
                                        clientSocket.close();
                                    } catch (IOException e) {
                                    }
                                }
                            });
                        }
                        catch (SocketTimeoutException e) {
                        }
                    }
                    serverSocket.close();
                } catch (IOException e) {
                }
            }
        };
        serverThread.start();
    }

    public void stop() {
        terminate.set(true);
        if (serverThread != null) {
            try {
                serverThread.join();
            } catch (InterruptedException e) {
            }
        }
        threadPoolExecutor.shutdown();
    }
}
