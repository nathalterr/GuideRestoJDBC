package ch.hearc.ig.guideresto.persistence.mapper;

import ch.hearc.ig.guideresto.business.CompleteEvaluation;
import ch.hearc.ig.guideresto.business.Restaurant;
import ch.hearc.ig.guideresto.persistence.AbstractMapper;
import java.sql.*;
import java.util.HashSet;
import java.util.Set;
import java.util.LinkedHashSet;
import static ch.hearc.ig.guideresto.persistence.ConnectionUtils.getConnection;
import ch.hearc.ig.guideresto.business.CompleteEvaluation;
import ch.hearc.ig.guideresto.business.Restaurant;
import ch.hearc.ig.guideresto.persistence.AbstractMapper;
import java.sql.*;
import java.util.*;

import static ch.hearc.ig.guideresto.persistence.ConnectionUtils.getConnection;

public class CompleteEvaluationMapper extends AbstractMapper<CompleteEvaluation> {

    // ⚡ Cache d'identité
    private static final Map<Integer, CompleteEvaluation> identityMap = new HashMap<>();

    private final Connection connection;
    private RestaurantMapper restaurantMapper;
    private GradeMapper gradeMapper;

    public CompleteEvaluationMapper() {
        this.connection = getConnection();
    }

    public CompleteEvaluationMapper(RestaurantMapper rm) {
        this.connection = getConnection();
        this.restaurantMapper = rm;
        this.gradeMapper = new GradeMapper();
    }

    public CompleteEvaluationMapper(RestaurantMapper restaurantMapper, GradeMapper gradeMapper) {
        this.connection = getConnection();
        this.restaurantMapper = restaurantMapper;
        this.gradeMapper = gradeMapper;
    }

    public void setDependencies(RestaurantMapper restaurantMapper, GradeMapper gradeMapper) {
        this.restaurantMapper = restaurantMapper;
        this.gradeMapper = gradeMapper;
    }

