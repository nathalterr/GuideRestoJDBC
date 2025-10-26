package ch.hearc.ig.guideresto.persistence;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class ConnectionUtils {

    private static final Logger logger = LogManager.getLogger();

    private static Connection connection;

    private ConnectionUtils() {
    }

    public static Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                createConnection();
            }
        } catch (SQLException e) {
            logger.error("Erreur en vérifiant la connexion : {}", e.getMessage(), e);
        }
        return connection;
    }

    public static void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                connection = null;
            }
        } catch (SQLException e) {
            logger.error("Erreur lors de la fermeture de la connexion : {}", e.getMessage(), e);
        }
    }

    private static void createConnection() {
        try {
            ResourceBundle dbProps = ResourceBundle.getBundle("database");
            String url = dbProps.getString("database.url");
            String username = dbProps.getString("database.username");
            String password = dbProps.getString("database.password");

            connection = DriverManager.getConnection(url, username, password);
            connection.setAutoCommit(false);

        } catch (SQLException ex) {
            logger.error("Erreur SQL lors de la création de la connexion : {}", ex.getMessage(), ex);
        } catch (MissingResourceException ex) {
            logger.error("Impossible de trouver le fichier de propriétés : {}", ex.getMessage(), ex);
        }
    }
}
