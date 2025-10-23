package ch.hearc.ig.guideresto.services;

import ch.hearc.ig.guideresto.business.*;
import ch.hearc.ig.guideresto.persistence.AbstractMapper;
import ch.hearc.ig.guideresto.persistence.ConnectionUtils;

import java.sql.*;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import static ch.hearc.ig.guideresto.persistence.ConnectionUtils.getConnection;

public class RestaurantMapper extends AbstractMapper<Restaurant> {

    private Connection connection;

    public RestaurantMapper() {
        this.connection = getConnection();
    }

    @Override
    public Restaurant findById(int id) {
        String sql = "SELECT numero, nom, description, site_web, adresse, fk_type, fk_vill FROM RESTAURANTS WHERE numero = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    // On récupère le type et la ville via leurs mappers
                    RestaurantType type = new RestaurantTypeMapper().findById(rs.getInt("fk_type"));
                    City city = new CityMapper().findById(rs.getInt("fk_vill"));
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

    public Set<Restaurant> findByName(String partialName) throws SQLException {
        Set<Restaurant> restaurants = new LinkedHashSet<>();
        String sql = "SELECT numero, nom, description, site_web, fk_vill, fk_type, adresse FROM RESTAURANTS WHERE LOWER(nom) LIKE LOWER(?)";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, "%" + partialName + "%"); // "contient"
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    City city = new CityMapper().findById(rs.getInt("fk_vill"));
                    RestaurantType type = new RestaurantTypeMapper().findById(rs.getInt("fk_type"));
                    restaurants.add(new Restaurant(
                            rs.getInt("numero"),
                            rs.getString("nom"),
                            rs.getString("description"),
                            rs.getString("site_web"),
                            new Localisation(rs.getString("adresse"), city),
                            type
                    ));
                }
            }
        }
        return restaurants;
    }

    public Restaurant findByCity(String cityName) {
        String sql = "SELECT r.numero, r.nom, r.description, r.site_web, r.adresse, r.fk_type, r.fk_vill FROM RESTAURANTS r INNER JOIN VILLES v ON r.fk_vill = v.id WHERE v.nom_vill = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, cityName);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    RestaurantType type = new RestaurantTypeMapper().findById(rs.getInt("fk_type"));
                    City city = new CityMapper().findById(rs.getInt("fk_vill"));
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
            }catch (SQLException e) {
                logger.error("Erreur findByCity Restaurant: {}", e.getMessage());
            }
            return null;
            } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Restaurant findByRestaurantType(String label) throws SQLException {
        String sql = "SELECT r.numero, r.nom, r.description, r.site_web, r.adresse, r.fk_type, r.fk_vill FROM RESTAURANTS r INNER JOIN TYPES_GASTRONOMIQUES t ON r.fk_type = t.numero WHERE t.libelle = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, label);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    RestaurantType type = new RestaurantTypeMapper().findById(rs.getInt("fk_type"));
                    City city = new CityMapper().findById(rs.getInt("fk_vill"));
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
            } catch (SQLException e) {
                logger.error("Erreur findByRestaurantType Restaurant: {}", e.getMessage());
            }
            return null;
        }
        }

    @Override
    public Set<Restaurant> findAll() {
        Set<Restaurant> restaurants = new HashSet<>();
        String sql = "SELECT numero, nom, description, site_web, adresse, fk_type, fk_vill FROM RESTAURANTS";
        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                RestaurantType type = new RestaurantTypeMapper().findById(rs.getInt("fk_type"));
                City city = new CityMapper().findById(rs.getInt("fk_vill"));
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
        try {
            int restId = restaurant.getId();

            // 1️⃣ Supprimer les Grades liés aux CompleteEvaluations de ce restaurant
            CompleteEvaluationMapper completeEvalMapper = new CompleteEvaluationMapper();
            GradeMapper gradeMapper = new GradeMapper();
            BasicEvaluationMapper basicEvalMapper = new BasicEvaluationMapper();

            for (CompleteEvaluation eval : completeEvalMapper.findByRestaurant(restaurant)) {
                for (Grade grade : gradeMapper.findByCompleteEvaluation(eval)) {
                    gradeMapper.delete(grade);
                }
                completeEvalMapper.delete(eval);
            }

            // 2️⃣ Supprimer tous les Likes du restaurant
            for (BasicEvaluation like : basicEvalMapper.findByRestaurant(restaurant)) {
                basicEvalMapper.delete(like);
            }

            // 3️⃣ Supprimer le restaurant lui-même
            return deleteById(restId);

        } catch (Exception ex) {
            logger.error("Erreur delete restaurant complet : {}", ex.getMessage());
            return false;
        }
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

    public boolean updateAddress(Restaurant restaurant, String newStreet, City newCity) throws SQLException {
        // 1️⃣ Update de la rue
        restaurant.getAddress().setStreet(newStreet);

        // 2️⃣ Si la ville change, mettre à jour l'objet en mémoire
        if (newCity != null && newCity != restaurant.getAddress().getCity()) {
            restaurant.getAddress().getCity().getRestaurants().remove(restaurant); // Ancienne ville
            restaurant.getAddress().setCity(newCity);
            newCity.getRestaurants().add(restaurant); // Nouvelle ville
        }

        // 3️⃣ Update en base
        String sql = "UPDATE RESTAURANTS SET adresse = ?, fk_vill = ? WHERE numero = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, restaurant.getAddress().getStreet());
            stmt.setInt(2, restaurant.getAddress().getCity().getId());
            stmt.setInt(3, restaurant.getId());
            int rows = stmt.executeUpdate();
            if (!connection.getAutoCommit()) connection.commit();
            return rows > 0;
        }
    }

}
