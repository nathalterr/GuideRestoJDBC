package ch.hearc.ig.guideresto.persistence;

import ch.hearc.ig.guideresto.business.*;

import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * @author cedric.baudet
 */
public class FakeItems {

    private static Set<RestaurantType> types;
    private static Set<Restaurant> restaurants;
    private static Set<EvaluationCriteria> criterias;
    private static Set<City> cities;

    private static boolean initDone = false;

    private static void init() {
        initDone = true;

        restaurants = new LinkedHashSet<>();
        types = new LinkedHashSet<>();
        criterias = new LinkedHashSet<>();
        cities = new LinkedHashSet<>();

        RestaurantType typeSuisse = new RestaurantType(1, "Cuisine suisse", "Cuisine classique et plats typiquement suisses");
        RestaurantType typeGastro = new RestaurantType(2, "Restaurant gastronomique", "Restaurant gastronomique de haut standing");

        types.add(typeSuisse);
        types.add(typeGastro);
        types.add(new RestaurantType(3, "Pizzeria", "Pizzas et autres spécialités italiennes"));

        EvaluationCriteria critService = new EvaluationCriteria(1, "Service", "Qualité du service");
        EvaluationCriteria critCuisine = new EvaluationCriteria(2, "Cuisine", "Qualité de la nourriture");
        EvaluationCriteria critCadre = new EvaluationCriteria(3, "Cadre", "L'ambiance et la décoration sont-elles bonnes ?");

        criterias.add(critService);
        criterias.add(critCuisine);
        criterias.add(critCadre);

        City city = new City(1, "2000", "Neuchatel");
        cities.add(city);

        Restaurant restaurant = new Restaurant(1, "Fleur-de-Lys", "Pizzeria au centre de Neuchâtel", "http://www.pizzeria-neuchatel.ch/", "Rue du Bassin 10", city, typeSuisse);
        city.getRestaurants().add(restaurant);
        typeSuisse.getRestaurants().add(restaurant);
        restaurant.getEvaluations().add(new BasicEvaluation(1, new Date(), restaurant, true, "1.2.3.4"));
        restaurant.getEvaluations().add(new BasicEvaluation(2, new Date(), restaurant, true, "1.2.3.5"));
        restaurant.getEvaluations().add(new BasicEvaluation(3, new Date(), restaurant, false, "1.2.3.6"));

        CompleteEvaluation ce = new CompleteEvaluation(1, new Date(), restaurant, "Génial !", "Toto");
        ce.getGrades().add(new Grade(1, 4, ce, critService));
        ce.getGrades().add(new Grade(2, 5, ce, critCuisine));
        ce.getGrades().add(new Grade(3, 4, ce, critCadre));
        restaurant.getEvaluations().add(ce);

        ce = new CompleteEvaluation(2, new Date(), restaurant, "Très bon", "Titi");
        ce.getGrades().add(new Grade(4, 4, ce, critService));
        ce.getGrades().add(new Grade(5, 4, ce, critCuisine));
        ce.getGrades().add(new Grade(6, 4, ce, critCadre));
        restaurant.getEvaluations().add(ce);

        restaurants.add(restaurant);

        restaurant = new Restaurant(2, "La Maison du Prussien", "Restaurant gastronomique renommé de Neuchâtel", "www.hotel-prussien.ch/‎", "Rue des Tunnels 11", city, typeGastro);
        typeGastro.getRestaurants().add(restaurant);
        restaurant.getEvaluations().add(new BasicEvaluation(4, new Date(), restaurant, true, "1.2.3.7"));
        restaurant.getEvaluations().add(new BasicEvaluation(5, new Date(), restaurant, true, "1.2.3.8"));
        restaurant.getEvaluations().add(new BasicEvaluation(6, new Date(), restaurant, true, "1.2.3.9"));
        ce = new CompleteEvaluation(3, new Date(), restaurant, "Un régal !", "Dupont");
        ce.getGrades().add(new Grade(7, 5, ce, critService));
        ce.getGrades().add(new Grade(8, 5, ce, critCuisine));
        ce.getGrades().add(new Grade(9, 5, ce, critCadre));
        restaurant.getEvaluations().add(ce);

        ce = new CompleteEvaluation(2, new Date(), restaurant, "Rien à dire, le top !", "Dupasquier");
        ce.getGrades().add(new Grade(10, 5, ce, critService));
        ce.getGrades().add(new Grade(11, 5, ce, critCuisine));
        ce.getGrades().add(new Grade(12, 5, ce, critCadre));
        restaurant.getEvaluations().add(ce);

        restaurants.add(restaurant);
    }

    public static Set<Restaurant> getAllRestaurants() {
        if (!initDone) {
            init();
        }

        return restaurants;
    }

    public static Set<EvaluationCriteria> getEvaluationCriterias() {
        if (!initDone) {
            init();
        }

        return criterias;
    }

    public static Set<RestaurantType> getRestaurantTypes() {
        if (!initDone) {
            init();
        }

        return types;
    }

    public static Set<City> getCities() {
        if (!initDone) {
            init();
        }

        return cities;
    }

}
