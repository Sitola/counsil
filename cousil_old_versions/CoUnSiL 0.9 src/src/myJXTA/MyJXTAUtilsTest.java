package myJXTA;

import junit.framework.TestCase;
import net.jxta.endpoint.Message;
import org.junit.Test;

import java.io.IOException;

/**
 * JUnit test for MyJXTAUtils
 * <p/>
 * User: Petr Holub (hopet@ics.muni.cz)
 * Date: 17.10.2007
 * Time: 13:13:32
 */
public class MyJXTAUtilsTest extends TestCase {

    @Test
    public void testDecodeMessage() {
        Integer firstElement = 100;
        String secondElement = "test string";

        Message msg = MyJXTAUtils.encodeMessage(firstElement, "firstElement", secondElement, "secondElement");

        assertTrue("Error in message validator", MyJXTAUtils.validateMessage(msg, "firstElement", "secondElement"));
        assertFalse("Error in message validator", MyJXTAUtils.validateMessage(msg, "secondElement", "firstElement"));
        assertFalse("Error in message validator", MyJXTAUtils.validateMessage(msg, "firstElement"));
        assertFalse("Error in message validator", MyJXTAUtils.validateMessage(msg, "firstElement", "secondElement", "thirdElement"));

        try {
            Object[] objects = MyJXTAUtils.validateAndDecodeMessage(msg, "firstElement", "secondElement");
            Integer testFirstElement = (Integer) objects[0];
            String testSecondElement = (String) objects[1];
            assertEquals(firstElement, testFirstElement);
            assertEquals(secondElement, testSecondElement);
        } catch (IOException e) {
            e.printStackTrace();
            assertTrue("Decoder shouldn't throw an exception!", false);
        }


        try {
            Integer testFirstElement = (Integer) MyJXTAUtils.decodeMessage(msg, "firstElement");
            String testSecondElement = (String) MyJXTAUtils.decodeMessage(msg, "secondElement");
            assertEquals(firstElement, testFirstElement);
            assertEquals(secondElement, testSecondElement);
        } catch (IOException e) {
            e.printStackTrace();
            assertTrue("Decoder shouldn't throw an exception!", false);
        }
    }

}
