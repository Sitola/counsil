/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package counsil;

/**
 *
 * @author xminarik
 */
public class Window{
    public String role; //temporary variabile for first implementation of recalculate, in future recalculae will be using other class as input or this wariabile get getter and setter
    boolean demanding;
    boolean active;
    Position position;

    public Window() {
        demanding = false;
        active = false;
    }
    
    public void setActive(){
        active = true;
    }
    
    void unsetActive() {
        active = false;
    }

    void setDemanding() {
        demanding = true;
    }
    
    void unsetDemanding() {
        demanding = false;
    }
    
    Position getPosition(){
        return position;
    }
    
    void setPosition(Position position){
        this.position = position;
    }
    
}

//is position in pixels or in % of screen ???
class Position{
    int x;
    int y;
    public Position(int x, int y){
        this.x = x;
        this.y = y;
    }
    public Position(){
        x = 0;
        y = 0;
    }

}