package buyhub;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author cristopher
 */
public class Utilities {
    public static final int HASH_LENGTH = 20;
    
    public static String hash(byte [] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte [] hash = digest.digest(data);
            
            StringBuilder hexString = new StringBuilder();
            for (int i = 0; i < hash.length; i++) {
                String hex = Integer.toHexString(0xff & hash[i]);
                if (hex.length() == 1)
                    hexString.append('0');
                hexString.append(hex);
            }
            
            return hexString.toString().substring(0, HASH_LENGTH);
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(Utilities.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }
    
    public static String getToken() {
        byte [] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        
        return hash(bytes);
    }
    
    public static String hash(String text) {
        return hash(text.getBytes(StandardCharsets.UTF_8));
    }
}
