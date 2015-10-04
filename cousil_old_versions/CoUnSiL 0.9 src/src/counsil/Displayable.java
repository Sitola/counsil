/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package counsil;


/**
 *
 * @author Peter
 */
public interface Displayable {
    void resize(int x, int y, int width, int height);
    void changePosition(int x, int y);
    int getWidth();
    int getHeight();
    
}
