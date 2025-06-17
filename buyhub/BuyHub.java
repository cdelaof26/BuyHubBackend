package buyhub;

import buyhub.users.Users;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.ws.rs.core.Response;

/**
 * @author cristopher
 */
public class BuyHub {
    public static final Gson gson = new GsonBuilder().registerTypeAdapter(
            byte[].class, new GsonBase64Serializer()
    ).setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS").create();
    
    private static DataSource pool;
    
    public static final String TOKEN_REFRESH = "UPDATE users SET session_expires = NOW() + INTERVAL 30 MINUTE WHERE session_token = ?;";
    
    static {
        try {
            pool = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/datasource_buyhub");
        } catch (NamingException ex) {
            Logger.getLogger(Users.class.getName()).log(Level.SEVERE, null, ex);
            System.exit(1);
        }
    }
    
    public static Object validateToken(Connection connection, String userId, String sessionToken) throws SQLException {
        PreparedStatement query = connection.prepareStatement("SELECT email FROM users WHERE user_id = ? AND session_token = ? AND session_expires > NOW();");
        try {
            query.setString(1, userId);
            query.setString(2, sessionToken);

            try (ResultSet rs = query.executeQuery()) {
                if (!rs.next())
                    return Response.status(400).entity(BuyHub.errorJson("La sesión ha expirado o los datos son inválidos. Inicia sesión nuevamente")).build();
            }
        } finally {
            query.close();
        }
        
        return null;
    }
    
    public static boolean updateToken(Connection connection, String sessionToken) throws SQLException {
        try (PreparedStatement query = connection.prepareStatement(TOKEN_REFRESH)) {
            query.setString(1, sessionToken);
            return query.execute();
        }
    }
    
    public static Connection getConnection() throws SQLException {
        return pool.getConnection();
    }
    
    public static String jsonOk() {
        JsonObject o = new JsonObject();
        o.addProperty("message", "ok");
        
        return o.toString();
    }
    
    public static String errorJson(String msg) {
        JsonObject o = new JsonObject();
        o.addProperty("error", msg);
        
        return o.toString();
    }
    
    public static String requiredValidationErrorJson(String field) {
        return errorJson("El campo " + field + " es obligatorio");
    }
    
    public static String lengthValidationErrorJson(String field, String len) {
        return errorJson("El campo " + field + " no puede contener más de " + len + " carácteres");
    }
}
