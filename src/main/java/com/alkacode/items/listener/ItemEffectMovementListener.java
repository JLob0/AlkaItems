package com.alkacode.items.listener;

import com.alkacode.items.AlkaItemsServices;
import com.alkacode.items.enchant.EnchantTrigger;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * NO_FALL_DAMAGE, DOUBLE_JUMP e DASH (efeitos de movimento) + ON_SNEAK/ON_SPRINT
 * (triggers de encantamento custom) - agrupados aqui porque todos reagem a eventos de
 * movimento/estado do jogador, diferente dos outros listeners que reagem a acoes
 * discretas (bater, minerar, pescar).
 *
 * <p>DOUBLE_JUMP e uma aproximacao (nao existe "pulo duplo real" na API do Bukkit): uma
 * task periodica ({@link #tick()}, chamada pela mesma task de EffectTickTask) reativa
 * {@code allowFlight} quando o jogador toca o chao, e o duplo-toque em pulo que o
 * cliente usa pra "comecar a voar" e interceptado aqui e convertido num impulso de
 * velocidade pra cima em vez de ligar o modo voo de verdade.
 */
public final class ItemEffectMovementListener implements Listener {

    private final AlkaItemsServices services;
    private final Map<UUID, Long> lastSneakTick = new ConcurrentHashMap<>();
    private final Map<UUID, Long> dashCooldownUntil = new ConcurrentHashMap<>();

    public ItemEffectMovementListener(AlkaItemsServices services) {
        this.services = services;
    }

    @EventHandler(ignoreCancelled = true)
    public void onFallDamage(EntityDamageEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL) {
            return;
        }
        if (event.getEntity() instanceof Player player && services.effectService.hasNoFallDamage(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onToggleFlight(PlayerToggleFlightEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() == org.bukkit.GameMode.CREATIVE || player.getGameMode() == org.bukkit.GameMode.SPECTATOR) {
            return;
        }
        if (!services.effectService.hasDoubleJump(player) || player.isOnGround()) {
            return;
        }
        event.setCancelled(true);
        player.setAllowFlight(false);
        double velocity = services.configManager.config().getDouble("double-jump.velocity", 0.5);
        Vector boost = player.getVelocity();
        boost.setY(velocity);
        player.setVelocity(boost);
    }

    @EventHandler(ignoreCancelled = true)
    public void onSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        if (!event.isSneaking()) {
            return;
        }

        var mainHand = player.getInventory().getItemInMainHand();
        services.enchantService.trigger(EnchantTrigger.ON_SNEAK, player, mainHand, null, event);

        if (!services.effectService.hasDash(player)) {
            return;
        }
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        Long last = lastSneakTick.put(uuid, now);
        long windowMillis = services.configManager.dashDoubleTapWindowTicks() * 50L;
        if (last == null || now - last > windowMillis) {
            return;
        }
        Long cooldown = dashCooldownUntil.get(uuid);
        if (cooldown != null && now < cooldown) {
            return;
        }
        dashCooldownUntil.put(uuid, now + services.configManager.dashCooldownTicks() * 50L);
        Vector direction = player.getLocation().getDirection().setY(0).normalize();
        player.setVelocity(direction.multiply(services.configManager.dashVelocity()).setY(0.2));
    }

    @EventHandler(ignoreCancelled = true)
    public void onSprint(PlayerToggleSprintEvent event) {
        if (!event.isSprinting()) {
            return;
        }
        Player player = event.getPlayer();
        services.enchantService.trigger(EnchantTrigger.ON_SPRINT, player, player.getInventory().getItemInMainHand(), null, event);
    }

    /** Chamada pela EffectTickTask a cada iteracao - reabilita allowFlight pra jogadores
     * com DOUBLE_JUMP assim que tocam o chao (senao o "pulo extra" so funciona uma vez). */
    public void tick() {
        for (Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
            if (!services.effectService.hasDoubleJump(player)) {
                continue;
            }
            if (player.isOnGround() && !player.getAllowFlight()
                    && player.getGameMode() != org.bukkit.GameMode.CREATIVE
                    && player.getGameMode() != org.bukkit.GameMode.SPECTATOR) {
                player.setAllowFlight(true);
            }
        }
    }
}
