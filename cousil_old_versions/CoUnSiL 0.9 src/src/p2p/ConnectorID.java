/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package p2p;

import java.io.Serializable;



/**
 *
 * @author maara
 */
public interface ConnectorID extends Comparable, Serializable {
    
    public Object getId();
}
