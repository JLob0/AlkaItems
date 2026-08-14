package com.alkacode.items.model;

import java.util.Map;

/** Wrapper fino sobre o {@code Map<String,Object>} de parametros de um efeito/encantamento -
 * evita repetir cast+null-check em cada aplicador (ver EffectService/EnchantService). */
public record ParamMap(Map<String, Object> raw) {

    public static final ParamMap EMPTY = new ParamMap(Map.of());

    public String getString(String key, String def) {
        Object v = raw.get(key);
        return v != null ? String.valueOf(v) : def;
    }

    public double getDouble(String key, double def) {
        Object v = raw.get(key);
        if (v instanceof Number n) return n.doubleValue();
        if (v instanceof String s) {
            try { return Double.parseDouble(s); } catch (NumberFormatException ignored) { }
        }
        return def;
    }

    public int getInt(String key, int def) {
        return (int) getDouble(key, def);
    }

    public boolean getBoolean(String key, boolean def) {
        Object v = raw.get(key);
        if (v instanceof Boolean b) return b;
        if (v instanceof String s) return Boolean.parseBoolean(s);
        return def;
    }
}
