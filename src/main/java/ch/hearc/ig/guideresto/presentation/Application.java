package ch.hearc.ig.guideresto.presentation;

import ch.hearc.ig.guideresto.business.*;
//import ch.hearc.ig.guideresto.persistence.FakeItems;
import ch.hearc.ig.guideresto.persistence.mapper.*;
import ch.hearc.ig.guideresto.services.UserService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.sql.SQLException;
import java.util.*;

/**
 * @author cedric.baudet
 * @author alain.matile
 */
public class Application {

    private static Scanner scanner;
    private static final Logger logger = LogManager.getLogger(Application.class);
    private static UserService userService;

    public static void main(String[] args) {

        scanner = new Scanner(System.in);

        System.out.println("Bienvenue dans GuideResto ! Que souhaitez-vous faire ?");
        int choice;
        do {
            printMainMenu();
            choice = readInt();
            proceedMainMenu(choice);
        } while (choice != 0);
    }

    /**
     * Affichage du menu principal de l'application
     */
    private static void printMainMenu() {
        System.out.println("======================================================");
        System.out.println("Que voulez-vous faire ?");
        System.out.println("1. Afficher la liste de tous les restaurants");
        System.out.println("2. Rechercher un restaurant par son nom");
        System.out.println("3. Rechercher un restaurant par ville");
        System.out.println("4. Rechercher un restaurant par son type de cuisine");
        System.out.println("5. Saisir un nouveau restaurant");
        System.out.println("0. Quitter l'application");
    }

    /**
     * On gère le choix saisi par l'utilisateur
     *
     * @param choice Un nombre entre 0 et 5.
     */
    private static void proceedMainMenu(int choice) {
        switch (choice) {
            case 1:
                showRestaurantsList();
                break;
            case 2:
                searchRestaurantByName();
                break;
            case 3:
                searchRestaurantByCity();
                break;
            case 4:
                searchRestaurantByType();
                break;
            case 5:
                addNewRestaurant();
                break;
            case 0:
                System.out.println("Au revoir !");
                break;
            default:
                System.out.println("Erreur : saisie incorrecte. Veuillez réessayer");
                break;
        }
    }

    /**
     * On affiche à l'utilisateur une liste de restaurants numérotés, et il doit en sélectionner un !
     *
     * @param restaurants Liste à afficher
     * @return L'instance du restaurant choisi par l'utilisateur
     */
    private static Restaurant pickRestaurant(Set<Restaurant> restaurants) {
        if (restaurants.isEmpty()) { // Si la liste est vide, on s'arrête là
            System.out.println("Aucun restaurant n'a été trouvé !");
            return null;
        }

        String result;
        for (Restaurant currentRest : restaurants) {
            result = "";
            result = "\"" + result + currentRest.getName() + "\" - " + currentRest.getAddress().getStreet() + " - ";
            result = result + currentRest.getAddress().getCity().getZipCode() + " " + currentRest.getAddress().getCity().getCityName();
            System.out.println(result);
        }

        System.out.println("Veuillez saisir le nom exact du restaurant dont vous voulez voir le détail, ou appuyez sur Enter pour revenir en arrière");
        String choice = readString();

        return searchRestaurantByName(restaurants, choice);
    }

