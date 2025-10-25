package ch.hearc.ig.guideresto.persistence.mapper;

import ch.hearc.ig.guideresto.business.City;
import ch.hearc.ig.guideresto.persistence.AbstractMapper;

import java.sql.*;
import java.util.HashSet;
import java.util.Set;

import static ch.hearc.ig.guideresto.persistence.ConnectionUtils.getConnection;

public class CityMapper extends AbstractMapper<City> {

    private final Connection connection;

    public CityMapper() {
        this.connection = getConnection();
    }

    @Override
    public City findById(int id) {
        String sql = "SELECT numero, code_postal, nom_ville FROM VILLES WHERE numero = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new City(
                            rs.getInt("numero"),
                            rs.getString("code_postal"),
                            rs.getString("nom_ville")
                    );
                }
            }
        } catch (SQLException e) {
            logger.error("findById SQLException: {}", e.getMessage());
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
                cities.add(new City(
                        rs.getInt("numero"),
                        rs.getString("code_postal"),
                        rs.getString("nom_ville")
                ));
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
            stmt.registerOutParameter(3, java.sql.Types.INTEGER);

            stmt.executeUpdate();

            int generatedId = stmt.getInt(3);
            city.setId(generatedId);

            if (!connection.getAutoCommit()) connection.commit();
            return city;

        } catch (SQLException e) {
            if (e.getErrorCode() == 1) { // Doublon
                logger.info("Ville '{}' déjà existante, récupération via findByName()", city.getCityName());
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
            return updated > 0;
        } catch (SQLException e) {
            logger.error("update SQLException: {}", e.getMessage());
        }
        return false;
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
            return deleted > 0;
        } catch (SQLException e) {
            logger.error("deleteById SQLException: {}", e.getMessage());
        }
        return false;
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

    // ✅ Trouve une ville par nom
    public City findByName(String name) throws SQLException {
        String sql = "SELECT numero, code_postal, nom_ville FROM VILLES WHERE nom_ville = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, name);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new City(
                            rs.getInt("numero"),
                            rs.getString("code_postal"),
                            rs.getString("nom_ville")
                    );
                }
            }
        }
        return null;
    }

    //Trouve une ville par code
    public City finByZipCode(String zipCode) throws SQLException {
        String sql = "SELECT numero, code_postal, nom_ville FROM VILLES WHERE code_postal = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, zipCode);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new City(
                            rs.getInt("numero"),
                            rs.getString("code_postal"),
                            rs.getString("nom_ville")
                    );
                }
            }
        }return null;
    }


    // ✅ Vérifie l'existence par nom (sans charger toute la ville)
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
