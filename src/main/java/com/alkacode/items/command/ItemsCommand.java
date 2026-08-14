package com.alkacode.items.command;

import com.alkacode.items.AlkaItemsServices;
import com.alkacode.items.gui.ItemEditorMenu;
import com.alkacode.items.gui.TemplateListMenu;
import com.alkacode.items.model.CustomEnchantment;
import com.alkacode.items.model.ItemTemplate;
import com.alkacode.items.service.ItemService;
import com.alkacode.items.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class ItemsCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of("create", "edit", "delete", "give", "save",
            "load", "list", "enchant", "removeenchant", "info", "reload");

    private final AlkaItemsServices services;

    public ItemsCommand(AlkaItemsServices services) {
        this.services = services;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            services.sendMessage(sender, "invalid-usage", Map.of("usage", "/alkaitems <" + String.join("|", SUBCOMMANDS) + ">"));
            return true;
        }
        if (!sender.hasPermission(permissionFor(args[0].toLowerCase()))) {
            services.sendMessage(sender, "no-permission", Map.of());
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "create" -> create(sender, args);
            case "edit" -> edit(sender, args);
            case "delete" -> delete(sender, args);
            case "give" -> give(sender, args);
            case "save" -> save(sender, args);
            case "load" -> load(sender, args);
            case "list" -> list(sender);
            case "enchant" -> enchant(sender, args);
            case "removeenchant" -> removeEnchant(sender, args);
            case "info" -> info(sender);
            case "reload" -> reload(sender);
            default -> services.sendMessage(sender, "invalid-usage", Map.of("usage", "/alkaitems <" + String.join("|", SUBCOMMANDS) + ">"));
        }
        return true;
    }

    private void create(CommandSender sender, String[] args) {
        if (args.length < 2) {
            services.sendMessage(sender, "invalid-usage", Map.of("usage", "/alkaitems create <id>"));
            return;
        }
        String id = args[1].toLowerCase();
        if (services.itemsConfig.exists(id)) {
            services.sendMessage(sender, "items.template-exists", Map.of("id", id));
            return;
        }
        services.itemsConfig.save(ItemTemplate.builder(id).name("<white>" + id).build());
        services.sendMessage(sender, "items.created", Map.of("id", id));
    }

    private void edit(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            services.sendMessage(sender, "player-only", Map.of());
            return;
        }
        if (args.length < 2) {
            services.sendMessage(sender, "invalid-usage", Map.of("usage", "/alkaitems edit <id>"));
            return;
        }
        ItemTemplate template = services.itemsConfig.get(args[1]);
        if (template == null) {
            services.sendMessage(sender, "unknown-template", Map.of("id", args[1]));
            return;
        }
        new ItemEditorMenu(player, services, template.id()).open();
    }

    private void delete(CommandSender sender, String[] args) {
        if (args.length < 2) {
            services.sendMessage(sender, "invalid-usage", Map.of("usage", "/alkaitems delete <id>"));
            return;
        }
        if (!services.itemsConfig.exists(args[1])) {
            services.sendMessage(sender, "unknown-template", Map.of("id", args[1]));
            return;
        }
        services.itemsConfig.delete(args[1]);
        services.sendMessage(sender, "items.deleted", Map.of("id", args[1]));
    }

    private void give(CommandSender sender, String[] args) {
        if (args.length < 3) {
            services.sendMessage(sender, "invalid-usage", Map.of("usage", "/alkaitems give <jogador> <id> [qtd]"));
            return;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            services.sendMessage(sender, "unknown-player", Map.of("name", args[1]));
            return;
        }
        int amount = args.length >= 4 ? parseIntOr(args[3], 1) : 1;
        ItemService.GiveResult result = services.itemService.giveItem(target, args[2], amount);
        if (result == ItemService.GiveResult.UNKNOWN_TEMPLATE) {
            services.sendMessage(sender, "unknown-template", Map.of("id", args[2]));
            return;
        }
        ItemTemplate template = services.itemsConfig.get(args[2]);
        String display = template != null ? TextUtil.plain(template.name()) : args[2];
        services.sendMessage(target, "items.given", Map.of("amount", String.valueOf(amount), "item", display));
        if (!sender.equals(target)) {
            services.sendMessage(sender, "items.give-success", Map.of("amount", String.valueOf(amount), "item", display, "player", target.getName()));
        }
    }

    private void save(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            services.sendMessage(sender, "player-only", Map.of());
            return;
        }
        if (args.length < 2) {
            services.sendMessage(sender, "invalid-usage", Map.of("usage", "/alkaitems save <id>"));
            return;
        }
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand.getType().isAir()) {
            services.sendMessage(sender, "items.no-item-in-hand", Map.of());
            return;
        }
        String id = args[1].toLowerCase();
        ItemTemplate.Builder builder = ItemTemplate.builder(id).material(hand.getType().name());
        if (hand.hasItemMeta() && hand.getItemMeta().hasDisplayName()) {
            builder.name(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().serialize(hand.getItemMeta().displayName()));
        } else {
            builder.name("<white>" + id);
        }
        if (hand.hasItemMeta() && hand.getItemMeta().hasLore()) {
            List<String> lore = new ArrayList<>();
            for (var line : hand.getItemMeta().lore()) {
                lore.add(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().serialize(line));
            }
            builder.lore(lore);
        }
        Map<String, Integer> enchants = new java.util.LinkedHashMap<>();
        hand.getEnchantments().forEach((ench, level) -> enchants.put(ench.getKey().getKey(), level));
        builder.vanillaEnchantments(enchants);
        if (hand.hasItemMeta()) {
            builder.unbreakable(hand.getItemMeta().isUnbreakable());
        }
        services.itemsConfig.save(builder.build());
        services.sendMessage(sender, "items.saved", Map.of("id", id));
    }

    private void load(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            services.sendMessage(sender, "player-only", Map.of());
            return;
        }
        if (args.length < 2) {
            services.sendMessage(sender, "invalid-usage", Map.of("usage", "/alkaitems load <id>"));
            return;
        }
        ItemService.GiveResult result = services.itemService.giveItem(player, args[1], 1);
        if (result == ItemService.GiveResult.UNKNOWN_TEMPLATE) {
            services.sendMessage(sender, "unknown-template", Map.of("id", args[1]));
            return;
        }
        services.sendMessage(sender, "items.loaded", Map.of("id", args[1]));
    }

    private void list(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            services.sendMessage(sender, "player-only", Map.of());
            return;
        }
        new TemplateListMenu(player, services, 0).open();
    }

    private void enchant(CommandSender sender, String[] args) {
        if (args.length < 4) {
            services.sendMessage(sender, "invalid-usage", Map.of("usage", "/alkaitems enchant <jogador> <encantamento> <nivel>"));
            return;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            services.sendMessage(sender, "unknown-player", Map.of("name", args[1]));
            return;
        }
        CustomEnchantment enchant = services.enchantsConfig.get(args[2]);
        if (enchant == null) {
            services.sendMessage(sender, "unknown-enchant", Map.of("id", args[2]));
            return;
        }
        int level = parseIntOr(args[3], 1);
        if (level < 1 || level > enchant.maxLevel()) {
            services.sendMessage(sender, "invalid-level", Map.of("max", String.valueOf(enchant.maxLevel())));
            return;
        }
        ItemStack hand = target.getInventory().getItemInMainHand();
        if (hand.getType().isAir()) {
            services.sendMessage(sender, "items.no-item-in-hand", Map.of());
            return;
        }
        services.pdc.addEnchant(hand, enchant.id(), level);
        services.sendMessage(sender, "items.enchant.applied", Map.of("enchant", enchant.displayName(), "level", String.valueOf(level)));
    }

    private void removeEnchant(CommandSender sender, String[] args) {
        if (args.length < 3) {
            services.sendMessage(sender, "invalid-usage", Map.of("usage", "/alkaitems removeenchant <jogador> <encantamento>"));
            return;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            services.sendMessage(sender, "unknown-player", Map.of("name", args[1]));
            return;
        }
        ItemStack hand = target.getInventory().getItemInMainHand();
        if (!services.pdc.hasEnchant(hand, args[2])) {
            services.sendMessage(sender, "items.enchant.not-present", Map.of());
            return;
        }
        services.pdc.removeEnchant(hand, args[2]);
        services.sendMessage(sender, "items.enchant.removed", Map.of("enchant", args[2]));
    }

    private void info(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            services.sendMessage(sender, "player-only", Map.of());
            return;
        }
        ItemStack hand = player.getInventory().getItemInMainHand();
        String id = services.pdc.getTemplateId(hand);
        services.sendMessage(player, "items.info.header", Map.of());
        if (id == null) {
            services.sendMessage(player, "items.info.not-alkaitems", Map.of());
            return;
        }
        services.sendMessage(player, "items.info.template", Map.of("id", id));
        Map<String, Integer> customEnchants = services.pdc.getCustomEnchants(hand);
        String list = customEnchants.isEmpty() ? "-" : customEnchants.entrySet().stream()
                .map(e -> e.getKey() + " " + e.getValue()).collect(Collectors.joining(", "));
        services.sendMessage(player, "items.info.enchants", Map.of("list", list));
        services.sendMessage(player, "items.info.soulbound", Map.of("value", String.valueOf(services.pdc.isSoulbound(hand))));
    }

    private void reload(CommandSender sender) {
        services.configManager.reload();
        services.itemsConfig.load();
        services.enchantsConfig.load();
        services.sendMessage(sender, "reloaded", Map.of());
    }

    private String permissionFor(String subcommand) {
        return switch (subcommand) {
            case "give" -> "alkaitems.give";
            case "info" -> "alkaitems.info";
            case "reload" -> "alkaitems.reload";
            default -> "alkaitems.admin";
        };
    }

    private int parseIntOr(String raw, int def) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            return def;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String lower = args[0].toLowerCase();
            return SUBCOMMANDS.stream().filter(s -> s.startsWith(lower)).collect(Collectors.toList());
        }
        if (args.length == 2) {
            return switch (args[0].toLowerCase()) {
                case "edit", "delete", "save", "load" -> services.itemsConfig.all().stream().map(ItemTemplate::id)
                        .filter(id -> id.startsWith(args[1].toLowerCase())).collect(Collectors.toList());
                case "give", "enchant", "removeenchant" -> Bukkit.getOnlinePlayers().stream().map(Player::getName)
                        .filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase())).collect(Collectors.toList());
                default -> List.of();
            };
        }
        if (args.length == 3 && "give".equalsIgnoreCase(args[0])) {
            return services.itemsConfig.all().stream().map(ItemTemplate::id)
                    .filter(id -> id.startsWith(args[2].toLowerCase())).collect(Collectors.toList());
        }
        if (args.length == 3 && "enchant".equalsIgnoreCase(args[0])) {
            return services.enchantsConfig.all().stream().map(CustomEnchantment::id)
                    .filter(id -> id.startsWith(args[2].toLowerCase())).collect(Collectors.toList());
        }
        return List.of();
    }
}
