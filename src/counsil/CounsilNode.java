/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package counsil;

import couniverse.core.NetworkNode;
import couniverse.ultragrid.UltraGridApplication;
import java.util.List;
import java.util.Timer;

/**
 *
 * @author pkajaba
 */
public class CounsilNode {
    Producer producer;
    Consumer consumer;
    NetworkNode node;
    String name;
    boolean isTalking;
    String role;
    Timer timer;

    CounsilNode(NetworkNode node) {
        this.node = node;
    }

    public void setConsumer(Consumer consumer) {
        this.consumer = consumer;
    }

    public void setIsTalking(boolean isTalking) {
        this.isTalking = isTalking;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setNode(NetworkNode node) {
        this.node = node;
    }

    public void setProducer(Producer producer) {
        this.producer = producer;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public void setTimer(Timer timer) {
        this.timer = timer;
    }
    
    public Consumer getConsumer() {
        return consumer;
    }

    public String getName() {
        return name;
    }

    public NetworkNode getNode() {
        return node;
    }

    public Producer getProducer() {
        return producer;
    }

    public String getRole() {
        return role;
    }

    public Timer getTimer() {
        return timer;
    }
}
