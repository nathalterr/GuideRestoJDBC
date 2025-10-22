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

public class GradeMapper extends AbstractMapper<Grade> {

    private static final Logger logger = LogManager.getLogger(GradeMapper.class);

    private final Connection connection;
    private final EvaluationCriteriaMapper criteriaMapper;
    private final CompleteEvaluationMapper evaluationMapper;

    public GradeMapper(Connection connection) {
        this.connection = connection;
        this.criteriaMapper = new EvaluationCriteriaMapper(connection);
        this.evaluationMapper = new CompleteEvaluationMapper(connection);
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
            stmt.setInt(1, id);
            int deleted = stmt.executeUpdate();
            if (!connection.getAutoCommit()) connection.commit();
            return deleted > 0;
        } catch (SQLException ex) {
            logger.error("Erreur delete Grade : {}", ex.getMessage());
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
}
