package scraper.database.registry;

import scraper.database.DatabaseManager;
import scraper.database.ParticularitiesMapper;
import scraper.model.CarDetails;
import scraper.model.Particularities;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ParticularitiesRegistry {
    private final ParticularitiesMapper particularitiesMapper;
    private final List<CarDetails> carDetails;

    private List<Particularities> particularitiesList = new ArrayList<>();

    public ParticularitiesRegistry(List<CarDetails> carDetails, LookupEntityRegistry lookupEntityRegistry, DatabaseManager dbManager) {
        this.carDetails = carDetails;
        this.particularitiesMapper = new ParticularitiesMapper(dbManager, lookupEntityRegistry);
    }

    public void processParticularities() throws SQLException {
        particularitiesMapper.saveBatch(carDetails);
        particularitiesList = particularitiesMapper.getAll();
    }

    public Integer getParticularitiesId(String link) { return findIdByLink(link, particularitiesList);}

    private Integer findIdByLink(String link, List<Particularities> particularities) {
        if (link == null || link.trim().isEmpty()) {
            return null;
        }
        return particularities.stream()
                .filter(e -> link.trim().equals(e.getLink()))
                .map(Particularities::getId)
                .findFirst()
                .orElse(null);
    }
}
