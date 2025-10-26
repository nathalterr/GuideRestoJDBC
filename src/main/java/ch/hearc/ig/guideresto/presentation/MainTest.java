package ch.hearc.ig.guideresto.presentation;

import ch.hearc.ig.guideresto.business.*;
import ch.hearc.ig.guideresto.persistence.mapper.*;

import java.util.Random;
import java.util.Set;

public class MainTest {

    public static void main(String[] args) throws Exception {

        System.out.println("==== INITIALISATION DES MAPPERS ====");
        CityMapper cityMapper = new CityMapper();
        RestaurantTypeMapper typeMapper = new RestaurantTypeMapper();
        GradeMapper gradeMapper = new GradeMapper();
        BasicEvaluationMapper basicEvalMapper = new BasicEvaluationMapper();
        RestaurantMapper restaurantMapper = new RestaurantMapper();
        CompleteEvaluationMapper completeEvalMapper = new CompleteEvaluationMapper();
        EvaluationCriteriaMapper critMapper = new EvaluationCriteriaMapper();

        System.out.println("==== CREATION DE LA VILLE ET DU TYPE ====");
        City city = new City("12345", "Testville");
        cityMapper.create(city);

        RestaurantType type = new RestaurantType("TestCuisine", "Cuisine de test");
        typeMapper.create(type);

        System.out.println("==== CREATION RESTAURANT ====");
        Restaurant rest = new Restaurant(
                null,
                "Le Testeur",
                "Description Test",
                "http://test.com",
                new Localisation("1 rue du Test", city),
                type
        );
        restaurantMapper.create(rest);

        System.out.println("==== CREATION BASIC EVALUATION ====");
        BasicEvaluation like1 = new BasicEvaluation(null, new java.util.Date(), rest, true, "127.0.0.1");
        BasicEvaluation like2 = new BasicEvaluation(null, new java.util.Date(), rest, false, "192.168.0.1");
        basicEvalMapper.create(like1);
        basicEvalMapper.create(like2);

        System.out.println("==== CREATION COMPLETE EVALUATION ====");
        CompleteEvaluation completeEval = new CompleteEvaluation(null, rest, "a l'aide", "userTest");
        completeEvalMapper.create(completeEval);

        System.out.println("==== CREATION GRADES SUR COMPLETE EVALUATION ====");
        Set<EvaluationCriteria> allCriteria = critMapper.findAll();
        if (allCriteria.isEmpty()) {
            critMapper.create(new EvaluationCriteria(null, "Service"));
            critMapper.create(new EvaluationCriteria(null, "Cuisine"));
            critMapper.create(new EvaluationCriteria(null, "Ambiance"));
            allCriteria = critMapper.findAll();
        }

        Random random = new Random();
        for (EvaluationCriteria crit : allCriteria) {
            Grade grade = new Grade(null, random.nextInt(6), completeEval, crit);
            gradeMapper.create(grade);
        }

        System.out.println("==== TEST: FIND ALL RESTAURANTS ====");
        Set<Restaurant> allRests = restaurantMapper.findAll();
        for (Restaurant r : allRests) {
            System.out.println("Restaurant: " + r.getName() + " (" + r.getId() + ")");
        }

        System.out.println("==== TEST: FIND BY CITY ====");
        Set<Restaurant> cityRests = restaurantMapper.findByCity("Testville");
        System.out.println("Restaurants in Testville: " + cityRests.size());

        System.out.println("==== TEST: FIND BY RESTAURANT TYPE ====");
        Set<Restaurant> typeRests = restaurantMapper.findByRestaurantType("TestCuisine");
        System.out.println("Restaurants of type TestCuisine: " + typeRests.size());

        System.out.println("==== TEST: FIND BY NAME ====");
        Set<Restaurant> foundByName = restaurantMapper.findByName("Le Testeur");
        System.out.println("Restaurants trouvés par nom : " + foundByName.size());
        for (Restaurant r : foundByName) {
            System.out.println("➡️ " + r.getName() + " (id=" + r.getId() + ")");
        }

        System.out.println("==== TEST: UPDATE RESTAURANT ====");
        rest.setName("Le Testeur Modifié");
        restaurantMapper.update(rest);
        System.out.println("Nom mis à jour: " + restaurantMapper.findById(rest.getId()).getName());

        System.out.println("==== TEST: UPDATE COMPLETE EVALUATION ====");
        completeEval.setComment("10.0.0.1");
        completeEvalMapper.update(completeEval);
        System.out.println("Commentaire mis à jour: " + completeEvalMapper.findById(completeEval.getId()).getComment());

        System.out.println("==== TEST: UPDATE BASIC EVALUATION ====");
        like1.setLikeRestaurant(false);
        basicEvalMapper.update(like1);
        System.out.println("Like1 mis à jour: " + basicEvalMapper.findById(like1.getId()).getLikeRestaurant());

        System.out.println("==== TEST: DELETE GRADES ====");
        for (Grade g : gradeMapper.findByCompleteEvaluation(completeEval)) {
            gradeMapper.delete(g);
        }
        System.out.println("Grades après suppression: " + gradeMapper.findByCompleteEvaluation(completeEval).size());

        System.out.println("==== TEST: DELETE COMPLETE EVALUATION ====");
        completeEvalMapper.delete(completeEval);
        System.out.println("CompleteEvaluations restantes: " + completeEvalMapper.findAll().size());

        System.out.println("==== TEST: DELETE BASIC EVALUATIONS ====");
        basicEvalMapper.delete(like1);
        basicEvalMapper.delete(like2);
        System.out.println("BasicEvaluations restantes: " + basicEvalMapper.findAll().size());

        System.out.println("==== TEST: DELETE RESTAURANT ====");
        restaurantMapper.delete(rest);
        System.out.println("Restaurants restantes: " + restaurantMapper.findAll().size());

        System.out.println("==== FIN DU TEST COMPLET ====");
    }
}
