package monitoring.lifeservice;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketTimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Liveness testing UDP server
 * <p/>
 * User: Petr Holub (hopet@ics.muni.cz)
 * Date: 24.10.2007
 * Time: 9:02:39
 */
public class LifeServerUDP {

    final static int readBufferSize = 65636; // buffer size in bytes
    final static int socketTimeout = 500;
    final AtomicBoolean terminate;
    Thread serverThread;

    public LifeServerUDP() {
        terminate = new AtomicBoolean(false);
    }

    public void start() {
        serverThread = new Thread() {
            public void run() {
                try {
                    byte[] readBuffer = new byte[readBufferSize];
                    DatagramPacket packet = new DatagramPacket(readBuffer, 0, readBuffer.length);

                    // TODO: does this cover all the local interfaces?
                    DatagramSocket serverSocket = new DatagramSocket(LifeSetup.LIFE_PORT_NUMBER);
                    serverSocket.setSoTimeout(socketTimeout);
                    while (!terminate.get()) {
                        try {
                            serverSocket.receive(packet);
                            serverSocket.send(new DatagramPacket(packet.getData(), packet.getOffset(), packet.getLength(), packet.getAddress(), packet.getPort()));
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
    }


}
