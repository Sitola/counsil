/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package counsil;

import com.fasterxml.jackson.databind.node.ObjectNode;
import couniverse.core.Core;
import couniverse.ultragrid.UltraGridProducerApplication;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author pkajaba
 */
public class Producer {

    private UltraGridProducerApplication sound;
    private UltraGridProducerApplication presentation;
    private UltraGridProducerApplication video;
    private ObjectNode prodConfig;
    private Core core;
    private String role;

    public Producer(Core core, String role) {
        this.core = core;
        this.role = role;
        prodConfig = core.newApplicationTemplate("producer");
    }

    Producer(TypeOfContent typeOfContent, String producer, String role) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private String createIdent(String type) {
        return core.getLocalNode().getName() + "-" + type + "-" + role;
    }
    
    private void createProducer(String ident, String audio, String video){
        try {
            prodConfig.put("video", video);
            prodConfig.put("audio", audio);
            prodConfig.put("name", ident);
            core.startApplication(prodConfig, "producer");
        } catch (IOException ex) {
            Logger.getLogger(Producer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void createSound(String soundSettings) {
        String identification = createIdent("SOUND");
        //Gotta check if it's null or empty string
        createProducer(identification, soundSettings, null);
    }

    public void createPresentation(String presentationSettings) {
        String identification = createIdent("PRESENTATION");
        //Gotta check if it's null or empty string
        createProducer(identification, null, presentationSettings);
    }

    public void createVideo(String videoSettings) {
        String identification = createIdent("VIDEO");
        createProducer(identification, null, videoSettings);
    }

    public UltraGridProducerApplication getSound() {
        return sound;
    }

    public UltraGridProducerApplication getPresentation() {
        return presentation;
    }

    public UltraGridProducerApplication getVideo() {
        return video;
    }


}
