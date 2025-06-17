package buyhub.products;

import buyhub.BuyHub;
import buyhub.users.Users;
import java.sql.SQLException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;


// La URL del servicio web es http://localhost:8080/buyhub/rest/products
// Métodos
//   POST
//     /
//   POST - Require [User-Id] and [Session-Token] header parameters
//     create
//     addToBag

/**
 * @author cristopher
 */
@Path("products")
public class Products {
    @POST
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getProducts(String json) {
        ProductParams productParams = (ProductParams) BuyHub.gson.fromJson(json, ProductParams.class);
        String search = null;
        if (productParams != null)
            search = productParams.search;
        
        String sentence = "SELECT s.product_id, s.name, s.description, s.price, s.available, fa.photo_id, fa.photo FROM buyhub_db.stock AS s LEFT JOIN buyhub_db.product_photo AS fa ON s.product_id = fa.product_id ";
        if (search == null || search.trim().equals(""))
            sentence += "ORDER BY s.product_id;";
        else
            sentence += "WHERE s.name LIKE '%?%' ORDER BY s.product_id;";
        
        try (Connection connection = BuyHub.getConnection()) {
            PreparedStatement query = connection.prepareStatement(sentence);
            try {
                if (search != null)
                    query.setString(1, search);

                ArrayList<String> products = new ArrayList<>();
                try (ResultSet rs = query.executeQuery()) {
                    while (rs.next())
                        products.add(
                            BuyHub.gson.toJson(Product.mapResultSetToProduct(rs))
                        );
                }
                
                return Response.ok().entity(BuyHub.gson.toJson(products)).build();
            } finally {
                query.close();
            }
        } catch (SQLException e) {
            Logger.getLogger(Users.class.getName()).log(Level.SEVERE, null, e);
            return Response.status(400).entity(BuyHub.errorJson(e.getMessage())).build();
        }
    }
    
    @POST
    @Path("create")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createProduct(String json, @HeaderParam("User-Id") String userId, @HeaderParam("Session-Token") String sessionToken) throws SQLException {
        if (userId == null || sessionToken == null)
            return Response.status(400).entity(BuyHub.errorJson("Se requiere de una sesión activa")).build();
        
        Product product;
        try {
            product = (Product) BuyHub.gson.fromJson(json, Product.class);
        } catch (NumberFormatException e) {
            Logger.getLogger(Users.class.getName()).log(Level.SEVERE, null, e);
            return Response.status(400).entity(BuyHub.errorJson("Campos precio unitario o cantidad disponible no cuentan con un valor adecuado")).build();
        }
        
        if (product == null)
            return Response.status(400).entity(BuyHub.errorJson("create product method expects a JSON body")).build();
        
        String name = product.getName();
        if (name == null)
            return Response.status(400).entity(BuyHub.requiredValidationErrorJson("nombre")).build();
        if (name.length() > 64)
            return Response.status(400).entity(BuyHub.lengthValidationErrorJson("nombre", "64")).build();
        
        String description = product.getDescription();
        if (description == null)
            return Response.status(400).entity(BuyHub.requiredValidationErrorJson("descripción")).build();
        if (description.length() > 512)
            return Response.status(400).entity(BuyHub.lengthValidationErrorJson("descripción", "512")).build();
        
        Float price = product.getPrice();
        if (price == null)
            return Response.status(400).entity(BuyHub.requiredValidationErrorJson("precio")).build();
        if (price <= 0)
            return Response.status(400).entity(BuyHub.errorJson("El campo precio debe contener un número positivo")).build();
        
        Integer available = product.getAvailableStock();
        if (available == null)
            return Response.status(400).entity(BuyHub.requiredValidationErrorJson("cantidad")).build();
        if (available < 0)
            return Response.status(400).entity(BuyHub.errorJson("El campo cantidad debe contener un número entero positivo")).build();
        
        Connection connection = BuyHub.getConnection();
        try {
            Object validation = BuyHub.validateToken(connection, userId, sessionToken);
            if (validation != null)
                return (Response) validation;
            
            connection.setAutoCommit(false);
            
            BuyHub.updateToken(connection, sessionToken);
            
            PreparedStatement query = connection.prepareStatement(
                    "INSERT INTO stock (name, description, price, available) "
                            + "VALUES (?, ?, ?, ?)"
            );
            
            try {
                query.setString(1, name);
                query.setString(2, description);
                query.setFloat(3, price);
                query.setInt(4, available);

                query.execute();
            } finally {
                query.close();
            }
            
//            if (product.getPhoto() == null)
//                throw new SQLException("No photo :(");
            
            if (product.getPhoto() != null) {
                query = connection.prepareStatement("INSERT INTO product_photo(photo, product_id) VALUES (?, LAST_INSERT_ID())");
                try {
                    query.setBytes(1, product.getPhoto());
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
    @Path("addToBag")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response addToBag(String json, @HeaderParam("User-Id") String userId, @HeaderParam("Session-Token") String sessionToken) throws SQLException {
        if (userId == null || sessionToken == null)
            return Response.status(400).entity(BuyHub.errorJson("Se requiere de una sesión activa")).build();
        
        Item product = (Item) BuyHub.gson.fromJson(json, Item.class);
        if (product == null)
            return Response.status(400).entity(BuyHub.errorJson("addToBag method expects a JSON body")).build();
        
        if (product.id == null)
            return Response.status(400).entity(BuyHub.requiredValidationErrorJson("id del producto")).build();
        
        Integer amount = product.amount;
        if (amount == null)
            return Response.status(400).entity(BuyHub.requiredValidationErrorJson("cantidad")).build();
        if (amount < 0)
            return Response.status(400).entity(BuyHub.errorJson("El campo cantidad debe contener un número entero positivo")).build();
        
        Connection connection = BuyHub.getConnection();
        try {
            Object validation = BuyHub.validateToken(connection, userId, sessionToken);
            if (validation != null)
                return (Response) validation;
            
            PreparedStatement query = connection.prepareStatement("SELECT available FROM stock WHERE product_id = ?;");
            int available;
            
            try {
                query.setInt(1, product.id);
                ResultSet rs = query.executeQuery();
                if (!rs.next())
                    return Response.status(400).entity(BuyHub.errorJson("El producto no existe!?")).build();
                
                available = rs.getInt(1);
                if (available < amount)
                    return Response.status(400).entity(BuyHub.errorJson("No hay suficientes articulos")).build();
            } finally {
                query.close();
            }
            
            connection.setAutoCommit(false);
            
            BuyHub.updateToken(connection, sessionToken);
            
            query = connection.prepareStatement("INSERT INTO bag (user_id, product_id, amount) VALUES (?, ?, ?)");
            
            try {
                query.setString(1, userId);
                query.setInt(2, product.id);
                query.setInt(3, amount);
                
                query.execute();
            } finally {
                query.close();
            }
            
            query = connection.prepareStatement("UPDATE stock SET available = ? WHERE product_id = ?;");
            
            try {
                query.setInt(1, available - amount);
                query.setInt(2, product.id);
                
                query.execute();
            } finally {
                query.close();
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
}
