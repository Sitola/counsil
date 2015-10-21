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
public interface Layout {

    /**
     *
     */
    public void Layout();
    public void addWindow(Window win);
    public void removeWindow(Window win);
    public Window getWindow();
}
