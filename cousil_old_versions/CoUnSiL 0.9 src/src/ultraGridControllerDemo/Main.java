package ultraGridControllerDemo;

import java.io.IOException;
import java.util.List;

public class Main {

    // TODO: The package is probably obsolete. Verify and ideally remove
    public static void main(String[] args) throws IOException, InterruptedException {
        String [] params = new String[4];
        params[0] = "-t";
        params[1] = "testcard:32:32:30:UYVY";
        params[2] = "-d";
        params[3] = "sdl";
        UltraGridCoUniverseControl ugControl = new UltraGridCoUniverseControl("uv", params, "localhost");

        ugControl.run();
        List<UltraGridStatistics > stats;
        int i = 0;
        while((stats = ugControl.popStatistics()) != null) {
            if(i++ == 1) {
                ugControl.changeReceiver("hd1.fi.muni.cz", 0);
            }
            if(i == 3) {
                ugControl.changeReceiver("localhost", 0);
            }
            i++;
        }
        ugControl.waitFor();
        ugControl.printMessages();
    }
}
