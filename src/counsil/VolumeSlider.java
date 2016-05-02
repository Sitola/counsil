/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package counsil;

import java.awt.BorderLayout;
import java.awt.Color;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 *
 * @author Desanka
 */
public class VolumeSlider extends JFrame {

    private static VolumeSlider instance = null;
    private static final Object lock = new Object();
    
    public static VolumeSlider getInstance() {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = new VolumeSlider();
                    
                }
            }                    
        }
        return instance;
    }
        
    /**
     * Creates volume slider with volumeValue selected
     * @param val 
     */
    private VolumeSlider() {
        
        super("Volume");
        setLayout(new BorderLayout());
        
        setUndecorated(true);
        setBackground(new Color(0, 0, 0, (float) 1)); 
        setAlwaysOnTop(true);
        setResizable(false);
        setLocationRelativeTo(null);    
        setType(Type.UTILITY);

        getRootPane().setBorder(BorderFactory.createMatteBorder(1, 1, 1, 1, Color.BLACK));  
                
        JSlider volume = new JSlider(JSlider.HORIZONTAL,0, 10, 5);   
        volume.setMajorTickSpacing(1);
        volume.setPaintTicks(true);
        volume.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent ce) {
                float value = volume.getValue();
                float ratio = 65535 * (value / 10);   
                String path = "C:\\UltraGrid\\UltraGrid\\nircmd-x64\\nircmdc.exe";
                String arg1 = "setsysvolume";
                try {
                    Process process = new ProcessBuilder(path, arg1, Float.toString(ratio)).start();
                    process.waitFor();
                } catch (IOException ex) {
                    Logger.getLogger(VolumeSlider.class.getName()).log(Level.SEVERE, null, ex);
                } catch (InterruptedException ex) {
                    Logger.getLogger(VolumeSlider.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });

        add(volume, BorderLayout.CENTER);
        pack();
        setVisible(true);
    }

}
