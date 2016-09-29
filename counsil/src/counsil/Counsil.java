package counsil;

import java.awt.Frame;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ResourceBundle;
import java.util.Scanner;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import org.jnativehook.NativeHookException;
import org.json.JSONException;
import org.json.JSONObject;
import wddman.WDDManException;

/**
 *
 * @author xminarik
 */
public class Counsil {
    
    /**
     * test if file is json file
     * @param jsonFile
     * @return if file is json
     */
    private static boolean isJsonFile(File jsonFile){
        try{
            String entireFileText = new Scanner(jsonFile).useDelimiter("\\A").next();
            JSONObject jsonObject = new JSONObject(entireFileText);
        }catch(JSONException | FileNotFoundException ex){
            return false;
        }
        return true;
    }
    /**
     * @param args the command line arguments
     * @throws java.io.IOException
     * @throws java.lang.InterruptedException
     * @throws org.json.JSONException
     * @throws wddman.WDDManException
     * @throws java.io.FileNotFoundException
     * @throws org.jnativehook.NativeHookException
     */
    public static void main(String[] args) {
        ResourceBundle englishLanguageBundle = ResourceBundle.getBundle("resources_en_EN");
        ResourceBundle slovakLanguageBundle = ResourceBundle.getBundle("resources_sk_SK");
        ResourceBundle czechLanguageBundle = ResourceBundle.getBundle("resources_cs_CZ");
        try{
            String defoultAddress = "configs/clientConfig.json";
            File clientConfigurationFile = null;
            if(args.length >= 1){
                String configAddress = args[0];
                clientConfigurationFile = new File(configAddress);
            }else{
                clientConfigurationFile = new File(defoultAddress);
            }

            if(isJsonFile(clientConfigurationFile)){
                InitialMenu im = new InitialMenu(clientConfigurationFile); 
            }else{
                JOptionPane.showMessageDialog(new JFrame(),
                    englishLanguageBundle.getString("ERROR_NOT_CORRECT_CONFIG") + "\n" +
                        czechLanguageBundle.getString("ERROR_NOT_CORRECT_CONFIG") + "\n" +
                        slovakLanguageBundle.getString("ERROR_NOT_CORRECT_CONFIG"),
                    englishLanguageBundle.getString("ERROR") + "/" +
                        czechLanguageBundle.getString("ERROR") + "/" +
                        slovakLanguageBundle.getString("ERROR"),
                    JOptionPane.ERROR_MESSAGE);
                System.exit(10);
            }
        }catch(Exception err){
            JOptionPane.showMessageDialog(new Frame(),
                    englishLanguageBundle.getString("ERROR_UNDOCUMENTED") + "\n" +
                        czechLanguageBundle.getString("ERROR_UNDOCUMENTED") + "\n" +
                        slovakLanguageBundle.getString("ERROR_UNDOCUMENTED"), 
                    englishLanguageBundle.getString("ERROR") + "/" +
                        czechLanguageBundle.getString("ERROR") + "/" +
                        slovakLanguageBundle.getString("ERROR"),
                    JOptionPane.ERROR_MESSAGE);
            System.exit(11);
        } 
    }
}
