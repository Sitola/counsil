package appControllers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;

/**
 * Thread for reading from an input pipe. Upon every line read from the pipe.
 * a new event Reporter.report(...) is invoked.
 * The thread terminates when the pipe to be read from is closed.
 */
class PipeReader extends Thread {

    static interface Reporter {
        public void report(String line);
    }
    final BufferedReader in;
    final Reporter reporter;

    /**
     * @param in Pipe to be monitored
     * @param reporter report(line) is called upon reception of a new line from the pipe
     */
    PipeReader(BufferedReader in, Reporter reporter) {
        setDaemon(true);
        this.in = in;
        this.reporter = reporter;
    }

    @Override
    public void run() {
        try {
            while (true) {
                String ln = in.readLine();
                reporter.report(ln == null ? "EOF" : ln);
                if (ln == null) {
                    break;
                }
            }
        } catch (IOException ex) {
            reporter.report("EOF");
        }
    }
}