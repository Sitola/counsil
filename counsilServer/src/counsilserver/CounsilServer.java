/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package counsilserver;


import java.io.File;
import java.io.IOException;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONException;
import org.json.JSONObject;


/**
 *
 * @author xminarik
 */
public class CounsilServer {

    
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        System.out.println("CoUnSil server is composed from:");
        System.out.println("  web server - respond to client requests about topology");
        System.out.println("  CoUnivers servers - created based on roomConfiguration.json, each server serve one virtual room");
        try {
            File configFile = new File("roomConfiguration.json");
            if(!configFile.exists()){
                System.err.println("missing roomConfiguration.json");
                System.exit(1);
            }
                    
            String entireFileText = new Scanner(configFile).useDelimiter("\\A").next();
            if(entireFileText == null){
                System.err.println("can't parse roomConfiguration.json");
                System.exit(1);
            }
            JSONObject input = new JSONObject(entireFileText);     
            
            //TO DO: chack configuration file
            webServer ws = new webServer(input);
            Thread webServerThread = new Thread(ws);
            webServerThread.start();
            
            counsilServerCreation csc = new counsilServerCreation();
            csc.startServers(input);
            System.out.println("CoUnSil server started");
            Runtime.getRuntime().addShutdownHook(new Thread(){
                @Override
                public void run(){
                    webServerThread.interrupt();
                    csc.stopServers();
                    System.out.println("CoUnSil server stopped");
                }
            });
        } catch (JSONException | IOException ex ) {
            System.err.println("CoUnSil server have crashed");
            Logger.getLogger(CounsilServer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
