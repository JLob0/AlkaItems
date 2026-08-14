package com.alkacode.items.listener;

import com.alkacode.items.AlkaItemsServices;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

/** Limpa o estado em memoria (ref-counts de efeitos ativos) ao sair - senao um jogador
 * que sai equipado e volta com o mesmo item reaplicaria por cima de contadores obsoletos. */
public final class PlayerCleanupListener implements Listener {

    private final AlkaItemsServices services;

    public PlayerCleanupListener(AlkaItemsServices services) {
        this.services = services;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        services.effectService.clearPlayer(event.getPlayer());
    }
}
