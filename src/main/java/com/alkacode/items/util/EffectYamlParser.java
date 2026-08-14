package com.alkacode.items.util;

import com.alkacode.items.effect.EffectTrigger;
import com.alkacode.items.effect.EffectType;
import com.alkacode.items.model.ItemEffect;
import com.alkacode.items.model.ParamMap;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

/** Parser de uma lista YAML de efeitos ({@code type} + parametros diretos) pra
 * {@code List<ItemEffect>} - usado tanto por items.yml (config/ItemsConfig) quanto
 * por enchants.yml (config/EnchantsConfig, campos de set-bonus). */
public final class EffectYamlParser {

    private EffectYamlParser() {
    }

    public static List<ItemEffect> parseList(Logger logger, String contextLabel, List<Map<?, ?>> rawList, EffectTrigger trigger) {
        List<ItemEffect> result = new ArrayList<>();
        for (Map<?, ?> raw : rawList) {
            Object typeRaw = raw.get("type");
            if (!(typeRaw instanceof String typeStr)) {
                continue;
            }
            EffectType type;
            try {
                type = EffectType.valueOf(typeStr.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                logger.warning(contextLabel + " com efeito de tipo invalido: " + typeStr);
                continue;
            }
            Map<String, Object> params = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : raw.entrySet()) {
                if (!"type".equals(entry.getKey())) {
                    params.put(String.valueOf(entry.getKey()), entry.getValue());
                }
            }
            result.add(new ItemEffect(type, trigger, new ParamMap(params)));
        }
        return result;
    }
}
