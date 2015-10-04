package myJXTA;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import net.jxta.discovery.DiscoveryService;
import net.jxta.document.AdvertisementFactory;
import net.jxta.document.MimeMediaType;
import net.jxta.document.StructuredDocumentFactory;
import net.jxta.document.XMLDocument;
import net.jxta.endpoint.ByteArrayMessageElement;
import net.jxta.endpoint.Message;
import net.jxta.endpoint.MessageElement;
import net.jxta.id.IDFactory;
import net.jxta.peergroup.PeerGroup;
import net.jxta.peergroup.PeerGroupID;
import net.jxta.pipe.OutputPipe;
import net.jxta.pipe.PipeID;
import net.jxta.pipe.PipeService;
import net.jxta.platform.ModuleSpecID;
import net.jxta.protocol.ModuleImplAdvertisement;
import net.jxta.protocol.PeerGroupAdvertisement;
import net.jxta.protocol.PipeAdvertisement;
import networkRepresentation.EndpointNetworkNode;
import org.apache.log4j.Logger;
import p2p.CoUniverseMessage;
import p2p.MessageType;

/**
 * Varopis utilities to avoid code duplication
 * <p/>
 * User: Petr Holub (hopet@ics.muni.cz)
 * Date: 30.7.2007
 * Time: 16:07:05
 */
public class MyJXTAUtils {
    static Logger logger = Logger.getLogger("myJXTA");
    private static String localHostName = null;

    @Deprecated
    public static OutputPipe createOutputPipe(PipeService pipeService, EndpointNetworkNode targetNode, byte[] targetNodeInputPipeAdv) throws IOException {
        OutputPipe targetNodeOutputPipe = null;
        int attempt = 0;
        int maxAttempts = 1;

        ByteArrayInputStream input = new ByteArrayInputStream(targetNodeInputPipeAdv);

        XMLDocument xml = (XMLDocument) StructuredDocumentFactory.newStructuredDocument(MimeMediaType.XMLUTF8, input);
        PipeAdvertisement nodeOutputPipeAdvertisement = (PipeAdvertisement) AdvertisementFactory.newAdvertisement(xml);
        while ((targetNodeOutputPipe == null) && (attempt < maxAttempts)){
            //noinspection EmptyCatchBlock
            try {
                attempt++;
                targetNodeOutputPipe = pipeService.createOutputPipe(nodeOutputPipeAdvertisement, 3000);
            } catch (IOException e) {
                MyJXTAUtils.logger.warn("Failed to create output pipe to peer " + targetNode.getNodeName() + " <"+nodeOutputPipeAdvertisement+">. Trying again...");
            }
        }
        if (targetNodeOutputPipe == null) {
            throw new IOException("Failed to create output pipe.");
        }

        
        return targetNodeOutputPipe;
    }

    /**
     * JXTA Message Decoder
     *
     * @param msg         JXTAMessage to decode
     * @param msgElemName name of the MessageElement to be decoded from the message
     * @return Object encoded within a MessageElement with a specified name
     * @throws IOException on decodeMessageElement() failed
     */
    @Deprecated
    public static Object decodeMessage(Message msg, String msgElemName) throws IOException {
        MessageElement msgElem = msg.getMessageElement(msgElemName);
        if (msgElem != null) {
            return MyJXTAUtils.decodeMessageElement(msgElem);
        } else {
            return null;
        }
    }

    /**
     * JXTA Message structure validator
     * <p/>
     * Validates if incomming JXTA Message consists of expected message elements given by their names.
     * Message elements must be given in expected orderas they were encoded into the message.
     *
     * @param msg       JXTAMessage to validate
     * @param structure names of the MessageElements included in the message
     * @return true for valid (i.e. with expected MessageElements) JXTA Message, false otherwise
     */
    @Deprecated
    public static boolean validateMessage(Message msg, String... structure) {
        try {
            return validateAndDecodeMessage(msg, structure) != null;
        } catch (IOException e) {
            // TODO: rewrite using logger when JXTA is wrapped in JXTA Connector
            e.printStackTrace();
            return false;
        }
    }


