package ch.hearc.ig.guideresto.services;

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
    public RestaurantType create(RestaurantType object) {
        String seqSql = getSequenceQuery();
        try (PreparedStatement seqStmt = connection.prepareStatement(seqSql);
             ResultSet rs = seqStmt.executeQuery()) {
            if (rs.next()) {
                int id = rs.getInt(1);
                object.setId(id);
            }
        } catch (SQLException ex) {
            logger.error("create sequence SQLException: {}", ex.getMessage());
        }

        String insertSql = "INSERT INTO TYPES_GASTRONOMIQUES (numero, libelle, description) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(insertSql)) {
            stmt.setInt(1, object.getId());
            stmt.setString(2, object.getLabel());
            stmt.setString(3, object.getDescription());
            stmt.executeUpdate();
            if (!connection.getAutoCommit()) {
                connection.commit();
            }
        } catch (SQLException ex) {
            logger.error("create insert SQLException: {}", ex.getMessage());
        }

        return object;
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
}
