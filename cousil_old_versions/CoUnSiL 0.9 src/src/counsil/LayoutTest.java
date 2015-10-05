/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package counsil;

import networkRepresentation.EndpointUserRole;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import javax.swing.JFrame;
import org.json.simple.parser.ParseException;
import wddman.OperatingSystem;
import wddman.UnsupportedOperatingSystemException;
import wddman.WDDMan;
import wddman.WDDManException;

/**
 *
 * @author Peter
 */
public class LayoutTest {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException, InterruptedException, UnsupportedOperatingSystemException, WDDManException, ParseException {
        // TODO code application logic here
        
       JFrame frame1 = new JFrame();
       frame1.setTitle("teacher");
       frame1.setVisible(true);
       frame1.setBounds (0, 0, 400, 300);
       
       JFrame frame2 = new JFrame();
       frame2.setTitle("interpreter");
       frame2.setVisible(true);
       frame2.setBounds (0, 0, 400, 300);
       
       JFrame removedFrame = null;
       
       for(int i =0; i<4; i++){
            JFrame studentFrame = new JFrame();
            studentFrame.setTitle("student"+i);
            studentFrame.setVisible(true);
            
            studentFrame.setBounds (0, 0, 400, 300);
            if(i==1){
                studentFrame.setBounds(0, 0, 300, 300);
                removedFrame=studentFrame;
            }
       }
        
       HashMap<String, EndpointUserRole> roleMap = new HashMap<>();
       
       roleMap.put("teacher",new EndpointUserRole("teacher"));
       roleMap.put("interpreter",new EndpointUserRole("interpreter") );
       for(int i=0;i<4; i++){
       roleMap.put( "student"+i,new EndpointUserRole("student"));
       }
       
       String pathToConfig = "C:\\Users\\Peter\\Desktop\\JSON.txt";
       EndpointUserRole nodeRole = new EndpointUserRole("student");

//       UserController.initialize(roleMap, pathToConfig, nodeRole);
       Thread.sleep(2000);
       removedFrame.setVisible(false);
       UserController.userDisconnected("student1");
       
       
        
    }

}
