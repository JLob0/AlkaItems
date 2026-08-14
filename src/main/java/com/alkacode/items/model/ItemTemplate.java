package com.alkacode.items.model;

import java.util.List;
import java.util.Map;

/**
 * Molde de item salvo em items.yml. {@code material} aceita tanto um enum vanilla
 * ("DIAMOND_SWORD") quanto um id do ItemsAdder ("itemsadder:custom_sword") - ver
 * {@link com.alkacode.items.hook.ItemsAdderHook#isCustomId(String)}.
 */
public final class ItemTemplate {

    private final String id;
    private final String material;
    private final String name;
    private final List<String> lore;
    private final Map<String, Integer> vanillaEnchantments;
    private final Map<String, Integer> customEnchantments;
    private final List<ItemEffect> effectsOnEquip;
    private final List<ItemEffect> effectsOnHold;
    private final List<ItemEffect> effectsOnUse;
    private final List<ItemEffect> effectsPassive;
    private final boolean soulbound;
    private final boolean glow;
    private final boolean unbreakable;
    private final int customModelData;
    private final String itemsAdderId;
    private final Map<String, Double> attributes;
    private final int maxDurability;
    private final boolean hideEnchants;
    private final boolean hideAttributes;
    private final String vipRequired;
    private final int rankRequired;
    private final String color;

    private ItemTemplate(Builder b) {
        this.id = b.id;
        this.material = b.material;
        this.name = b.name;
        this.lore = List.copyOf(b.lore);
        this.vanillaEnchantments = Map.copyOf(b.vanillaEnchantments);
        this.customEnchantments = Map.copyOf(b.customEnchantments);
        this.effectsOnEquip = List.copyOf(b.effectsOnEquip);
        this.effectsOnHold = List.copyOf(b.effectsOnHold);
        this.effectsOnUse = List.copyOf(b.effectsOnUse);
        this.effectsPassive = List.copyOf(b.effectsPassive);
        this.soulbound = b.soulbound;
        this.glow = b.glow;
        this.unbreakable = b.unbreakable;
        this.customModelData = b.customModelData;
        this.itemsAdderId = b.itemsAdderId;
        this.attributes = Map.copyOf(b.attributes);
        this.maxDurability = b.maxDurability;
        this.hideEnchants = b.hideEnchants;
        this.hideAttributes = b.hideAttributes;
        this.vipRequired = b.vipRequired;
        this.rankRequired = b.rankRequired;
        this.color = b.color;
    }

    public String id() { return id; }
    public String material() { return material; }
    public String name() { return name; }
    public List<String> lore() { return lore; }
    public Map<String, Integer> vanillaEnchantments() { return vanillaEnchantments; }
    public Map<String, Integer> customEnchantments() { return customEnchantments; }
    public List<ItemEffect> effectsOnEquip() { return effectsOnEquip; }
    public List<ItemEffect> effectsOnHold() { return effectsOnHold; }
    public List<ItemEffect> effectsOnUse() { return effectsOnUse; }
    public List<ItemEffect> effectsPassive() { return effectsPassive; }
    public boolean soulbound() { return soulbound; }
    public boolean glow() { return glow; }
    public boolean unbreakable() { return unbreakable; }
    public int customModelData() { return customModelData; }
    public String itemsAdderId() { return itemsAdderId; }
    public Map<String, Double> attributes() { return attributes; }
    public int maxDurability() { return maxDurability; }
    public boolean hideEnchants() { return hideEnchants; }
    public boolean hideAttributes() { return hideAttributes; }
    public String vipRequired() { return vipRequired; }
    public int rankRequired() { return rankRequired; }
    public String color() { return color; }

    public boolean hasVipRequirement() { return vipRequired != null && !vipRequired.isBlank(); }
    public boolean hasRankRequirement() { return rankRequired > 0; }

