package ch.hearc.ig.guideresto.persistence.mapper;

import ch.hearc.ig.guideresto.business.RestaurantType;
import ch.hearc.ig.guideresto.persistence.AbstractMapper;

import java.sql.*;
import java.util.HashSet;
import java.util.Set;

import static ch.hearc.ig.guideresto.persistence.ConnectionUtils.getConnection;
import ch.hearc.ig.guideresto.business.RestaurantType;
import ch.hearc.ig.guideresto.persistence.AbstractMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.*;

import static ch.hearc.ig.guideresto.persistence.ConnectionUtils.getConnection;

public class RestaurantTypeMapper extends AbstractMapper<RestaurantType> {

    private static final Logger logger = LoggerFactory.getLogger(RestaurantTypeMapper.class);
    private final Connection connection;
    private final Map<Integer, RestaurantType> identityMap = new HashMap<>();

    public RestaurantTypeMapper() {
        this.connection = getConnection();
    }

    @Override
    public RestaurantType findById(int id) {
        // 🔹 Vérifie d'abord dans le cache
        if (identityMap.containsKey(id)) {
            logger.info("⚡ RestaurantType {} récupéré depuis l'Identity Map", id);
            return identityMap.get(id);
        }

        String sql = "SELECT numero, libelle, description FROM TYPES_GASTRONOMIQUES WHERE numero = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    RestaurantType type = new RestaurantType(
                            rs.getInt("numero"),
                            rs.getString("libelle"),
                            rs.getString("description")
                    );

                    // Ajout dans le cache
                    identityMap.put(type.getId(), type);
                    return type;
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
                    int id = rs.getInt("numero");

                    // 🔹 Vérifie le cache avant de créer un nouvel objet
                    if (identityMap.containsKey(id)) {
                        logger.info("⚡ RestaurantType '{}' récupéré depuis l'Identity Map", label);
                        return identityMap.get(id);
                    }

                    RestaurantType type = new RestaurantType(
                            id,
                            rs.getString("libelle"),
                            rs.getString("description")
                    );

                    identityMap.put(id, type);
                    return type;
                }
            }
        } catch (SQLException ex) {
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
                int id = rs.getInt("numero");

                RestaurantType type = identityMap.get(id);
                if (type == null) {
                    type = new RestaurantType(
                            id,
                            rs.getString("libelle"),
                            rs.getString("description")
                    );
                    identityMap.put(id, type);
                }

                types.add(type);
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
            // 🔹 Paramètres de l'insertion
            stmt.setString(1, type.getLabel());
            stmt.setString(2, type.getDescription());
            stmt.registerOutParameter(3, Types.INTEGER); // ID généré

            // 🔹 Exécution
            stmt.executeUpdate();

            // 🔹 Récupération de l'ID généré et mise à jour de l'objet
            int generatedId = stmt.getInt(3);
            type.setId(generatedId);

            // 🔹 Ajout au cache
            identityMap.put(generatedId, type);

            // 🔹 Commit si nécessaire
            if (!connection.getAutoCommit()) connection.commit();

            return type;

        } catch (SQLException e) {
            // Gestion d'un doublon
            if (e.getErrorCode() == 1) { // ORA-00001: doublon
                logger.info("Type '{}' déjà existant, récupération via findByName()", type.getLabel());
                try {
                    return findByName(type.getLabel());
                } catch (SQLException ex) {
                    logger.error("Erreur findByName après doublon: {}", ex.getMessage());
                }
            } else {
                logger.error("Erreur create RestaurantType: {}", e.getMessage());
            }

            // 🔹 Rollback si erreur
            try {
                connection.rollback();
            } catch (SQLException r) {
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

            if (affected > 0) {
                identityMap.put(object.getId(), object); // 🔹 Mise à jour du cache
            }

            if (!connection.getAutoCommit()) connection.commit();
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

            if (affected > 0) {
                identityMap.remove(id); // 🔹 Supprimer du cache
            }

            if (!connection.getAutoCommit()) connection.commit();
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
                    int id = rs.getInt("numero");

                    if (identityMap.containsKey(id)) {
                        return identityMap.get(id);
                    }

                    RestaurantType type = new RestaurantType(
                            id,
                            rs.getString("libelle"),
                            rs.getString("description")
                    );

                    identityMap.put(id, type);
                    return type;
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
                return rs.next();
            }
        } catch (SQLException ex) {
            logger.error("existsByName SQLException: {}", ex.getMessage());
            throw ex;
        }
    }
}
