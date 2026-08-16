<div align="center">

# AlkaItems

### Motor de itens, encantamentos e efeitos customizados

Templates de item, encantamentos customizados com gatilhos e efeitos, e
sistema de bônus de conjunto — tudo construído sobre o AlkaCore, para a rede
de plugins AlkaStudio.

![Java](https://img.shields.io/badge/Java-21-orange)
![Minecraft](https://img.shields.io/badge/Minecraft-1.21.8-green)
![Version](https://img.shields.io/badge/Version-1.0.1-blue)
![License](https://img.shields.io/badge/License-Proprietary-red)

</div>

---

## 📋 Sobre o Projeto

O **AlkaItems** é o motor de itens customizados da rede AlkaStudio. Ele dá
vida a templates de item reutilizáveis, encantamentos próprios com gatilhos e
efeitos variados, e itens que reagem a serem equipados, segurados ou usados —
tudo persistido via `PersistentDataContainer` nativo do Paper, sem NBT-API de
terceiros.

O AlkaItems **complementa** o ItemsAdder e o AdvancedEnchantments — os dois
continuam totalmente úteis no servidor e coexistem normalmente com os itens e
encantamentos do AlkaItems.

## ✨ Funcionalidades Principais

| Módulo | Descrição |
| --- | --- |
| 🧩 **Templates de item** | Molde reutilizável (material, nome, lore, encantamentos, efeitos, flags) instanciado sob demanda, com persistência nativa do Paper. |
| ⚡ **Encantamentos customizados** | 10 gatilhos (ao acertar, matar, minerar, atirar, sofrer dano, pescar, morrer, agachar, correr, etc.) com chance por nível e 16 tipos de efeito, coexistindo com encantamentos vanilla e do AdvancedEnchantments. |
| ✨ **Efeitos de item** | Efeitos contínuos ao equipar, segurar ou usar um item, com contagem por referência para nunca remover o efeito antes da hora quando duas fontes coincidem. |
| 🔒 **Soulbound** | Itens que não caem na morte nem por `/drop` manual. |
| 👑 **Requisito de VIP/Rank** | Itens que só podem ser equipados/usados por jogadores com determinado tier de VIP ou rank, integrado via PlaceholderAPI. |
| 🎨 **Editor in-game** | `/alkaitems edit <id>` para ajustar campos escalares e flags direto pelo chat/clique, sem precisar editar YAML na mão. |
| 🛡️ **Set Bonus** | Bônus contínuo que ativa quando o jogador equipa um conjunto de peças com o mesmo encantamento ou grupo, desligando automaticamente ao remover qualquer peça. |
| 🔌 **API pública** | `AlkaItemsAPI` para outros plugins darem itens, checarem posse e consultarem templates/encantamentos. |

## 🎮 Comandos

| Comando | Descrição | Permissão |
| --- | --- | --- |
| `/alkaitems info` | Vê informações do item na mão | `alkaitems.info` |
| `/alkaitems give` | Dá um item customizado a um jogador | `alkaitems.give` |
| `/alkaitems create\|edit\|delete\|save\|load\|list\|enchant\|removeenchant\|reload` | Gerenciamento administrativo de itens e encantamentos | `alkaitems.admin` |

Aliases: `/ai`, `/alkai`.

## 🔗 Integrações

- **AlkaCore** (obrigatória) — GUI compartilhada (`BaseGui`).
- **ItemsAdder** e **AdvancedEnchantments** — coexistem com os itens e
  encantamentos do AlkaItems, sem sobreposição.
- **PlaceholderAPI** — checagem de VIP/rank exigido para equipar itens.
- **AlkaVips** — templates de item como recompensa de ativação de VIP.
- **AlkaRankUp** — checagem de rank exigido via placeholder.

## 🔧 Tecnologias Utilizadas

- **Java 21** · **Gradle** (com `shadow`)
- **Paper API 1.21.8**
- **Adventure/MiniMessage** para mensagens e GUI
- `PersistentDataContainer` nativo para persistência de item

## ⚙️ Instalação

1. Instale o **AlkaCore** primeiro.
2. Coloque `AlkaItems.jar` na pasta `plugins/` do servidor (Paper **1.21.8+**).
3. Reinicie o servidor.
4. Configure templates e encantamentos em `items.yml` e `enchants.yml`, depois
   `/alkaitems reload`.

## 🔐 Permissões

| Permissão | Padrão | Descrição |
| --- | --- | --- |
| `alkaitems.admin` | op | Comandos administrativos do AlkaItems |
| `alkaitems.give` | op | Dar itens customizados a outros jogadores |
| `alkaitems.info` | true | Ver informações do item na mão |
| `alkaitems.reload` | op | Recarregar configuração |

## 📝 Licença

> ⚠️ **Projeto proprietário da AlkaStudio.**
>
> Código fonte destinado exclusivamente ao uso interno da rede `Alka*`.
> Reprodução, distribuição ou uso não autorizado não são permitidos.

## 🎯 Créditos

- **Desenvolvido por**: MestreDEV — AlkaStudio
- **Parte de**: todo o ecossistema `Alka*`

---

<div align="center">

**Desenvolvido com ❤️ pela AlkaStudio**

[![AlkaStudio](https://img.shields.io/badge/AlkaStudio-JLob0-blue)](https://github.com/JLob0)

</div>
