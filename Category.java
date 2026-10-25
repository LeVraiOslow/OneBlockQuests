package fr.oneblockquests;

import java.util.List;

public record Category(String id, int index, String name, String icon, String description,
                       int slot, List<Quest> quests) {
}
