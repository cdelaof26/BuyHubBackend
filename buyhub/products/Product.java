package buyhub.products;

import java.sql.SQLException;
import java.sql.ResultSet;

/**
 * @author cristopher
 */
public class Product {
    private Integer productId;
    private String name;
    private String description;
    private Float price;
    private Integer availableStock;
    private byte [] photo;

    public Integer getProductId() {
        return productId;
    }

    public void setProductId(Integer productId) {
        this.productId = productId;
    }

    public String getName() {
        if (name != null && name.trim().equals(""))
            return null;
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        if (description != null && description.trim().equals(""))
            return null;
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Float getPrice() {
        return price;
    }

    public void setPrice(Float price) {
        this.price = price;
    }

    public Integer getAvailableStock() {
        return availableStock;
    }

    public void setAvailableStock(Integer availableStock) {
        this.availableStock = availableStock;
    }

    public byte[] getPhoto() {
        return photo;
    }

    public void setPhoto(byte[] photo) {
        this.photo = photo;
    }
    
    public static Product mapResultSetToProduct(ResultSet rs) throws SQLException {
        Product user = new Product();
        user.setProductId(rs.getInt("product_id"));
        user.setName(rs.getString("name"));
        user.setDescription(rs.getString("description"));
        user.setPrice(rs.getFloat("price"));
        user.setAvailableStock(rs.getInt("available"));
        try { user.setPhoto(rs.getBytes("photo")); } catch (SQLException e) { }
        return user;
    }
}