    /**
     * Combined validator and decoder for JXTA Message structure
     * <p/>
     * Validates if incomming JXTA Message consists of expected message elements given by their names.
     * Message elements must be given in expected orderas they were encoded into the message. It returns
     * a list of extracted object in the same order as specified by the structure
     *
     * @param msg       JXTAMessage to validate
     * @param structure structure names of the MessageElements included in the message
     * @return iff the Message is valid according to the pattern specified using structure parameter, returns decoded elements in the same order. Otherwise returns null
     * @throws java.io.IOException when decodeMessage() failed
     */
    @Deprecated
    public static Object[] validateAndDecodeMessage(Message msg, String... structure) throws IOException {
        ArrayList<Object> objects = new ArrayList<Object>();
        for (int i = 0; i < structure.length; i++) {
            for (int j = 0; j < i; j++) {
                if (structure[i].equals(structure[j])) {
                    assert false : "Message structure check failed.";
                }
            }
        }

        Message.ElementIterator msgIterator = msg.getMessageElements();
        assert msgIterator != null;

        int i = 0;
        while (msgIterator.hasNext() && i < structure.length) {
            MessageElement me = msgIterator.next();
            if (!me.getElementName().equals(structure[i])) {
                return null;
            } else {
                objects.add(decodeMessageElement(me));
            }
            i++;
        }
        if (msgIterator.hasNext()) {
            return null;
        }
        if (i != structure.length) {
            return null;
        }
        Object[] os = new Object[objects.size()];
        os = objects.toArray(os);
        return os;
    }

    /**
     * JXTA Message encoder
     * <p/>
     * Both object and description must not be null
     *
     * @param elements Tuples Object, String where Object is encoded as JXTAMessage element and described by String
     * @return JXTA Message
     */
    @Deprecated
    public static Message encodeMessage(Object... elements) {
        Message message = new Message();

        // Some checks first
        assert elements.length >= 2 && elements.length % 2 == 0;

        int i = 0;
        while (i < elements.length) {

            assert elements[i] != null && elements[i + 1] != null;
            // The second element has to be a string (message element name)
            assert elements[i + 1] instanceof String;

            MessageElement msgElem;
            msgElem = encodeMessageElement(elements[i], (String) elements[i + 1]);
            message.addMessageElement(msgElem);

            i += 2;
        }

        return message;
    }

    /**
     * Clears JXTA component cache given its path
     *
     * @param path of the cache
     */

    public static void clearCache(String path) {
        clearCacheCM(new File(path, "cm"));
    }

    /**
     * Purges JXTA cache including all config files
     *
     * @param path of the cache
     */

    public static void purgeCache(String path) {
        File topDir = new File(path);
        if (topDir.exists()) {
            ArrayList<String> filesToDelete = new ArrayList<String>() {
                {
                    add("PlatformConfig");
                    add("config.properties");
                }
            };
            for (String file : filesToDelete) {
                File platformConfig = new File(path, file);
                if (platformConfig.exists()) {
                    platformConfig.delete();
                    MyJXTAUtils.logger.info("Platform config " + platformConfig.toString() + " removed.");
                }
            }
            clearCacheCM(new File(path, "cm"));
        }

    }

    /**
     * Clears JXTA component cache given its path
     *
     * @param rootDir path of the cache
     */

    private static void clearCacheCM(final File rootDir) {
        try {
            if (rootDir.exists()) {
                File[] list = rootDir.listFiles();
                for (File aList : list) {
                    if (aList.isDirectory()) {
                        clearCacheCM(aList);
                    } else {
                        aList.delete();
                    }
                }
            }
            rootDir.delete();
            MyJXTAUtils.logger.info("Cache component " + rootDir.toString() + " cleared.");
        }
        catch (Throwable t) {
            MyJXTAUtils.logger.warn("Unable to clear " + rootDir.toString());
        }
    }

