/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package counsilserver;

import couniverse.Main;
import couniverse.clientServerCommunication.ServerConnector;
import couniverse.core.Core;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author xminarik
 */
public class counsilServerCreation {
    
        
    private final List<Core> couniverses = new ArrayList<Core>();
    private final List<ServerConnector> serverConnectors = new ArrayList<ServerConnector>();
    
    public void startServers(JSONObject input){
        
            couniverse.core.utils.MyLogger.setup();
        JSONArray rooms = input.optJSONArray("rooms");
        
        for(int i = 0; i < rooms.length(); i++){
            JSONObject room;
            try {
                room = rooms.getJSONObject(i);
                createconfiguration(room);
                startServer(room);
            } catch (JSONException ex) {
                Logger.getLogger(counsilServerCreation.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    public void startServer(JSONObject input){
        String name = "error";
        try {            
            name = input.getString("name");
        } catch (JSONException ex) {
            System.err.println("error missing name from configuration");
            Logger.getLogger(counsilServerCreation.class.getName()).log(Level.SEVERE, null, ex);
        }
        try{
            Core core = Main.startCoUniverse("nodeConfig_" + name + ".json");
            couniverses.add(core);
            //ServerConnector connector = new ServerConnector(input.getInt("comunication port"));
            //connector.joinUniverse();
            //serverConnectors.add(connector);
        } catch (IOException ex) {
            System.err.println("error while creationg server " + name);
            Logger.getLogger(counsilServerCreation.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InterruptedException ex) {
            System.err.println("server interupted " + name);
            Logger.getLogger(counsilServerCreation.class.getName()).log(Level.SEVERE, null, ex);
        }
        System.out.println("starting CoUniverse server " + name);
    }
    
    public void stopServers(){
        System.out.println("stopping CoUniverse servers");
        for(int i = 0; i < couniverses.size(); i++){
            Core core = couniverses.get(i);
            core.stop();
        }
    }
    
    public void createconfiguration(JSONObject input){
        JSONObject roomConfiguration = new JSONObject();
        JSONObject connector = new JSONObject();
        JSONObject localNode = new JSONObject();
        JSONObject consumer = new JSONObject();
        try {
            connector.put("serverAddress",input.getString("server ip"));
            connector.put("serverPort", input.getInt("comunication port"));
            connector.put("startServer", "true");

            String name = input.getString("name") + "_server";
            JSONObject interfaceInside = new JSONObject();
            interfaceInside.put("name", name);
            interfaceInside.put("address", input.getString("server ip"));
            interfaceInside.put("bandwidth", 1000);
            interfaceInside.put("isFullDuplex", true);
            interfaceInside.put("subnetName", input.getString("name"));
            interfaceInside.put("properties", new JSONObject());
            JSONArray interfaces = new JSONArray();
            interfaces.put(interfaceInside);

            JSONObject properties = new JSONObject();
            properties.put("agc", input.getString("agc"));
            properties.put("distributor", true);
            if(input.has("dummy compress")){
                if(input.getString("dummy compress").compareTo("") != 0){
                    properties.put("dummy-compress", input.getString("dummy compress"));
                }
            }
            if(input.has("dummy distributor compress")){
                if(input.getString("dummy distributor compress").compareTo("") != 0){
                    properties.put("dummy-distributor-compress", input.getString("dummy distributor compress"));
                }
            }
            
            localNode.put("name", name);
            localNode.put("interfaces", interfaces);
            localNode.put("properties", properties);

            JSONObject templates = new JSONObject();
            JSONObject JSONdistributor = new JSONObject();
            JSONdistributor.put("path", input.getString("distributor path"));
            JSONdistributor.put("arguments", "8M");
            templates.put("distributor", JSONdistributor);

            roomConfiguration.put("connector", connector);
            roomConfiguration.put("localNode", localNode);
            roomConfiguration.put("templates", templates);

            FileWriter file = new FileWriter("nodeConfig_" + input.getString("name") + ".json");
            file.write(roomConfiguration.toString());
            file.flush();
            file.close();
        } catch (JSONException | IOException ex) {
            Logger.getLogger(counsilServerCreation.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
