package buyhub.users;

import buyhub.BuyHub;
import buyhub.Utilities;
import com.google.gson.JsonObject;
import java.sql.SQLException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Consumes;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

// La URL del servicio web es http://localhost:8080/buyhub/rest/
// Métodos
//   POST
//     login
//     signup
//     validate

@Path("/")
public class Users {    
    @POST
    @Path("signup")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response signup(String json) throws SQLException {
        User user = (User) BuyHub.gson.fromJson(json, User.class);
        if (user == null)
            return Response.status(400).entity(BuyHub.errorJson("Sign up method expects a JSON body")).build();
        
        String password = user.getPassword();
        if (user.getEmail() == null || password == null)
            return Response.status(400).entity(BuyHub.errorJson("Se requiere de un correo y una contraseña")).build();

        password = Utilities.hash(password);
        if (password == null)
            return Response.serverError().build();
        
        String name = user.getName();
        if (name == null)
            return Response.status(400).entity(BuyHub.requiredValidationErrorJson("nombre")).build();
        if (name.length() > 100)
            return Response.status(400).entity(BuyHub.lengthValidationErrorJson("nombre", "100")).build();
        
        String paternalSurname = user.getPaternalSurname();
        if (paternalSurname == null)
            return Response.status(400).entity(BuyHub.requiredValidationErrorJson("apellido paterno")).build();
        if (paternalSurname.length() > 100)
            return Response.status(400).entity(BuyHub.lengthValidationErrorJson("apellido paterno", "100")).build();
        
        String maternalSurname = user.getMaternalSurname();
        if (maternalSurname != null && maternalSurname.length() > 100)
            return Response.status(400).entity(BuyHub.lengthValidationErrorJson("apellido materno", "100")).build();
        
        if (user.getBirthday() == null)
            return Response.status(400).entity(BuyHub.requiredValidationErrorJson("fecha de nacimiento")).build();
        
        Connection connection = BuyHub.getConnection();
        try {
            connection.setAutoCommit(false);
            
            PreparedStatement query = connection.prepareStatement(
                    "INSERT INTO users (email, password, name, paternal_surname, maternal_surname, birthday, phone_number) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?);"
            );
            
            try {
                query.setString(1, user.getEmail());
                query.setString(2, password);
                query.setString(3, name);
                query.setString(4, paternalSurname);
                
                if (maternalSurname != null)
                    query.setString(5, maternalSurname);
                else
                    query.setNull(5, Types.VARCHAR);
                
                query.setTimestamp(6, user.getBirthday());
                
                if (user.getPhoneNumber() != null)
                    query.setLong(7, user.getPhoneNumber());
                else
                    query.setNull(7, Types.BIGINT);
                
                query.execute();
            } finally {
                query.close();
            }
            
            if (user.getPhoto() != null) {
                query = connection.prepareStatement("INSERT INTO user_photo(photo, user_id) VALUES (?, LAST_INSERT_ID())");
                try {
                    query.setBytes(1, user.getPhoto());
                    query.executeUpdate();
                } finally {
                    query.close();
                }
            }
            
            connection.commit();
            return Response.ok().entity(BuyHub.jsonOk()).build();
        } catch (SQLException e) {
            Logger.getLogger(Users.class.getName()).log(Level.SEVERE, null, e);
            
            connection.rollback();
            return Response.status(400).entity(BuyHub.errorJson(e.getMessage())).build();
        } finally {
            connection.setAutoCommit(true);
            connection.close();
        }
    }

    @POST
    @Path("login")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response login(String json) throws SQLException {
        User user = (User) BuyHub.gson.fromJson(json, User.class);
        if (user == null)
            return Response.status(400).entity(BuyHub.errorJson("login method expects a JSON body")).build();
        
        String email = user.getEmail();
        String password = user.getPassword();
        if (email == null || password == null)
            return Response.status(400).entity(BuyHub.errorJson("Se requiere de un correo y una contraseña")).build();

        password = Utilities.hash(password);
        if (password == null)
            return Response.serverError().build();
                
        String token = Utilities.getToken();
        int userId = -1;
        
        try (Connection connection = BuyHub.getConnection()) {
            PreparedStatement query = connection.prepareStatement("SELECT * FROM users WHERE email = ? and password = ?");
            try {
                query.setString(1, email);
                query.setString(2, password);

                try (ResultSet rs = query.executeQuery()) {
                    if (!rs.next())
                        return Response.status(400).entity(BuyHub.errorJson("Correo o contraseña incorrectos")).build();
                    userId = rs.getInt("user_id");
                }
            } finally {
                query.close();
            }
            
            query = connection.prepareStatement("UPDATE users SET session_token = ?, session_expires = NOW() + INTERVAL 30 MINUTE WHERE email = ?;");
            try {
                query.setString(1, token);
                query.setString(2, email);
                query.executeUpdate();
            } finally {
                query.close();
            }
            
            JsonObject responseJson = new JsonObject();
            responseJson.addProperty("session_token", token);
            responseJson.addProperty("user_id", userId);

            return Response.ok().entity(responseJson.toString()).build();
        } catch (SQLException e) {
            Logger.getLogger(Users.class.getName()).log(Level.SEVERE, null, e);
            return Response.status(400).entity(BuyHub.errorJson(e.getMessage())).build();
        }
    }
    
    @POST
    @Path("validate")
    @Produces(MediaType.APPLICATION_JSON)
    public Response validate(@HeaderParam("User-Id") String userId, @HeaderParam("Session-Token") String sessionToken) throws SQLException {
        try (Connection connection = BuyHub.getConnection()) {
            Object result = BuyHub.validateToken(connection, userId, sessionToken);
            if (result != null)
                return (Response) result;
            
            BuyHub.updateToken(connection, sessionToken);
            
            return Response.ok().entity(BuyHub.jsonOk()).build();
        } catch (SQLException e) {
            Logger.getLogger(Users.class.getName()).log(Level.SEVERE, null, e);
            return Response.status(400).entity(BuyHub.errorJson(e.getMessage())).build();
        }
    }
}