    /**
     * Get advertisement of the specified pipe. In CoUniverse, the advertisements
     * are created locally, then looked for the same instances. In order for them
     * to match, the arguments should match
     * @param parentGroup Parrent group of the pipe. Usually, the couniversePeerGroup is used
     * @param pipeName Unique name of the pipe
     * @param pipeServiceType net.jxta.pipe.PipeService member String
     * @return Advertisement for the specified pipe
     */
    public static PipeAdvertisement getPipeAdvertisement(PeerGroup parentGroup, String pipeName, String pipeServiceType) {

        // Creating a Pipe Advertisement
        PipeAdvertisement ad = (PipeAdvertisement) AdvertisementFactory.newAdvertisement(PipeAdvertisement.getAdvertisementType());
        PipeID id = IDFactory.newPipeID(parentGroup.getPeerGroupID(), pipeName.getBytes());

        ad.setPipeID(id);
        ad.setType(pipeServiceType);
        ad.setName(pipeName);
        ad.setDescription("BiDirectional pipe " + pipeName);

        return ad;
    }

    /**
     * Checks that the objects are valid for given message type.
     * <p/>
     *
     * @param messageType to check against
     * @param objects     to be checked
     * @return true if object comply with the message type, false otherwise
     */
    @Deprecated
    // TODO: remove or at least modify; the check should pe done upon creation of the CoUniverseMessage object
    static boolean validateObjectsForSending(MessageType messageType, Object... objects) {

        // TODO: REMOVE! Debug only
        if (true) return true;

        System.out.println("Validating message:");
        System.out.println("  Type:    " + (Arrays.toString(messageType.getElementIdentifiers())));
        System.out.println("  Message: " + (Arrays.toString(objects)));

        if (messageType.getElementIdentifiers().length != objects.length) {
            System.err.println("Failed to verify object to send!");
            throw new IllegalArgumentException("message does not match its specification");
            // return false;
        }

        for (Object object : objects) {
            if (!(object instanceof java.io.Serializable)) {
                System.err.println("Failed to verify object to send!");
                throw new IllegalArgumentException("message not serializable");
                // return false;
            }
        }

        // TODO: validate, that conform to JavaBean structure
        try {
            for (Object object : objects) {
                Introspector.getBeanInfo(object.getClass());
            }
        } catch (IntrospectionException e) {
            System.err.println("Failed to verify object to send!");
            throw new IllegalArgumentException("Does not conform bean structure!", e);
            // return false;
        }

        return true;
    }

    static void deleteFileOrDirectory(File file) throws IOException {
        if (!file.exists()) return;
        if (file.isDirectory()) {
            for (File c : file.listFiles())
                deleteFileOrDirectory(c);
        }
        if (!file.delete()) {
            throw new IOException("Failed to delete file: " + file);
        }
    }

    /**
     * Encode the given CoUniverse message into a JXTA message
     * @param couniverseMessage Message to encode
     * @return JXTA compliant message
     */
    public static Message encodeMessage(CoUniverseMessage couniverseMessage) {
        Message jxtaMessage = new Message();

        MessageType type = couniverseMessage.type;
        MessageElement element = encodeMessageElement(type, MessageType.MESSAGE_TYPE_ID);
        jxtaMessage.addMessageElement(element);

        element = encodeMessageElement(couniverseMessage.sender, CoUniverseMessage.SENDER_IDENTIFIER_STRING);
        jxtaMessage.addMessageElement(element);

        element = encodeMessageElement(couniverseMessage.receiver, CoUniverseMessage.RECEIVER_IDENTIFIER_STRING);
        jxtaMessage.addMessageElement(element);

        for (String id : type.getElementIdentifiers()) {
            element = MyJXTAUtils.encodeMessageElement(couniverseMessage.getElement(id), id);
            jxtaMessage.addMessageElement(element);
        }

        return jxtaMessage;
    }

