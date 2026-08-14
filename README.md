# AlkaItems

Motor de itens customizados, efeitos ao equipar/segurar/usar e encantamentos
customizados para a rede AlkaStudio (Paper 1.21.8 / Java 21) — construído sobre o
AlkaCore. Substitui ItemEdit + ItemTag + parte das actions do ItemsAdder;
**complementa** (não substitui) o ItemsAdder e o AdvancedEnchantments — os dois
continuam totalmente úteis e ficam como softdependency via reflection, nunca
importados direto.

## O que faz

- **Templates de item** (`items.yml`) — molde reutilizável (material, nome, lore,
  encantamentos vanilla + customizados, efeitos, flags) instanciado em `ItemStack`
  via PDC nativo do Paper (`PersistentDataContainer`, zero NBT-API de terceiro).
- **Encantamentos customizados** (`enchants.yml`) — 10 triggers (`ON_HIT`, `ON_KILL`,
  `ON_BLOCK_BREAK`, `ON_DAMAGE_TAKEN`, `ON_SHOOT`, `ON_PROJECTILE_HIT`, `ON_FISH`,
  `ON_DEATH`, `ON_SNEAK`, `ON_SPRINT`) com chance por nível e 16 tipos de efeito
  (poção, dano, cura, teleporte, raio, explosão, partícula, som, comando,
  multiplicador de drop/XP, lifesteal, dodge, crítico). Coexiste com encantamentos
  vanilla e do AdvancedEnchantments no mesmo item.
- **Efeitos de item** (substitui o ItemTag) — `ON_EQUIP`/`ON_UNEQUIP` (armadura),
  `ON_HOLD`/`ON_UNHOLD` (mão principal/secundária), `ON_USE` (clique), `ON_PASSIVE`
  (presença no inventário). 14 tipos: poção contínua, atributo, comando, partícula,
  som, velocidade, pulo, voo, visão noturna, respiração aquática, resistência ao
  fogo, sem dano de queda, pulo duplo, dash. Contagem por referência (`Map<UUID,
  Integer>`) evita que duas fontes do mesmo efeito se removam cedo demais.
- **Soulbound** — não dropa na morte (restaurado no respawn) nem por `/drop` manual.
- **VIP/rank requerido pra equipar/usar** — `vip-required`/`rank-required` no
  template, checados via placeholder do PlaceholderAPI (`%alkavips_has_vip_<tier>%`,
  `%alkarankup_rank_index%`) — nunca importa `com.alkacode.vips.*`/
  `com.alkacode.rankup.*` direto. Sem PAPI instalado, a checagem fica desativada.
- **Editor prático** (`/alkaitems edit <id>`) — campos escalares/flags clicáveis
  (boolean cicla no clique, texto/número via chat), sem seletor de material
  paginado nem editor de lore linha-a-linha (escolha de escopo confirmada com o
  usuário — criar um item novo do zero continua sendo mais rápido editando
  `items.yml` + `/alkaitems reload`).
- **API pública** (`api/AlkaItemsAPI`, registrada via ServicesManager e também
  exposta em `AlkaItemsPlugin#getAPI()`) — `giveItem`, `hasItem` (dedupe de
  soulbound), `getTemplate`, `getEnchant`, `hasEnchant`, `getEnchantLevel`.
- **Integração com AlkaVips** — `item-rewards:` por tier em `vips.yml` do AlkaVips
  entrega templates na primeira ativação (não-acumulada) do VIP, via
  `hook/AlkaItemsHook` (ServicesManager + reflection, nunca importa
  `com.alkacode.items.*`). Opcional — sem AlkaItems instalado, o VIP funciona normal.

## Dependências

- **AlkaCore** (hard dependency) — GUI compartilhada (`BaseGui`).
- **ItemsAdder**, **AdvancedEnchantments** — soft-dependency via **reflection**
  (nunca `compileOnly` na API deles — nenhum dos dois tem artefato Maven público
  confiável, mesmo padrão já usado em AlkaVips/AlkaMines/AlkaAnvil).
- **PlaceholderAPI** — soft-dependency direta (compileOnly, API estável e amplamente
  usada no ecossistema) — só necessária pras checagens de `vip-required`/
  `rank-required`.

## Limitações conhecidas (v1.0.0)

- **DOUBLE_JUMP/DASH são aproximações** — não existe pulo duplo real na API do
  Bukkit; usa o truque de religar `allowFlight` ao tocar o chão e interceptar
  `PlayerToggleFlightEvent` pra converter em um impulso de velocidade.
- **Multiplicar drops de verdade** (`DROP_MULTIPLIER` num encantamento
  `ON_BLOCK_BREAK`) só multiplica o XP dropado (`BlockBreakEvent#setExpToDrop`) —
  multiplicar os ITENS dropados exigiria cancelar o break vanilla e dropar manual
  (perderia Fortune/Silk Touch corretos), fora de escopo nesta versão.
- **Registro do template no namespace do ItemsAdder** (`itemsadder-id` no template,
  pro item do AlkaItems ganhar textura/modelo do IA) é só um campo reservado —
  registrar de verdade exigiria interagir com a API de resource-pack do ItemsAdder,
  não implementado. A direção inversa (usar `material: "itemsadder:algum_id"` pra
  referenciar um item JÁ existente do IA) funciona normalmente.
- **Migração/import automático de definições do AdvancedEnchantments** (proposto na
  especificação original) não foi implementado — só apply/get/hasEnchant via
  reflection na API real do AE (verificada via `javap`, ver
  `reference-advancedenchantments-api`). Os dois sistemas coexistem, mas não há
  conversão de um AE-enchant existente pra um CustomEnchantment do AlkaItems.
- **"unique_items_collected"** (placeholder do spec original) não foi implementado —
  exigiria persistência por jogador que este plugin deliberadamente não tem
  (templates/encantamentos são só YAML, sem tabela no banco).
- Efeitos de item com **referência circular ao mesmo `PotionEffectType`** vindos de
  DUAS fontes diferentes (ex: dois itens dando Regeneração) são contados por
  referência corretamente; `ATTRIBUTE`/`SPEED`/`JUMP` usam uma `NamespacedKey`
  determinística por (template, trigger, índice do efeito), então empilham como
  qualquer `AttributeModifier` vanilla, sem esse cuidado extra.

## Origem

Construído a partir de uma especificação pré-escrita (`AlkaItems_Spec.md`/
`AlkaItems_Spec_v1.1.md`) que propunha importar a API do ItemsAdder e do
AdvancedEnchantments diretamente (`compileOnly`, por serem "licenciados/comprados").
Essa parte foi revertida por instrução explícita do usuário para manter o padrão já
usado no resto do ecossistema — reflection + softdepend, os dois continuam
complementares, nunca substituídos. Escopo das GUIs de edição (prático vs. paridade
total com o CustomAnvil de referência) decidido com o usuário antes da
implementação.
