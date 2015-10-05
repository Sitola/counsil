package ultraGridControllerDemo;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: martin
 * Date: 24.6.13
 * Time: 9:46
 * To change this template use File | Settings | File Templates.
 */

public class UltraGridCoUniverseControl {
    private enum type {
        PRODUCER,
        DISTRIBUTOR
    }

    private type UltraGridType;
    private Process pr;
    private String[] cmdarray;

    private ServerSocket serverSocket;
    private Socket socket;
    private BufferedReader socketReader;
    private PrintWriter socketWriter;

    private ReaderThread readerThread;
    private ProcessGuardThread processGuardThread;

    private final Object changePropertyLock;

    public static final int UGControlPort = 5054;

    private class ReaderThread extends Thread {
        public ReaderThread(BufferedReader reader) {
            this.reader = reader;
        }
        public void run() {
            String line;
            try {
                while((line = reader.readLine()) != null) {
                    synchronized(this) {
                        if(line.startsWith("stats")) {
                            stats = line;
                        } else {
                            response = line;
                        }
                        notifyAll();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            synchronized(this) {
                exited = true;
                notifyAll();
            }
        }

        synchronized String getStatLine() {
            while (stats == null && !exited) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            if(exited)
                return null;
            String ret = stats;
            stats = null;
            return ret;
        }

        synchronized String getResponse() {
            while (response == null && !exited) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            if(exited)
                return null;
            String ret = response;
            response = null;
            return ret;
        }

        private String stats, response;
        boolean exited;
        private BufferedReader reader;
    }

    private class ProcessGuardThread extends Thread {
        public void run() {
            try {
                pr.waitFor();
            } catch (InterruptedException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
            try {
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }
    }

    public UltraGridCoUniverseControl(String path, String[] params, String host) {
        UltraGridType = type.PRODUCER;
        cmdarray = new String[2 + params.length + 2];
        cmdarray[0] = path;
        int index = 1;
        for(String item : params) {
            cmdarray[index++] = item;
        }
        cmdarray[index] = host;

        changePropertyLock = new Object();
    }

    public UltraGridCoUniverseControl(String path, ConsumerParam[] consumers) {
        UltraGridType = type.DISTRIBUTOR;
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public void run() throws IOException, InterruptedException {
        
        Runtime rt = Runtime.getRuntime();
        serverSocket = new ServerSocket(0);
        cmdarray[cmdarray.length - 2] = "--control-port";
        cmdarray[cmdarray.length - 1] = String.valueOf(serverSocket.getLocalPort()) + ":1";
        pr = rt.exec(cmdarray);
        processGuardThread = new ProcessGuardThread();
        processGuardThread.start();

        //Thread.sleep(1000);
        socket = serverSocket.accept();
        socketReader = new BufferedReader(new InputStreamReader(
                socket.getInputStream()));
        socketWriter = new PrintWriter(
                socket.getOutputStream(), true);

        readerThread = new ReaderThread(socketReader);
        readerThread.start();
    }

    public void waitFor() throws InterruptedException {
        pr.waitFor();
        readerThread.join();
        processGuardThread.join();
    }

    void printMessages() throws IOException {
        String line=null;
        BufferedReader inputStream = new BufferedReader(new InputStreamReader(pr.getInputStream()));
        while((line=inputStream.readLine()) != null) {
            System.out.println(line);
        }
    }

    private List<UltraGridStatistics> parseStatistics(String line) {
        String[] splitted = line.split(" ");

        List<UltraGridStatistics> ret = new LinkedList<UltraGridStatistics>();
        assert(splitted.length % 2 == 1 && splitted[0].equals("stats"));

        for(int i = 1; i < splitted.length; i += 2) {
            UltraGridStatistics stat = new UltraGridStatistics();
            stat.name = splitted[i];
            stat.value = Long.parseLong(splitted[i + 1]);
            ret.add(stat);
        }

        return ret;
    }


    List<UltraGridStatistics> popStatistics() throws IOException {
        String line = readerThread.getStatLine();

        if(line != null) {
            System.out.println(line);

            return parseStatistics(line);
        } else {
            return null;
        }
    }

    private boolean change(String what, int port) throws IOException {
        synchronized(changePropertyLock) {

            if(UltraGridType == type.DISTRIBUTOR) {
                what = "port " + port + " " + what;
            }
            
            socketWriter.println(what);

            String line = readerThread.getResponse();
            if(line == null) {
                throw new IOException("Socket closed.");
            }

            System.out.println(line);

            long code = Long.parseLong(line.split(" ")[0]);
            if(code >= 200 && code < 300) {
                return true;
            } else {
                return false;
            }
        }
    }

    boolean changeReceiver(String newReceiver, int port) throws IOException {
        return change("receiver " + newReceiver, port);
    }

    boolean changeFEC(String newFEC, int port) throws IOException {
        return change("fec " + newFEC, port);
    }

    boolean changeCompress(String newCompress, int port) throws IOException {
        return change("compress " + newCompress, port);
    }

    boolean changeCompressParam(String compressParam, int port) throws IOException {
        return change("compress param " + compressParam, port );
    }

}
