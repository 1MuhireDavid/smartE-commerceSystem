package org.ecommerce.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.ecommerce.dao.ActivityLogDAO;
import org.ecommerce.dao.UserDAO;
import org.ecommerce.model.ActivityLog;
import org.ecommerce.model.User;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Controller for the Activity Logs tab.
 *
 * <p>Demonstrates the NoSQL / JSONB design:
 * <ul>
 *   <li>The filter bar shows a plain event-type filter (relational WHERE clause).
 *   <li>The JSONB query bar shows a {@code ->>} key/value search inside the JSON payload.
 *   <li>The "Log Event" form inserts a new row using {@code PGobject("jsonb")} —
 *       different event types carry completely different payload shapes, which is
 *       exactly why JSONB was chosen over fixed columns.
 * </ul>
 */
public class ActivityLogController {

    // ── Table + filter controls ───────────────────────────────────────────────
    @FXML private ComboBox<String>    eventTypeFilter;
    @FXML private TextField           jsonKeyField;
    @FXML private TextField           jsonValueField;
    @FXML private TableView<ActivityLog>            logTable;
    @FXML private TableColumn<ActivityLog, String>  colId;
    @FXML private TableColumn<ActivityLog, String>  colUser;
    @FXML private TableColumn<ActivityLog, String>  colEventType;
    @FXML private TableColumn<ActivityLog, String>  colPayload;
    @FXML private TableColumn<ActivityLog, String>  colDate;
    @FXML private Label statsLabel;
    @FXML private Label feedbackLabel;

    // ── Log-event form ────────────────────────────────────────────────────────
    @FXML private ComboBox<String>  newEventType;
    @FXML private ComboBox<User>    userCombo;
    @FXML private TextArea          payloadArea;

    private final ActivityLogDAO dao     = new ActivityLogDAO();
    private final UserDAO        userDAO = new UserDAO();

    private static final int MAX_ROWS = 200;
    private static final DateTimeFormatter DT_FMT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // JSON templates auto-filled when the user picks an event type
    private static final Map<String, String> TEMPLATES = Map.of(
        "page_view",     "{\n  \"page\": \"product\",\n  \"product_id\": 1,\n  \"product_name\": \"Example Product\",\n  \"duration_sec\": 30\n}",
        "add_to_cart",   "{\n  \"product_id\": 1,\n  \"quantity\": 1,\n  \"unit_price\": 29.99,\n  \"cart_id\": 1\n}",
        "search",        "{\n  \"query\": \"example\",\n  \"results_count\": 5,\n  \"filters\": {},\n  \"response_ms\": 10\n}",
        "checkout_start","{\n  \"cart_id\": 1,\n  \"items\": 2,\n  \"subtotal\": 59.98\n}",
        "review_posted", "{\n  \"product_id\": 1,\n  \"rating\": 5,\n  \"review_id\": 1,\n  \"verified_purchase\": false\n}",
        "order_placed",  "{\n  \"order_number\": \"ORD-2026-000001\",\n  \"total\": 34.99,\n  \"payment_method\": \"card\"\n}",
        "system_event",  "{\n  \"event\": \"cache_cleared\",\n  \"reason\": \"manual\",\n  \"affected_keys\": 0\n}"
    );

    @FXML
    public void initialize() {
        setupColumns();
        setupEventTypeFilter();
        setupNewEventForm();
        loadLogs();
        loadStats();
    }

    public void refresh() {
        setupEventTypeFilter();
        loadLogs();
        loadStats();
    }

    // ── Setup ─────────────────────────────────────────────────────────────────

