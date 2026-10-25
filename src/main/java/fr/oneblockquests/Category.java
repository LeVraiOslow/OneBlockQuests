package fr.oneblockquests;

import java.util.List;

/** slot : emplacement par défaut dans le menu principal (menus/main.yml le remplace). */
public record Category(String id, int index, String name, String description,
                       int slot, List<Quest> quests) {
}
