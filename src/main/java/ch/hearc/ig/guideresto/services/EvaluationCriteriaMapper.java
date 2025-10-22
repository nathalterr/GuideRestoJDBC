package ch.hearc.ig.guideresto.services;

import ch.hearc.ig.guideresto.business.EvaluationCriteria;
import ch.hearc.ig.guideresto.persistence.AbstractMapper;
import ch.hearc.ig.guideresto.persistence.ConnectionUtils;

import java.sql.*;
import java.util.HashSet;
import java.util.Set;

public class EvaluationCriteriaMapper extends AbstractMapper<EvaluationCriteria> {

    private final Connection connection;

    public EvaluationCriteriaMapper(Connection connection) {
        this.connection = connection;
    }

    @Override
    public EvaluationCriteria findById(int id) {
        String sql = "SELECT numero, nom, description FROM CRITERES_EVALUATION WHERE numero = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new EvaluationCriteria(
                            rs.getInt("numero"),
                            rs.getString("nom"),
                            rs.getString("description")
                    );
                }
            }
        } catch (SQLException e) {
            logger.error("Erreur lors du findById : {}", e.getMessage());
        }
        return null;
    }

    @Override
    public Set<EvaluationCriteria> findAll() {
        Set<EvaluationCriteria> criteres = new HashSet<>();
        String sql = "SELECT numero, nom, description FROM CRITERES_EVALUATION";

        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                criteres.add(new EvaluationCriteria(
                        rs.getInt("numero"),
                        rs.getString("nom"),
                        rs.getString("description")
                ));
            }
        } catch (SQLException e) {
            logger.error("Erreur lors du findAll : {}", e.getMessage());
        }
        return criteres;
    }

    @Override
    public EvaluationCriteria create(EvaluationCriteria critere) {
        String seqSql = getSequenceQuery();
        try (PreparedStatement seqStmt = connection.prepareStatement(seqSql);
             ResultSet rs = seqStmt.executeQuery()) {
            if (rs.next()) {
                critere.setId(rs.getInt(1));
            }
        } catch (SQLException e) {
            logger.error("Erreur lors de la récupération de la séquence : {}", e.getMessage());
            return null;
        }

        String insertSql = "INSERT INTO CRITERES_EVALUATION (numero, nom, description) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(insertSql)) {
            stmt.setInt(1, critere.getId());
            stmt.setString(2, critere.getName());
            stmt.setString(3, critere.getDescription());
            stmt.executeUpdate();
            if (!connection.getAutoCommit()) connection.commit();
            return critere;
        } catch (SQLException e) {
            logger.error("Erreur lors de l’insertion : {}", e.getMessage());
            return null;
        }
    }

    @Override
    public boolean update(EvaluationCriteria critere) {
        String sql = "UPDATE CRITERES_EVALUATION SET nom = ?, description = ? WHERE numero = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, critere.getName());
            stmt.setString(2, critere.getDescription());
            stmt.setInt(3, critere.getId());
            int rows = stmt.executeUpdate();
            if (!connection.getAutoCommit()) connection.commit();
            return rows > 0;
        } catch (SQLException e) {
            logger.error("Erreur lors de la mise à jour : {}", e.getMessage());
            return false;
        }
    }

    @Override
    public boolean delete(EvaluationCriteria critere) {
        return deleteById(critere.getId());
    }

    @Override
    public boolean deleteById(int id) {
        String sql = "DELETE FROM CRITERES_EVALUATION WHERE numero = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            int rows = stmt.executeUpdate();
            if (!connection.getAutoCommit()) connection.commit();
            return rows > 0;
        } catch (SQLException e) {
            logger.error("Erreur lors de la suppression : {}", e.getMessage());
            return false;
        }
    }

    @Override
    protected String getSequenceQuery() {
        return "SELECT SEQ_CRITERES_EVALUATION.NEXTVAL FROM dual";
    }

    @Override
    protected String getExistsQuery() {
        return "SELECT 1 FROM CRITERES_EVALUATION WHERE numero = ?";
    }

    @Override
    protected String getCountQuery() {
        return "SELECT COUNT(*) FROM CRITERES_EVALUATION";
    }
}
