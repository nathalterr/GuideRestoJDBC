package ch.hearc.ig.guideresto.persistence.mapper;

import ch.hearc.ig.guideresto.business.BasicEvaluation;
import ch.hearc.ig.guideresto.business.Restaurant;
import ch.hearc.ig.guideresto.persistence.AbstractMapper;
import java.sql.*;
import java.util.HashSet;
import java.util.Set;
import static ch.hearc.ig.guideresto.persistence.ConnectionUtils.getConnection;
import ch.hearc.ig.guideresto.business.BasicEvaluation;
import ch.hearc.ig.guideresto.business.Restaurant;
import ch.hearc.ig.guideresto.persistence.AbstractMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.*;

import static ch.hearc.ig.guideresto.persistence.ConnectionUtils.getConnection;

public class BasicEvaluationMapper extends AbstractMapper<BasicEvaluation> {

    private static final Logger logger = LoggerFactory.getLogger(BasicEvaluationMapper.class);
    private final Connection connection;
    private final RestaurantMapper restaurantMapper;
    private final Map<Integer, BasicEvaluation> identityMap = new HashMap<>();

    public BasicEvaluationMapper() {
        this.connection = getConnection();
        this.restaurantMapper = new RestaurantMapper();
    }

    @Override
    public BasicEvaluation findById(int id) {
        if (identityMap.containsKey(id)) {
            logger.info("⚡ BasicEvaluation {} récupérée depuis l'Identity Map", id);
            return identityMap.get(id);
        }

        String sql = "SELECT numero, date_eval, appreciation, adresse_ip, fk_rest FROM LIKES WHERE numero = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Restaurant restaurant = restaurantMapper.findById(rs.getInt("fk_rest"));
                    BasicEvaluation eval = new BasicEvaluation(
                            rs.getInt("numero"),
                            rs.getDate("date_eval"),
                            restaurant,
                            "Y".equalsIgnoreCase(rs.getString("appreciation")),
                            rs.getString("adresse_ip")
                    );
                    identityMap.put(eval.getId(), eval);
                    return eval;
                }
            }
        } catch (SQLException ex) {
            logger.error("SQLException in findById: {}", ex.getMessage());
        }
        return null;
    }

    @Override
    public Set<BasicEvaluation> findAll() {
        Set<BasicEvaluation> evaluations = new HashSet<>();
        String sql = "SELECT numero, date_eval, appreciation, adresse_ip, fk_rest FROM LIKES";
        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                int id = rs.getInt("numero");
                BasicEvaluation eval = identityMap.get(id);
                if (eval == null) {
                    Restaurant restaurant = restaurantMapper.findById(rs.getInt("fk_rest"));
                    eval = new BasicEvaluation(
                            id,
                            rs.getDate("date_eval"),
                            restaurant,
                            "Y".equalsIgnoreCase(rs.getString("appreciation")),
                            rs.getString("adresse_ip")
                    );
                    identityMap.put(id, eval);
                }
                evaluations.add(eval);
            }
        } catch (SQLException ex) {
            logger.error("SQLException in findAll: {}", ex.getMessage());
        }
        return evaluations;
    }

    @Override
    public BasicEvaluation create(BasicEvaluation eval) {
        String sql = "INSERT INTO LIKES (date_eval, appreciation, adresse_ip, fk_rest) VALUES (?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql, new String[]{"numero"})) {
            stmt.setDate(1, new java.sql.Date(eval.getVisitDate().getTime()));
            stmt.setString(2, eval.getLikeRestaurant() != null && eval.getLikeRestaurant() ? "Y" : "N");
            stmt.setString(3, eval.getIpAddress());
            stmt.setInt(4, eval.getRestaurant().getId());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) throw new SQLException("Création échouée, aucune ligne insérée.");

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    eval.setId(generatedKeys.getInt(1));
                    identityMap.put(eval.getId(), eval);
                } else {
                    throw new SQLException("Impossible de récupérer l'ID généré.");
                }
            }

            if (!connection.getAutoCommit()) connection.commit();
            return eval;

        } catch (SQLException e) {
            logger.error("Erreur create BasicEvaluation: {}", e.getMessage());
            try { if (!connection.getAutoCommit()) connection.rollback(); } catch (SQLException r) { logger.error("Rollback failed: {}", r.getMessage()); }
            return null;
        }
    }

    @Override
    public boolean update(BasicEvaluation eval) {
        String sql = "UPDATE LIKES SET date_eval = ?, appreciation = ?, adresse_ip = ?, fk_rest = ? WHERE numero = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setDate(1, new java.sql.Date(eval.getVisitDate().getTime()));
            stmt.setString(2, eval.getLikeRestaurant() != null && eval.getLikeRestaurant() ? "Y" : "N");
            stmt.setString(3, eval.getIpAddress());
            stmt.setInt(4, eval.getRestaurant().getId());
            stmt.setInt(5, eval.getId());

            int rows = stmt.executeUpdate();
            if (!connection.getAutoCommit()) connection.commit();
            if (rows > 0) identityMap.put(eval.getId(), eval);
            return rows > 0;

        } catch (SQLException ex) {
            logger.error("SQLException in update: {}", ex.getMessage());
            try { if (!connection.getAutoCommit()) connection.rollback(); } catch (SQLException r) { logger.error("Rollback failed: {}", r.getMessage()); }
            return false;
        }
    }

    @Override
    public boolean delete(BasicEvaluation eval) {
        return deleteById(eval.getId());
    }

    @Override
    public boolean deleteById(int id) {
        String sql = "DELETE FROM LIKES WHERE numero = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            int rows = stmt.executeUpdate();
            if (!connection.getAutoCommit()) connection.commit();
            if (rows > 0) identityMap.remove(id);
            return rows > 0;
        } catch (SQLException ex) {
            logger.error("SQLException in deleteById: {}", ex.getMessage());
            try { if (!connection.getAutoCommit()) connection.rollback(); } catch (SQLException r) { logger.error("Rollback failed: {}", r.getMessage()); }
            return false;
        }
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
        Set<BasicEvaluation> evaluations = new HashSet<>();
        String sql = "SELECT numero, date_eval, appreciation, adresse_ip, fk_rest FROM LIKES WHERE fk_rest = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, restaurant.getId());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("numero");
                    BasicEvaluation eval = identityMap.get(id);
                    if (eval == null) {
                        eval = new BasicEvaluation(
                                id,
                                rs.getDate("date_eval"),
                                restaurant,
                                "Y".equalsIgnoreCase(rs.getString("appreciation")),
                                rs.getString("adresse_ip")
                        );
                        identityMap.put(id, eval);
                    }
                    evaluations.add(eval);
                }
            }
        } catch (SQLException ex) {
            logger.error("Erreur findByRestaurant BasicEvaluation: {}", ex.getMessage());
        }
        return evaluations;
    }

    public BasicEvaluation findByIpAndRest(String ip, int restaurantId) throws SQLException {
        String sql = "SELECT numero, date_eval, appreciation, adresse_ip, fk_rest FROM LIKES WHERE adresse_ip = ? AND fk_rest = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, ip);
            stmt.setInt(2, restaurantId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int id = rs.getInt("numero");
                    if (identityMap.containsKey(id)) return identityMap.get(id);

                    BasicEvaluation eval = new BasicEvaluation();
                    eval.setId(id);
                    eval.setVisitDate(rs.getDate("date_eval"));
                    eval.setLikeRestaurant("Y".equalsIgnoreCase(rs.getString("appreciation")));
                    eval.setIpAddress(rs.getString("adresse_ip"));
                    eval.setRestaurant(restaurantMapper.findById(rs.getInt("fk_rest")));
                    identityMap.put(id, eval);
                    return eval;
                }
            }
        }
        return null;
    }
}

