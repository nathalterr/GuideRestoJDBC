package ch.hearc.ig.guideresto.presentation;

import ch.hearc.ig.guideresto.business.*;
import ch.hearc.ig.guideresto.services.*;
import ch.hearc.ig.guideresto.persistence.ConnectionUtils;

import java.sql.Connection;
import java.sql.SQLException;
public class MainTest {
    public static void main(String[] args) {
        Connection connection = ConnectionUtils.getConnection();

        RestaurantTypeMapper typeMapper = new RestaurantTypeMapper();
        CityMapper cityMapper = new CityMapper();
        RestaurantMapper restaurantMapper = new RestaurantMapper();

        try {
            System.out.println("===== TEST RESTAURANT + CITY MAPPERS =====");

            // Type
            RestaurantType type = new RestaurantType("TestTyp3eMain2", "D23escription type test");
            RestaurantType createdType = typeMapper.create(type);
            if (createdType == null) {
                createdType = typeMapper.findByName(type.getLabel());
                System.out.println("Fallback findByName type -> " + (createdType != null ? createdType.getId() : "null"));
            }
            System.out.println("Type final id=" + (createdType != null ? createdType.getId() : "null"));

            // City
            City city = new City("1000", "TestCityMain");
            City createdCity = cityMapper.create(city);
            if (createdCity == null) {
                createdCity = cityMapper.findByName(city.getCityName());
                System.out.println("Fallback findByName city -> " + (createdCity != null ? createdCity.getId() : "null"));
            }
            System.out.println("City final id=" + (createdCity != null ? createdCity.getId() : "null"));

            // Restaurant (utilise les objets retournés)
            Localisation loc = new Localisation("Rue Initiale", createdCity);
            Restaurant restaurant = new Restaurant(null, "TestRestaurantMain", "Desc test", "www.test.com", loc, createdType);
            Restaurant createdRestaurant = restaurantMapper.create(restaurant);
            System.out.println("Restaurant final id=" + (createdRestaurant != null ? createdRestaurant.getId() : "null"));

            connection.commit();
            System.out.println("✅ Commit global effectué.");
        } catch (Exception e) {
            e.printStackTrace();
            try { connection.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
        } finally {
            ConnectionUtils.closeConnection();
            System.out.println("🔒 Connexion fermée.");
        }
    }
}