    public Builder toBuilder() {
        Builder b = new Builder(id);
        b.material = material;
        b.name = name;
        b.lore = new java.util.ArrayList<>(lore);
        b.vanillaEnchantments = new java.util.LinkedHashMap<>(vanillaEnchantments);
        b.customEnchantments = new java.util.LinkedHashMap<>(customEnchantments);
        b.effectsOnEquip = new java.util.ArrayList<>(effectsOnEquip);
        b.effectsOnHold = new java.util.ArrayList<>(effectsOnHold);
        b.effectsOnUse = new java.util.ArrayList<>(effectsOnUse);
        b.effectsPassive = new java.util.ArrayList<>(effectsPassive);
        b.soulbound = soulbound;
        b.glow = glow;
        b.unbreakable = unbreakable;
        b.customModelData = customModelData;
        b.itemsAdderId = itemsAdderId;
        b.attributes = new java.util.LinkedHashMap<>(attributes);
        b.maxDurability = maxDurability;
        b.hideEnchants = hideEnchants;
        b.hideAttributes = hideAttributes;
        b.vipRequired = vipRequired;
        b.rankRequired = rankRequired;
        b.color = color;
        return b;
    }

    public static Builder builder(String id) {
        return new Builder(id);
    }

    public static final class Builder {
        private final String id;
        private String material = "STONE";
        private String name = "";
        private List<String> lore = new java.util.ArrayList<>();
        private Map<String, Integer> vanillaEnchantments = new java.util.LinkedHashMap<>();
        private Map<String, Integer> customEnchantments = new java.util.LinkedHashMap<>();
        private List<ItemEffect> effectsOnEquip = new java.util.ArrayList<>();
        private List<ItemEffect> effectsOnHold = new java.util.ArrayList<>();
        private List<ItemEffect> effectsOnUse = new java.util.ArrayList<>();
        private List<ItemEffect> effectsPassive = new java.util.ArrayList<>();
        private boolean soulbound;
        private boolean glow;
        private boolean unbreakable;
        private int customModelData;
        private String itemsAdderId = "";
        private Map<String, Double> attributes = new java.util.LinkedHashMap<>();
        private int maxDurability;
        private boolean hideEnchants;
        private boolean hideAttributes;
        private String vipRequired = "";
        private int rankRequired;
        private String color = "";

        private Builder(String id) { this.id = id; }

        public Builder material(String v) { this.material = v; return this; }
        public Builder name(String v) { this.name = v; return this; }
        public Builder lore(List<String> v) { this.lore = new java.util.ArrayList<>(v); return this; }
        public Builder vanillaEnchantments(Map<String, Integer> v) { this.vanillaEnchantments = new java.util.LinkedHashMap<>(v); return this; }
        public Builder customEnchantments(Map<String, Integer> v) { this.customEnchantments = new java.util.LinkedHashMap<>(v); return this; }
        public Builder effectsOnEquip(List<ItemEffect> v) { this.effectsOnEquip = new java.util.ArrayList<>(v); return this; }
        public Builder effectsOnHold(List<ItemEffect> v) { this.effectsOnHold = new java.util.ArrayList<>(v); return this; }
        public Builder effectsOnUse(List<ItemEffect> v) { this.effectsOnUse = new java.util.ArrayList<>(v); return this; }
        public Builder effectsPassive(List<ItemEffect> v) { this.effectsPassive = new java.util.ArrayList<>(v); return this; }
        public Builder soulbound(boolean v) { this.soulbound = v; return this; }
        public Builder glow(boolean v) { this.glow = v; return this; }
        public Builder unbreakable(boolean v) { this.unbreakable = v; return this; }
        public Builder customModelData(int v) { this.customModelData = v; return this; }
        public Builder itemsAdderId(String v) { this.itemsAdderId = v; return this; }
        public Builder attributes(Map<String, Double> v) { this.attributes = new java.util.LinkedHashMap<>(v); return this; }
        public Builder maxDurability(int v) { this.maxDurability = v; return this; }
        public Builder hideEnchants(boolean v) { this.hideEnchants = v; return this; }
        public Builder hideAttributes(boolean v) { this.hideAttributes = v; return this; }
        public Builder vipRequired(String v) { this.vipRequired = v; return this; }
        public Builder rankRequired(int v) { this.rankRequired = v; return this; }
        public Builder color(String v) { this.color = v; return this; }

        public ItemTemplate build() { return new ItemTemplate(this); }
    }
}
