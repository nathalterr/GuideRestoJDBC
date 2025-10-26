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

import java.util.Set;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Set;
import java.util.Set;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Set;

import static ch.hearc.ig.guideresto.persistence.ConnectionUtils.getConnection;

public class UserService {
    private Connection connection;
    private CityMapper cityMapper;
    private RestaurantTypeMapper typeMapper;
    private GradeMapper gradeMapper;
    private RestaurantMapper restaurantMapper;
    private EvaluationCriteria evaluationCriteria;
    private BasicEvaluation basicEvaluation;
    private CompleteEvaluationMapper completeEvaluationMapper;

    public UserService() {
        MapperFactory mapperFactory = new MapperFactory();
        cityMapper = mapperFactory.getCityMapper();
        typeMapper = mapperFactory.getTypeMapper();
        gradeMapper = mapperFactory.getGradeMapper();
        restaurantMapper = mapperFactory.getRestaurantMapper();
        evaluationCriteria = new EvaluationCriteria();
        basicEvaluation = new BasicEvaluation();
        completeEvaluationMapper = new CompleteEvaluationMapper();
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

    public Set<Restaurant> findRestaurantsByName(String name) {
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

    public boolean updateRestaurantAddress(Restaurant restaurant, String newStreet, City city) {
        return restaurantMapper.updateAddress(restaurant, newStreet, city);
    }

    public boolean deleteRestaurant(Restaurant restaurant) {
        return restaurantMapper.delete(restaurant);
    }

    public RestaurantType findTypeInSetByLabel(Set<RestaurantType> types, String label) {
        return types.stream()
                .filter(t -> t.getLabel().equalsIgnoreCase(label))
                .findFirst()
                .orElse(null);
    }

    public City findCityByZipCode(Set<City> cities, String zipCode) {
        return cities.stream()
                .filter(c -> c.getZipCode().equalsIgnoreCase(zipCode))
                .findFirst()
                .orElse(null);
    }

}
