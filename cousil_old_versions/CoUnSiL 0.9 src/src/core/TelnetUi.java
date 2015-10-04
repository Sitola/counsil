package core;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * @author Lukáš Ručka, 359687
 *
 */
public class TelnetUi extends Thread {
    private int port;
    private ServerSocket socket;
    public boolean localStop;

    public static final int DefaultPort = myJXTA.MyJXTAConnector.DEFAULT_COUNIVERSE_TCP_PORT+1;

    public TelnetUi(int port) throws IOException {
        this.port = port;
        localStop = false;
        socket = new ServerSocket(port, 3);
        socket.setSoTimeout(50);
    }

    public int getPort() {
        return port;
    }

    public void run() {
        while(!UiStub.stopFlag.get() && !localStop) {
            Socket client = null;
            try {
                client = socket.accept();
            } catch (SocketTimeoutException ex) {
                continue;
            } catch (IOException ex) {
                Logger.getLogger(TelnetUi.class.getName()).log(Level.ERROR, "Failed to accept connections on telnet ui, exitting thread", ex);
                return;
            }
            if (client == null) continue;

            UiStub stub = null;
            try {
                stub = new UiStub(client.getOutputStream(), client.getInputStream());
            } catch (IOException ex) {
                Logger.getLogger(TelnetUi.class.getName()).log(Level.ERROR, "Unable to open streams to client", ex);
            }
            Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Client " + client.getInetAddress().toString() + " has connected to the telnet UI\n");
            stub.start();
        }
    }
}
