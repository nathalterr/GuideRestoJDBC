package ch.hearc.ig.guideresto.services;

import ch.hearc.ig.guideresto.business.Grade;
import ch.hearc.ig.guideresto.business.CompleteEvaluation;
import ch.hearc.ig.guideresto.business.EvaluationCriteria;
import ch.hearc.ig.guideresto.persistence.AbstractMapper;
import ch.hearc.ig.guideresto.persistence.ConnectionUtils;
import ch.hearc.ig.guideresto.services.CompleteEvaluationMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;
import java.util.HashSet;
import java.util.Set;

import static ch.hearc.ig.guideresto.persistence.ConnectionUtils.getConnection;

public class GradeMapper extends AbstractMapper<Grade> {

    private static final Logger logger = LogManager.getLogger(GradeMapper.class);

    private final Connection connection;
    private final EvaluationCriteriaMapper criteriaMapper;
    private final CompleteEvaluationMapper evaluationMapper;

    public GradeMapper() {
        this.connection = getConnection();
        this.criteriaMapper = new EvaluationCriteriaMapper();
        this.evaluationMapper = new CompleteEvaluationMapper();
    }

    @Override
    public Grade findById(int id) {
        String sql = "SELECT numero, note, fk_comm, fk_crit FROM NOTES WHERE numero = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int evalId = rs.getInt("fk_comm");
                    int critId = rs.getInt("fk_crit");
                    CompleteEvaluation eval = evaluationMapper.findById(evalId);
                    EvaluationCriteria crit = criteriaMapper.findById(critId);
                    return new Grade(
                            rs.getInt("numero"),
                            rs.getInt("note"),
                            eval,
                            crit
                    );
                }
            }
        } catch (SQLException ex) {
            logger.error("Erreur findById Grade : {}", ex.getMessage());
        }
        return null;
    }

    @Override
    public Set<Grade> findAll() {
        Set<Grade> grades = new HashSet<>();
        String sql = "SELECT numero, note, fk_comm, fk_crit FROM NOTES";

        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                int evalId = rs.getInt("fk_comm");
                int critId = rs.getInt("fk_crit");
                CompleteEvaluation eval = evaluationMapper.findById(evalId);
                EvaluationCriteria crit = criteriaMapper.findById(critId);

                Grade grade = new Grade(
                        rs.getInt("numero"),
                        rs.getInt("note"),
                        eval,
                        crit
                );
                grades.add(grade);
            }
        } catch (SQLException ex) {
            logger.error("Erreur findAll Grade : {}", ex.getMessage());
        }
        return grades;
    }

    @Override
    public Grade create(Grade grade) {
        String sql = "INSERT INTO NOTES (numero, note, fk_comm, fk_crit) VALUES (?, ?, ?, ?)";
        Integer newId = getSequenceValue();
        grade.setId(newId);

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, newId);
            stmt.setInt(2, grade.getGrade());
            stmt.setInt(3, grade.getEvaluation().getId());
            stmt.setInt(4, grade.getCriteria().getId());
            stmt.executeUpdate();

            if (!connection.getAutoCommit()) connection.commit();

            logger.info("Grade inséré avec ID {}", newId);
            return grade;
        } catch (SQLException ex) {
            logger.error("Erreur create Grade : {}", ex.getMessage());
        }
        return null;
    }

    @Override
    public boolean update(Grade grade) {
        String sql = "UPDATE NOTES SET note = ?, fk_comm = ?, fk_crit = ? WHERE numero = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, grade.getGrade());
            stmt.setInt(2, grade.getEvaluation().getId());
            stmt.setInt(3, grade.getCriteria().getId());
            stmt.setInt(4, grade.getId());
            int updated = stmt.executeUpdate();
            if (!connection.getAutoCommit()) connection.commit();
            return updated > 0;
        } catch (SQLException ex) {
            logger.error("Erreur update Grade : {}", ex.getMessage());
        }
        return false;
    }

    @Override
    public boolean delete(Grade grade) {
        return deleteById(grade.getId());
    }

    @Override
    public boolean deleteById(int id) {
        String sql = "DELETE FROM NOTES WHERE numero = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            System.out.println("Suppression Grade ID=" + id + " en cours...");
            stmt.setInt(1, id);
            int deleted = stmt.executeUpdate();
            System.out.println("Nombre de lignes supprimées pour Grade : " + deleted);
            if (!connection.getAutoCommit()) connection.commit();
            return deleted > 0;
        } catch (SQLException ex) {
            System.err.println("Erreur delete Grade : " + ex.getMessage());
            ex.printStackTrace();
            try { connection.rollback(); } catch (SQLException e) { e.printStackTrace(); }
        }
        return false;
    }

    @Override
    protected String getSequenceQuery() {
        return "SELECT SEQ_NOTES.NEXTVAL FROM dual";
    }

    @Override
    protected String getExistsQuery() {
        return "SELECT 1 FROM NOTES WHERE numero = ?";
    }

    @Override
    protected String getCountQuery() {
        return "SELECT COUNT(*) FROM NOTES";
    }
    public Set<Grade> findByCompleteEvaluation(CompleteEvaluation completeEval) {
        Set<Grade> grades = new HashSet<>();
        String sql = "SELECT numero, note, fk_comm, fk_crit FROM NOTES WHERE fk_comm = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, completeEval.getId());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    // Récupérer le critère lié
                    EvaluationCriteria crit = new EvaluationCriteriaMapper().findById(rs.getInt("fk_crit"));
                    grades.add(new Grade(
                            rs.getInt("numero"),
                            rs.getInt("note"),
                            completeEval,
                            crit
                    ));
                }
            }
        } catch (SQLException ex) {
            logger.error("Erreur findByCompleteEvaluation Grade : {}", ex.getMessage());
        }
        return grades;
    }

}
