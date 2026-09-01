package com.bgconnect.gestor;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.HashSet;
import java.util.Set;

public final class PendingOrderState {
    private static final String PREF = "bgconnect_pending_orders";
    private static final String KEY_COUNT = "pending_count";
    private static final String KEY_IDS = "seen_order_ids";

    private PendingOrderState() {}

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    public static synchronized int getCount(Context c) {
        return Math.max(0, prefs(c).getInt(KEY_COUNT, 0));
    }

    public static synchronized int registerIncoming(Context c, String orderId) {
        SharedPreferences p = prefs(c);
        Set<String> ids = new HashSet<>(p.getStringSet(KEY_IDS, new HashSet<>()));
        int count = Math.max(0, p.getInt(KEY_COUNT, 0));
        if (orderId != null && !orderId.trim().isEmpty() && !"0".equals(orderId) && ids.add(orderId)) {
            count++;
        } else if (orderId == null || orderId.trim().isEmpty() || "0".equals(orderId)) {
            count++;
        }
        p.edit().putInt(KEY_COUNT, count).putStringSet(KEY_IDS, ids).apply();
        return count;
    }

    public static synchronized void syncCount(Context c, int count) {
        int safe = Math.max(0, count);
        SharedPreferences.Editor e = prefs(c).edit().putInt(KEY_COUNT, safe);
        if (safe == 0) e.remove(KEY_IDS);
        e.apply();
    }
}
