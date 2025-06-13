package scraper.database.registry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import scraper.database.ParticularitiesMapper;
import scraper.model.CarDetails;
import scraper.model.Particularities;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ParticularitiesRegistryTest {

    private ParticularitiesMapper particularitiesMapper;
    private List<CarDetails> carDetails;
    private ParticularitiesRegistry registry;

    @BeforeEach
    void setUp() {
        particularitiesMapper = mock(ParticularitiesMapper.class);
        carDetails = new ArrayList<>();
        registry = new ParticularitiesRegistry(carDetails, particularitiesMapper);
    }

    @Test
    void constructor_InitializesFieldsCorrectly() {
        assertEquals(carDetails, registry.carDetails);
        assertNotNull(registry.particularitiesMapper);
        assertTrue(registry.particularitiesList.isEmpty());
    }

    @Test
    void processParticularities_CallsMapperMethods() throws SQLException {
        List<Particularities> mockParticularities = Arrays.asList(
                new Particularities.Builder()
                        .id(1)
                        .link("link1")
                        .author("author1")
                        .yearOfFabrication(2020)
                        .wheelSide(1)
                        .nrOfSeats(5)
                        .body(1)
                        .nrOfDoors(4)
                        .engineCapacity(2000)
                        .horsepower(150)
                        .petrolType(1)
                        .gearsType(1)
                        .tractionType(1)
                        .color(1)
                        .build(),
                new Particularities.Builder()
                        .id(2)
                        .link("link2")
                        .author("author2")
                        .yearOfFabrication(2021)
                        .wheelSide(2)
                        .nrOfSeats(7)
                        .body(2)
                        .nrOfDoors(5)
                        .engineCapacity(2500)
                        .horsepower(180)
                        .petrolType(2)
                        .gearsType(2)
                        .tractionType(2)
                        .color(2)
                        .build()
        );
        when(particularitiesMapper.getAll()).thenReturn(mockParticularities);

        registry.processParticularities();

        verify(particularitiesMapper).saveBatch(carDetails);
        verify(particularitiesMapper).getAll();
        assertEquals(mockParticularities, registry.particularitiesList);
    }

    @Test
    void processParticularities_ThrowsSQLException() throws SQLException {
        doThrow(new SQLException("DB error")).when(particularitiesMapper).saveBatch(carDetails);

        SQLException exception = assertThrows(SQLException.class, () -> registry.processParticularities());
        assertEquals("DB error", exception.getMessage());
        verify(particularitiesMapper).saveBatch(carDetails);
        verify(particularitiesMapper, never()).getAll();
    }

    @Test
    void getParticularitiesId_ReturnsIdForValidLink() {
        Particularities particularity = new Particularities.Builder()
                .id(1)
                .link("link")
                .author("author")
                .yearOfFabrication(2020)
                .wheelSide(1)
                .nrOfSeats(5)
                .body(1)
                .nrOfDoors(4)
                .engineCapacity(2000)
                .horsepower(150)
                .petrolType(1)
                .gearsType(1)
                .tractionType(1)
                .color(1)
                .build();
        registry.particularitiesList = Collections.singletonList(particularity);

        Integer id = registry.getParticularitiesId("link");

        assertEquals(1, id);
    }

    @Test
    void getParticularitiesId_ReturnsNullForNullLink() {
        Integer id = registry.getParticularitiesId(null);
        assertNull(id);
    }

    @Test
    void getParticularitiesId_ReturnsNullForEmptyLink() {
        Integer id = registry.getParticularitiesId(" ");
        assertNull(id);
    }

    @Test
    void getParticularitiesId_ReturnsNullForNonExistentLink() {
        Particularities particularity = new Particularities.Builder()
                .id(1)
                .link("link")
                .author("author")
                .yearOfFabrication(2020)
                .wheelSide(1)
                .nrOfSeats(5)
                .body(1)
                .nrOfDoors(4)
                .engineCapacity(2000)
                .horsepower(150)
                .petrolType(1)
                .gearsType(1)
                .tractionType(1)
                .color(1)
                .build();
        registry.particularitiesList = Collections.singletonList(particularity);

        Integer id = registry.getParticularitiesId("non-existent-link");
        assertNull(id);
    }
}