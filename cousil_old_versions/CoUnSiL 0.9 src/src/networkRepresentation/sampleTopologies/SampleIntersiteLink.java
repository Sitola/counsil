/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package networkRepresentation.sampleTopologies;

import java.util.ArrayList;
import networkRepresentation.NetworkSite;

/**
 *
 * @author Pavel Troubil <pavel@ics.muni.cz>
 */
public interface SampleIntersiteLink {
    String getSubnet();
    
    SampleSite getFromSite();
    SampleSite getToSite();
    ArrayList<SamplePhysicalLink> getPhysicals();
}
