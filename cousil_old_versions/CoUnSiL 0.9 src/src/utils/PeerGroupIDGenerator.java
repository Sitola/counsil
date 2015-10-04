package utils;

import java.io.*;
import java.net.*;
import java.util.*;

import net.jxta.credential.*;
import net.jxta.discovery.*;
import net.jxta.endpoint.*;
import net.jxta.id.*;
import net.jxta.peergroup.*;
import net.jxta.pipe.*;
import net.jxta.protocol.*;

import net.jxta.membership.Authenticator;
import net.jxta.membership.MembershipService;

import net.jxta.document.*;
import net.jxta.platform.*;
import net.jxta.rendezvous.*;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.log4j.Logger;

public class PeerGroupIDGenerator {
    static Logger logger = Logger.getLogger("utils");

    public static final net.jxta.peergroup.PeerGroupID createInfrastructurePeerGroupID(String clearTextID, String function) {
        PeerGroupIDGenerator.logger.info("Creating peer group ID =  clearText:'" + clearTextID + "' , function:'" + function + "'");
        byte[] digest = generateHash(clearTextID, function);
        net.jxta.peergroup.PeerGroupID peerGroupID = IDFactory.newPeerGroupID(digest);
        return peerGroupID;
    }

    /**
     * Generates an SHA-1 digest hash of the string: clearTextID+"-"+function or: clearTextID if function was blank.<p>
     * <p/>
     * Note that the SHA-1 used only creates a 20 byte hash.<p>
     *
     * @param clearTextID A string that is to be hashed. This can be any string used for hashing or hiding data.
     * @param function    A function related to the clearTextID string. This is used to create a hash associated with clearTextID so that it is a uique code.
     * @return array of bytes containing the hash of the string: clearTextID+"-"+function or clearTextID if function was blank. Can return null if SHA-1 does not exist on platform.
     */
    public static final byte[] generateHash(String clearTextID, String function) {
        String id;

        if (function == null) {
            id = clearTextID;
        } else {
            id = clearTextID + function;
        }
        byte[] buffer = id.getBytes();

        MessageDigest algorithm = null;

        try {
            algorithm = MessageDigest.getInstance("MD5");
        } catch (Exception e) {
            PeerGroupIDGenerator.logger.error("Cannot load selected Digest Hash implementation");
            return null;
        }

        // Generate the digest.
        algorithm.reset();
        algorithm.update(buffer);

        try {
            byte[] digest1 = algorithm.digest();
            return digest1;
        } catch (Exception de) {
            PeerGroupIDGenerator.logger.error("Failed to creat a digest.");
            return null;
        }
    }

    public static void main(String[] args) {

        if (args.length == 1) {
            PeerGroupID netPeerGroupID = null;
            String      netPeerGroupName = args[0];
            PipeID      pipeID = null;

            netPeerGroupID = createInfrastructurePeerGroupID(netPeerGroupName, "");
            pipeID = IDFactory.newPipeID(netPeerGroupID, generateHash("AGC Input Pipe", ""));

            PeerGroupIDGenerator.logger.info("Peer Group ID: " + netPeerGroupID.toString());
            PeerGroupIDGenerator.logger.info("Pipe ID: " + pipeID.toString());
        } else {
            System.out.println("Usage: PeerGroupIDGenerator peerGroupName");
        }
    }
}
