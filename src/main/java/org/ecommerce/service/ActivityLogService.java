package org.ecommerce.service;

import org.ecommerce.dao.ActivityLogDAO;

public class ActivityLogService {

    private static final ActivityLogService INSTANCE = new ActivityLogService();
    public static ActivityLogService get() { return INSTANCE; }

    private final ActivityLogDAO dao = new ActivityLogDAO();
    private ActivityLogService() {}

    /** Fire-and-forget: swallows all exceptions so logging never breaks business logic. */
    public void log(Long userId, String eventType, Object... kv) {
        try {
            dao.insert(userId, eventType, json(kv));
        } catch (Exception ignored) {}
    }

    private static String json(Object... kv) {
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < kv.length - 1; i += 2) {
            if (i > 0) sb.append(',');
            sb.append('"').append(kv[i]).append("\":");
            Object v = kv[i + 1];
            if (v == null)
                sb.append("null");
            else if (v instanceof Number || v instanceof Boolean)
                sb.append(v);
            else
                sb.append('"').append(v.toString()
                        .replace("\\", "\\\\")
                        .replace("\"", "\\\"")).append('"');
        }
        return sb.append('}').toString();
    }
}
