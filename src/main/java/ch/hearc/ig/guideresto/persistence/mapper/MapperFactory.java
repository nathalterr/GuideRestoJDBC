package ch.hearc.ig.guideresto.persistence.mapper;

import ch.hearc.ig.guideresto.persistence.mapper.*;

public class MapperFactory {

    private final CityMapper cityMapper;
    private final RestaurantTypeMapper typeMapper;
    private final RestaurantMapper restaurantMapper;
    private final GradeMapper gradeMapper;
    private final BasicEvaluationMapper basicEvalMapper;
    private final CompleteEvaluationMapper completeEvalMapper;
    private final EvaluationCriteriaMapper criteriaMapper;

    public MapperFactory() {
        this.cityMapper = new CityMapper();
        this.typeMapper = new RestaurantTypeMapper();
        this.gradeMapper = new GradeMapper();
        this.basicEvalMapper = new BasicEvaluationMapper();
        this.restaurantMapper = new RestaurantMapper();
        this.completeEvalMapper = new CompleteEvaluationMapper();
        this.criteriaMapper = new EvaluationCriteriaMapper();
        this.restaurantMapper.setDependencies(completeEvalMapper, gradeMapper, basicEvalMapper);
        this.completeEvalMapper.setDependencies(restaurantMapper,gradeMapper);
    }

    public CityMapper getCityMapper() { return cityMapper; }
    public RestaurantTypeMapper getTypeMapper() { return typeMapper; }
    public RestaurantMapper getRestaurantMapper() { return restaurantMapper; }
    public GradeMapper getGradeMapper() { return gradeMapper; }
    public BasicEvaluationMapper getBasicEvalMapper() { return basicEvalMapper; }
    public CompleteEvaluationMapper getCompleteEvalMapper() { return completeEvalMapper; }
    public EvaluationCriteriaMapper getCriteriaMapper() { return criteriaMapper; }
}
