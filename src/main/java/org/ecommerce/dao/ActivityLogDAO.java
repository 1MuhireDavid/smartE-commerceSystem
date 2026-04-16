package org.ecommerce.dao;

import org.ecommerce.config.DatabaseConfig;
import org.ecommerce.model.ActivityLog;
import org.postgresql.util.PGobject;

import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Data-access layer for the activity_logs table.
 *
 * <h2>NoSQL / JSONB design</h2>
 * <p>Each event type carries a different payload structure, so a fixed-column
 * schema would require many nullable columns or a separate table per event.
 * PostgreSQL JSONB solves this elegantly: one {@code event_data} column stores
 * any shape of JSON while remaining fully indexed and queryable.
 *
 * <h3>Writing JSONB</h3>
 * <p>The PostgreSQL JDBC driver requires a {@link PGobject} with type {@code "jsonb"}
 * for parameterised inserts — casting a plain string with {@code ?::jsonb} in SQL
 * also works, but {@code PGobject} is the idiomatic approach.
 *
 * <h3>Reading JSONB</h3>
 * <p>{@code ResultSet.getString()} returns the stored JSON as a plain Java
 * {@code String}, which is then kept as-is in {@link ActivityLog#getEventData()}.
 *
 * <h3>Querying inside JSONB</h3>
 * <p>PostgreSQL's {@code ->>} operator extracts a top-level key as text, enabling
 * queries like {@code WHERE event_data->>'product_id' = '1'}.  This is shown in
 * {@link #findByJsonField}.
 */
public class ActivityLogDAO {

    private static final String BASE_SELECT = """
            SELECT l.log_id, l.user_id, u.full_name AS user_name,
                   l.event_type,
                   l.event_data::text AS event_data,
                   l.logged_at
            FROM   activity_logs l
            LEFT JOIN users u ON u.user_id = l.user_id
            """;

    private Connection conn() throws SQLException {
        return DatabaseConfig.getInstance().getConnection();
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    /** Most-recent {@code limit} log entries across all event types. */
    public List<ActivityLog> findRecent(int limit) throws SQLException {
        String sql = BASE_SELECT + " ORDER BY l.logged_at DESC LIMIT ?";
        List<ActivityLog> list = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    /** Filter by event type only. Pass {@code null} or blank to return all types. */
    public List<ActivityLog> findByEventType(String eventType, int limit) throws SQLException {
        boolean filter = eventType != null && !eventType.isBlank();
        String sql = BASE_SELECT
                + (filter ? " WHERE l.event_type = ?" : "")
                + " ORDER BY l.logged_at DESC LIMIT ?";
        List<ActivityLog> list = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            int idx = 1;
            if (filter) ps.setString(idx++, eventType);
            ps.setInt(idx, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    /**
     * JSONB field query — uses the PostgreSQL {@code ->>} text-extraction operator.
     *
     * <p>Example: {@code findByJsonField("search", "query", "macbook", 50)} generates:
     * <pre>  WHERE l.event_type = 'search' AND l.event_data->>'query' = 'macbook'</pre>
     *
     * @param eventType optional event-type filter (null = all types)
     * @param jsonKey   top-level JSONB key to match
     * @param jsonValue expected string value of that key
     * @param limit     max rows to return
     */
    public List<ActivityLog> findByJsonField(String eventType, String jsonKey,
                                             String jsonValue, int limit)
            throws SQLException {
        boolean typeFilter = eventType != null && !eventType.isBlank();
        String sql = BASE_SELECT
                + " WHERE l.event_data->>? = ?"
                + (typeFilter ? " AND l.event_type = ?" : "")
                + " ORDER BY l.logged_at DESC LIMIT ?";
        List<ActivityLog> list = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, jsonKey);
            ps.setString(2, jsonValue);
            int idx = 3;
            if (typeFilter) ps.setString(idx++, eventType);
            ps.setInt(idx, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    /** Distinct event types currently stored — used to populate the filter combo. */
    public List<String> findDistinctEventTypes() throws SQLException {
        List<String> types = new ArrayList<>();
        try (Statement st = conn().createStatement();
             ResultSet rs = st.executeQuery(
                "SELECT DISTINCT event_type FROM activity_logs ORDER BY event_type")) {
            while (rs.next()) types.add(rs.getString(1));
        }
        return types;
    }

    /** Event-type breakdown, ordered by count descending — used for the stats bar. */
    public Map<String, Integer> countByEventType() throws SQLException {
        Map<String, Integer> counts = new LinkedHashMap<>();
        String sql = "SELECT event_type, COUNT(*)::int AS cnt " +
                     "FROM activity_logs GROUP BY event_type ORDER BY cnt DESC";
        try (Statement st = conn().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) counts.put(rs.getString("event_type"), rs.getInt("cnt"));
        }
        return counts;
    }

    public int countTotal() throws SQLException {
        try (Statement st = conn().createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM activity_logs")) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    // ── Write ─────────────────────────────────────────────────────────────────

    /**
     * Insert a new activity log event.
     *
     * <p>The {@code jsonPayload} is wrapped in a {@link PGobject} of type
     * {@code "jsonb"}.  PostgreSQL will validate that the string is well-formed
     * JSON and reject it with an error if it is not — no client-side JSON parsing
     * library required.
     *
     * @param userId      the acting user, or {@code null} for system events
     * @param eventType   short label, e.g. {@code "page_view"}
     * @param jsonPayload valid JSON object string, e.g. {@code {"product_id":1}}
     */
    public ActivityLog insert(Long userId, String eventType, String jsonPayload)
            throws SQLException {
        String sql = """
                INSERT INTO activity_logs (user_id, event_type, event_data)
                VALUES (?, ?, ?)
                RETURNING log_id, logged_at
                """;
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            if (userId != null) ps.setLong(1, userId);
            else                ps.setNull(1, Types.BIGINT);
            ps.setString(2, eventType);

            // PGobject is the correct way to pass JSONB to the PostgreSQL driver.
            // The driver cannot infer the ::jsonb cast from a plain setString call.
            PGobject pg = new PGobject();
            pg.setType("jsonb");
            pg.setValue(jsonPayload);
            ps.setObject(3, pg);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    ActivityLog log = new ActivityLog();
                    log.setLogId(rs.getLong("log_id"));
                    log.setUserId(userId);
                    log.setEventType(eventType);
                    log.setEventData(jsonPayload);
                    log.setLoggedAt(rs.getTimestamp("logged_at").toLocalDateTime());
                    return log;
                }
            }
        }
        throw new SQLException("Insert returned no row");
    }

    // ── Mapping ───────────────────────────────────────────────────────────────

    private ActivityLog map(ResultSet rs) throws SQLException {
        ActivityLog log = new ActivityLog();
        log.setLogId(rs.getLong("log_id"));
        long uid = rs.getLong("user_id");
        if (!rs.wasNull()) log.setUserId(uid);
        log.setUserName(rs.getString("user_name"));
        log.setEventType(rs.getString("event_type"));
        log.setEventData(rs.getString("event_data"));
        Timestamp ts = rs.getTimestamp("logged_at");
        if (ts != null) log.setLoggedAt(ts.toLocalDateTime());
        return log;
    }
}
