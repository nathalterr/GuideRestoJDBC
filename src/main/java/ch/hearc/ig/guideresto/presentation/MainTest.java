package ch.hearc.ig.guideresto.presentation;

import ch.hearc.ig.guideresto.business.*;
import ch.hearc.ig.guideresto.services.*;

public class MainTest {
    public static void main(String[] args) {
        try {
            // --- MAPPERS ---
            CityMapper cityMapper = new CityMapper();
            RestaurantTypeMapper typeMapper = new RestaurantTypeMapper();
            RestaurantMapper restMapper = new RestaurantMapper();
            CompleteEvaluationMapper compEvalMapper = new CompleteEvaluationMapper();
            BasicEvaluationMapper basicEvalMapper = new BasicEvaluationMapper();
            EvaluationCriteriaMapper critereMapper = new EvaluationCriteriaMapper();
            GradeMapper gradeMapper = new GradeMapper();

            // --- 1️⃣ Création ville ---
            City ville = new City("TestCity", "12345");
            ville = cityMapper.create(ville);
            System.out.println("Ville créée, ID: " + ville.getId());

            // --- 2️⃣ Type gastronomique ---
            RestaurantType type = new RestaurantType("Cuisine Test", "Test description");
            type = typeMapper.create(type);
            System.out.println("Type gastronomique créé, ID: " + type.getId());

            // --- 3️⃣ Restaurant ---
            Localisation localisation = new Localisation("TestStreet", ville);
            Restaurant resto = new Restaurant("Le Testeur", "Restaurant pour tests unitaires", "http://test.com", localisation, type);
            resto = restMapper.create(resto);
            System.out.println("Restaurant créé, ID: " + resto.getId());

            // --- 4️⃣ Critères d'évaluation ---
            EvaluationCriteria crit1 = new EvaluationCriteria("Propreté", "Propreté du restaurant");
            EvaluationCriteria crit2 = new EvaluationCriteria("Service", "Qualité du service");
            crit1 = critereMapper.create(crit1);
            crit2 = critereMapper.create(crit2);
            System.out.println("Critères créés, IDs: " + crit1.getId() + ", " + crit2.getId());

            // --- 5️⃣ Évaluation complète ---
            CompleteEvaluation evalComp = new CompleteEvaluation(new java.util.Date(), resto, "Ambiance et service excellents !", "tester1");
            evalComp = compEvalMapper.create(evalComp);
            System.out.println("Évaluation complète créée, ID: " + evalComp.getId());

            // --- 6️⃣ Grades ---
            Grade grade1 = new Grade(5, evalComp, crit1);
            Grade grade2 = new Grade(4, evalComp, crit2);
            grade1 = gradeMapper.create(grade1);
            grade2 = gradeMapper.create(grade2);
            System.out.println("Grades créés, IDs: " + grade1.getId() + ", " + grade2.getId());

            // --- 7️⃣ Basic evaluation (like) ---
            BasicEvaluation basicEval = new BasicEvaluation(new java.util.Date(), resto, true, "127.0.0.1");
            basicEval = basicEvalMapper.create(basicEval);
            System.out.println("BasicEvaluation créée, ID: " + basicEval.getId());

            // --- ✅ Vérifications ---
            System.out.println("Tout a été créé avec succès !");
            System.out.println("Restaurant: " + resto.getId() + ", EvalComp: " + evalComp.getId() +
                    ", Grades: [" + grade1.getId() + ", " + grade2.getId() + "], Like: " + basicEval.getId());

            // --- 8️⃣ Nettoyage dans l'ordre correct ---
            // 1. Supprimer les Grades (dépendent de l'évaluation)
            gradeMapper.delete(grade1);
            gradeMapper.delete(grade2);

            // 2. Supprimer l'Évaluation complète / Commentaires
            compEvalMapper.delete(evalComp);

            // 3. Supprimer les Likes (BasicEvaluation)
            basicEvalMapper.delete(basicEval);

            // 4. Supprimer le Restaurant
            restMapper.delete(resto);

            // 5. Supprimer le Type et la Ville
            typeMapper.delete(type);
            cityMapper.delete(ville);

            // 6. Supprimer les Critères
            critereMapper.delete(crit1);
            critereMapper.delete(crit2);

            System.out.println("Nettoyage terminé avec succès !");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
