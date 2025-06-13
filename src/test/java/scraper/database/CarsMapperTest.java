package scraper.database;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import scraper.database.registry.LookupEntityRegistry;
import scraper.database.registry.ParticularitiesRegistry;
import scraper.model.CarDetails;
import scraper.model.Cars;

import java.sql.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CarsMapperTest {
    private DatabaseManager dbManager;
    private LookupEntityRegistry lookupRegistry;
    private ParticularitiesRegistry particularitiesRegistry;
    private Connection conn;
    private PreparedStatement stmt;

    private CarsMapper carsMapper;

    @BeforeEach
    void setUp() {
        dbManager = mock(DatabaseManager.class);
        lookupRegistry = mock(LookupEntityRegistry.class);
        particularitiesRegistry = mock(ParticularitiesRegistry.class);
        conn = mock(Connection.class);
        stmt = mock(PreparedStatement.class);
        carsMapper = new CarsMapper(lookupRegistry, particularitiesRegistry, dbManager);
    }

    @Test
    void testMap_CarDetailsToCars() {
        CarDetails carDetails = mock(CarDetails.class);
        when(carDetails.getLink()).thenReturn("http://999.md/car");
        when(carDetails.getRegion()).thenReturn("Chisinau");
        when(carDetails.getMileage()).thenReturn(100000);
        when(carDetails.getEurPrice()).thenReturn(15000);
        when(carDetails.getUpdateDate()).thenReturn("11 iun. 2025, 11:37");
        when(carDetails.getAdType()).thenReturn("Vând");
        when(lookupRegistry.getAdTypeId("Vând")).thenReturn(1);
        when(particularitiesRegistry.getParticularitiesId("http://999.md/car")).thenReturn(1);

        Cars car = carsMapper.map(carDetails);

        assertNotNull(car);
        assertEquals("http://999.md/car", car.getLink());
        assertEquals("Chisinau", car.getRegion());
        assertEquals(Integer.valueOf(100000), car.getMileage());
        assertEquals(Integer.valueOf(15000), car.getPriceEur());
        assertEquals("11 iun. 2025, 11:37", car.getUpdateDate());
        assertNotNull(car.getAdType());
        assertEquals("Vând", car.getAdType().getName());
        assertEquals(1, car.getParticularities());
    }

    @Test
    void testMap_NullAdType() {
        CarDetails carDetails = mock(CarDetails.class);
        when(carDetails.getLink()).thenReturn("http://999.md/car");
        when(carDetails.getAdType()).thenReturn(null);
        when(particularitiesRegistry.getParticularitiesId("http://999.md/car")).thenReturn(1);

        Cars car = carsMapper.map(carDetails);

        assertNotNull(car);
        assertEquals("http://999.md/car", car.getLink());
        assertNull(car.getAdType());
        assertEquals(1, car.getParticularities());
    }

    @Test
    void testMap_NullFields() {
        CarDetails carDetails = mock(CarDetails.class);
        when(carDetails.getLink()).thenReturn(null);
        when(carDetails.getRegion()).thenReturn(null);
        when(carDetails.getMileage()).thenReturn(null);
        when(carDetails.getEurPrice()).thenReturn(null);
        when(carDetails.getUpdateDate()).thenReturn(null);
        when(carDetails.getAdType()).thenReturn(null);
        when(particularitiesRegistry.getParticularitiesId(null)).thenReturn(1);

        Cars car = carsMapper.map(carDetails);

        assertNotNull(car);
        assertNull(car.getLink());
        assertNull(car.getRegion());
        assertNull(car.getMileage());
        assertNull(car.getPriceEur());
        assertNull(car.getUpdateDate());
        assertNull(car.getAdType());
        assertEquals(1, car.getParticularities());
    }

    @Test
    void testSaveBatch_SuccessfulExecution() throws SQLException {
        CarDetails carDetails = mock(CarDetails.class);
        when(carDetails.getLink()).thenReturn("http://999.md/car");
        when(carDetails.getRegion()).thenReturn("Chisinau");
        when(carDetails.getMileage()).thenReturn(100000);
        when(carDetails.getEurPrice()).thenReturn(15000);
        when(carDetails.getUpdateDate()).thenReturn("11 iun. 2025, 11:37");
        when(carDetails.getAdType()).thenReturn("Vând");
        List<CarDetails> carDetailsList = List.of(carDetails);

        when(dbManager.getConnection()).thenReturn(conn);
        when(dbManager.prepareStatement(eq(conn), anyString())).thenReturn(stmt);
        when(lookupRegistry.getAdTypeId("Vând")).thenReturn(1);
        when(particularitiesRegistry.getParticularitiesId("http://999.md/car")).thenReturn(1);
        when(stmt.executeBatch()).thenReturn(new int[]{1});

        carsMapper.saveBatch(carDetailsList);

        verify(dbManager).getConnection();
        verify(dbManager).prepareStatement(eq(conn), anyString());
        verify(stmt).setString(1, "http://999.md/car");
        verify(stmt).setString(2, "Chisinau");
        verify(stmt).setInt(3, 100000);
        verify(stmt).setInt(4, 15000);
        verify(stmt).setTimestamp(eq(5), any(Timestamp.class));
        verify(stmt).setInt(6, 1);
        verify(stmt).setLong(7, 1);
        verify(stmt).addBatch();
        verify(stmt).executeBatch();
        verify(conn).close();
        verify(stmt).close();
    }

    @Test
    void testSaveBatch_NullValues() throws SQLException {
        CarDetails carDetails = mock(CarDetails.class);
        when(carDetails.getLink()).thenReturn(null);
        when(carDetails.getRegion()).thenReturn(null);
        when(carDetails.getMileage()).thenReturn(null);
        when(carDetails.getEurPrice()).thenReturn(null);
        when(carDetails.getUpdateDate()).thenReturn(null);
        when(carDetails.getAdType()).thenReturn(null);
        List<CarDetails> carDetailsList = List.of(carDetails);

        when(dbManager.getConnection()).thenReturn(conn);
        when(dbManager.prepareStatement(eq(conn), anyString())).thenReturn(stmt);
        when(lookupRegistry.getAdTypeId(null)).thenReturn(null);
        when(particularitiesRegistry.getParticularitiesId(null)).thenReturn(1);
        when(stmt.executeBatch()).thenReturn(new int[]{1});

        carsMapper.saveBatch(carDetailsList);

        verify(stmt).setNull(1, Types.VARCHAR);
        verify(stmt).setNull(2, Types.VARCHAR);
        verify(stmt).setNull(3, Types.INTEGER);
        verify(stmt).setNull(4, Types.INTEGER);
        verify(stmt).setNull(5, Types.TIMESTAMP);
        verify(stmt).setNull(6, Types.INTEGER);
        verify(stmt).setLong(7, 1);
        verify(stmt).addBatch();
        verify(stmt).executeBatch();
    }

    @Test
    void testSaveBatch_EmptyList() throws SQLException {
        List<CarDetails> carDetailsList = List.of();

        when(dbManager.getConnection()).thenReturn(conn);
        when(dbManager.prepareStatement(eq(conn), anyString())).thenReturn(stmt);
        when(stmt.executeBatch()).thenReturn(new int[]{});

        carsMapper.saveBatch(carDetailsList);

        verify(stmt, never()).setString(anyInt(), anyString());
        verify(stmt, never()).setInt(anyInt(), anyInt());
        verify(stmt, never()).setTimestamp(anyInt(), any());
        verify(stmt, never()).setLong(anyInt(), anyLong());
        verify(stmt, never()).addBatch();
        verify(stmt).executeBatch();
        verify(conn).close();
        verify(stmt).close();
    }

    @Test
    void testSaveBatch_SQLException() throws SQLException {
        CarDetails carDetails = mock(CarDetails.class);
        when(carDetails.getLink()).thenReturn("http://999.md/car");
        List<CarDetails> carDetailsList = List.of(carDetails);

        when(dbManager.getConnection()).thenReturn(conn);
        when(dbManager.prepareStatement(eq(conn), anyString())).thenReturn(stmt);
        when(stmt.executeBatch()).thenThrow(new SQLException("Database error"));

        SQLException thrown = assertThrows(SQLException.class, () -> carsMapper.saveBatch(carDetailsList));
        assertEquals("Database error", thrown.getMessage());
        verify(conn).close();
        verify(stmt).close();
    }

    @Test
    void testSaveBatch_InvalidDateFormat() throws SQLException {
        CarDetails carDetails = mock(CarDetails.class);
        when(carDetails.getLink()).thenReturn("http://999.md/car");
        when(carDetails.getRegion()).thenReturn("Chisinau");
        when(carDetails.getMileage()).thenReturn(100000);
        when(carDetails.getEurPrice()).thenReturn(15000);
        when(carDetails.getUpdateDate()).thenReturn("Invalid Date");
        when(carDetails.getAdType()).thenReturn("Vând");
        List<CarDetails> carDetailsList = List.of(carDetails);

        when(dbManager.getConnection()).thenReturn(conn);
        when(dbManager.prepareStatement(eq(conn), anyString())).thenReturn(stmt);
        when(lookupRegistry.getAdTypeId("Vând")).thenReturn(1);
        when(particularitiesRegistry.getParticularitiesId("http://999.md/car")).thenReturn(1);
        when(stmt.executeBatch()).thenReturn(new int[]{1});

        carsMapper.saveBatch(carDetailsList);

        verify(stmt).setNull(5, Types.TIMESTAMP);
        verify(stmt).addBatch();
        verify(stmt).executeBatch();
    }
}