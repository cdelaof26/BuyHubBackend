package buyhub.users;

import buyhub.BuyHub;
import buyhub.Utilities;
import java.sql.SQLException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Types;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

// La URL del servicio web es http://localhost:8080/buyhub/rest/
// Métodos
//   POST
//     login
//     signup

@Path("/")
public class Users {    
    @POST
    @Path("signup")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response signup(String json) throws SQLException {
        ParamUser p = (ParamUser) BuyHub.gson.fromJson(json, ParamUser.class);
        String password = p.getPassword();
        if (p.getEmail() == null || password == null)
            return Response.status(400).entity(BuyHub.gson.toJson(new Error("Se requiere de un correo y una contraseña"))).build();

        password = Utilities.hash(password);
        if (password == null)
            return Response.serverError().build();
        
        if (p.getName() == null)
            return Response.status(400).entity(BuyHub.gson.toJson(new Error("El campo nombre es obligatorio"))).build();
        
        if (p.getPaternalSurname() == null)
            return Response.status(400).entity(BuyHub.gson.toJson(new Error("El campo apellido paterno es obligatorio"))).build();
        
        if (p.getBirthday() == null)
            return Response.status(400).entity(BuyHub.gson.toJson(new Error("El campo fecha de nacimiento es obligatorio"))).build();
        
        Connection connection = BuyHub.getConnection();
        try {
            connection.setAutoCommit(false);
            
            PreparedStatement query = connection.prepareStatement(
                    "INSERT INTO usuarios (email, nombre, apellido_paterno, apellido_materno, fecha_nacimiento, telefono, password) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?);"
            );
            
            try {
                query.setString(1, p.getEmail());
                query.setString(2, p.getName());
                query.setString(3, p.getPaternalSurname());
                
                if (p.getMaternalSurname() != null)
                    query.setString(4, p.getMaternalSurname());
                else
                    query.setNull(4, Types.VARCHAR);
                
                query.setTimestamp(5, p.getBirthday());
                
                if (p.getPhoneNumber() != null)
                    query.setLong(6, p.getPhoneNumber());
                else
                    query.setNull(6, Types.BIGINT);
                
                query.setString(7, password);
                query.execute();
            } finally {
                query.close();
            }
            
            if (p.getPhoto() != null) {
                query = connection.prepareStatement("INSERT INTO fotos_usuarios(foto, id_usuario) VALUES (?, LAST_INSERT_ID())");
                try {
                    query.setBytes(1, p.getPhoto());
                    query.executeUpdate();
                } finally {
                    query.close();
                }
            }
            
            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            return Response.status(400).entity(BuyHub.gson.toJson(new Error(e.getMessage()))).build();
        } finally {
            connection.setAutoCommit(true);
            connection.close();
        }
        
        return Response.ok().entity("{\"message\": \"ok\"}").build();
    }

    @POST
    @Path("login")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response login(String json) throws SQLException {
        ParamUser p = (ParamUser) BuyHub.gson.fromJson(json, ParamUser.class);
        String email = p.getEmail();
        String password = p.getPassword();
        if (email == null || password == null)
            return Response.status(400).entity(BuyHub.gson.toJson(new Error("Se requiere de un correo y una contraseña"))).build();

        password = Utilities.hash(password);
        if (password == null)
            return Response.serverError().build();
                
        String token = Utilities.getToken();
        
        try (Connection connection = BuyHub.getConnection()) {
            PreparedStatement query = connection.prepareStatement("SELECT 1 FROM usuarios WHERE email=? and password=?");
            try {
                query.setString(1, email);
                query.setString(2, password);

                try (ResultSet rs = query.executeQuery()) {
                    if (!rs.next())
                        return Response.status(400).entity(BuyHub.gson.toJson(new Error("Correo o contraseña incorrectos"))).build();
                }
            } finally {
                query.close();
            }
            
            query = connection.prepareStatement("UPDATE usuarios SET token = ?, token_expiration = NOW() + INTERVAL 30 MINUTE WHERE email = ?;");
            try {
                query.setString(1, token);
                query.setString(2, email);
                query.executeUpdate();
            } finally {
                query.close();
            }
        } catch (SQLException e) {
            return Response.status(400).entity(BuyHub.gson.toJson(new Error(e.getMessage()))).build();
        }
        
        String responseJson = String.format("{\"token\": \"%s\"}", token);
        return Response.ok().entity(responseJson).build();
    }
}
