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

            // 🔹 1️⃣ Création / récupération du type
            RestaurantType type = new RestaurantType("TestTypeMain", "Description type test");
            if (!typeMapper.existsByName(type.getLabel())) {
                typeMapper.create(type);
                System.out.println("✅ Type créé : " + type.getId() + " - " + type.getLabel());
            } else {
                type = typeMapper.findByName(type.getLabel());
                System.out.println("ℹ️ Type déjà existant : " + type.getId() + " - " + type.getLabel());
            }

            // 🔹 2️⃣ Création / récupération de la ville
            City city = new City("1000", "TestCityMain");
            if (!cityMapper.existsByName(city.getCityName())) {
                cityMapper.create(city);
                System.out.println("✅ Ville créée : " + city.getId() + " - " + city.getCityName());
            } else {
                city = cityMapper.findByName(city.getCityName());
                System.out.println("ℹ️ Ville déjà existante : " + city.getId() + " - " + city.getCityName());
            }

            // 🔹 3️⃣ Création du restaurant
            Localisation loc = new Localisation("Rue Initiale", city);
            Restaurant restaurant = new Restaurant(null, "TestRestaurantMain", "Desc test", "www.test.com", loc, null);
            restaurantMapper.create(restaurant);
            System.out.println("✅ Restaurant créé : " + restaurant.getId());
            System.out.println("Adresse avant modif : " +
                    restaurant.getAddress().getStreet() + ", " +
                    restaurant.getAddress().getCity().getCityName());

            // 🔹 4️⃣ Création / récupération de la nouvelle ville
            City newCity = new City("2000", "NouvelleVille");
            if (!cityMapper.existsByName(newCity.getCityName())) {
                cityMapper.create(newCity);
                System.out.println("✅ Nouvelle ville créée : " + newCity.getCityName());
            } else {
                newCity = cityMapper.findByName(newCity.getCityName());
                System.out.println("ℹ️ Ville déjà existante : " + newCity.getCityName());
            }

            // 🔹 5️⃣ Mise à jour adresse
            boolean updated = restaurantMapper.updateAddress(restaurant, "Nouvelle Rue 123", newCity);
            System.out.println(updated
                    ? "✅ Adresse du restaurant mise à jour avec succès"
                    : "❌ Échec de la mise à jour de l’adresse");

            System.out.println("Adresse après modif : " +
                    restaurant.getAddress().getStreet() + ", " +
                    restaurant.getAddress().getCity().getCityName());

            // 🔹 6️⃣ Suppression du restaurant
            boolean deleted = restaurantMapper.delete(restaurant);
            System.out.println(deleted
                    ? "✅ Restaurant supprimé avec succès"
                    : "❌ Échec de la suppression du restaurant");

            connection.commit();
            System.out.println("✅ Commit global effectué.");

        } catch (SQLException e) {
            System.err.println("💥 Erreur SQL : " + e.getMessage());
            e.printStackTrace();
            try {
                connection.rollback();
                System.out.println("↩️ Rollback global effectué.");
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        } finally {
            ConnectionUtils.closeConnection();
            System.out.println("🔒 Connexion fermée.");
        }
    }
}
