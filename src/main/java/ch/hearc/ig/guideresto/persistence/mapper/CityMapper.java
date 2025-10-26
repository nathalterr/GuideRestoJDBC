package ch.hearc.ig.guideresto.persistence.mapper;

import ch.hearc.ig.guideresto.business.City;
import ch.hearc.ig.guideresto.persistence.AbstractMapper;

import java.sql.*;
import java.util.HashSet;
import java.util.Set;

import static ch.hearc.ig.guideresto.persistence.ConnectionUtils.getConnection;
import ch.hearc.ig.guideresto.business.City;
import ch.hearc.ig.guideresto.persistence.AbstractMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.*;

import static ch.hearc.ig.guideresto.persistence.ConnectionUtils.getConnection;

public class CityMapper extends AbstractMapper<City> {

    private static final Logger logger = LoggerFactory.getLogger(CityMapper.class);
    private final Connection connection;
    private final Map<Integer, City> identityMap = new HashMap<>();

    public CityMapper() {
        this.connection = getConnection();
    }

    @Override
    public City findById(int id) {
        // Vérifie le cache d'abord
        if (identityMap.containsKey(id)) {
            return identityMap.get(id);
        }

        String sql = "SELECT numero, code_postal, nom_ville FROM VILLES WHERE numero = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    City city = new City(
                            id,
                            rs.getString("code_postal"),
                            rs.getString("nom_ville")
                    );

                    identityMap.put(id, city);
                    return city;
                }
            }
        } catch (SQLException e) {
            logger.error("Erreur findById City: {}", e.getMessage());
        }

        return null;
    }

    @Override
    public Set<City> findAll() {
        Set<City> cities = new HashSet<>();
        String sql = "SELECT numero, code_postal, nom_ville FROM VILLES";
        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                int id = rs.getInt("numero");
                City city = identityMap.get(id);
                if (city == null) {
                    city = new City(
                            id,
                            rs.getString("code_postal"),
                            rs.getString("nom_ville")
                    );
                    identityMap.put(id, city);
                }
                cities.add(city);
            }
        } catch (SQLException e) {
            logger.error("findAll SQLException: {}", e.getMessage());
        }
        return cities;
    }

    @Override
    public City create(City city) {
        String sql = "BEGIN INSERT INTO VILLES (code_postal, nom_ville) " +
                "VALUES (?, ?) RETURNING numero INTO ?; END;";

        try (CallableStatement stmt = connection.prepareCall(sql)) {
            stmt.setString(1, city.getZipCode());
            stmt.setString(2, city.getCityName());
            stmt.registerOutParameter(3, Types.INTEGER);

            stmt.executeUpdate();
            int generatedId = stmt.getInt(3);
            city.setId(generatedId);
            identityMap.put(generatedId, city);

            if (!connection.getAutoCommit()) connection.commit();
            return city;

        } catch (SQLException e) {
            if (e.getErrorCode() == 1) { // doublon
                try {
                    return findByName(city.getCityName());
                } catch (SQLException ex) {
                    logger.error("Erreur findByName après doublon: {}", ex.getMessage());
                }
            } else {
                logger.error("Erreur create City: {}", e.getMessage());
            }
            try { connection.rollback(); } catch (SQLException r) {
                logger.error("Rollback failed: {}", r.getMessage());
            }
            return null;
        }
    }

    @Override
    public boolean update(City city) {
        String sql = "UPDATE VILLES SET code_postal = ?, nom_ville = ? WHERE numero = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, city.getZipCode());
            stmt.setString(2, city.getCityName());
            stmt.setInt(3, city.getId());
            int updated = stmt.executeUpdate();
            if (!connection.getAutoCommit()) connection.commit();
            if (updated > 0) identityMap.put(city.getId(), city);
            return updated > 0;
        } catch (SQLException e) {
            logger.error("update SQLException: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public boolean delete(City city) {
        return deleteById(city.getId());
    }

    @Override
    public boolean deleteById(int id) {
        String sql = "DELETE FROM VILLES WHERE numero = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            int deleted = stmt.executeUpdate();
            if (!connection.getAutoCommit()) connection.commit();
            if (deleted > 0) identityMap.remove(id);
            return deleted > 0;
        } catch (SQLException e) {
            logger.error("deleteById SQLException: {}", e.getMessage());
            return false;
        }
    }

    @Override
    protected String getSequenceQuery() {
        return "SELECT SEQ_VILLES.NEXTVAL FROM dual";
    }

    @Override
    protected String getExistsQuery() {
        return "SELECT 1 FROM VILLES WHERE numero = ?";
    }

    @Override
    protected String getCountQuery() {
        return "SELECT COUNT(*) FROM VILLES";
    }

    public City findByName(String name) throws SQLException {
        // Vérifie d'abord si la ville est dans le cache
        for (City cachedCity : identityMap.values()) {
            if (cachedCity.getCityName().equalsIgnoreCase(name)) {
                return cachedCity;
            }
        }

        String sql = "SELECT numero, code_postal, nom_ville FROM VILLES WHERE nom_ville = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, name);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int id = rs.getInt("numero");

                    // Vérifie encore le cache au cas où
                    if (identityMap.containsKey(id)) return identityMap.get(id);

                    City city = new City(
                            id,
                            rs.getString("code_postal"),
                            rs.getString("nom_ville")
                    );

                    identityMap.put(id, city);
                    return city;
                }
            }
        } catch (SQLException e) {
            logger.error("Erreur findByName City: {}", e.getMessage());
            throw e;
        }

        return null;
    }

    public City findByZipCode(String zipCode) throws SQLException {
        String sql = "SELECT numero, code_postal, nom_ville FROM VILLES WHERE code_postal = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, zipCode);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int id = rs.getInt("numero");
                    if (identityMap.containsKey(id)) return identityMap.get(id);
                    City city = new City(
                            id,
                            rs.getString("code_postal"),
                            rs.getString("nom_ville")
                    );
                    identityMap.put(id, city);
                    return city;
                }
            }
        }
        return null;
    }

    public boolean existsByName(String name) throws SQLException {
        String sql = "SELECT 1 FROM VILLES WHERE nom_ville = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, name);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }
}
