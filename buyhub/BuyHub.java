package buyhub;

import buyhub.users.Users;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * @author cristopher
 */
public class BuyHub {
    public static final Gson gson = new GsonBuilder().registerTypeAdapter(
            byte[].class, new GsonBase64Serializer()
    ).setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS").create();
    
    private static DataSource pool;
    
    static {		
        try {
            pool = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/datasource_buyhub");
        } catch (NamingException ex) {
            Logger.getLogger(Users.class.getName()).log(Level.SEVERE, null, ex);
            System.exit(1);
        }
    }
    
    public static Connection getConnection() throws SQLException {
        return pool.getConnection();
    }
}
