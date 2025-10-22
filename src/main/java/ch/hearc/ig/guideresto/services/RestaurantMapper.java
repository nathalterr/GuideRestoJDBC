package ch.hearc.ig.guideresto.services;

import ch.hearc.ig.guideresto.business.City;
import ch.hearc.ig.guideresto.business.Restaurant;
import ch.hearc.ig.guideresto.business.RestaurantType;
import ch.hearc.ig.guideresto.persistence.AbstractMapper;
import ch.hearc.ig.guideresto.persistence.ConnectionUtils;

import java.sql.*;
import java.util.HashSet;
import java.util.Set;

public class RestaurantMapper extends AbstractMapper<Restaurant> {

    private Connection connection;

    public RestaurantMapper(Connection connection) {
        this.connection = connection;
    }

    @Override
    public Restaurant findById(int id) {
        String sql = "SELECT numero, nom, description, site_web, adresse, fk_type, fk_vill FROM RESTAURANTS WHERE numero = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    // On récupère le type et la ville via leurs mappers
                    RestaurantType type = new RestaurantTypeMapper(connection).findById(rs.getInt("fk_type"));
                    City city = new CityMapper(connection).findById(rs.getInt("fk_vill"));
                    return new Restaurant(
                            rs.getInt("numero"),
                            rs.getString("nom"),
                            rs.getString("description"),
                            rs.getString("site_web"),
                            rs.getString("adresse"),
                            city,
                            type
                    );
                }
            }
        } catch (SQLException e) {
            logger.error("Erreur findById Restaurant: {}", e.getMessage());
        }
        return null;
    }

    public Restaurant findByName(String name) {
        String sql = "SELECT numero, nom, description, site_web, adresse, fk_type, fk_vill FROM RESTAURANTS WHERE nom = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, name);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    RestaurantType type = new RestaurantTypeMapper(connection).findById(rs.getInt("fk_type"));
                    City city = new CityMapper(connection).findById(rs.getInt("fk_vill"));
                    return new Restaurant(
                            rs.getInt("numero"),
                            rs.getString("nom"),
                            rs.getString("description"),
                            rs.getString("site_web"),
                            rs.getString("adresse"),
                            city,
                            type
                    );
                }
            }
        } catch (SQLException e) {
            logger.error("Erreur findByName Restaurant: {}", e.getMessage());
        }
        return null;
    }

    @Override
    public Set<Restaurant> findAll() {
        Set<Restaurant> restaurants = new HashSet<>();
        String sql = "SELECT numero, nom, description, site_web, adresse, fk_type, fk_vill FROM RESTAURANTS";
        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                RestaurantType type = new RestaurantTypeMapper(connection).findById(rs.getInt("fk_type"));
                City city = new CityMapper(connection).findById(rs.getInt("fk_vill"));
                Restaurant r = new Restaurant(
                        rs.getInt("numero"),
                        rs.getString("nom"),
                        rs.getString("description"),
                        rs.getString("site_web"),
                        rs.getString("adresse"),
                        city,
                        type
                );
                restaurants.add(r);
            }
        } catch (SQLException e) {
            logger.error("Erreur findAll Restaurant: {}", e.getMessage());
        }
        return restaurants;
    }

    @Override
    public Restaurant create(Restaurant restaurant) {
        String insertSql = "INSERT INTO RESTAURANTS (numero, nom, description, site_web, adresse, fk_type, fk_vill) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try {
            if (restaurant.getId() == null) {
                restaurant.setId(getSequenceValue());
            }
            try (PreparedStatement stmt = connection.prepareStatement(insertSql)) {
                stmt.setInt(1, restaurant.getId());
                stmt.setString(2, restaurant.getName());
                stmt.setString(3, restaurant.getDescription());
                stmt.setString(4, restaurant.getWebsite());
                stmt.setString(5, restaurant.getAddress().getStreet());
                stmt.setInt(6, restaurant.getType().getId());
                stmt.setInt(7, restaurant.getAddress().getCity().getId());
                stmt.executeUpdate();
            }

            if (!connection.getAutoCommit()) connection.commit();
            return restaurant;
        } catch (SQLException e) {
            logger.error("Erreur create Restaurant: {}", e.getMessage());
            try { connection.rollback(); } catch (SQLException ex) { logger.error("Rollback failed: {}", ex.getMessage()); }
        }
        return null;
    }

    @Override
    public boolean update(Restaurant restaurant) {
        String sql = "UPDATE RESTAURANTS SET nom = ?, description = ?, site_web = ?, adresse = ?, fk_type = ?, fk_vill = ? WHERE numero = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, restaurant.getName());
            stmt.setString(2, restaurant.getDescription());
            stmt.setString(3, restaurant.getWebsite());
            stmt.setString(4, restaurant.getAddress().getStreet());
            stmt.setInt(5, restaurant.getType().getId());
            stmt.setInt(6, restaurant.getAddress().getCity().getId());
            stmt.setInt(7, restaurant.getId());
            int rows = stmt.executeUpdate();
            if (!connection.getAutoCommit()) connection.commit();
            return rows > 0;
        } catch (SQLException e) {
            logger.error("Erreur update Restaurant: {}", e.getMessage());
            try { connection.rollback(); } catch (SQLException ex) { logger.error("Rollback failed: {}", ex.getMessage()); }
            return false;
        }
    }

    @Override
    public boolean delete(Restaurant restaurant) {
        return deleteById(restaurant.getId());
    }

    @Override
    public boolean deleteById(int id) {
        String sql = "DELETE FROM RESTAURANTS WHERE numero = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            int rows = stmt.executeUpdate();
            if (!connection.getAutoCommit()) connection.commit();
            return rows > 0;
        } catch (SQLException e) {
            logger.error("Erreur delete Restaurant: {}", e.getMessage());
            try { connection.rollback(); } catch (SQLException ex) { logger.error("Rollback failed: {}", ex.getMessage()); }
            return false;
        }
    }

    @Override
    protected String getSequenceQuery() {
        return "SELECT SEQ_RESTAURANTS.NEXTVAL FROM dual";
    }

    @Override
    protected String getExistsQuery() {
        return "SELECT 1 FROM RESTAURANTS WHERE numero = ?";
    }

    @Override
    protected String getCountQuery() {
        return "SELECT COUNT(*) FROM RESTAURANTS";
    }
}
