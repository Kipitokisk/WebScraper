package scraper.database.registry;

import scraper.database.*;
import scraper.database.lookupEntityMappers.*;
import scraper.model.CarDetails;
import scraper.model.lookupEntity.*;

import java.sql.SQLException;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class LookupEntityRegistry {
    private Set<WheelSide> wheelSideSet ;
    private Set<NrOfSeats> nrOfSeatsSet;
    private Set<Body> bodySet;
    private Set<NrOfDoors> nrOfDoorsSet;
    private Set<EngineCapacity> engineCapacitySet;
    private Set<Horsepower> horsepowerSet;
    private Set<PetrolType> petrolTypeSet;
    private Set<GearsType> gearsTypeSet;
    private Set<TractionType> tractionTypeSet;
    private Set<Color> colorSet;
    private Set<AdType> adTypeSet;

    private final WheelSideMapper wheelSideMapper;
    private final NrOfSeatsMapper nrOfSeatsMapper;
    private final BodyMapper bodyMapper;
    private final NrOfDoorsMapper nrOfDoorsMapper;
    private final EngineCapacityMapper engineCapacityMapper;
    private final HorsepowerMapper horsepowerMapper;
    private final PetrolTypeMapper petrolTypeMapper;
    private final GearsTypeMapper gearsTypeMapper;
    private final TractionTypeMapper tractionTypeMapper;
    private final ColorMapper colorMapper;
    private final AdTypeMapper adTypeMapper;

    public LookupEntityRegistry(List<CarDetails> carDetails, DatabaseManager dbManager) {
        this.wheelSideSet = mapToSet(carDetails, car -> new WheelSide(car.getWheelSide()));
        this.nrOfSeatsSet = mapToSet(carDetails, car -> new NrOfSeats(car.getNrOfSeats()));
        this.bodySet = mapToSet(carDetails, car -> new Body(car.getBody()));
        this.nrOfDoorsSet = mapToSet(carDetails, car -> new NrOfDoors(car.getNrOfDoors()));
        this.engineCapacitySet = mapToSet(carDetails, car -> new EngineCapacity(car.getEngineCapacity()));
        this.horsepowerSet = mapToSet(carDetails, car -> new Horsepower(car.getHorsepower()));
        this.petrolTypeSet = mapToSet(carDetails, car -> new PetrolType(car.getPetrolType()));
        this.gearsTypeSet = mapToSet(carDetails, car -> new GearsType(car.getGearsType()));
        this.tractionTypeSet = mapToSet(carDetails, car -> new TractionType(car.getTractionType()));
        this.colorSet = mapToSet(carDetails, car -> new Color(car.getColor()));
        this.adTypeSet = mapToSet(carDetails, car -> new AdType(car.getAdType()));

        this.wheelSideMapper = new WheelSideMapper(dbManager);
        this.nrOfSeatsMapper = new NrOfSeatsMapper(dbManager);
        this.bodyMapper = new BodyMapper(dbManager);
        this.nrOfDoorsMapper = new NrOfDoorsMapper(dbManager);
        this.engineCapacityMapper = new EngineCapacityMapper(dbManager);
        this.horsepowerMapper = new HorsepowerMapper(dbManager);
        this.petrolTypeMapper = new PetrolTypeMapper(dbManager);
        this.gearsTypeMapper = new GearsTypeMapper(dbManager);
        this.tractionTypeMapper = new TractionTypeMapper(dbManager);
        this.colorMapper = new ColorMapper(dbManager);
        this.adTypeMapper = new AdTypeMapper(dbManager);
    }

    private <T extends LookupEntity> void processEntity(Set<T> set, LookupEntityMapper<T> mapper, Consumer<Set<T>> setter)
            throws SQLException {
        mapper.saveBatch(set);
        setter.accept(new HashSet<>(mapper.getAll()));
    }

    private static <T> Set<T> mapToSet(List<CarDetails> carDetails, Function<CarDetails, T> mapper) {
        return carDetails.stream().map(mapper).collect(Collectors.toSet());
    }


    public void processLookupEntities() throws SQLException {
        processEntity(adTypeSet, adTypeMapper, this::setAdTypeSet);
        processEntity(bodySet, bodyMapper, this::setBodySet);
        processEntity(colorSet, colorMapper, this::setColorSet);
        processEntity(engineCapacitySet, engineCapacityMapper, this::setEngineCapacitySet);
        processEntity(gearsTypeSet, gearsTypeMapper, this::setGearsTypeSet);
        processEntity(horsepowerSet, horsepowerMapper, this::setHorsepowerSet);
        processEntity(nrOfDoorsSet, nrOfDoorsMapper, this::setNrOfDoorsSet);
        processEntity(nrOfSeatsSet, nrOfSeatsMapper, this::setNrOfSeatsSet);
        processEntity(petrolTypeSet, petrolTypeMapper, this::setPetrolTypeSet);
        processEntity(tractionTypeSet, tractionTypeMapper, this::setTractionTypeSet);
        processEntity(wheelSideSet, wheelSideMapper, this::setWheelSideSet);
    }

    public void setWheelSideSet(Set<WheelSide> wheelSideSet) { this.wheelSideSet = wheelSideSet;}
    public void setNrOfSeatsSet(Set<NrOfSeats> nrOfSeatsSet) { this.nrOfSeatsSet = nrOfSeatsSet;}
    public void setBodySet(Set<Body> bodySet) { this.bodySet = bodySet;}
    public void setNrOfDoorsSet(Set<NrOfDoors> nrOfDoorsSet) { this.nrOfDoorsSet = nrOfDoorsSet;}
    public void setEngineCapacitySet(Set<EngineCapacity> engineCapacitySet) { this.engineCapacitySet = engineCapacitySet;}
    public void setHorsepowerSet(Set<Horsepower> horsepowerSet) { this.horsepowerSet = horsepowerSet;}
    public void setPetrolTypeSet(Set<PetrolType> petrolTypeSet) { this.petrolTypeSet = petrolTypeSet;}
    public void setGearsTypeSet(Set<GearsType> gearsTypeSet) { this.gearsTypeSet = gearsTypeSet;}
    public void setTractionTypeSet(Set<TractionType> tractionTypeSet) { this.tractionTypeSet = tractionTypeSet;}
    public void setColorSet(Set<Color> colorSet) { this.colorSet = colorSet;}
    public void setAdTypeSet(Set<AdType> adTypeSet) { this.adTypeSet = adTypeSet;}

    public Integer getWheelSideId(String name) { return findIdByName(name, wheelSideSet); }
    public Integer getNrOfSeatsId(String name) { return findIdByName(name, nrOfSeatsSet); }
    public Integer getBodyId(String name) { return findIdByName(name, bodySet); }
    public Integer getNrOfDoorsId(String name) { return findIdByName(name, nrOfDoorsSet); }
    public Integer getEngineCapacityId(String name) { return findIdByName(name, engineCapacitySet); }
    public Integer getHorsepowerId(String name) { return findIdByName(name, horsepowerSet); }
    public Integer getPetrolTypeId(String name) { return findIdByName(name, petrolTypeSet); }
    public Integer getGearsTypeId(String name) { return findIdByName(name, gearsTypeSet); }
    public Integer getTractionTypeId(String name) { return findIdByName(name, tractionTypeSet); }
    public Integer getColorId(String name) { return findIdByName(name, colorSet); }
    public Integer getAdTypeId(String name) { return findIdByName(name, adTypeSet); }

    private Integer findIdByName(String name, Set<? extends LookupEntity> entities) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }
        return entities.stream()
                .filter(e -> name.trim().equals(e.getName()))
                .map(LookupEntity::getId)
                .findFirst()
                .orElse(null);
    }
}