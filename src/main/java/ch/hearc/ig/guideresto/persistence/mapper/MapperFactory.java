package ch.hearc.ig.guideresto.persistence.mapper;

public class MapperFactory {

    private final CityMapper cityMapper;
    private final RestaurantTypeMapper typeMapper;
    private final RestaurantMapper restaurantMapper;
    private final GradeMapper gradeMapper;
    private final BasicEvaluationMapper basicEvalMapper;
    private final CompleteEvaluationMapper completeEvalMapper;
    private final EvaluationCriteriaMapper criteriaMapper;

    public MapperFactory() {
        // 🔹 Crée d'abord les mappers "indépendants"
        this.cityMapper = new CityMapper();
        this.typeMapper = new RestaurantTypeMapper();
        this.gradeMapper = new GradeMapper();
        this.basicEvalMapper = new BasicEvaluationMapper();
        this.criteriaMapper = new EvaluationCriteriaMapper();

        // 🔹 Crée le mapper restaurant sans dépendances
        this.restaurantMapper = new RestaurantMapper();

        // 🔹 Crée le mapper complete evaluation avec restaurantMapper et gradeMapper
        this.completeEvalMapper = new CompleteEvaluationMapper(restaurantMapper, gradeMapper);

        // 🔹 Injecte le mapper completeEval et grade dans restaurantMapper pour les opérations "delete"
        this.restaurantMapper.setDependenciesEval(this.completeEvalMapper, this.gradeMapper, this.basicEvalMapper);
        this.restaurantMapper.setDependenciesCityType(this.cityMapper, this.typeMapper);
    }

    public CityMapper getCityMapper() { return cityMapper; }
    public RestaurantTypeMapper getTypeMapper() { return typeMapper; }
    public RestaurantMapper getRestaurantMapper() { return restaurantMapper; }
    public GradeMapper getGradeMapper() { return gradeMapper; }
    public BasicEvaluationMapper getBasicEvalMapper() { return basicEvalMapper; }
    public CompleteEvaluationMapper getCompleteEvalMapper() { return completeEvalMapper; }
    public EvaluationCriteriaMapper getCriteriaMapper() { return criteriaMapper; }

}
