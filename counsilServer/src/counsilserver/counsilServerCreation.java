/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package counsilserver;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
    
    
    private Core couniverse;

    public counsilServerCreation() {
        
        this.couniverse = null;
    }
    
    public void startServer(JSONObject input){
        //couniverse.core.utils.MyLogger.setup();
        JSONObject serverConfiguration = createConfiguration(input);
        try{
            ObjectNode configNode = (ObjectNode) new ObjectMapper().readTree(serverConfiguration.toString());
            couniverse = Main.startCoUniverse(configNode);
        } catch (IOException ex) {
            System.err.println("error while creationg server");
            Logger.getLogger(counsilServerCreation.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InterruptedException ex) {
            System.err.println("server interupted");
            Logger.getLogger(counsilServerCreation.class.getName()).log(Level.SEVERE, null, ex);
        }
        System.out.println("starting CoUniverse server");
    }
    
    public void stopServers(){
        System.out.println("stopping CoUniverse servers");
        couniverse.stop();
    }
    
    public JSONObject createConfiguration(JSONObject input){
        JSONObject serverConfiguration = new JSONObject();
        JSONObject connector = new JSONObject();
        JSONObject localNode = new JSONObject();
        JSONObject templates = new JSONObject();
        
        
        try {
            JSONArray roomsArray = input.getJSONArray("rooms");
            
            connector.put("serverAddress",input.getString("server ip"));
            connector.put("serverPort", input.getInt("comunication port"));
            connector.put("startServer", "true");

            String name = input.getString("server name") + "_server";
            JSONObject interfaceInside = new JSONObject();
            interfaceInside.put("name", name);
            interfaceInside.put("address", input.getString("server ip"));
            interfaceInside.put("bandwidth", 1000);
            interfaceInside.put("isFullDuplex", true);
            interfaceInside.put("subnetName", "world");
            interfaceInside.put("properties", new JSONObject());
            JSONArray interfaces = new JSONArray();
            interfaces.put(interfaceInside);
            
            JSONObject properties = new JSONObject();
            properties.put("agc", input.getString("agc"));
            properties.put("distributor", true);
            properties.put("dummy-compress", input.getString("dummy compress"));
            properties.put("dummy-distributor-compress", input.getString("dummy distributor compress"));
            //properties.put("rooms", rooms);
                
            for(int i=0;i<roomsArray.length();i++){
                JSONObject room = roomsArray.getJSONObject(i);
                properties.put("rooms", room.getString("name"));
            }
            
            localNode.put("name", name);
            localNode.put("interfaces", interfaces);
            localNode.put("properties", properties);

            JSONObject distributorJSON = new JSONObject();
            distributorJSON.put("path", input.getString("distributor path"));
            distributorJSON.put("arguments", "8M");
            
            templates.put("distributor", distributorJSON);

            serverConfiguration.put("connector", connector);
            serverConfiguration.put("localNode", localNode);
            serverConfiguration.put("templates", templates);
            
        } catch (JSONException ex) {
            Logger.getLogger(counsilServerCreation.class.getName()).log(Level.SEVERE, null, ex);
        }
        return serverConfiguration;
    }
}
