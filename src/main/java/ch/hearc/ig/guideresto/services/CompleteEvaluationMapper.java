package ch.hearc.ig.guideresto.services;

import ch.hearc.ig.guideresto.business.CompleteEvaluation;
import ch.hearc.ig.guideresto.business.Grade;
import ch.hearc.ig.guideresto.business.Restaurant;
import ch.hearc.ig.guideresto.persistence.AbstractMapper;
import ch.hearc.ig.guideresto.services.RestaurantMapper;
import java.sql.*;
import java.util.HashSet;
import java.util.Set;
import java.util.LinkedHashSet;

import static ch.hearc.ig.guideresto.persistence.ConnectionUtils.getConnection;

public class CompleteEvaluationMapper extends AbstractMapper<CompleteEvaluation> {

    private final Connection connection;
    private final RestaurantMapper restaurantMapper; // pour gérer la FK

    public CompleteEvaluationMapper() {
        this.connection = getConnection();
        this.restaurantMapper = new RestaurantMapper();
    }

    @Override
    public CompleteEvaluation findById(int id) {
        String sql = "SELECT numero, date_eval, commentaire, nom_utilisateur, fk_rest FROM COMMENTAIRES WHERE numero = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Restaurant restaurant = restaurantMapper.findById(rs.getInt("fk_rest"));
                    return new CompleteEvaluation(
                            rs.getInt("numero"),
                            rs.getDate("date_eval"),
                            restaurant,
                            rs.getString("commentaire"),
                            rs.getString("nom_utilisateur")
                    );
                }
            }
        } catch (SQLException e) {
            logger.error("Erreur lors du findById : {}", e.getMessage());
        }
        return null;
    }

    @Override
    public Set<CompleteEvaluation> findAll() {
        Set<CompleteEvaluation> evaluations = new HashSet<>();
        String sql = "SELECT numero, date_eval, commentaire, nom_utilisateur, fk_rest FROM COMMENTAIRES";

        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                Restaurant restaurant = restaurantMapper.findById(rs.getInt("fk_rest"));
                evaluations.add(new CompleteEvaluation(
                        rs.getInt("numero"),
                        rs.getDate("date_eval"),
                        restaurant,
                        rs.getString("commentaire"),
                        rs.getString("nom_utilisateur")
                ));
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

            if (!connection.getAutoCommit()) {
                connection.commit();
            }

            return evaluation;

        } catch (SQLException e) {
            if (e.getErrorCode() == 1) { // Doublon (par ex. même utilisateur + même resto)
                logger.info("Évaluation complète déjà existante (user: {}, resto: {}), récupération via findByUserAndRest()",
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
            return rows > 0;
        } catch (SQLException e) {
            logger.error("Erreur lors de la mise à jour : {}", e.getMessage());
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
            // 1️⃣ Supprimer toutes les notes liées à cette évaluation
            String deleteNotesSql = "DELETE FROM NOTES WHERE fk_comm = ?";
            try (PreparedStatement stmt = connection.prepareStatement(deleteNotesSql)) {
                stmt.setInt(1, id);
                int deletedNotes = stmt.executeUpdate();
                System.out.println("Nombre de notes supprimées pour l'évaluation ID=" + id + " : " + deletedNotes);
            }

            // 2️⃣ Supprimer le commentaire correspondant
            String deleteCommentSql = "DELETE FROM COMMENTAIRES WHERE numero = ?";
            try (PreparedStatement stmt = connection.prepareStatement(deleteCommentSql)) {
                stmt.setInt(1, id);
                int deletedComments = stmt.executeUpdate();
                System.out.println("Nombre de commentaires supprimés pour l'évaluation ID=" + id + " : " + deletedComments);

                if (!connection.getAutoCommit()) connection.commit();
                return deletedComments > 0;
            }

        } catch (SQLException ex) {
            logger.error("Erreur delete CompleteEvaluation: {}", ex.getMessage());
            ex.printStackTrace();
            try {
                if (connection != null && !connection.getAutoCommit()) {
                    connection.rollback();
                }
            } catch (SQLException rollbackEx) {
                rollbackEx.printStackTrace();
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
                    CompleteEvaluation eval = new CompleteEvaluation(
                            rs.getInt("numero"),
                            rs.getDate("date_eval"),
                            restaurant,
                            rs.getString("commentaire"),
                            rs.getString("nom_utilisateur")
                    );

                    // 🔹 Charger les grades pour cette évaluation
                    GradeMapper gradeMapper = new GradeMapper();
                    eval.getGrades().addAll(gradeMapper.findByEvaluation(eval));

                    evaluations.add(eval);
                }
            }
        } catch (SQLException ex) {
            System.err.println("Erreur findByRestaurant CompleteEvaluation : " + ex.getMessage());
            ex.printStackTrace();
        }

        return evaluations;
    }

    public CompleteEvaluation findByUserAndRest(String username, int restaurantId) throws SQLException {
        String sql = "SELECT numero, date_eval, commentaire, nom_utilisateur, fk_rest " +
                "FROM COMMENTAIRES WHERE nom_utilisateur = ? AND fk_rest = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.setInt(2, restaurantId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    CompleteEvaluation eval = new CompleteEvaluation();
                    eval.setId(rs.getInt("numero"));
                    eval.setVisitDate(rs.getDate("date_eval"));
                    eval.setComment(rs.getString("commentaire"));
                    eval.setUsername(rs.getString("nom_utilisateur"));
                    // si tu veux hydrater le restaurant :
                    // eval.setRestaurant(new Restaurant(rs.getInt("fk_rest")));
                    return eval;
                }
            }
        }
        return null;
    }


}


