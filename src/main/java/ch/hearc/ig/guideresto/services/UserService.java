package ch.hearc.ig.guideresto.services;

import ch.hearc.ig.guideresto.business.City;
import ch.hearc.ig.guideresto.business.Restaurant;
import ch.hearc.ig.guideresto.business.RestaurantType;

import ch.hearc.ig.guideresto.business.RestaurantType;
import ch.hearc.ig.guideresto.persistence.mapper.RestaurantTypeMapper;
import ch.hearc.ig.guideresto.business.*;
import ch.hearc.ig.guideresto.persistence.mapper.*;
import ch.hearc.ig.guideresto.business.City;
import ch.hearc.ig.guideresto.persistence.mapper.CityMapper;

import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.util.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Set;
import java.util.Set;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Set;
import java.util.stream.Collectors;

import static ch.hearc.ig.guideresto.persistence.ConnectionUtils.getConnection;
import static ch.hearc.ig.guideresto.presentation.Application.readString;

public class UserService {
    private Connection connection;
    private CityMapper cityMapper;
    private RestaurantTypeMapper typeMapper;
    private GradeMapper gradeMapper;
    private RestaurantMapper restaurantMapper;
    private EvaluationCriteriaMapper evaluationCriteriaMapper;
    private BasicEvaluationMapper basicEvaluation;
    private CompleteEvaluationMapper completeEvaluationMapper;

    public UserService() {
        MapperFactory mapperFactory = new MapperFactory();
        cityMapper = mapperFactory.getCityMapper();
        typeMapper = mapperFactory.getTypeMapper();
        gradeMapper = mapperFactory.getGradeMapper();
        restaurantMapper = mapperFactory.getRestaurantMapper();
        evaluationCriteriaMapper = mapperFactory.getCriteriaMapper();
        basicEvaluation = mapperFactory.getBasicEvalMapper();
        completeEvaluationMapper = mapperFactory.getCompleteEvalMapper();
    }
    public Set<City> getAllCities() {
        return cityMapper.findAll();
    }

