package com.alkacode.items.api;

import com.alkacode.items.model.CustomEnchantment;
import com.alkacode.items.model.ItemTemplate;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * API publica do AlkaItems pra outros plugins consumirem - SEMPRE via reflection do
 * lado de quem consome (nunca importar com.alkacode.items.* direto, mesma regra de
 * qualquer softdepend nesse ecossistema). Acesso: {@code AlkaItemsPlugin#getAPI()}.
 */
public interface AlkaItemsAPI {

    /** Da {@code amount}x do template pro jogador - false se o template nao existir. */
    boolean giveItem(Player player, String templateId, int amount);

    /** Verifica se o jogador ja possui uma instancia desse template em algum lugar do
     * inventario (armadura, mao, slots normais) - usado por integracoes que nao querem
     * duplicar itens soulbound (ex: recompensa de VIP). */
    boolean hasItem(Player player, String templateId);

    ItemTemplate getTemplate(String id);

    CustomEnchantment getEnchant(String id);

    boolean hasEnchant(ItemStack item, String enchantId);

    int getEnchantLevel(ItemStack item, String enchantId);
}