    /**
     * Decode the received JXTA message
     * @param jxtaMessage Message to decode
     * @return CoUniverse representation of the message.
     */
    public static CoUniverseMessage decodeMessage(Message jxtaMessage) {

        try {
            MyJXTAConnectorID sender   = (MyJXTAConnectorID) MyJXTAUtils.decodeMessageElement(jxtaMessage.getMessageElement(CoUniverseMessage.SENDER_IDENTIFIER_STRING  ));
            MyJXTAConnectorID receiver = (MyJXTAConnectorID) MyJXTAUtils.decodeMessageElement(jxtaMessage.getMessageElement(CoUniverseMessage.RECEIVER_IDENTIFIER_STRING));
            
            Serializable o = MyJXTAUtils.decodeMessageElement(jxtaMessage.getMessageElement(MessageType.MESSAGE_TYPE_ID));
            if (! (o instanceof MessageType)) {
                return null;
            }

            MessageType type = (MessageType) o;
            logger.debug("Received message " + jxtaMessage + " of type " + type);
            Serializable[] content = new Serializable[type.getElementIdentifiers().length];

            for (int i = 0; i < type.getElementIdentifiers().length; i++) {
                content[i] = decodeMessageElement(jxtaMessage.getMessageElement(type.getElementIdentifiers()[i]));
            }

            CoUniverseMessage couniverseMessage = new CoUniverseMessage(type, content, sender, receiver);
            return couniverseMessage;

        } catch (IndexOutOfBoundsException ex) {
            logger.warn("Failed to decode message " + jxtaMessage + " with " + ex);
        } catch (IOException ex) {
            logger.warn("Failed to decode message " + jxtaMessage + " with " + ex);
        }
        return null;
    }

    /**
     * MessageElement encoder
     *
     * @param o        Object to encode
     * @param elemName MessageElement name
     * @return MessageElement
     */
    private static MessageElement encodeMessageElement(Object o, String elemName) {
        MessageElement messageElem;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        XMLEncoder xmlenc = new XMLEncoder(output);

        xmlenc.writeObject(o);
        xmlenc.close();
        messageElem = new ByteArrayMessageElement(elemName, MimeMediaType.XMLUTF8, output.toByteArray(), null);

        return messageElem;
    }

    /**
     * MessgaElement decoder
     *
     * @param messageElem incomming MessageElement
     * @return Object decoded from the incomming MessageElement
     * @throws IndexOutOfBoundsException on creating new input stream from messageElement byte array
     * @throws IOException               on input.close() failed
     */
    private static Serializable decodeMessageElement(MessageElement messageElem) throws IndexOutOfBoundsException, IOException {
        Serializable o;
        ByteArrayInputStream input = new ByteArrayInputStream(messageElem.getBytes(true));
        XMLDecoder xmldec = new XMLDecoder(input);

        o = (Serializable) xmldec.readObject();

        xmldec.close();
        input.close();

        return o;
    }


    /**
     * Creates peer group for the universe. This is the basic collaborative space.
     * <p/>
     *
     * @param rootGroup to base the universe upon
     * @return created peer group
     * @throws Exception in case of any failure
     */
    public static PeerGroup createCoUniversePeerGroup(PeerGroup rootGroup) throws Exception {
        String name = "Collab Universe App Group";
        String desc = "Collab Universe playground";
        String gmsid = "urn:jxta:uuid-8B5DBA3AB32748F287F3A238235352E16E691351F90A4B47AEE18590569B0AF506";
        MyJXTAConnector.logger.info("Creating group:  " + name + ", " + desc);
        DiscoveryService disco = rootGroup.getDiscoveryService();
        ModuleImplAdvertisement implAdv = rootGroup.getAllPurposePeerGroupImplAdvertisement();
        implAdv.setModuleSpecID((ModuleSpecID) IDFactory.fromURI(new URI(gmsid)));
        implAdv.setDescription("Collaborative Universe Implementation");
        implAdv.setProvider("sitola.fi.muni.cz");
        implAdv.setUri("http://sitola.fi.muni.cz/CollabUniverse");
        disco.publish(implAdv);
        disco.remotePublish(implAdv);
        PeerGroupID groupID = IDFactory.newPeerGroupID(rootGroup.getPeerGroupID(), name.getBytes());
        PeerGroup newGroup = rootGroup.newGroup(groupID, implAdv, name, desc);
        PeerGroupAdvertisement groupAdv = newGroup.getPeerGroupAdvertisement();
        disco.publish(groupAdv);
        disco.remotePublish(null, groupAdv);
        return newGroup;
    }
    
    public static String getLocalHostName() {
        
        if (localHostName != null) return localHostName;
        
        try {
            localHostName = InetAddress.getLocalHost().getCanonicalHostName();
        } catch (UnknownHostException ex) {}
        
        if (localHostName == null) {
            localHostName = "256.0.0." + (new Random().nextInt(256));
        }
        
        return localHostName;
    }
    
}
