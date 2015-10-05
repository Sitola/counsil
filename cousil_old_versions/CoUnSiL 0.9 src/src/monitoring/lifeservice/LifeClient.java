package monitoring.lifeservice;

import java.net.*;
import java.io.IOException;
import java.io.DataInputStream;
import java.io.PrintStream;

/**
 * Liveness testing classes
 * <p/>
 * User: Petr Holub (hopet@ics.muni.cz)
 * Date: 23.10.2007
 * Time: 16:46:07
 */
public class LifeClient {

    final static int readBufferSize = 65636; // buffer size in bytes

    public static long testAliveTCP(InetAddress address, int timeout) {
        //noinspection UnusedAssignment
        long RTT = -1;
        try {
            byte[] readBuffer = new byte[readBufferSize];
            int readBytes;

            Socket socket = new Socket();
            // TODO: does this cover all the local interfaces?
            socket.bind(null);
            try {
                long preCallTime = System.currentTimeMillis();
                socket.connect(new InetSocketAddress(address, LifeSetup.LIFE_PORT_NUMBER), timeout);
                long postCallTime = System.currentTimeMillis();
                RTT = postCallTime - preCallTime;
            }
            catch(SocketTimeoutException e) {
                return -1;
            }
            DataInputStream input = new DataInputStream(socket.getInputStream());
            PrintStream output = new PrintStream(socket.getOutputStream());
            output.write(LifeSetup.TEST_BYTES);
            output.flush();
            readBytes = input.read(readBuffer, 0, readBuffer.length);
            if (!compareArrays(readBuffer, 0, readBytes, LifeSetup.TEST_BYTES, 0, LifeSetup.TEST_BYTES.length)) {
                return -1;
            }
            output.close();
            input.close();
            socket.close();

        } catch (IOException e) {
            return -1;
        }
        

        return RTT;
    }

    public static long testAliveUDP(InetAddress address, int timeout) {
        long RTT = -1;
        
        byte[] readBuffer = new byte[readBufferSize];
        DatagramPacket recvPacket = new DatagramPacket(readBuffer, 0, readBuffer.length);

        try {
            DatagramSocket socket = new DatagramSocket();
            socket.send(new DatagramPacket(LifeSetup.TEST_BYTES, 0, LifeSetup.TEST_BYTES.length, address, LifeSetup.LIFE_PORT_NUMBER));
            long startTime = System.currentTimeMillis();
            long recvTime;
            boolean receivedReply = false;
            do {
                // timeout shouldn't be less than 10ms
                socket.setSoTimeout((int) ((startTime + timeout - System.currentTimeMillis() > 10) ? startTime + timeout - System.currentTimeMillis() : 10));
                try {
                    socket.receive(recvPacket);
                }
                catch (SocketTimeoutException e) {
                    continue;
                }
                recvTime = System.currentTimeMillis();
                if (recvPacket.getAddress().equals(address) && recvPacket.getPort() == LifeSetup.LIFE_PORT_NUMBER) {
                    if (!compareArrays(recvPacket.getData(), recvPacket.getOffset(), recvPacket.getLength(), LifeSetup.TEST_BYTES, 0, LifeSetup.TEST_BYTES.length)) {
                        return -1;
                    }
                    receivedReply = true;
                    RTT = recvTime - startTime;
                }
            } while (!receivedReply && System.currentTimeMillis() < startTime+timeout);
        } catch (SocketException e) {
            return -1;
        } catch (IOException e) {
            return -1;
        }

                                    
        return RTT;
    }


    private static boolean compareArrays(byte[] array1, int offset1, int length1, byte[] array2, int offset2, int length2) {
        if (array1.length < length1 || array2.length < length2) {
            throw new IllegalArgumentException("Wrong array comparison request.");
        }
        if (length1 != length2) {
            return false;
        }
        for (int i = 0; i < length1; i++) {
            if (array1[i+offset1] != array2[i+offset2]) {
                return false;
            }
        }
        return true;
    }
}
