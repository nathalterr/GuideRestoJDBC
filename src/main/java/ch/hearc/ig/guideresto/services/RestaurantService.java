package ch.hearc.ig.guideresto.services;

import ch.hearc.ig.guideresto.business.*;
import ch.hearc.ig.guideresto.persistence.mapper.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Set;

public class RestaurantService {

    private final RestaurantMapper restaurantMapper;
    private final CityMapper cityMapper;
    private final RestaurantTypeMapper typeMapper;

    public RestaurantService(RestaurantMapper restaurantMapper,
                             CityMapper cityMapper,
                             RestaurantTypeMapper typeMapper) {
        this.restaurantMapper = restaurantMapper;
        this.cityMapper = cityMapper;
        this.typeMapper = typeMapper;
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
        Connection conn = restaurantMapper.getConnection();
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
}
