package ch.hearc.ig.guideresto.services;

import ch.hearc.ig.guideresto.business.City;
import ch.hearc.ig.guideresto.persistence.mapper.CityMapper;

import java.util.Set;

public class CityService {

    private final CityMapper cityMapper;

    public CityService(CityMapper cityMapper) {
        this.cityMapper = cityMapper;
    }

    public Set<City> getAllCities() {
        return cityMapper.findAll();
    }

    public City findCityByName(String name) {
        try {
            return cityMapper.findByName(name);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public City addCity(String name, String zipCode) {
        City city = new City(null, zipCode, name);
        return cityMapper.create(city);
    }
}

