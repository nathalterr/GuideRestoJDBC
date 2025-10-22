package ch.hearc.ig.guideresto.services;

import ch.hearc.ig.guideresto.business.CompleteEvaluation;
import ch.hearc.ig.guideresto.business.Restaurant;
import ch.hearc.ig.guideresto.persistence.AbstractMapper;
import ch.hearc.ig.guideresto.services.RestaurantMapper;
import java.sql.*;
import java.util.HashSet;
import java.util.Set;

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
        String seqSql = getSequenceQuery();
        try (PreparedStatement seqStmt = connection.prepareStatement(seqSql);
             ResultSet rs = seqStmt.executeQuery()) {
            if (rs.next()) {
                evaluation.setId(rs.getInt(1));
            }
        } catch (SQLException e) {
            logger.error("Erreur lors de la récupération de la séquence : {}", e.getMessage());
            return null;
        }

        String insertSql = "INSERT INTO COMMENTAIRES (numero, date_eval, commentaire, nom_utilisateur, fk_rest) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(insertSql)) {
            stmt.setInt(1, evaluation.getId());
            stmt.setDate(2, new java.sql.Date(evaluation.getVisitDate().getTime()));
            stmt.setString(3, evaluation.getComment());
            stmt.setString(4, evaluation.getUsername());
            stmt.setInt(5, evaluation.getRestaurant().getId());
            stmt.executeUpdate();

            if (!connection.getAutoCommit()) connection.commit();
            return evaluation;
        } catch (SQLException e) {
            logger.error("Erreur lors de l’insertion : {}", e.getMessage());
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
        System.out.println("suppression en cours");
        String sql = "DELETE FROM COMMENTAIRES WHERE numero = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            System.out.println("suppression 1");
            stmt.setInt(1, id);
            System.out.println("suppression 1.2");
            int rows = stmt.executeUpdate();
            System.out.println("suppression 1.5");
            if (!connection.getAutoCommit()) connection.commit();
            System.out.println("suppression 2");
            return rows > 0;
        } catch (SQLException e) {
            logger.error("Erreur lors de la suppression : {}", e.getMessage());
            System.out.println("suppression 3");
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
}


