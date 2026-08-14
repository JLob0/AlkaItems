package com.alkacode.items.hook;

import com.alkacode.items.config.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Checa "vip-required"/"rank-required" (ItemTemplate) SEM importar com.alkacode.vips.*
 * nem com.alkacode.rankup.* diretamente - passa pelos placeholders dinamicos que
 * AlkaVips/AlkaRankUp ja publicam via PlaceholderAPI (%alkavips_has_vip_<tier>%,
 * %alkarankup_rank_index%), o mesmo ponto de integracao que AlkaKits ja usa pra
 * checar VIP sem depender do jar de AlkaVips. Sem PlaceholderAPI instalado, as duas
 * checagens ficam DESATIVADAS (item libera pra todo mundo) - documentado, nao e bug.
 * PlaceholderAPI em si e sempre compileOnly+import direto no ecossistema (API estavel
 * e amplamente usada, diferente do AE/ItemsAdder que sao plugins pagos sem artefato
 * Maven publico).
 */
public final class RequirementHook {

    private final JavaPlugin plugin;
    private final ConfigManager config;

    public RequirementHook(JavaPlugin plugin, ConfigManager config) {
        this.plugin = plugin;
        this.config = config;
    }

    private boolean papiAvailable() {
        return Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");
    }

    public boolean hasVip(Player player, String tierId) {
        if (!papiAvailable() || tierId == null || tierId.isBlank()) {
            return true;
        }
        String result = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player,
                "%alkavips_has_vip_" + tierId.toLowerCase(java.util.Locale.ROOT) + "%");
        return "true".equalsIgnoreCase(result);
    }

    /** rank-required e um INDICE MINIMO (%alkarankup_rank_index%), nao o id de um rank especifico -
     * mais simples de comparar sem AlkaItems precisar conhecer a ordem dos ranks do AlkaRankUp. */
    public boolean hasRank(Player player, int minRankIndex) {
        if (!papiAvailable() || minRankIndex <= 0) {
            return true;
        }
        String result = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, config.rankIndexPlaceholder());
        try {
            return Integer.parseInt(result.trim()) >= minRankIndex;
        } catch (NumberFormatException e) {
            plugin.getLogger().fine("RequirementHook: placeholder de rank retornou valor nao-numerico: " + result);
            return true;
        }
    }
}