    public City findCityByName(String name) {
        try {
            return cityMapper.findByName(name);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public City addCity(String name, String zipCode) {
        City city = new City(null, zipCode, name);
        return cityMapper.create(city);
    }
    // Récupère tous les types
    public Set<RestaurantType> getAllTypes() {
        return typeMapper.findAll();
    }

    // Crée un nouveau type (optionnel)
    public RestaurantType addType(String label, String description) {
        RestaurantType type = new RestaurantType(null, label, description);
        return typeMapper.create(type);
    }

    // Recherche par libellé
    public RestaurantType findByLabel(String label) {
        return typeMapper.findByLabel(label);
    }
    public Set<Restaurant> getAllRestaurants() {
        return restaurantMapper.findAll();
    }

    public Set<Restaurant> findRestaurantsByName(String name) throws SQLException{
        return restaurantMapper.findByName(name);
    }

    public Set<Restaurant> findRestaurantsByCity(String cityName) {
        Set<Restaurant> all = restaurantMapper.findAll();
        all.removeIf(r -> !r.getAddress().getCity().getCityName().equalsIgnoreCase(cityName));
        return all;
    }

    public Set<Restaurant> findRestaurantsByType(String typeLabel) {
        RestaurantType type = typeMapper.findByLabel(typeLabel);
        if (type == null) return Set.of();
        Set<Restaurant> all = restaurantMapper.findAll();
        all.removeIf(r -> !r.getType().getId().equals(type.getId()));
        return all;
    }

    public Restaurant addRestaurant(String name, String description, String website,
                                    String street, City city, RestaurantType type) {
        Restaurant restaurant = new Restaurant(null, name, description, website, street, city, type);

        // Gestion transactionnelle simple
        Connection conn = getConnection();
        boolean autoCommit = true;
        try {
            autoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);

            restaurantMapper.create(restaurant);

            conn.commit();
        } catch (SQLException e) {
            try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            return null;
        } finally {
            try { conn.setAutoCommit(autoCommit); } catch (SQLException ignored) {}
        }
        return restaurant;
    }

    public boolean updateRestaurant(Restaurant restaurant) {
        return restaurantMapper.update(restaurant);
    }

    public boolean updateRestaurantAddress(Restaurant restaurant, String newStreet, City city) throws SQLException{
        return restaurantMapper.updateAddress(restaurant, newStreet, city);
    }

    public boolean deleteRestaurantService(Restaurant restaurant) {
        return restaurantMapper.delete(restaurant);
    }


    public int countLikes(Restaurant restaurant, boolean like) {
        if (restaurant == null || restaurant.getEvaluations() == null) return 0;

        int count = 0;
        for (Evaluation currentEval : restaurant.getEvaluations()) {
            if (currentEval instanceof BasicEvaluation) {
                Boolean b = ((BasicEvaluation) currentEval).getLikeRestaurant();
                if (b != null && b == like) {
                    count++;
                }
            }
        }
        return count;
    }


    public Set<Evaluation> getEvaluations(Restaurant restaurant) {
        return restaurant != null ? restaurant.getEvaluations() : Set.of();
    }
    public BasicEvaluation addBasicEvaluation(Restaurant restaurant, Boolean like) {
        if (restaurant == null || like == null) return null;

        String ipAddress;
        try {
            ipAddress = Inet4Address.getLocalHost().toString();
        } catch (UnknownHostException ex) {
            ipAddress = "Indisponible";
            throw new RuntimeException(ex);
        }

        BasicEvaluation eval = new BasicEvaluation(null, new Date(), restaurant, like, ipAddress);

        // Persistance
        BasicEvaluationMapper BEM = new BasicEvaluationMapper();
        BEM.create(eval);

        // Ajout à l'objet restaurant
        restaurant.getEvaluations().add(eval);

        return eval;
    }

    public CompleteEvaluation addCompleteEvaluation(Restaurant restaurant, String username, String comment, Map<EvaluationCriteria, Integer> notes) {
        if (restaurant == null || username == null || notes == null) return null;

        CompleteEvaluation eval = new CompleteEvaluation(null, new Date(), restaurant, comment, username);

        for (Map.Entry<EvaluationCriteria, Integer> entry : notes.entrySet()) {
            Grade grade = new Grade(null, entry.getValue(), eval, entry.getKey());
            eval.getGrades().add(grade);
        }

        // Persistance
        CompleteEvaluationMapper evalMapper = new CompleteEvaluationMapper();
        evalMapper.create(eval);

        GradeMapper gradeMapper = new GradeMapper();
        for (Grade g : eval.getGrades()) {
            gradeMapper.create(g);
        }

        // Ajouter à l'objet restaurant
        restaurant.getEvaluations().add(eval);

        return eval;
    }
    public boolean updateRestaurantDetails(Restaurant restaurant, String newName, String newDescription,
                                           String newWebsite, RestaurantType newType,
                                           String newStreet, City newCity) {
        if (restaurant == null || newName == null || newStreet == null || newCity == null) return false;

        // Mise à jour de l'objet
        restaurant.setName(newName);
        restaurant.setDescription(newDescription);
        restaurant.setWebsite(newWebsite);
        if (newType != null) {
            restaurant.setType(newType);
        }
        restaurant.getAddress().setStreet(newStreet);
        restaurant.getAddress().setCity(newCity);

        // Persistance
        return restaurantMapper.update(restaurant);
    }

    public City addOrGetCity(String cityName, String postalCode) throws SQLException{
        City city = cityMapper.findByName(cityName);
        if (city == null) {
            city = new City(null, postalCode, cityName);
            cityMapper.create(city);
        }
        return city;
    }
    public Set<BasicEvaluation> getBasicEvaluations(Restaurant restaurant) {
        if (restaurant == null) return Set.of();

        // Suppose que BasicEvaluationMapper a une méthode findByRestaurant
        Set<BasicEvaluation> basicEvals = new BasicEvaluationMapper().findByRestaurant(restaurant);

        // Ajoute au restaurant pour que countLikes fonctionne
        restaurant.getEvaluations().removeIf(e -> e instanceof BasicEvaluation);
        restaurant.getEvaluations().addAll(basicEvals);

        return basicEvals;
    }
    public Set<CompleteEvaluation> getCompleteEvaluations(Restaurant restaurant) {
        if (restaurant == null) return Set.of();

        // 🔹 Récupère depuis le mapper
        Set<CompleteEvaluation> completeEvalsFromDB = completeEvaluationMapper.findByRestaurant(restaurant);

        // 🔹 Ajoute au restaurant pour que la méthode fonctionne ensuite
        restaurant.getEvaluations().removeIf(e -> e instanceof CompleteEvaluation);
        restaurant.getEvaluations().addAll(completeEvalsFromDB);

        return completeEvalsFromDB;
    }


}
