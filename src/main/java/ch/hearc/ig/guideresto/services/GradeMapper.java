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
import java.util.LinkedHashSet;
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
        String sql = "BEGIN INSERT INTO NOTES (note, fk_comm, fk_crit) " +
                "VALUES (?, ?, ?) RETURNING numero INTO ?; END;";

        try (CallableStatement stmt = connection.prepareCall(sql)) {
            stmt.setInt(1, grade.getGrade());
            stmt.setInt(2, grade.getEvaluation().getId());
            stmt.setInt(3, grade.getCriteria().getId());
            stmt.registerOutParameter(4, java.sql.Types.INTEGER);

            stmt.executeUpdate();

            int generatedId = stmt.getInt(4);
            grade.setId(generatedId);

            if (!connection.getAutoCommit()) {
                connection.commit();
            }

            return grade;
        } catch (SQLException e) {
            if (e.getErrorCode() == 1) { // Doublon
                logger.info("Note déjà existante pour critère {} et évaluation {}, récupération via findByEvalAndCrit()",
                        grade.getEvaluation().getId(), grade.getCriteria().getId());
                try {
                    return findByEvalAndCrit(grade.getEvaluation().getId(), grade.getCriteria().getId());
                } catch (SQLException ex) {
                    logger.error("Erreur findByEvalAndCrit après doublon: {}", ex.getMessage());
                }
            } else {
                logger.error("Erreur create Grade: {}", e.getMessage());
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
            System.out.println("Nombre de lignes supprimées pour Grade ID=" + id + " : " + deleted);

            if (!connection.getAutoCommit()) connection.commit();

            if (deleted == 0) {
                System.out.println("Attention : aucun grade trouvé pour l'ID " + id + ". Vérifier l'existence.");
            }

            return deleted > 0;
        } catch (SQLException ex) {
            System.err.println("Erreur lors de la suppression du Grade ID=" + id + " : " + ex.getMessage());
            ex.printStackTrace();
            try {
                connection.rollback();
            } catch (SQLException e) {
                System.err.println("Rollback échoué : " + e.getMessage());
                e.printStackTrace();
            }
            return false;
        }
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
    public Grade findByEvalAndCrit(int evalId, int critId) throws SQLException {
        String sql = "SELECT numero, note, fk_comm, fk_crit FROM NOTES WHERE fk_comm = ? AND fk_crit = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, evalId);
            stmt.setInt(2, critId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Grade grade = new Grade();
                    grade.setId(rs.getInt("numero"));
                    grade.setGrade(rs.getInt("note"));

                    // si tu veux, tu peux hydrater les sous-objets
                    // grade.setEvaluation(new CompleteEvaluation(rs.getInt("fk_comm")));
                    // grade.setCriteria(new EvaluationCriteria(rs.getInt("fk_crit")));

                    return grade;
                }
            }
        }
        return null;
    }

    public Set<Grade> findByEvaluation(CompleteEvaluation eval) {
        Set<Grade> grades = new LinkedHashSet<>();
        String sql = """
                     SELECT g.numero, g.note, g.fk_crit
                     FROM NOTES g
                     WHERE g.fk_comm = ?
                     """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, eval.getId());
            try (ResultSet rs = stmt.executeQuery()) {
                EvaluationCriteriaMapper critMapper = new EvaluationCriteriaMapper();
                while (rs.next()) {
                    int gradeId = rs.getInt("numero");
                    int noteValue = rs.getInt("note");
                    int critId = rs.getInt("fk_crit");

                    EvaluationCriteria crit = critMapper.findById(critId);
                    if (crit != null) {
                        Grade grade = new Grade(gradeId, noteValue, eval, crit);
                        grades.add(grade);
                    }
                }
            }
        } catch (SQLException ex) {
            System.err.println("Erreur findByEvaluation GradeMapper : " + ex.getMessage());
            ex.printStackTrace();
        }

        return grades;
    }


}
