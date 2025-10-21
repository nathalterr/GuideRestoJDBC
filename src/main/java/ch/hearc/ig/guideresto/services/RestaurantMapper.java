package ch.hearc.ig.guideresto.services;

import ch.hearc.ig.guideresto.business.IBusinessObject;
import ch.hearc.ig.guideresto.business.Restaurant;
import ch.hearc.ig.guideresto.persistence.AbstractMapper;
import ch.hearc.ig.guideresto.persistence.ConnectionUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Set;

public class RestaurantMapper extends AbstractMapper {


    public void insert(Restaurant p) {
        Connection c = ConnectionUtils.getConnection();

        try (
                PreparedStatement s = c.prepareStatement("INSERT INTO RESTAURANT (NOM, DESCRIPTION, WEBSITE, EVALUATIONS, ADDRESS, TYPE) VALUES ()")
        ) {
            s.setString(1, p.getName());
            s.executeUpdate();
        } catch (SQLException e) { // évidemment, on gère les exceptidddons }
        }
    }
    public void update(Restaurant p) { }
    public void delete(Restaurant p) { }
    public void findByName(Restaurant p) {
        Connection c = ConnectionUtils.getConnection();
    }
    public Set findAll(){
        return null;
    }

    protected boolean isCacheEmpty() {return false; }
    public boolean exists(Integer id) {return false;}
    public int count() {return 1;}
    protected Integer getSequenceValue() {return 1;}
    public  IBusinessObject findById(int id){return null;};

    public  IBusinessObject create(IBusinessObject object){return null;};
    public  boolean update(IBusinessObject object){return false;};
    public  boolean delete(IBusinessObject object){return false;};
    public boolean deleteById(int id){return false;};
    protected String getSequenceQuery(){return "salu";};
    protected String getExistsQuery(){return "salu";}
    protected String getCountQuery(){return "salu";};
}
