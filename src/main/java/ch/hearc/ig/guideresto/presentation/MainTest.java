package ch.hearc.ig.guideresto.presentation;

import ch.hearc.ig.guideresto.business.*;
import ch.hearc.ig.guideresto.services.*;

import java.util.Set;

public class MainTest {


    public static void main(String[] args) throws Exception {
        RestaurantMapper mapper = new RestaurantMapper();

        // 🔹 1️⃣ Récupérer une ville et un type existants dans la DB
        City testCity = new CityMapper().findById(1);               // ID d’une ville existante
        RestaurantType testType = new RestaurantTypeMapper().findById(1); // ID d’un type existant

        if (testCity == null || testType == null) {
            System.out.println("Erreur : il faut au moins une ville et un type existants dans la DB !");
            return;
        }

        // 🔹 2️⃣ Création d'un restaurant
        Restaurant newRest = new Restaurant(
                null,
                "TestRestaurant",
                "Description test",
                "http://test.com",
                new Localisation("123 Test Street", testCity),
                testType
        );

        // Création en DB et ajout au cache
        Restaurant created = mapper.create(newRest);
        System.out.println("Créé : " + created);

        // 🔹 3️⃣ Récupération via findById (doit passer par le cache)
        Restaurant cached = mapper.findById(created.getId());
        System.out.println("Récupéré depuis cache : " + cached);
        System.out.println("Même instance ? " + (created == cached)); // doit être true

        // 🔹 4️⃣ Mise à jour
        created.setName("TestRestaurantModifié");
        mapper.update(created);

        // Récupération à nouveau pour vérifier cache mis à jour
        Restaurant updated = mapper.findById(created.getId());
        System.out.println("Après update : " + updated.getName());

        // 🔹 5️⃣ Recherche par nom
        Set<Restaurant> byName = mapper.findByName("TestRestaurantModifié");
        System.out.println("Recherche par nom : " + byName);

        // 🔹 6️⃣ Recherche par ville
        Set<Restaurant> byCity = mapper.findByCity(testCity.getCityName());
        System.out.println("Recherche par ville : " + byCity);

        // Si tu veux récupérer un seul restaurant depuis le Set
        Restaurant firstFromCity = byCity.isEmpty() ? null : byCity.iterator().next();
        System.out.println("Premier restaurant de la ville : " + firstFromCity);

        // 🔹 7️⃣ Recherche par type
        Set<Restaurant> byType = mapper.findByRestaurantType(testType.getLabel());
        System.out.println("Recherche par type : " + byType);

        // 🔹 8️⃣ Suppression
        boolean deleted = mapper.delete(created);
        System.out.println("Suppression : " + deleted);

        // Vérification cache après suppression
        Restaurant afterDelete = mapper.findById(created.getId());
        System.out.println("Après suppression (doit être null) : " + afterDelete);
    }
}