    private void setupColumns() {
        colId.setCellValueFactory(c ->
            new SimpleStringProperty(String.valueOf(c.getValue().getLogId())));
        colUser.setCellValueFactory(c ->
            new SimpleStringProperty(c.getValue().getDisplayUser()));
        colEventType.setCellValueFactory(c ->
            new SimpleStringProperty(c.getValue().getEventType()));
        colPayload.setCellValueFactory(c -> {
            String data = c.getValue().getEventData();
            // Compact for table display — collapse whitespace
            String compact = data == null ? "" : data.replaceAll("\\s+", " ");
            return new SimpleStringProperty(compact.length() > 120
                ? compact.substring(0, 120) + "…"
                : compact);
        });
        colDate.setCellValueFactory(c -> {
            var dt = c.getValue().getLoggedAt();
            return new SimpleStringProperty(dt == null ? "" : dt.format(DT_FMT));
        });

        // Double-click row to see full JSON payload in a dialog
        logTable.setRowFactory(tv -> {
            TableRow<ActivityLog> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) {
                    showPayloadDialog(row.getItem());
                }
            });
            return row;
        });
    }

    private void setupEventTypeFilter() {
        String current = eventTypeFilter.getValue();
        List<String> types = new ArrayList<>();
        types.add("All");
        try {
            types.addAll(dao.findDistinctEventTypes());
        } catch (SQLException e) {
            showError("Could not load event types: " + e.getMessage());
        }
        eventTypeFilter.setItems(FXCollections.observableArrayList(types));
        eventTypeFilter.setValue(types.contains(current) ? current : "All");
    }

    private void setupNewEventForm() {
        List<String> eventTypes = new ArrayList<>(TEMPLATES.keySet());
        newEventType.setItems(FXCollections.observableArrayList(eventTypes));
        newEventType.valueProperty().addListener((obs, old, sel) -> {
            if (sel != null) payloadArea.setText(TEMPLATES.getOrDefault(sel, "{}"));
        });
        newEventType.setValue("page_view");

        try {
            List<User> users = userDAO.findAll();
            userCombo.setItems(FXCollections.observableArrayList(users));
            userCombo.setPromptText("system (no user)");
        } catch (SQLException e) {
            showError("Could not load users: " + e.getMessage());
        }
    }

    // ── Data loading ──────────────────────────────────────────────────────────

    private void loadLogs() {
        String selectedType = eventTypeFilter.getValue();
        String jsonKey   = jsonKeyField.getText()  == null ? "" : jsonKeyField.getText().trim();
        String jsonValue = jsonValueField.getText() == null ? "" : jsonValueField.getText().trim();

        try {
            List<ActivityLog> rows;
            if (!jsonKey.isEmpty() && !jsonValue.isEmpty()) {
                // JSONB key/value query using the ->> operator
                String typeArg = "All".equals(selectedType) ? null : selectedType;
                rows = dao.findByJsonField(typeArg, jsonKey, jsonValue, MAX_ROWS);
            } else {
                String typeArg = "All".equals(selectedType) ? null : selectedType;
                rows = dao.findByEventType(typeArg, MAX_ROWS);
            }
            logTable.setItems(FXCollections.observableArrayList(rows));
            feedbackLabel.setText("");
        } catch (SQLException e) {
            showError("Query failed: " + e.getMessage());
        }
    }

    private void loadStats() {
        try {
            int total = dao.countTotal();
            Map<String, Integer> counts = dao.countByEventType();
            StringBuilder sb = new StringBuilder("Total events: " + total + "  |  ");
            counts.forEach((type, count) ->
                sb.append(type).append(": ").append(count).append("  "));
            statsLabel.setText(sb.toString());
        } catch (SQLException e) {
            statsLabel.setText("Stats unavailable.");
        }
    }

    // ── Handlers ──────────────────────────────────────────────────────────────

    @FXML private void handleSearch() { loadLogs(); }

    @FXML private void handleClear() {
        eventTypeFilter.setValue("All");
        jsonKeyField.clear();
        jsonValueField.clear();
        loadLogs();
    }

    @FXML
    private void handleLogEvent() {
        String eventType = newEventType.getValue();
        String payload   = payloadArea.getText() == null ? "" : payloadArea.getText().trim();
        User   user      = userCombo.getValue();

        if (eventType == null || eventType.isBlank()) {
            showError("Select an event type.");
            return;
        }
        if (payload.isBlank()) {
            showError("JSON payload cannot be empty.");
            return;
        }
        if (!payload.startsWith("{") || !payload.endsWith("}")) {
            showError("Payload must be a JSON object ( { … } ).");
            return;
        }

        try {
            Long userId = (user != null) ? user.getUserId() : null;
            dao.insert(userId, eventType, payload);
            showSuccess("Event logged.");
            setupEventTypeFilter();
            loadLogs();
            loadStats();
        } catch (SQLException e) {
            showError("Insert failed: " + e.getMessage());
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void showPayloadDialog(ActivityLog log) {
        Alert dlg = new Alert(Alert.AlertType.INFORMATION);
        dlg.setTitle("Full JSON Payload");
        dlg.setHeaderText(log.getEventType() + "  —  " + log.getDisplayUser());
        TextArea ta = new TextArea(log.getEventData());
        ta.setEditable(false);
        ta.setWrapText(true);
        ta.setPrefSize(500, 250);
        dlg.getDialogPane().setContent(ta);
        dlg.getDialogPane().setMinWidth(520);
        dlg.showAndWait();
    }

    private void showSuccess(String msg) {
        feedbackLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-size: 12;");
        feedbackLabel.setText(msg);
    }

    private void showError(String msg) {
        feedbackLabel.setStyle("-fx-text-fill: #c0392b; -fx-font-size: 12;");
        feedbackLabel.setText(msg);
    }
}