    @Override
    public CompleteEvaluation findById(int id) {
        // ✅ Vérifie d'abord le cache
        if (identityMap.containsKey(id)) {
            System.out.println("⚡ Évaluation " + id + " récupérée depuis l'Identity Map");
            return identityMap.get(id);
        }

        String sql = "SELECT numero, date_eval, commentaire, nom_utilisateur, fk_rest FROM COMMENTAIRES WHERE numero = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Restaurant restaurant = restaurantMapper.findById(rs.getInt("fk_rest"));
                    CompleteEvaluation eval = new CompleteEvaluation(
                            rs.getInt("numero"),
                            rs.getDate("date_eval"),
                            restaurant,
                            rs.getString("commentaire"),
                            rs.getString("nom_utilisateur")
                    );

                    // Ajout au cache
                    identityMap.put(eval.getId(), eval);

                    // 🔹 Charge aussi les notes associées
                    eval.getGrades().addAll(gradeMapper.findByEvaluation(eval));

                    return eval;
                }
            }
        } catch (SQLException e) {
            logger.error("Erreur lors du findById : {}", e.getMessage());
        }
        return null;
    }

    @Override
    public Set<CompleteEvaluation> findAll() {
        Set<CompleteEvaluation> evaluations = new LinkedHashSet<>();
        String sql = "SELECT numero, date_eval, commentaire, nom_utilisateur, fk_rest FROM COMMENTAIRES";

        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                int id = rs.getInt("numero");
                CompleteEvaluation eval = identityMap.get(id);

                if (eval == null) {
                    Restaurant restaurant = restaurantMapper.findById(rs.getInt("fk_rest"));
                    eval = new CompleteEvaluation(
                            id,
                            rs.getDate("date_eval"),
                            restaurant,
                            rs.getString("commentaire"),
                            rs.getString("nom_utilisateur")
                    );
                    identityMap.put(id, eval);
                }

                // 🔹 Lazy-load des notes seulement si besoin
                if (eval.getGrades().isEmpty()) {
                    eval.getGrades().addAll(gradeMapper.findByEvaluation(eval));
                }

                evaluations.add(eval);
            }
        } catch (SQLException e) {
            logger.error("Erreur lors du findAll : {}", e.getMessage());
        }
        return evaluations;
    }

    @Override
    public CompleteEvaluation create(CompleteEvaluation evaluation) {
        String sql = "BEGIN INSERT INTO COMMENTAIRES (date_eval, commentaire, nom_utilisateur, fk_rest) " +
                "VALUES (?, ?, ?, ?) RETURNING numero INTO ?; END;";
        try (CallableStatement stmt = connection.prepareCall(sql)) {

            stmt.setDate(1, new java.sql.Date(evaluation.getVisitDate().getTime()));
            stmt.setString(2, evaluation.getComment());
            stmt.setString(3, evaluation.getUsername());
            stmt.setInt(4, evaluation.getRestaurant().getId());
            stmt.registerOutParameter(5, java.sql.Types.INTEGER);

            stmt.executeUpdate();

            int generatedId = stmt.getInt(5);
            evaluation.setId(generatedId);

            // ✅ Ajout dans le cache
            identityMap.put(generatedId, evaluation);

            if (!connection.getAutoCommit()) connection.commit();

            return evaluation;

        } catch (SQLException e) {
            if (e.getErrorCode() == 1) {
                logger.info("Évaluation déjà existante (user: {}, resto: {}), récupération via findByUserAndRest()",
                        evaluation.getUsername(), evaluation.getRestaurant().getId());
                try {
                    return findByUserAndRest(evaluation.getUsername(), evaluation.getRestaurant().getId());
                } catch (SQLException ex) {
                    logger.error("Erreur findByUserAndRest après doublon: {}", ex.getMessage());
                }
            } else {
                logger.error("Erreur create CompleteEvaluation: {}", e.getMessage());
            }

            try {
                connection.rollback();
            } catch (SQLException r) {
                logger.error("Rollback failed: {}", r.getMessage());
            }

            return null;
        }
    }

    @Override
    public boolean update(CompleteEvaluation evaluation) {
        String sql = "UPDATE COMMENTAIRES SET date_eval = ?, commentaire = ?, nom_utilisateur = ?, fk_rest = ? WHERE numero = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setDate(1, new java.sql.Date(evaluation.getVisitDate().getTime()));
            stmt.setString(2, evaluation.getComment());
            stmt.setString(3, evaluation.getUsername());
            stmt.setInt(4, evaluation.getRestaurant().getId());
            stmt.setInt(5, evaluation.getId());

            int rows = stmt.executeUpdate();

            if (!connection.getAutoCommit()) connection.commit();

            if (rows > 0) {
                // ✅ Mise à jour du cache
                identityMap.put(evaluation.getId(), evaluation);
            }

            return rows > 0;
        } catch (SQLException e) {
            logger.error("Erreur update CompleteEvaluation : {}", e.getMessage());
            return false;
        }
    }

    @Override
    public boolean delete(CompleteEvaluation evaluation) {
        return deleteById(evaluation.getId());
    }

    @Override
    public boolean deleteById(int id) {
        try {
            // Supprimer d'abord les notes liées
            String deleteNotesSql = "DELETE FROM NOTES WHERE fk_comm = ?";
            try (PreparedStatement stmt = connection.prepareStatement(deleteNotesSql)) {
                stmt.setInt(1, id);
                stmt.executeUpdate();
            }

            // Supprimer le commentaire
            String deleteCommentSql = "DELETE FROM COMMENTAIRES WHERE numero = ?";
            try (PreparedStatement stmt = connection.prepareStatement(deleteCommentSql)) {
                stmt.setInt(1, id);
                int deleted = stmt.executeUpdate();

                if (!connection.getAutoCommit()) connection.commit();

                if (deleted > 0) {
                    identityMap.remove(id);
                }

                return deleted > 0;
            }

        } catch (SQLException ex) {
            logger.error("Erreur deleteById CompleteEvaluation: {}", ex.getMessage());
            try {
                if (!connection.getAutoCommit()) connection.rollback();
            } catch (SQLException rollbackEx) {
                logger.error("Rollback échoué : {}", rollbackEx.getMessage());
            }
            return false;
        }
    }

    @Override
    protected String getSequenceQuery() {
        return "SELECT SEQ_EVAL.NEXTVAL FROM dual";
    }

    @Override
    protected String getExistsQuery() {
        return "SELECT 1 FROM COMMENTAIRES WHERE numero = ?";
    }

    @Override
    protected String getCountQuery() {
        return "SELECT COUNT(*) FROM COMMENTAIRES";
    }

    public Set<CompleteEvaluation> findByRestaurant(Restaurant restaurant) {
        Set<CompleteEvaluation> evaluations = new LinkedHashSet<>();
        String sql = "SELECT numero, date_eval, commentaire, nom_utilisateur FROM COMMENTAIRES WHERE fk_rest = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, restaurant.getId());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("numero");
                    CompleteEvaluation eval = identityMap.get(id);

                    if (eval == null) {
                        eval = new CompleteEvaluation(
                                id,
                                rs.getDate("date_eval"),
                                restaurant,
                                rs.getString("commentaire"),
                                rs.getString("nom_utilisateur")
                        );
                        identityMap.put(id, eval);
                    }

                    if (eval.getGrades().isEmpty()) {
                        eval.getGrades().addAll(gradeMapper.findByEvaluation(eval));
                    }

                    evaluations.add(eval);
                }
            }
        } catch (SQLException ex) {
            logger.error("Erreur findByRestaurant CompleteEvaluation : {}", ex.getMessage());
        }

        return evaluations;
    }

    public CompleteEvaluation findByUserAndRest(String username, int restaurantId) throws SQLException {
        for (CompleteEvaluation eval : identityMap.values()) {
            if (eval.getUsername().equalsIgnoreCase(username)
                    && eval.getRestaurant() != null
                    && eval.getRestaurant().getId() == restaurantId) {
                System.out.println("⚡ Évaluation trouvée dans le cache pour " + username);
                return eval;
            }
        }

        String sql = "SELECT numero, date_eval, commentaire, nom_utilisateur, fk_rest " +
                "FROM COMMENTAIRES WHERE nom_utilisateur = ? AND fk_rest = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.setInt(2, restaurantId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Restaurant rest = restaurantMapper.findById(restaurantId);
                    CompleteEvaluation eval = new CompleteEvaluation(
                            rs.getInt("numero"),
                            rs.getDate("date_eval"),
                            rest,
                            rs.getString("commentaire"),
                            rs.getString("nom_utilisateur")
                    );

                    identityMap.put(eval.getId(), eval);
                    return eval;
                }
            }
        }
        return null;
    }
}



