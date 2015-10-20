/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package counsil;

/**
 *
 * @author desanka
 */
class DisplayableWindow {
    
    private int height;
    private int width;
    private Position position;
    private final Window transparent;
    private final Window content;
    
    DisplayableWindow(wddman.Window window, String role){
        
        this.transparent = new Window(window, role, false);
        this.content = new Window(window, role, true);
    }
    
    public void adjustWindow(){
        
    }
    
    Position getPosition(){
        return position;
    }
    
    void setPosition(Position position){
        this.position = position;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public Window getContent() {
        return content;
    }

    public Window getTransparent() {
        return transparent;
    }
    
    

    
    
}
