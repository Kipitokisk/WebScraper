package scraper.database.lookupEntityMappers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import scraper.database.DatabaseManager;
import scraper.model.lookupEntity.AdType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AdTypeMapperTest {
    private DatabaseManager dbManager;
    private Connection conn;
    private PreparedStatement stmt;
    private ResultSet rs;
    private AdTypeMapper adTypeMapper;

    @BeforeEach
    void setUp() {
        dbManager = mock(DatabaseManager.class);
        conn = mock(Connection.class);
        stmt = mock(PreparedStatement.class);
        rs = mock(ResultSet.class);
        adTypeMapper = new AdTypeMapper(dbManager);
    }

    @Test
    void testSaveBatch_SuccessfulExecution() throws SQLException {
        AdType adType = mock(AdType.class);
        when(adType.getName()).thenReturn("Vând");
        Set<AdType> adTypes = new HashSet<>(Collections.singleton(adType));
        when(dbManager.getConnection()).thenReturn(conn);
        when(dbManager.prepareStatement(conn, "INSERT INTO ad_type (name) VALUES (?) ON CONFLICT (name) DO NOTHING"))
                .thenReturn(stmt);
        when(stmt.executeBatch()).thenReturn(new int[]{1});

        adTypeMapper.saveBatch(adTypes);

        verify(dbManager).getConnection();
        verify(dbManager).prepareStatement(conn, "INSERT INTO ad_type (name) VALUES (?) ON CONFLICT (name) DO NOTHING");
        verify(stmt).setString(1, "Vând");
        verify(stmt).addBatch();
        verify(stmt).executeBatch();
        verify(conn).close();
        verify(stmt).close();
    }

    @Test
    void testSaveBatch_NullOrEmptyEntities() throws SQLException {
        Set<AdType> nullEntities = null;
        Set<AdType> emptyEntities = new HashSet<>();

        adTypeMapper.saveBatch(nullEntities);
        adTypeMapper.saveBatch(emptyEntities);

        verify(dbManager, never()).getConnection();
        verify(dbManager, never()).prepareStatement(any(), anyString());
        verify(stmt, never()).setString(anyInt(), anyString());
        verify(stmt, never()).addBatch();
        verify(stmt, never()).executeBatch();
    }

    @Test
    void testSaveBatch_NullOrEmptyName() throws SQLException {
        AdType nullNameAdType = mock(AdType.class);
        AdType emptyNameAdType = mock(AdType.class);
        AdType blankNameAdType = mock(AdType.class);
        AdType validAdType = mock(AdType.class);
        when(nullNameAdType.getName()).thenReturn(null);
        when(emptyNameAdType.getName()).thenReturn("");
        when(blankNameAdType.getName()).thenReturn("  ");
        when(validAdType.getName()).thenReturn("Vând");
        Set<AdType> adTypes = new HashSet<>(Arrays.asList(nullNameAdType, emptyNameAdType, blankNameAdType, validAdType));
        when(dbManager.getConnection()).thenReturn(conn);
        when(dbManager.prepareStatement(conn, "INSERT INTO ad_type (name) VALUES (?) ON CONFLICT (name) DO NOTHING"))
                .thenReturn(stmt);
        when(stmt.executeBatch()).thenReturn(new int[]{1});

        adTypeMapper.saveBatch(adTypes);

        verify(stmt).setString(1, "Vând");
        verify(stmt, times(1)).addBatch();
        verify(stmt).executeBatch();
        verify(conn).close();
        verify(stmt).close();
    }

    @Test
    void testSaveBatch_SQLException() throws SQLException {
        AdType adType = mock(AdType.class);
        when(adType.getName()).thenReturn("Vând");
        Set<AdType> adTypes = new HashSet<>(Collections.singleton(adType));
        when(dbManager.getConnection()).thenReturn(conn);
        when(dbManager.prepareStatement(conn, "INSERT INTO ad_type (name) VALUES (?) ON CONFLICT (name) DO NOTHING"))
                .thenReturn(stmt);
        when(stmt.executeBatch()).thenThrow(new SQLException("Database error"));

        SQLException thrown = assertThrows(SQLException.class, () -> adTypeMapper.saveBatch(adTypes));
        assertEquals("Database error", thrown.getMessage());
        verify(conn).close();
        verify(stmt).close();
    }

    @Test
    void testGetAll_SuccessfulRetrieval() throws SQLException {
        AdType adType = mock(AdType.class);
        when(adType.getName()).thenReturn("Vând");
        when(adType.getId()).thenReturn(1);
        when(dbManager.getConnection()).thenReturn(conn);
        when(dbManager.prepareStatement(conn, "SELECT id, name FROM ad_type")).thenReturn(stmt);
        when(stmt.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true, false);
        when(rs.getInt("id")).thenReturn(1);
        when(rs.getString("name")).thenReturn("Vând");
        doAnswer(invocation -> {
            when(adType.getName()).thenReturn(invocation.getArgument(0));
            return adType;
        }).when(adType).setName("Vând");
        doAnswer(invocation -> {
            when(adType.getId()).thenReturn(invocation.getArgument(0));
            return null;
        }).when(adType).setId(1);

        Set<AdType> result = adTypeMapper.getAll();

        assertNotNull(result);
        assertEquals(1, result.size());
        AdType retrieved = result.iterator().next();
        assertEquals("Vând", retrieved.getName());
        assertEquals(1, retrieved.getId());
        verify(dbManager).getConnection();
        verify(dbManager).prepareStatement(conn, "SELECT id, name FROM ad_type");
        verify(stmt).executeQuery();
        verify(rs).close();
        verify(conn).close();
        verify(stmt).close();
    }

    @Test
    void testGetAll_EmptyResultSet() throws SQLException {
        when(dbManager.getConnection()).thenReturn(conn);
        when(dbManager.prepareStatement(conn, "SELECT id, name FROM ad_type")).thenReturn(stmt);
        when(stmt.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(false);

        Set<AdType> result = adTypeMapper.getAll();

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(rs).close();
        verify(conn).close();
        verify(stmt).close();
    }

    @Test
    void testGetAll_SQLException() throws SQLException {
        when(dbManager.getConnection()).thenReturn(conn);
        when(dbManager.prepareStatement(conn, "SELECT id, name FROM ad_type"))
                .thenThrow(new SQLException("Database error"));

        SQLException thrown = assertThrows(SQLException.class, () -> adTypeMapper.getAll());
        assertEquals("Database error", thrown.getMessage());
        verify(conn).close();
    }
}