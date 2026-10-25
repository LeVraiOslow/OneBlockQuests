# OneBlockQuests

Système de quêtes linéaires par catégories pour serveur OneBlock (Paper 26.2, Java 25).

## Compiler

Prérequis : JDK 25 et Maven (ou ouvrir le dossier dans IntelliJ IDEA).

    mvn clean package

Le plugin est généré dans `target/OneBlockQuests.jar`. Copie-le dans `plugins/`.

## Installation

1. **Retire le plugin Quests (PikaMug)** : celui-ci le remplace et utilise les mêmes commandes (`/quest`, `/quests`).
2. Supprime les commandes `/quetes` de zMenu pour éviter un conflit de nom.
3. Dépendances (toutes facultatives mais conseillées) : Vault + ton plugin d'économie, LuckPerms, PlaceholderAPI, Nexo.
4. Démarre le serveur : les dossiers `config/`, `messages/` et `menus/` sont créés dans `plugins/OneBlockQuests/`.

## Fichiers de configuration

| Fichier | Contenu |
|---|---|
| `config/config.yml` | Réglages généraux : permissions, récompenses, **bossbar**, déplacements, sons |
| `config/quests.yml` | Catégories et quêtes |
| `messages/messages.yml` | Tous les messages (préfixe compris) |
| `menus/main.yml` | Menu principal : titre, taille, décor, slot / icône / nom / lore de chaque catégorie, objets libres |
| `menus/category.yml` | Modèle des menus de catégorie (copié dans `menus/categories/`) |
| `menus/categories/<id>.yml` | Un fichier par catégorie (créés automatiquement) : titre, taille, slot de chaque quête, icônes, textes, objets libres |

`/quetes reload` recharge tout. Les clés manquantes sont complétées par les valeurs par défaut.

Mise à jour depuis la 1.0.x : `quests.yml` est déplacé automatiquement dans `config/` ;
l'ancien `config.yml` est conservé sous le nom `config.yml.old` (reporte tes réglages dans les nouveaux fichiers).

## Menus personnalisables

Chaque menu se règle librement : `size`, `title`, `filler`, et une section `items` pour ajouter des objets
(`slot` ou `slots`, `icon`, `name`, `lore`, `amount`, `glow`, `action` : `NONE`, `MAIN`, `CLOSE`, `COMMAND`).
Dans `menus/categories/<id>.yml`, `quest-slots` donne l'emplacement de chaque quête dans l'ordre.

Les icônes acceptent plusieurs choix séparés par `|` : `nexo:yess|LIME_DYE` utilise l'item Nexo, ou le
colorant si Nexo est absent. Par défaut : `nexo:yess` pour une quête terminée, `nexo:quest` pour les catégories
du menu principal (accessibles ou verrouillées).

Les fichiers de menu portent une clé `version`. Si une mise à jour du plugin change leur format, l'ancien
fichier est sauvegardé en `.old` et remplacé par le nouveau.

## Bossbar

`bossbar.enabled` dans `config/config.yml` : `false` désactive complètement la bossbar sur le serveur
(la commande `/quetes bossbar` répond alors qu'elle est désactivée). Si `true`, chaque joueur peut la
masquer pour lui avec `/quetes bossbar`.

Le format (`bossbar.format`) affiche par défaut l'objectif de la quête : `{objective}` (« Casse 16 bûches de chêne »).
Il vient du champ `objective` de la quête dans `config/quests.yml`, ou de sa description sans le point final.

## Progression des catégories

Un joueur ne peut ouvrir que sa catégorie actuelle et celles déjà terminées ; les suivantes restent
verrouillées (vérifié à l'ouverture et à chaque clic).

## Commandes

| Commande | Description |
|---|---|
| `/quetes` (alias `/quest`, `/quets`, `/quete`, `/q`) | Ouvre le menu |
| `/quetes bossbar` | Active/désactive la bossbar |
| `/quetes reload` | Recharge config et quêtes (admin) |
| `/quetes admin reset <joueur>` | Remet le joueur à la quête 1 |
| `/quetes admin set <joueur> <catégorie> <n>` | Place le joueur sur une quête |
| `/quetes admin complete <joueur>` | Termine sa quête en cours (avec récompense) |
| `/quetes admin sync <joueur>` | Redonne les permissions selon son avancement |

Permissions : `oneblockquests.use` (défaut : tous), `oneblockquests.admin` (défaut : op).

## Permissions données par le plugin (LuckPerms)

- Au premier join : `oneblockquests.category.debutant` et `oneblockquests.quest.debutant.1`
- Quête N terminée : `oneblockquests.quest.<catégorie>.<N+1>`
- Dernière quête terminée : `oneblockquests.category.<suivante>` et `oneblockquests.quest.<suivante>.1`

La commande utilisée est réglable dans `config.yml` (`permissions.grant-command`).
La progression réelle est stockée par le plugin (`plugins/OneBlockQuests/data/`) ;
les permissions servent aux autres plugins (accès à des zones, menus, etc.).

## Placeholders (PlaceholderAPI)

`%oneblockquests_<clé>%` renvoie le texte sans couleur.
`%oneblockquests_<clé>_colored%` renvoie le texte avec couleurs.

Clés : `category`, `number`, `name`, `description`, `progress`, `required`, `percent`,
`reward`, `completed`, `total`, `display`, `finished`, `category_completed`, `category_total`.

Exemple pour CustomNameplates : `%oneblockquests_display%`
(format réglable dans `config/config.yml` > `placeholders.display-format`).

## Quêtes et anti-farm

Les blocs posés par un joueur ne comptent pas pour les quêtes BREAK. Exceptions : les arbres qui poussent
(la bûche du bas compte), et les cultures (blé, carottes...) qui ne comptent que lorsqu'elles sont mûres.

## Quêtes

Édite `config/quests.yml`. Types : BREAK, PLACE, KILL, CRAFT, FISH, BREED, ENCHANT, SMELT, WALK, SWIM.
`WALK` (marcher) et `SWIM` (nager) comptent en blocs parcourus : `amount: 500` = 500 blocs. Pas de `target` nécessaire.
`target` accepte ANY, un Material / EntityType, une liste, ou des jokers (`*_LOG`).
Nombre de quêtes par catégorie limité par le nombre de slots listés dans `menus/category.yml` (28 par défaut).

## Couleurs, icônes, Nexo

- Couleurs : `&a`, `&#RRGGBB`, `#RRGGBB`.
- Icônes de menu : `MATERIAL`, `MATERIAL:customModelData` ou `nexo:id_item`.
- Les glyphes Nexo s'écrivent directement (caractère unicode du glyphe) dans les textes,
  et les placeholders PlaceholderAPI (`%...%`) sont résolus dans les menus et la bossbar.
