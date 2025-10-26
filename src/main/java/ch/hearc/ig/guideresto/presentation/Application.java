package ch.hearc.ig.guideresto.presentation;

import ch.hearc.ig.guideresto.business.*;
//import ch.hearc.ig.guideresto.persistence.FakeItems;
import ch.hearc.ig.guideresto.persistence.mapper.*;
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
        RestaurantMapper restaurantMapper = new RestaurantMapper();
        Restaurant restaurant = pickRestaurant(restaurantMapper.findAll());

        if (restaurant != null) { // Si l'utilisateur a choisi un restaurant, on l'affiche, sinon on ne fait rien et l'application va réafficher le menu principal
            showRestaurant(restaurant);
        }
    }

    /**
     * Affiche une liste de restaurants dont le nom contient une chaîne de caractères saisie par l'utilisateur
     */
    private static void searchRestaurantByName() {
        System.out.println("Veuillez entrer une partie du nom recherché : ");
        String research = readString();

        RestaurantMapper restaurantMapper = new RestaurantMapper();
        try {
            // Récupère directement tous les restaurants dont le nom contient la chaîne recherchée
            Set<Restaurant> restaurants = restaurantMapper.findByName(research);

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
        RestaurantMapper restaurantMapper = new RestaurantMapper();
        Set<Restaurant> allRestaurants = restaurantMapper.findAll();
        Set<Restaurant> filtered = new LinkedHashSet<>();
        for (Restaurant rest : allRestaurants) {
            if (rest.getAddress().getCity().getCityName().toUpperCase().contains(research.toUpperCase()))
                filtered.add(rest);
        }
        Restaurant chosen = pickRestaurant(filtered);
        if (chosen != null) showRestaurant(chosen);
    }
    /**
     * L'utilisateur choisit une ville parmi celles présentes dans le système.
     *
     * @param cities La liste des villes à présnter à l'utilisateur
     * @return La ville sélectionnée, ou null si aucune ville n'a été choisie.
     */
    private static City pickCity(Set<City> cities) {
        System.out.println("Villes disponibles :");
        for (City c : cities) System.out.println(c.getZipCode() + " " + c.getCityName());
        System.out.println("Entrez le NPA, ou 'NEW' pour créer une nouvelle ville :");
        String choice = readString();

        CityMapper cityMapper = new CityMapper();
        if (choice.equalsIgnoreCase("NEW")) {
            System.out.print("Nom de la nouvelle ville : ");
            String name = readString();
            System.out.print("Code postal : ");
            String zip = readString();
            City newCity = new City(null, name, zip);
            cityMapper.create(newCity);
            return newCity;
        } else {
            return cities.stream()
                    .filter(c -> c.getZipCode().equalsIgnoreCase(choice))
                    .findFirst().orElse(null);
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
        RestaurantTypeMapper typeMapper = new RestaurantTypeMapper();
        RestaurantMapper restaurantMapper = new RestaurantMapper();

        Set<RestaurantType> types = typeMapper.findAll();
        RestaurantType chosenType = pickRestaurantType(types);
        if (chosenType == null) return;

        Set<Restaurant> all = restaurantMapper.findAll();
        Set<Restaurant> filtered = new LinkedHashSet<>();
        for (Restaurant rest : all) {
            if (rest.getType().getId() == chosenType.getId())
                filtered.add(rest);
        }

        Restaurant chosen = pickRestaurant(filtered);
        if (chosen != null) showRestaurant(chosen);
    }

    /**
     * Le programme demande les informations nécessaires à l'utilisateur puis crée un nouveau restaurant dans le système.
     */
    private static void addNewRestaurant() {
        RestaurantMapper restMapper = new RestaurantMapper();
        CityMapper cityMapper = new CityMapper();
        RestaurantTypeMapper typeMapper = new RestaurantTypeMapper();

        System.out.print("Nom du restaurant : ");
        String name = readString();
        System.out.print("Description : ");
        String desc = readString();
        System.out.print("Site web : ");
        String website = readString();
        System.out.print("Rue : ");
        String street = readString();

        City city = null;
        do {
            city = pickCity(cityMapper.findAll());
        } while (city == null);

        RestaurantType type = null;
        do {
            type = pickRestaurantType(typeMapper.findAll());
        } while (type == null);

        Restaurant restaurant = new Restaurant(null, name, desc, website, street, city, type);
        restMapper.create(restaurant);
        System.out.println("✅ Restaurant ajouté avec succès !");
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

        // 🔹 BasicEvaluations
        BasicEvaluationMapper bem = new BasicEvaluationMapper();
        Set<BasicEvaluation> basicEvalsFromDB = bem.findByRestaurant(restaurant);

        // 🔹 CompleteEvaluations avec leurs grades
        CompleteEvaluationMapper cem = new CompleteEvaluationMapper();
        Set<CompleteEvaluation> completeEvalsFromDB = cem.findByRestaurant(restaurant);

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
        String ipAddress;
        try {
            ipAddress = Inet4Address.getLocalHost().toString(); // Permet de retrouver l'adresse IP locale de l'utilisateur.
        } catch (UnknownHostException ex) {
            logger.error("Error - Couldn't retreive host IP address");
            ipAddress = "Indisponible";
        }
        BasicEvaluation eval = new BasicEvaluation(1, new Date(), restaurant, like, ipAddress);
        restaurant.getEvaluations().add(eval);
        BasicEvaluationMapper BEM =  new BasicEvaluationMapper();
        BEM.create(eval);

        System.out.println("Votre vote a été pris en compte !");
    }

    /**
     * Crée une évaluation complète pour le restaurant. L'utilisateur doit saisir toutes les informations (dont un commentaire et quelques notes)
     *
     * @param restaurant Le restaurant à évaluer
     */
    private static void evaluateRestaurant(Restaurant restaurant) {
        EvaluationCriteriaMapper criteriaMapper = new EvaluationCriteriaMapper();
        CompleteEvaluationMapper evalMapper = new CompleteEvaluationMapper();
        GradeMapper gradeMapper = new GradeMapper();

        System.out.print("Nom d'utilisateur : ");
        String username = readString();

        System.out.print("Commentaire : ");
        String comment = readString();

        // 🔹 1. Créer l'évaluation en mémoire
        CompleteEvaluation eval = new CompleteEvaluation(null, new Date(), restaurant, comment, username);

        // 🔹 2. Ajouter les notes à l'évaluation
        Set<EvaluationCriteria> criteres = criteriaMapper.findAll();
        for (EvaluationCriteria crit : criteres) {
            int note;
            do {
                System.out.print(crit.getName() + " (1-5) : ");
                note = readInt();
            } while (note < 1 || note > 5); // Validation simple
            Grade grade = new Grade(null, note, eval, crit);
            eval.getGrades().add(grade);
        }

        // 🔹 3. Persister l'évaluation et récupérer son ID
        evalMapper.create(eval);

        // 🔹 4. Persister chaque grade lié à cette évaluation
        for (Grade g : eval.getGrades()) {
            gradeMapper.create(g);
        }

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

        // 🔹 1. Nom, description, site web
        System.out.print("Nouveau nom : ");
        restaurant.setName(readString());

        System.out.print("Nouvelle description : ");
        restaurant.setDescription(readString());

        System.out.print("Nouveau site web : ");
        restaurant.setWebsite(readString());

        // 🔹 2. Type de restaurant
        RestaurantTypeMapper rtm = new RestaurantTypeMapper();
        RestaurantType newType = pickRestaurantType(rtm.findAll());
        if (newType != null && newType != restaurant.getType()) {
            restaurant.setType(newType);
        }

        // 🔹 3. Adresse (rue + ville)
        System.out.print("Nouvelle rue : ");
        String newStreet = readString();

        System.out.print("Nom de la ville : ");
        String cityName = readString();

        CityMapper cityMapper = new CityMapper();
        try {
            City dbCity = cityMapper.findByName(cityName);
            if (dbCity == null) {
                System.out.print("Code postal pour la nouvelle ville : ");
                String postalCode = readString();

                dbCity = new City(null, postalCode, cityName);
                cityMapper.create(dbCity);
                System.out.println("Nouvelle ville créée : " + dbCity.getCityName());
            }
            restaurant.getAddress().setStreet(newStreet);
            restaurant.getAddress().setCity(dbCity);

            // 🔹 4. Mise à jour en base
            RestaurantMapper restaurantMapper = new RestaurantMapper();
            boolean updated = restaurantMapper.update(restaurant);
            if (updated) {
                System.out.println("Restaurant mis à jour avec succès !");
            } else {
                System.out.println("Erreur lors de la mise à jour du restaurant.");
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la mise à jour de l'adresse : " + e.getMessage());
            e.printStackTrace();
        }
    }


    /**
     * Permet à l'utilisateur de mettre à jour l'adresse du restaurant.
     * Par soucis de simplicité, l'utilisateur doit tout resaisir.
     *
     * @param restaurant Le restaurant dont l'adresse doit être mise à jour.
     */
    private static void editRestaurantAddress(Restaurant restaurant) {
        System.out.println("Edition de l'adresse d'un restaurant !");

        // 🔹 1. Demande de la nouvelle rue
        System.out.print("Nouvelle rue : ");
        String newStreet = readString();

        // 🔹 2. Demande du nom de la ville
        System.out.print("Nom de la ville : ");
        String cityName = readString();

        CityMapper cityMapper = new CityMapper();
        RestaurantMapper restaurantMapper = new RestaurantMapper();

        try {
            // 🔹 3. Vérifie si la ville existe déjà dans la DB
            City dbCity = cityMapper.findByName(cityName);
            if (dbCity == null) {
                // Si la ville n'existe pas, création
                System.out.print("Code postal pour la nouvelle ville : ");
                String postalCode = readString();

                dbCity = new City(null, cityName, postalCode);
                cityMapper.create(dbCity);
                System.out.println("Nouvelle ville créée : " + dbCity.getCityName());
            }

            // 🔹 4. Mise à jour de l'adresse du restaurant
            boolean updated = restaurantMapper.updateAddress(restaurant, newStreet, dbCity);
            if (updated) {
                System.out.println("Adresse mise à jour avec succès !");
            } else {
                System.out.println("Erreur lors de la mise à jour de l'adresse.");
            }

        } catch (SQLException ex) {
            System.err.println("Erreur lors de la mise à jour de l'adresse : " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    /**
     * Après confirmation par l'utilisateur, supprime complètement le restaurant et toutes ses évaluations du référentiel.
     *
     * @param restaurant Le restaurant à supprimer.
     */
    private static void deleteRestaurant(Restaurant restaurant) {
        System.out.println("Etes-vous sûr de vouloir supprimer ce restaurant ? (O/n)");
        String choice = readString();
        if (choice.equals("o") || choice.equals("O")) {
            RestaurantMapper restaurantMapper = new RestaurantMapper();
            restaurantMapper.delete(restaurant);
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
