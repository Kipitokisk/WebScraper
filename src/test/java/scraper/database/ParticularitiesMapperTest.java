package scraper.database;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import scraper.database.registry.LookupEntityRegistry;
import scraper.model.CarDetails;
import scraper.model.Particularities;

import java.sql.*;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ParticularitiesMapperTest {
    private DatabaseManager dbManager;
    private LookupEntityRegistry lookupRegistry;
    private Connection conn;
    private PreparedStatement stmt;
    private ResultSet rs;

    private ParticularitiesMapper particularitiesMapper;

    @BeforeEach
    void setUp() {
        dbManager = mock(DatabaseManager.class);
        lookupRegistry = mock(LookupEntityRegistry.class);
        conn = mock(Connection.class);
        stmt = mock(PreparedStatement.class);
        rs = mock(ResultSet.class);
        particularitiesMapper = new ParticularitiesMapper(dbManager, lookupRegistry);
    }

    @Test
    void testMap_CarDetailsToParticularities() {
        CarDetails carDetails = mock(CarDetails.class);
        when(carDetails.getLink()).thenReturn("http://999.md/car");
        when(carDetails.getAuthor()).thenReturn("JohnDoe");
        when(carDetails.getYearOfFabrication()).thenReturn(2020);
        when(carDetails.getWheelSide()).thenReturn("Stânga");
        when(carDetails.getNrOfSeats()).thenReturn("5");
        when(carDetails.getBody()).thenReturn("Sedan");
        when(carDetails.getNrOfDoors()).thenReturn("4");
        when(carDetails.getEngineCapacity()).thenReturn("2000");
        when(carDetails.getHorsepower()).thenReturn("150");
        when(carDetails.getPetrolType()).thenReturn("Diesel");
        when(carDetails.getGearsType()).thenReturn("Automată");
        when(carDetails.getTractionType()).thenReturn("Din față");
        when(carDetails.getColor()).thenReturn("Alb");
        when(lookupRegistry.getWheelSideId("Stânga")).thenReturn(1);
        when(lookupRegistry.getNrOfSeatsId("5")).thenReturn(2);
        when(lookupRegistry.getBodyId("Sedan")).thenReturn(3);
        when(lookupRegistry.getNrOfDoorsId("4")).thenReturn(4);
        when(lookupRegistry.getEngineCapacityId("2000")).thenReturn(5);
        when(lookupRegistry.getHorsepowerId("150")).thenReturn(6);
        when(lookupRegistry.getPetrolTypeId("Diesel")).thenReturn(7);
        when(lookupRegistry.getGearsTypeId("Automată")).thenReturn(8);
        when(lookupRegistry.getTractionTypeId("Din față")).thenReturn(9);
        when(lookupRegistry.getColorId("Alb")).thenReturn(10);

        Particularities particularities = particularitiesMapper.map(carDetails);

        assertNotNull(particularities);
        assertEquals("http://999.md/car", particularities.getLink());
        assertEquals("JohnDoe", particularities.getAuthor());
        assertEquals(Integer.valueOf(2020), particularities.getYearOfFabrication());
        assertEquals(Integer.valueOf(1), particularities.getWheelSide());
        assertEquals(Integer.valueOf(2), particularities.getNrOfSeats());
        assertEquals(Integer.valueOf(3), particularities.getBody());
        assertEquals(Integer.valueOf(4), particularities.getNrOfDoors());
        assertEquals(Integer.valueOf(5), particularities.getEngineCapacity());
        assertEquals(Integer.valueOf(6), particularities.getHorsepower());
        assertEquals(Integer.valueOf(7), particularities.getPetrolType());
        assertEquals(Integer.valueOf(8), particularities.getGearsType());
        assertEquals(Integer.valueOf(9), particularities.getTractionType());
        assertEquals(Integer.valueOf(10), particularities.getColor());
    }

    @Test
    void testMap_NullFields() {
        CarDetails carDetails = mock(CarDetails.class);
        when(carDetails.getLink()).thenReturn(null);
        when(carDetails.getAuthor()).thenReturn(null);
        when(carDetails.getYearOfFabrication()).thenReturn(null);
        when(carDetails.getWheelSide()).thenReturn(null);
        when(carDetails.getNrOfSeats()).thenReturn(null);
        when(carDetails.getBody()).thenReturn(null);
        when(carDetails.getNrOfDoors()).thenReturn(null);
        when(carDetails.getEngineCapacity()).thenReturn(null);
        when(carDetails.getHorsepower()).thenReturn(null);
        when(carDetails.getPetrolType()).thenReturn(null);
        when(carDetails.getGearsType()).thenReturn(null);
        when(carDetails.getTractionType()).thenReturn(null);
        when(carDetails.getColor()).thenReturn(null);
        when(lookupRegistry.getWheelSideId(null)).thenReturn(null);
        when(lookupRegistry.getNrOfSeatsId(null)).thenReturn(null);
        when(lookupRegistry.getBodyId(null)).thenReturn(null);
        when(lookupRegistry.getNrOfDoorsId(null)).thenReturn(null);
        when(lookupRegistry.getEngineCapacityId(null)).thenReturn(null);
        when(lookupRegistry.getHorsepowerId(null)).thenReturn(null);
        when(lookupRegistry.getPetrolTypeId(null)).thenReturn(null);
        when(lookupRegistry.getGearsTypeId(null)).thenReturn(null);
        when(lookupRegistry.getTractionTypeId(null)).thenReturn(null);
        when(lookupRegistry.getColorId(null)).thenReturn(null);

        Particularities particularities = particularitiesMapper.map(carDetails);

        assertNotNull(particularities);
        assertNull(particularities.getLink());
        assertNull(particularities.getAuthor());
        assertNull(particularities.getYearOfFabrication());
        assertNull(particularities.getWheelSide());
        assertNull(particularities.getNrOfSeats());
        assertNull(particularities.getBody());
        assertNull(particularities.getNrOfDoors());
        assertNull(particularities.getEngineCapacity());
        assertNull(particularities.getHorsepower());
        assertNull(particularities.getPetrolType());
        assertNull(particularities.getGearsType());
        assertNull(particularities.getTractionType());
        assertNull(particularities.getColor());
    }

    @Test
    void testSaveBatch_SuccessfulExecution() throws SQLException {
        CarDetails carDetails = mock(CarDetails.class);
        when(carDetails.getLink()).thenReturn("http://999.md/car");
        when(carDetails.getAuthor()).thenReturn("JohnDoe");
        when(carDetails.getYearOfFabrication()).thenReturn(2020);
        when(carDetails.getWheelSide()).thenReturn("Stânga");
        when(carDetails.getNrOfSeats()).thenReturn("5");
        when(carDetails.getBody()).thenReturn("Sedan");
        when(carDetails.getNrOfDoors()).thenReturn("4");
        when(carDetails.getEngineCapacity()).thenReturn("2000");
        when(carDetails.getHorsepower()).thenReturn("150");
        when(carDetails.getPetrolType()).thenReturn("Diesel");
        when(carDetails.getGearsType()).thenReturn("Automată");
        when(carDetails.getTractionType()).thenReturn("Din față");
        when(carDetails.getColor()).thenReturn("Alb");
        when(lookupRegistry.getWheelSideId("Stânga")).thenReturn(1);
        when(lookupRegistry.getNrOfSeatsId("5")).thenReturn(2);
        when(lookupRegistry.getBodyId("Sedan")).thenReturn(3);
        when(lookupRegistry.getNrOfDoorsId("4")).thenReturn(4);
        when(lookupRegistry.getEngineCapacityId("2000")).thenReturn(5);
        when(lookupRegistry.getHorsepowerId("150")).thenReturn(6);
        when(lookupRegistry.getPetrolTypeId("Diesel")).thenReturn(7);
        when(lookupRegistry.getGearsTypeId("Automată")).thenReturn(8);
        when(lookupRegistry.getTractionTypeId("Din față")).thenReturn(9);
        when(lookupRegistry.getColorId("Alb")).thenReturn(10);
        List<CarDetails> carDetailsList = List.of(carDetails);

        when(dbManager.getConnection()).thenReturn(conn);
        when(dbManager.prepareStatement(eq(conn), anyString())).thenReturn(stmt);
        when(stmt.executeBatch()).thenReturn(new int[]{1});

        particularitiesMapper.saveBatch(carDetailsList);

        verify(dbManager).getConnection();
        verify(dbManager).prepareStatement(eq(conn), anyString());
        verify(stmt).setString(1, "JohnDoe");
        verify(stmt).setString(2, "http://999.md/car");
        verify(stmt).setInt(3, 2020);
        verify(stmt).setInt(4, 1);
        verify(stmt).setInt(5, 2);
        verify(stmt).setInt(6, 3);
        verify(stmt).setInt(7, 4);
        verify(stmt).setInt(8, 5);
        verify(stmt).setInt(9, 6);
        verify(stmt).setInt(10, 7);
        verify(stmt).setInt(11, 8);
        verify(stmt).setInt(12, 9);
        verify(stmt).setInt(13, 10);
        verify(stmt).addBatch();
        verify(stmt).executeBatch();
        verify(conn).close();
        verify(stmt).close();
    }

    @Test
    void testSaveBatch_NullValues() throws SQLException {
        CarDetails carDetails = mock(CarDetails.class);
        when(carDetails.getLink()).thenReturn(null);
        when(carDetails.getAuthor()).thenReturn(null);
        when(carDetails.getYearOfFabrication()).thenReturn(null);
        when(carDetails.getWheelSide()).thenReturn(null);
        when(carDetails.getNrOfSeats()).thenReturn(null);
        when(carDetails.getBody()).thenReturn(null);
        when(carDetails.getNrOfDoors()).thenReturn(null);
        when(carDetails.getEngineCapacity()).thenReturn(null);
        when(carDetails.getHorsepower()).thenReturn(null);
        when(carDetails.getPetrolType()).thenReturn(null);
        when(carDetails.getGearsType()).thenReturn(null);
        when(carDetails.getTractionType()).thenReturn(null);
        when(carDetails.getColor()).thenReturn(null);
        when(lookupRegistry.getWheelSideId(null)).thenReturn(null);
        when(lookupRegistry.getNrOfSeatsId(null)).thenReturn(null);
        when(lookupRegistry.getBodyId(null)).thenReturn(null);
        when(lookupRegistry.getNrOfDoorsId(null)).thenReturn(null);
        when(lookupRegistry.getEngineCapacityId(null)).thenReturn(null);
        when(lookupRegistry.getHorsepowerId(null)).thenReturn(null);
        when(lookupRegistry.getPetrolTypeId(null)).thenReturn(null);
        when(lookupRegistry.getGearsTypeId(null)).thenReturn(null);
        when(lookupRegistry.getTractionTypeId(null)).thenReturn(null);
        when(lookupRegistry.getColorId(null)).thenReturn(null);
        List<CarDetails> carDetailsList = List.of(carDetails);

        when(dbManager.getConnection()).thenReturn(conn);
        when(dbManager.prepareStatement(eq(conn), anyString())).thenReturn(stmt);
        when(stmt.executeBatch()).thenReturn(new int[]{1});

        particularitiesMapper.saveBatch(carDetailsList);

        verify(stmt).setNull(1, Types.VARCHAR);
        verify(stmt).setNull(2, Types.VARCHAR);
        verify(stmt).setNull(3, Types.INTEGER);
        verify(stmt).setNull(4, Types.INTEGER);
        verify(stmt).setNull(5, Types.INTEGER);
        verify(stmt).setNull(6, Types.INTEGER);
        verify(stmt).setNull(7, Types.INTEGER);
        verify(stmt).setNull(8, Types.INTEGER);
        verify(stmt).setNull(9, Types.INTEGER);
        verify(stmt).setNull(10, Types.INTEGER);
        verify(stmt).setNull(11, Types.INTEGER);
        verify(stmt).setNull(12, Types.INTEGER);
        verify(stmt).setNull(13, Types.INTEGER);
        verify(stmt).addBatch();
        verify(stmt).executeBatch();
    }

    @Test
    void testSaveBatch_EmptyList() throws SQLException {
        List<CarDetails> carDetailsList = Collections.emptyList();

        when(dbManager.getConnection()).thenReturn(conn);
        when(dbManager.prepareStatement(eq(conn), anyString())).thenReturn(stmt);
        when(stmt.executeBatch()).thenReturn(new int[]{});

        particularitiesMapper.saveBatch(carDetailsList);

        verify(stmt, never()).setString(anyInt(), anyString());
        verify(stmt, never()).setInt(anyInt(), anyInt());
        verify(stmt, never()).setNull(anyInt(), anyInt());
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

        SQLException thrown = assertThrows(SQLException.class, () -> particularitiesMapper.saveBatch(carDetailsList));
        assertEquals("Database error", thrown.getMessage());
        verify(conn).close();
        verify(stmt).close();
    }

    @Test
    void testGetAll_SuccessfulRetrieval() throws SQLException {
        when(dbManager.getConnection()).thenReturn(conn);
        when(dbManager.prepareStatement(eq(conn), anyString())).thenReturn(stmt);
        when(stmt.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true, false);
        when(rs.getInt("id")).thenReturn(1);
        when(rs.getString("link")).thenReturn("http://999.md/car");
        when(rs.getString("author")).thenReturn("JohnDoe");
        when(rs.getInt("year_of_fabrication")).thenReturn(2020);
        when(rs.getInt("wheel_side_id")).thenReturn(1);
        when(rs.getInt("nr_of_seats_id")).thenReturn(2);
        when(rs.getInt("body_id")).thenReturn(3);
        when(rs.getInt("nr_of_doors_id")).thenReturn(4);
        when(rs.getInt("engine_capacity_id")).thenReturn(5);
        when(rs.getInt("horsepower_id")).thenReturn(6);
        when(rs.getInt("petrol_type_id")).thenReturn(7);
        when(rs.getInt("gears_type_id")).thenReturn(8);
        when(rs.getInt("traction_type_id")).thenReturn(9);
        when(rs.getInt("color_id")).thenReturn(10);

        List<Particularities> result = particularitiesMapper.getAll();

        assertNotNull(result);
        assertEquals(1, result.size());
        Particularities particularities = result.get(0);
        assertEquals(1, particularities.getId());
        assertEquals("http://999.md/car", particularities.getLink());
        assertEquals("JohnDoe", particularities.getAuthor());
        assertEquals(Integer.valueOf(2020), particularities.getYearOfFabrication());
        assertEquals(Integer.valueOf(1), particularities.getWheelSide());
        assertEquals(Integer.valueOf(2), particularities.getNrOfSeats());
        assertEquals(Integer.valueOf(3), particularities.getBody());
        assertEquals(Integer.valueOf(4), particularities.getNrOfDoors());
        assertEquals(Integer.valueOf(5), particularities.getEngineCapacity());
        assertEquals(Integer.valueOf(6), particularities.getHorsepower());
        assertEquals(Integer.valueOf(7), particularities.getPetrolType());
        assertEquals(Integer.valueOf(8), particularities.getGearsType());
        assertEquals(Integer.valueOf(9), particularities.getTractionType());
        assertEquals(Integer.valueOf(10), particularities.getColor());
        verify(conn).close();
        verify(stmt).close();
        verify(rs).close();
    }

    @Test
    void testGetAll_EmptyResultSet() throws SQLException {
        when(dbManager.getConnection()).thenReturn(conn);
        when(dbManager.prepareStatement(eq(conn), anyString())).thenReturn(stmt);
        when(stmt.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(false);

        List<Particularities> result = particularitiesMapper.getAll();

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(conn).close();
        verify(stmt).close();
        verify(rs).close();
    }

    @Test
    void testGetAll_SQLException() throws SQLException {
        when(dbManager.getConnection()).thenReturn(conn);
        when(dbManager.prepareStatement(eq(conn), anyString())).thenThrow(new SQLException("Database error"));

        SQLException thrown = assertThrows(SQLException.class, () -> particularitiesMapper.getAll());
        assertEquals("Database error", thrown.getMessage());
        verify(conn).close();
    }
}