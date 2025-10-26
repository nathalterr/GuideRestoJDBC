package ch.hearc.ig.guideresto.presentation;

import ch.hearc.ig.guideresto.business.*;
import ch.hearc.ig.guideresto.persistence.mapper.*;
import java.util.Set;

public class MainTest {
    public static void main(String[] args) {
            // 🔹 Initialisation mappers

            CityMapper cityMapper = new CityMapper();
            RestaurantTypeMapper typeMapper = new RestaurantTypeMapper();
            RestaurantMapper restaurantMapper = new RestaurantMapper();

            // 🔹 Inject dependencies si nécessaire
            restaurantMapper.setDependenciesCityType(cityMapper, typeMapper);

            System.out.println("==== TEST CACHE IDENTITY MAP ====");

            // 🔹 Crée une city
        City testCity = new City(null, "CacheVille");
        testCity.setZipCode("12345"); // Obligatoire sinon insert échoue
        testCity.setId(cityMapper.getSequenceValue());
        cityMapper.create(testCity);
            // 🔹 Crée un type de restaurant
            RestaurantType testType = new RestaurantType(null, "CacheCuisine", "Test cache");
            testType = typeMapper.create(testType);

            // 🔹 Crée un restaurant
            Restaurant testRestaurant = new Restaurant(
                    null,
                    "CacheResto",
                    "Restaurant pour test cache",
                    "http://cache.test",
                    new Localisation("1 rue Cache", testCity),
                    testType
            );
            testRestaurant = restaurantMapper.create(testRestaurant);

            System.out.println("Restaurant créé: " + testRestaurant.getName() + " (id=" + testRestaurant.getId() + ")");

            // 🔹 Premier findById → doit lire la base
            System.out.println("\n-- Premier findById (devrait accéder DB) --");
            Restaurant r1 = restaurantMapper.findById(testRestaurant.getId());

            // 🔹 Second findById → doit utiliser cache
            System.out.println("\n-- Second findById (devrait utiliser cache) --");
            Restaurant r2 = restaurantMapper.findById(testRestaurant.getId());

            // 🔹 Modification de l'objet
            r2.setName("CacheRestoModifié");
            restaurantMapper.update(r2);

            // 🔹 findById après update → doit refléter le changement via cache
            System.out.println("\n-- findById après update (cache doit refléter changement) --");
            Restaurant r3 = restaurantMapper.findById(testRestaurant.getId());
            System.out.println("Nom actuel: " + r3.getName());

            // 🔹 findAll → doit remplir cache et récupérer tout
            System.out.println("\n-- findAll (devrait remplir/mettre à jour cache) --");
            Set<Restaurant> allRestaurants = restaurantMapper.findAll();
            for (Restaurant r : allRestaurants) {
                System.out.println("➡️ " + r.getName() + " (id=" + r.getId() + ")");
            }

            // 🔹 Suppression → cache doit être mis à jour
            System.out.println("\n-- Suppression restaurant --");
            restaurantMapper.delete(testRestaurant);
            Restaurant r4 = restaurantMapper.findById(testRestaurant.getId());
            System.out.println("Après suppression, findById renvoie: " + r4);
        }
    }
