package fr.oneblockquests;

public final class PlayerData {
    public String category;
    public int position;      // index (0-based) de la quête en cours dans la catégorie
    public int progress;
    public boolean finished;
    public boolean bossbar = true;
    public boolean dirty;
    public boolean isNew;
}
