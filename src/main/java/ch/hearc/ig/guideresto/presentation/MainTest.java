package ch.hearc.ig.guideresto.presentation;

import ch.hearc.ig.guideresto.business.*;
import ch.hearc.ig.guideresto.persistence.AbstractMapper;
import ch.hearc.ig.guideresto.services.*;
import ch.hearc.ig.guideresto.persistence.ConnectionUtils;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Date;

public class MainTest {

    public static void main(String[] args) {
        Connection c = ConnectionUtils.getConnection();

        // Mappers
        RestaurantTypeMapper typeMapper = new RestaurantTypeMapper(c);
        CityMapper cityMapper = new CityMapper(c);
        RestaurantMapper restaurantMapper = new RestaurantMapper(c);
        EvaluationCriteriaMapper criteriaMapper = new EvaluationCriteriaMapper(c);
        CompleteEvaluationMapper completeEvalMapper = new CompleteEvaluationMapper(c);
        GradeMapper gradeMapper = new GradeMapper(c);
        BasicEvaluationMapper basicEvalMapper = new BasicEvaluationMapper(c);

        try {
            // 🔹 Création des entités de test
            RestaurantType type = new RestaurantType("TestType_" + System.currentTimeMillis(), "Description type test");
            typeMapper.create(type);
            City city = new City("1000", "TestCity_" + System.currentTimeMillis());
            cityMapper.create(city);
            Restaurant restaurant = new Restaurant(null, "TestRestaurant_" + System.currentTimeMillis(), "Description restaurant", "www.test.com", new Localisation("Rue du Test 1", city), type);
            restaurantMapper.create(restaurant);
            EvaluationCriteria criteria = new EvaluationCriteria("Critère Test", "Description critère test");
            criteriaMapper.create(criteria);
            CompleteEvaluation completeEval = new CompleteEvaluation(new Date(), restaurant, "Commentaire test", "UserTest");
            completeEvalMapper.create(completeEval);
            Grade grade = null;
            if (criteria.getId() != 0 && completeEval.getId() != 0) {
                grade = new Grade(5, completeEval, criteria);
                gradeMapper.create(grade);
            }
            BasicEvaluation basicEval = new BasicEvaluation(new Date(), restaurant, true, "127.0.0.1");
            basicEvalMapper.create(basicEval);

            c.commit();
            System.out.println("Toutes les entités de test créées ✅");

            // 🔹 Suppression des tests dans le bon ordre
            if (grade != null) deleteWithLog(gradeMapper, grade.getId(), "Grade");
            deleteWithLog(completeEvalMapper, completeEval.getId(), "CompleteEvaluation");
            deleteWithLog(basicEvalMapper, basicEval.getId(), "BasicEvaluation");
            deleteWithLog(restaurantMapper, restaurant.getId(), "Restaurant");
            deleteWithLog(criteriaMapper, criteria.getId(), "EvaluationCriteria");
            deleteWithLog(cityMapper, city.getId(), "City");
            deleteWithLog(typeMapper, type.getId(), "RestaurantType");

            c.commit();
            System.out.println("Toutes les entités de test supprimées ✅");

        } catch (SQLException e) {
            System.err.println("Erreur SQL globale : " + e.getMessage());
            e.printStackTrace();
            try { c.rollback(); System.out.println("Rollback global effectué ✅"); } catch (SQLException ex) { ex.printStackTrace(); }
        } finally {
            ConnectionUtils.closeConnection();
            System.out.println("Connexion fermée ✅");
        }
    }

    private static void deleteWithLog(AbstractMapper<?> mapper, int id, String entityName) {
        System.out.println("Suppression de " + entityName + " avec ID : " + id);
        boolean success = mapper.deleteById(id);
        System.out.println(entityName + (success ? " supprimé ✅" : " NON supprimé ❌"));
    }
}