    /**
     * Affiche la liste de tous les restaurants, sans filtre
     */
    private static void showRestaurantsList() {

            System.out.println("Liste des restaurants : ");

            // ⚡ On utilise le service au lieu du mapper
            Set<Restaurant> restaurants = userService.getAllRestaurants();

            Restaurant restaurant = pickRestaurant(restaurants);

            if (restaurant != null) {
                showRestaurant(restaurant);
            }
        }
        /**
     * Affiche une liste de restaurants dont le nom contient une chaîne de caractères saisie par l'utilisateur
     */
    private static void searchRestaurantByName() {
        System.out.println("Veuillez entrer une partie du nom recherché : ");
        String research = readString();

        try {
            // ⚡ On passe par le service, plus par le mapper
            Set<Restaurant> restaurants = userService.findRestaurantsByName(research);

            if (restaurants.isEmpty()) {
                System.out.println("Aucun restaurant trouvé pour : " + research);
                return;
            }

            // L'utilisateur choisit un restaurant parmi les résultats
            Restaurant restaurant = pickRestaurant(restaurants);
            if (restaurant != null) {
                showRestaurant(restaurant);
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de la recherche des restaurants : " + e.getMessage());
            e.printStackTrace();
        }
    }


    /**
     * Affiche une liste de restaurants dont le nom de la ville contient une chaîne de caractères saisie par l'utilisateur
     */
    private static void searchRestaurantByCity() {
        System.out.print("Entrez une partie du nom de la ville : ");
        String research = readString();
        try {
            // ⚡ On passe par le service au lieu du mapper
            Set<Restaurant> filtered = userService.findRestaurantsByCity(research);

            if (filtered.isEmpty()) {
                System.out.println("Aucun restaurant trouvé dans une ville contenant : " + research);
                return;
            }

            Restaurant chosen = pickRestaurant(filtered);
            if (chosen != null) {
                showRestaurant(chosen);
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de la recherche par ville : " + e.getMessage());
            e.printStackTrace();
        }
    }
    /**
     * L'utilisateur choisit une ville parmi celles présentes dans le système.
     *
     * @param cities La liste des villes à présnter à l'utilisateur
     * @return La ville sélectionnée, ou null si aucune ville n'a été choisie.
     */
    private static City pickCity(Set<City> cities) {
        System.out.println("Villes disponibles :");
        for (City c : cities) {
            System.out.println(c.getZipCode() + " " + c.getCityName());
        }

        System.out.println("Entrez le NPA, ou 'NEW' pour créer une nouvelle ville :");
        String choice = readString();

        if (choice.equalsIgnoreCase("NEW")) {
            System.out.print("Nom de la nouvelle ville : ");
            String name = readString();
            System.out.print("Code postal : ");
            String zip = readString();

            // ⚡ Création via le service, pas le mapper
            City newCity = userService.addCity(name, zip);
            return newCity;
        } else {
            return cities.stream()
                    .filter(c -> c.getZipCode().equalsIgnoreCase(choice))
                    .findFirst()
                    .orElse(null);
        }
    }

    /**
     * L'utilisateur choisit un type de restaurant parmis ceux présents dans le système.
     *
     * @param types La liste des types de restaurant à présnter à l'utilisateur
     * @return Le type sélectionné, ou null si aucun type n'a été choisi.
     */
    private static RestaurantType pickRestaurantType(Set<RestaurantType> types) {
        System.out.println("Voici la liste des types possibles, veuillez entrer le libellé exact du type désiré : ");
        for (RestaurantType currentType : types) {
            System.out.println("\"" + currentType.getLabel() + "\" : " + currentType.getDescription());
        }
        String choice = readString();

        return searchTypeByLabel(types, choice);
    }

    /**
     * L'utilisateur commence par sélectionner un type de restaurant, puis sélectionne un des restaurants proposés s'il y en a.
     * Si l'utilisateur sélectionne un restaurant, ce dernier lui sera affiché.
     */
    private static void searchRestaurantByType() {
        try {
            // Récupère tous les types via le service si tu en as un,
            // sinon tu peux passer par un Set déjà connu
            Set<RestaurantType> types = userService.getAllTypes(); // si tu as un service pour les types
            RestaurantType chosenType = pickRestaurantType(types);
            if (chosenType == null) return;

            // ⚡ Utilisation du service pour filtrer par type
            Set<Restaurant> filtered = userService.findRestaurantsByType(chosenType.getLabel());

            if (filtered.isEmpty()) {
                System.out.println("Aucun restaurant trouvé pour le type : " + chosenType.getLabel());
                return;
            }

            Restaurant chosen = pickRestaurant(filtered);
            if (chosen != null) showRestaurant(chosen);

        } catch (Exception e) {
            System.err.println("Erreur lors de la recherche par type : " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Le programme demande les informations nécessaires à l'utilisateur puis crée un nouveau restaurant dans le système.
     */
    private static void addNewRestaurant() {
        System.out.print("Nom du restaurant : ");
        String name = readString();
        System.out.print("Description : ");
        String desc = readString();
        System.out.print("Site web : ");
        String website = readString();
        System.out.print("Rue : ");
        String street = readString();

        // Sélection ou création de la ville
        City city = null;
        do {
            city = pickCity(userService.getAllCities());
        } while (city == null);

        // Sélection du type de restaurant
        RestaurantType type = null;
        do {
            type = pickRestaurantType(userService.getAllTypes());
        } while (type == null);

        // Création via le service
        Restaurant restaurant = userService.addRestaurant(name, desc, website, street, city, type);

        if (restaurant != null) {
            System.out.println("✅ Restaurant ajouté avec succès !");
        } else {
            System.out.println("❌ Une erreur est survenue lors de l'ajout du restaurant.");
        }
    }

    /**
     * Affiche toutes les informations du restaurant passé en paramètre, puis affiche le menu des actions disponibles sur ledit restaurant
     *
     * @param restaurant Le restaurant à afficher
     */

    private static void showRestaurant(Restaurant restaurant) {
        System.out.println("Affichage d'un restaurant : ");
        StringBuilder sb = new StringBuilder();

        // 🔹 Infos générales
        sb.append(restaurant.getName()).append("\n")
                .append(restaurant.getDescription()).append("\n")
                .append(restaurant.getType().getLabel()).append("\n")
                .append(restaurant.getWebsite()).append("\n")
                .append(restaurant.getAddress().getStreet()).append(", ")
                .append(restaurant.getAddress().getCity().getZipCode()).append(" ")
                .append(restaurant.getAddress().getCity().getCityName()).append("\n");

        try {
            // 🔹 Récupérer les évaluations via les services
            Set<BasicEvaluation> basicEvalsFromDB = userService.findByRestaurant(restaurant);
            Set<CompleteEvaluation> completeEvalsFromDB = userService.findByRestaurant(restaurant);

            // 🔹 Mettre à jour les évaluations du restaurant
            restaurant.getEvaluations().removeIf(e -> e instanceof CompleteEvaluation);
            restaurant.getEvaluations().addAll(completeEvalsFromDB);

            // 🔹 Likes/dislikes
            sb.append("Nombre de likes : ").append(countLikes(basicEvalsFromDB, true)).append("\n")
                    .append("Nombre de dislikes : ").append(countLikes(basicEvalsFromDB, false)).append("\n");

            // 🔹 Afficher les CompleteEvaluations
            sb.append("\nÉvaluations complètes reçues :\n");
            for (Evaluation eval : restaurant.getEvaluations()) {
                if (eval instanceof CompleteEvaluation ce) {
                    sb.append("Evaluation de : ").append(ce.getUsername()).append("\n")
                            .append("Commentaire : ").append(ce.getComment()).append("\n");

                    if (ce.getGrades().isEmpty()) {
                        sb.append("Aucune note disponible\n");
                    } else {
                        for (Grade g : ce.getGrades()) {
                            sb.append(g.getCriteria().getName())
                                    .append(" : ").append(g.getGrade()).append("/5\n");
                        }
                    }
                    sb.append("\n"); // séparateur
                }
            }

        } catch (Exception e) {
            System.err.println("Erreur lors de la récupération des évaluations : " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println(sb);

        // 🔹 Menu actions
        int choice;
        do {
            showRestaurantMenu();
            choice = readInt();
            proceedRestaurantMenu(choice, restaurant);
        } while (choice != 0 && choice != 6);
    }


    /**
     * Parcourt la liste et compte le nombre d'évaluations basiques positives ou négatives en fonction du paramètre likeRestaurant
     *
     * @param evaluations    La liste des évaluations à parcourir
     * @param likeRestaurant Veut-on le nombre d'évaluations positives ou négatives ?
     * @return Le nombre d'évaluations positives ou négatives trouvées
     */
    private static int countLikes(Set<BasicEvaluation> evaluations, Boolean likeRestaurant) {
        int count = 0;
        for (Evaluation currentEval : evaluations) {
            if (currentEval instanceof BasicEvaluation && ((BasicEvaluation) currentEval).getLikeRestaurant() == likeRestaurant) {
                count++;
            }
        }
        return count;
    }

    /**
     * Retourne un String qui contient le détail complet d'une évaluation si elle est de type "CompleteEvaluation". Retourne null s'il s'agit d'une BasicEvaluation
     *
     * @param eval L'évaluation à afficher
     * @return Un String qui contient le détail complet d'une CompleteEvaluation, ou null s'il s'agit d'une BasicEvaluation
     */
    private static String getCompleteEvaluationDescription(Evaluation eval) {
        StringBuilder result = new StringBuilder();

        if (eval instanceof CompleteEvaluation) {
            CompleteEvaluation ce = (CompleteEvaluation) eval;
            result.append("Evaluation de : ").append(ce.getUsername()).append("\n");
            result.append("Commentaire : ").append(ce.getComment()).append("\n");
            for (Grade currentGrade : ce.getGrades()) {
                result.append(currentGrade.getCriteria().getName()).append(" : ").append(currentGrade.getGrade()).append("/5").append("\n");
            }
        }

        return result.toString();
    }

    /**
     * Affiche dans la console un ensemble d'actions réalisables sur le restaurant actuellement sélectionné !
     */
    private static void showRestaurantMenu() {
        System.out.println("======================================================");
        System.out.println("Que souhaitez-vous faire ?");
        System.out.println("1. J'aime ce restaurant !");
        System.out.println("2. Je n'aime pas ce restaurant !");
        System.out.println("3. Faire une évaluation complète de ce restaurant !");
        System.out.println("4. Editer ce restaurant");
        System.out.println("5. Editer l'adresse du restaurant");
        System.out.println("6. Supprimer ce restaurant");
        System.out.println("0. Revenir au menu principal");
    }

    /**
     * Traite le choix saisi par l'utilisateur
     *
     * @param choice     Un numéro d'action, entre 0 et 6. Si le numéro ne se trouve pas dans cette plage, l'application ne fait rien et va réafficher le menu complet.
     * @param restaurant L'instance du restaurant sur lequel l'action doit être réalisée
     */
    private static void proceedRestaurantMenu(int choice, Restaurant restaurant) {
        switch (choice) {
            case 1:
                addBasicEvaluation(restaurant, true);
                break;
            case 2:
                addBasicEvaluation(restaurant, false);
                break;
            case 3:
                evaluateRestaurant(restaurant);
                break;
            case 4:
                editRestaurant(restaurant);
                break;
            case 5:
                editRestaurantAddress(restaurant);
                break;
            case 6:
                deleteRestaurant(restaurant);
                break;
            case 0:
                break;
            default:
                break;
        }
    }

    /**
     * Ajoute au restaurant passé en paramètre un like ou un dislike, en fonction du second paramètre.
     * L'IP locale de l'utilisateur est enregistrée. S'il s'agissait d'une application web, il serait préférable de récupérer l'adresse IP publique de l'utilisateur.
     *
     * @param restaurant Le restaurant qui est évalué
     * @param like       Est-ce un like ou un dislike ?
     */
    private static void addBasicEvaluation(Restaurant restaurant, Boolean like) {
        Restaurant myRestaurant = userService.getAllRestaurants().iterator().next(); // juste pour l'exemple
        userService.addBasicEvaluation(myRestaurant, like);
        System.out.println("Votre vote a été pris en compte !");
    }

    /**
     * Crée une évaluation complète pour le restaurant. L'utilisateur doit saisir toutes les informations (dont un commentaire et quelques notes)
     *
     * @param restaurant Le restaurant à évaluer
     */
    private static void evaluateRestaurant(Restaurant restaurant) {
        System.out.print("Nom d'utilisateur : ");
        String username = readString();

        System.out.print("Commentaire : ");
        String comment = readString();

        // Lire les notes pour chaque critère
        Map<EvaluationCriteria, Integer> notes = new HashMap<>();
        Set<EvaluationCriteria> criteres = new EvaluationCriteriaMapper().findAll();
        for (EvaluationCriteria crit : criteres) {
            int note;
            do {
                System.out.print(crit.getName() + " (1-5) : ");
                note = readInt();
            } while (note < 1 || note > 5);
            notes.put(crit, note);
        }

        // Déleguer à UserService
        userService.addCompleteEvaluation(restaurant, username, comment, notes);

        System.out.println("✅ Évaluation enregistrée avec succès !");
    }



    /**
     * Force l'utilisateur à saisir à nouveau toutes les informations du restaurant (sauf la clé primaire) pour le mettre à jour.
     * Par soucis de simplicité, l'utilisateur doit tout resaisir.
     *
     * @param restaurant Le restaurant à modifier
     */
    private static void editRestaurant(Restaurant restaurant) {
        System.out.println("Edition d'un restaurant !");

        System.out.print("Nouveau nom : ");
        String newName = readString();

        System.out.print("Nouvelle description : ");
        String newDescription = readString();

        System.out.print("Nouveau site web : ");
        String newWebsite = readString();

        RestaurantType newType = pickRestaurantType(userService.getAllTypes());

        System.out.print("Nouvelle rue : ");
        String newStreet = readString();

        System.out.print("Nom de la ville : ");
        String cityName = readString();

        City dbCity = userService.findCityByName(cityName);
        if (dbCity == null) {
            System.out.print("Code postal pour la nouvelle ville : ");
            String postalCode = readString();
            dbCity = userService.addOrGetCity(cityName, postalCode);
            System.out.println("Nouvelle ville créée : " + dbCity.getCityName());
        }

        boolean updated = userService.updateRestaurantDetails(restaurant, newName, newDescription, newWebsite, newType, newStreet, dbCity);
        System.out.println(updated ? "Restaurant mis à jour avec succès !" : "Erreur lors de la mise à jour.");
    }



    /**
     * Permet à l'utilisateur de mettre à jour l'adresse du restaurant.
     * Par soucis de simplicité, l'utilisateur doit tout resaisir.
     *
     * @param restaurant Le restaurant dont l'adresse doit être mise à jour.
     */
    private static void editRestaurantAddress(Restaurant restaurant) {
        System.out.println("Edition de l'adresse d'un restaurant !");

        System.out.print("Nouvelle rue : ");
        String newStreet = readString();

        System.out.print("Nom de la ville : ");
        String cityName = readString();

        City dbCity = userService.findCityByName(cityName);
        String postalCode = null;
        if (dbCity == null) {
            System.out.print("Code postal pour la nouvelle ville : ");
            postalCode = readString();
        }

        boolean updated = userService.updateRestaurantAddress(restaurant, newStreet, cityName, postalCode);
        System.out.println(updated ? "Adresse mise à jour avec succès !" : "Erreur lors de la mise à jour.");
    }


    /**
     * Après confirmation par l'utilisateur, supprime complètement le restaurant et toutes ses évaluations du référentiel.
     *
     * @param restaurant Le restaurant à supprimer.
     */
    private static void deleteRestaurant(Restaurant restaurant) {
        System.out.println("Etes-vous sûr de vouloir supprimer ce restaurant ? (O/n)");
        String choice = readString();
        if (choice.equalsIgnoreCase("o")) {
            boolean deleted = userService.deleteRestaurantService(restaurant);
            System.out.println(deleted ? "Restaurant supprimé avec succès !" : "Erreur lors de la suppression.");
        } else {
            System.out.println("Suppression annulée.");
        }
    }


    /**
     * Recherche dans le Set le restaurant comportant le nom passé en paramètre.
     * Retourne null si le restaurant n'est pas trouvé.
     *
     * @param restaurants Set de restaurants
     * @param name        Nom du restaurant à rechercher
     * @return L'instance du restaurant ou null si pas trouvé
     */
    private static Restaurant searchRestaurantByName(Set<Restaurant> restaurants, String name) {
        for (Restaurant current : restaurants) {
            if (current.getName().equalsIgnoreCase(name)) {
                return current;
            }
        }
        return null;
    }

    /**
     * Recherche dans le Set la ville comportant le code NPA passé en paramètre.
     * Retourne null si la ville n'est pas trouvée
     *
     * @param cities  Set de villes
     * @param zipCode NPA de la ville à rechercher
     * @return L'instance de la ville ou null si pas trouvé
     */
    private static City searchCityByZipCode(Set<City> cities, String zipCode) {
        for (City current : cities) {
            if (current.getZipCode().equalsIgnoreCase(zipCode)) {
                return current;
            }
        }
        return null;
    }

    /**
     * Recherche dans le Set le type comportant le libellé passé en paramètre.
     * Retourne null si aucun type n'est trouvé.
     *
     * @param types Set de types de restaurant
     * @param label Libellé du type recherché
     * @return L'instance RestaurantType ou null si pas trouvé
     */
    private static RestaurantType searchTypeByLabel(Set<RestaurantType> types, String label) {
        for (RestaurantType current : types) {
            if (current.getLabel().equalsIgnoreCase(label)) {
                return current;
            }
        }
        return null;
    }

    /**
     * readInt ne repositionne pas le scanner au début d'une ligne donc il faut le faire manuellement sinon
     * des problèmes apparaissent quand on demande à l'utilisateur de saisir une chaîne de caractères.
     *
     * @return Un nombre entier saisi par l'utilisateur au clavier
     */
    private static int readInt() {
        int i = 0;
        boolean success = false;
        do { // Tant que l'utilisateur n'aura pas saisi un nombre entier, on va lui demander une nouvelle saisie
            try {
                i = scanner.nextInt();
                success = true;
            } catch (InputMismatchException e) {
                System.out.println("Erreur ! Veuillez entrer un nombre entier s'il vous plaît !");
            } finally {
                scanner.nextLine();
            }

        } while (!success);

        return i;
    }

    /**
     * Méthode readString pour rester consistant avec readInt !
     *
     * @return Une chaîne de caractères saisie par l'utilisateur au clavier
     */
    private static String readString() {
        return scanner.nextLine();
    }

}
