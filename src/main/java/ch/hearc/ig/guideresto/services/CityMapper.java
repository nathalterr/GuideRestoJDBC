package ch.hearc.ig.guideresto.services;

import ch.hearc.ig.guideresto.business.City;
import ch.hearc.ig.guideresto.persistence.AbstractMapper;
import ch.hearc.ig.guideresto.persistence.ConnectionUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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
        String seqSql = getSequenceQuery();
        try (PreparedStatement seqStmt = connection.prepareStatement(seqSql);
             ResultSet rs = seqStmt.executeQuery()) {
            if (rs.next()) {
                city.setId(rs.getInt(1));
            }
        } catch (SQLException e) {
            logger.error("create sequence SQLException: {}", e.getMessage());
        }

        String insertSql = "INSERT INTO VILLES (numero, code_postal, nom_ville) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(insertSql)) {
            stmt.setInt(1, city.getId());
            stmt.setString(2, city.getZipCode());
            stmt.setString(3, city.getCityName());
            stmt.executeUpdate();
            if (!connection.getAutoCommit()) connection.commit();
        } catch (SQLException e) {
            logger.error("create insert SQLException: {}", e.getMessage());
        }
        return city;
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
}
