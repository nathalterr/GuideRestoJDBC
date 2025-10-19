package ch.hearc.ig.guideresto.business;

import ch.hearc.ig.guideresto.persistence.AbstractMapper;
import ch.hearc.ig.guideresto.persistence.ConnectionUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class RestaurantMapper implements AbstractMapper {


    public void insert(Restaurant p) {
        Connection c = ConnectionUtils.getConnection();

        try (
                PreparedStatement s = c.prepareStatement("INSERT INTO RESTAURANT (NOM, PRENOM) ...")
        ) {
            s.setString(1, p.getName());
            s.setString(2, p.getFirstName());
            s.executeUpdate();
        } catch (SQLException e) { // évidemment, on gère les exceptidddons }
        }
    }
    public void update(Restaurant p) { ...}
    public void delete(Restaurant p) { ...}
}
