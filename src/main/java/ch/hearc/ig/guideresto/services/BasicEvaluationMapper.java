package ch.hearc.ig.guideresto.services;

import ch.hearc.ig.guideresto.business.BasicEvaluation;
import ch.hearc.ig.guideresto.business.Restaurant;
import ch.hearc.ig.guideresto.persistence.AbstractMapper;
import java.sql.*;
import java.util.HashSet;
import java.util.Set;

public class BasicEvaluationMapper extends AbstractMapper<BasicEvaluation> {

    private final Connection connection;
    private final RestaurantMapper restaurantMapper;

    public BasicEvaluationMapper(Connection connection) {
        this.connection = connection;
        this.restaurantMapper = new RestaurantMapper(connection);
    }

    @Override
    public BasicEvaluation findById(int id) {
        String sql = "SELECT numero, date_eval, appreciation, adresse_ip, fk_rest FROM LIKES WHERE numero = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Restaurant restaurant = restaurantMapper.findById(rs.getInt("fk_rest"));
                    Boolean like = "Y".equalsIgnoreCase(rs.getString("appreciation"));
                    return new BasicEvaluation(
                            rs.getInt("numero"),
                            rs.getDate("date_eval"),
                            restaurant,
                            like,
                            rs.getString("adresse_ip")
                    );
                }
            }
        } catch (SQLException ex) {
            logger.error("SQLException in findById: {}", ex.getMessage());
        }
        return null;
    }

    @Override
    public Set<BasicEvaluation> findAll() {
        Set<BasicEvaluation> likes = new HashSet<>();
        String sql = "SELECT numero, date_eval, appreciation, adresse_ip, fk_rest FROM LIKES";
        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Restaurant restaurant = restaurantMapper.findById(rs.getInt("fk_rest"));
                Boolean like = "Y".equalsIgnoreCase(rs.getString("appreciation"));
                likes.add(new BasicEvaluation(
                        rs.getInt("numero"),
                        rs.getDate("date_eval"),
                        restaurant,
                        like,
                        rs.getString("adresse_ip")
                ));
            }

        } catch (SQLException ex) {
            logger.error("SQLException in findAll: {}", ex.getMessage());
        }
        return likes;
    }

    @Override
    public BasicEvaluation create(BasicEvaluation object) {
        String seqSql = "SELECT SEQ_EVAL.NEXTVAL FROM dual";
        try (PreparedStatement seqStmt = connection.prepareStatement(seqSql);
             ResultSet rs = seqStmt.executeQuery()) {

            if (rs.next()) {
                object.setId(rs.getInt(1));
            }
        } catch (SQLException ex) {
            logger.error("SQLException in create (sequence): {}", ex.getMessage());
        }

        String insertSql = "INSERT INTO LIKES (numero, date_eval, appreciation, adresse_ip, fk_rest) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(insertSql)) {
            stmt.setInt(1, object.getId());
            stmt.setDate(2, new java.sql.Date(object.getVisitDate().getTime()));
            stmt.setString(3, object.getLikeRestaurant() != null && object.getLikeRestaurant() ? "Y" : "N");
            stmt.setString(4, object.getIpAddress());
            stmt.setInt(5, object.getRestaurant().getId());
            stmt.executeUpdate();

            if (!connection.getAutoCommit()) {
                connection.commit();
            }
            return object;
        } catch (SQLException ex) {
            logger.error("SQLException in create: {}", ex.getMessage());
            try { connection.rollback(); } catch (SQLException e) { e.printStackTrace(); }
        }
        return null;
    }

    @Override
    public boolean update(BasicEvaluation object) {
        String sql = "UPDATE LIKES SET date_eval = ?, appreciation = ?, adresse_ip = ?, fk_rest = ? WHERE numero = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setDate(1, new java.sql.Date(object.getVisitDate().getTime()));
            stmt.setString(2, object.getLikeRestaurant() != null && object.getLikeRestaurant() ? "Y" : "N");
            stmt.setString(3, object.getIpAddress());
            stmt.setInt(4, object.getRestaurant().getId());
            stmt.setInt(5, object.getId());
            int rows = stmt.executeUpdate();
            connection.commit();
            return rows > 0;
        } catch (SQLException ex) {
            logger.error("SQLException in update: {}", ex.getMessage());
            try { connection.rollback(); } catch (SQLException e) { e.printStackTrace(); }
        }
        return false;
    }

    @Override
    public boolean delete(BasicEvaluation object) {
        return deleteById(object.getId());
    }

    @Override
    public boolean deleteById(int id) {
        String sql = "DELETE FROM LIKES WHERE numero = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            int rows = stmt.executeUpdate();
            connection.commit();
            return rows > 0;
        } catch (SQLException ex) {
            logger.error("SQLException in deleteById: {}", ex.getMessage());
            try { connection.rollback(); } catch (SQLException e) { e.printStackTrace(); }
        }
        return false;
    }

    @Override
    protected String getSequenceQuery() {
        return "SELECT SEQ_EVAL.NEXTVAL FROM dual";
    }

    @Override
    protected String getExistsQuery() {
        return "SELECT 1 FROM LIKES WHERE numero = ?";
    }

    @Override
    protected String getCountQuery() {
        return "SELECT COUNT(*) FROM LIKES";
    }
}
