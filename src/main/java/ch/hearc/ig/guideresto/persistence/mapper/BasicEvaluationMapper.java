package ch.hearc.ig.guideresto.persistence.mapper;

import ch.hearc.ig.guideresto.business.BasicEvaluation;
import ch.hearc.ig.guideresto.business.Restaurant;
import ch.hearc.ig.guideresto.persistence.AbstractMapper;
import java.sql.*;
import java.util.HashSet;
import java.util.Set;

import static ch.hearc.ig.guideresto.persistence.ConnectionUtils.getConnection;

public class BasicEvaluationMapper extends AbstractMapper<BasicEvaluation> {

    private final Connection connection;
    private final RestaurantMapper restaurantMapper;

    public BasicEvaluationMapper() {
        this.connection = getConnection();
        this.restaurantMapper = new RestaurantMapper();
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
        String sql = "INSERT INTO LIKES (date_eval, appreciation, adresse_ip, fk_rest) VALUES (?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql, new String[]{"numero"})) {

            stmt.setDate(1, new java.sql.Date(object.getVisitDate().getTime()));
            stmt.setString(2, (object.getLikeRestaurant() != null && object.getLikeRestaurant()) ? "Y" : "N");
            stmt.setString(3, object.getIpAddress());
            stmt.setInt(4, object.getRestaurant().getId());

            int affectedRows = stmt.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("La création de l'évaluation basique a échoué, aucune ligne insérée.");
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    object.setId(generatedKeys.getInt(1));
                } else {
                    throw new SQLException("La récupération de l'ID de l'évaluation basique a échoué.");
                }
            }

            if (!connection.getAutoCommit()) {
                connection.commit();
            }

            return object;

        } catch (SQLException e) {
            logger.error("Erreur create BasicEvaluation: {}", e.getMessage());
            try { connection.rollback(); } catch (SQLException r) { logger.error("Rollback failed: {}", r.getMessage()); }
            return null;
        }
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
    public Set<BasicEvaluation> findByRestaurant(Restaurant restaurant) {
        Set<BasicEvaluation> likes = new HashSet<>();
        String sql = "SELECT numero, date_eval, appreciation, adresse_ip, fk_rest FROM LIKES WHERE fk_rest = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, restaurant.getId());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    likes.add(new BasicEvaluation(
                            rs.getInt("numero"),
                            rs.getDate("date_eval"),
                            restaurant,
                            "Y".equalsIgnoreCase(rs.getString("appreciation")),
                            rs.getString("adresse_ip")
                    ));
                }
            }
        } catch (SQLException ex) {
            logger.error("Erreur findByRestaurant BasicEvaluation : {}", ex.getMessage());
        }
        return likes;
    }

    public BasicEvaluation findByIpAndRest(String ip, int restaurantId) throws SQLException {
        String sql = "SELECT numero, date_eval, appreciation, adresse_ip, fk_rest " +
                "FROM LIKES WHERE adresse_ip = ? AND fk_rest = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, ip);
            stmt.setInt(2, restaurantId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    BasicEvaluation eval = new BasicEvaluation();
                    eval.setId(rs.getInt("numero"));
                    eval.setVisitDate(rs.getDate("date_eval"));
                    eval.setLikeRestaurant("Y".equalsIgnoreCase(rs.getString("appreciation")));
                    eval.setIpAddress(rs.getString("adresse_ip"));
                    // éventuellement hydrater le restaurant :
                    // eval.setRestaurant(new Restaurant(rs.getInt("fk_rest")));
                    return eval;
                }
            }
        }
        return null;
    }


}
