package ch.hearc.ig.guideresto.persistence.mapper;

import ch.hearc.ig.guideresto.business.Grade;
import ch.hearc.ig.guideresto.business.CompleteEvaluation;
import ch.hearc.ig.guideresto.business.EvaluationCriteria;
import ch.hearc.ig.guideresto.persistence.AbstractMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.sql.*;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import static ch.hearc.ig.guideresto.persistence.ConnectionUtils.getConnection;
import ch.hearc.ig.guideresto.business.*;
import ch.hearc.ig.guideresto.persistence.AbstractMapper;
import java.sql.*;
import java.util.*;
import static ch.hearc.ig.guideresto.persistence.ConnectionUtils.getConnection;

public class GradeMapper extends AbstractMapper<Grade> {

    // 🔹 Cache Identity Map (static pour toute l’app)
    private static final Map<Integer, Grade> identityMap = new HashMap<>();

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
        // ✅ Vérifie d’abord le cache
        if (identityMap.containsKey(id)) {
            System.out.println("⚡ Grade " + id + " récupéré depuis l'Identity Map");
            return identityMap.get(id);
        }

        String sql = "SELECT numero, note, fk_comm, fk_crit FROM NOTES WHERE numero = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    CompleteEvaluation eval = evaluationMapper.findById(rs.getInt("fk_comm"));
                    EvaluationCriteria crit = criteriaMapper.findById(rs.getInt("fk_crit"));
                    Grade grade = new Grade(
                            rs.getInt("numero"),
                            rs.getInt("note"),
                            eval,
                            crit
                    );

                    // ✅ Stocker dans la Map
                    identityMap.put(id, grade);
                    return grade;
                }
            }
        } catch (SQLException ex) {
            System.err.println("Erreur findById Grade : " + ex.getMessage());
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
                int id = rs.getInt("numero");
                Grade grade = identityMap.get(id);

                if (grade == null) {
                    CompleteEvaluation eval = evaluationMapper.findById(rs.getInt("fk_comm"));
                    EvaluationCriteria crit = criteriaMapper.findById(rs.getInt("fk_crit"));
                    grade = new Grade(id, rs.getInt("note"), eval, crit);
                    identityMap.put(id, grade);
                }
                grades.add(grade);
            }
        } catch (SQLException ex) {
            System.err.println("Erreur findAll Grade : " + ex.getMessage());
        }
        return grades;
    }

    @Override
    public Grade create(Grade grade) {
        String sql = "BEGIN INSERT INTO NOTES (note, fk_comm, fk_crit) VALUES (?, ?, ?) RETURNING numero INTO ?; END;";
        try (CallableStatement stmt = connection.prepareCall(sql)) {
            stmt.setInt(1, grade.getGrade());
            stmt.setInt(2, grade.getEvaluation().getId());
            stmt.setInt(3, grade.getCriteria().getId());
            stmt.registerOutParameter(4, java.sql.Types.INTEGER);

            stmt.executeUpdate();

            int generatedId = stmt.getInt(4);
            grade.setId(generatedId);
            identityMap.put(generatedId, grade); // ✅ Ajouter au cache

            if (!connection.getAutoCommit()) connection.commit();
            return grade;

        } catch (SQLException e) {
            System.err.println("Erreur create Grade: " + e.getMessage());
            try { connection.rollback(); } catch (SQLException r) {
                System.err.println("Rollback échoué : " + r.getMessage());
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

            // ✅ Synchroniser le cache
            if (updated > 0) {
                identityMap.put(grade.getId(), grade);
            }

            return updated > 0;
        } catch (SQLException ex) {
            System.err.println("Erreur update Grade : " + ex.getMessage());
            return false;
        }
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

            // ✅ Retirer du cache
            if (deleted > 0) identityMap.remove(id);

            return deleted > 0;
        } catch (SQLException ex) {
            System.err.println("Erreur deleteById Grade : " + ex.getMessage());
            try { connection.rollback(); } catch (SQLException e) {
                System.err.println("Rollback échoué : " + e.getMessage());
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

    // 🔹 Utilitaires avec cache aussi
    public Set<Grade> findByCompleteEvaluation(CompleteEvaluation eval) {
        Set<Grade> grades = new HashSet<>();
        String sql = "SELECT numero, note, fk_comm, fk_crit FROM NOTES WHERE fk_comm = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, eval.getId());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("numero");
                    Grade grade = identityMap.get(id);
                    if (grade == null) {
                        EvaluationCriteria crit = criteriaMapper.findById(rs.getInt("fk_crit"));
                        grade = new Grade(id, rs.getInt("note"), eval, crit);
                        identityMap.put(id, grade);
                    }
                    grades.add(grade);
                }
            }
        } catch (SQLException ex) {
            System.err.println("Erreur findByCompleteEvaluation Grade : " + ex.getMessage());
        }

        return grades;
    }
    public Set<Grade> findByEvaluation(CompleteEvaluation eval) {
        Set<Grade> grades = new LinkedHashSet<>();
        String sql = "SELECT numero, note, fk_crit FROM NOTES WHERE fk_comm = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, eval.getId());
            try (ResultSet rs = stmt.executeQuery()) {
                EvaluationCriteriaMapper critMapper = new EvaluationCriteriaMapper();

                while (rs.next()) {
                    int gradeId = rs.getInt("numero");

                    // ✅ Vérifie le cache d'identité avant de créer un nouvel objet
                    Grade grade = identityMap.get(gradeId);
                    if (grade == null) {
                        int noteValue = rs.getInt("note");
                        int critId = rs.getInt("fk_crit");
                        Grade newGrade = new Grade(
                                gradeId,
                                noteValue,
                                eval,
                                critMapper.findById(critId)
                        );

                        identityMap.put(gradeId, newGrade);
                        grade = newGrade;
                    }

                    grades.add(grade);
                }
            }
        } catch (SQLException ex) {
            logger.error("Erreur findByEvaluation GradeMapper : {}", ex.getMessage());
        }

        return grades;
    }

}

