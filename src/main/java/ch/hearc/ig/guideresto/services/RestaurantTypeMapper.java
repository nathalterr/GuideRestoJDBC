package ch.hearc.ig.guideresto.services;

import ch.hearc.ig.guideresto.business.Restaurant;
import ch.hearc.ig.guideresto.business.RestaurantType;
import ch.hearc.ig.guideresto.persistence.AbstractMapper;
import ch.hearc.ig.guideresto.persistence.ConnectionUtils;

import java.sql.*;
import java.util.HashSet;
import java.util.Set;

import static ch.hearc.ig.guideresto.persistence.ConnectionUtils.getConnection;

public class RestaurantTypeMapper extends AbstractMapper<RestaurantType> {

    private final Connection connection;

    public RestaurantTypeMapper() {
        this.connection = getConnection();
    }

    @Override
    public RestaurantType findById(int id) {
        String sql = "SELECT numero, libelle, description FROM TYPES_GASTRONOMIQUES WHERE numero = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new RestaurantType(
                            rs.getInt("numero"),
                            rs.getString("libelle"),
                            rs.getString("description")
                    );
                }
            }
        } catch (SQLException ex) {
            logger.error("findById SQLException: {}", ex.getMessage());
        }
        return null;
    }

    public RestaurantType findByLabel(String label) {
        String sql = "SELECT numero, libelle, description FROM TYPES_GASTRONOMIQUES WHERE libelle = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, label);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new RestaurantType(
                            rs.getInt("numero"),
                            rs.getString("libelle"),
                            rs.getString("description")
                    );
                }
            }
        }catch (SQLException ex) {
            logger.error("findByLabel SQLException: {}", ex.getMessage());
        }
        return null;
    }

    @Override
    public Set<RestaurantType> findAll() {
        Set<RestaurantType> types = new HashSet<>();
        String sql = "SELECT numero, libelle, description FROM TYPES_GASTRONOMIQUES";

        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                types.add(new RestaurantType(
                        rs.getInt("numero"),
                        rs.getString("libelle"),
                        rs.getString("description")
                ));
            }
        } catch (SQLException ex) {
            logger.error("findAll SQLException: {}", ex.getMessage());
        }
        return types;
    }


    @Override
    public RestaurantType create(RestaurantType type) {
        String sql = "BEGIN INSERT INTO TYPES_GASTRONOMIQUES (libelle, description) " +
                "VALUES (?, ?) RETURNING numero INTO ?; END;";

        try (CallableStatement stmt = connection.prepareCall(sql)) {
            stmt.setString(1, type.getLabel());
            stmt.setString(2, type.getDescription());
            stmt.registerOutParameter(3, java.sql.Types.INTEGER);

            stmt.executeUpdate();

            int generatedId = stmt.getInt(3);
            type.setId(generatedId);

            if (!connection.getAutoCommit()) connection.commit();
            return type;

        } catch (SQLException e) {
            // Cas attendu : doublon (ORA-00001)
            if (e.getErrorCode() == 1) { // ORA-00001 unique constraint violated
                logger.info("Type '{}' déjà existant, récupération via findByName()", type.getLabel());
                try {
                    return findByName(type.getLabel());
                } catch (SQLException ex) {
                    logger.error("Erreur findByName après doublon: {}", ex.getMessage());
                }
            } else {
                logger.error("Erreur create RestaurantType: {}", e.getMessage());
            }

            try { connection.rollback(); } catch (SQLException r) {
                logger.error("Rollback failed: {}", r.getMessage());
            }
            return null;
        }
    }


    @Override
    public boolean update(RestaurantType object) {
        String sql = "UPDATE TYPES_GASTRONOMIQUES SET libelle = ?, description = ? WHERE numero = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, object.getLabel());
            stmt.setString(2, object.getDescription());
            stmt.setInt(3, object.getId());
            int affected = stmt.executeUpdate();
            if (!connection.getAutoCommit()) {
                connection.commit();
            }
            return affected > 0;
        } catch (SQLException ex) {
            logger.error("update SQLException: {}", ex.getMessage());
            return false;
        }
    }

    @Override
    public boolean delete(RestaurantType object) {
        return deleteById(object.getId());
    }

    @Override
    public boolean deleteById(int id) {
        String sql = "DELETE FROM TYPES_GASTRONOMIQUES WHERE numero = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            int affected = stmt.executeUpdate();
            if (!connection.getAutoCommit()) {
                connection.commit();
            }
            return affected > 0;
        } catch (SQLException ex) {
            logger.error("deleteById SQLException: {}", ex.getMessage());
            return false;
        }
    }

    @Override
    protected String getSequenceQuery() {
        return "SELECT SEQ_TYPES_GASTRONOMIQUES.NEXTVAL FROM dual";
    }

    @Override
    protected String getExistsQuery() {
        return "SELECT 1 FROM TYPES_GASTRONOMIQUES WHERE numero = ?";
    }

    @Override
    protected String getCountQuery() {
        return "SELECT COUNT(*) FROM TYPES_GASTRONOMIQUES";
    }
    public RestaurantType findByName(String name) throws SQLException {
        String sql = "SELECT numero, libelle, description FROM TYPES_GASTRONOMIQUES WHERE libelle = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, name);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new RestaurantType(
                            rs.getInt("numero"),
                            rs.getString("libelle"),
                            rs.getString("description")
                    );
                }
            }
        } catch (SQLException ex) {
            logger.error("findByName SQLException: {}", ex.getMessage());
            throw ex;
        }
        return null;
    }

    public boolean existsByName(String name) throws SQLException {
        String sql = "SELECT 1 FROM TYPES_GASTRONOMIQUES WHERE libelle = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, name);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next(); // Si une ligne existe → true
            }
        } catch (SQLException ex) {
            logger.error("existsByName SQLException: {}", ex.getMessage());
            throw ex;
        }
    }

}
