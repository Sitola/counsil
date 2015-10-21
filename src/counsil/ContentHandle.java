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

class ContentHandle {
    
    private final String role; //? public/private?
    private final String title;

    public ContentHandle(String role, String title) {
        this.role = role;
        this.title = title;
    }

    public String getRole() {
        return role;
    }

    public String getTitle() {
        return title;
    }         
   
}
