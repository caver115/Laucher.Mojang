package net.minecraft.launcher.game;

import com.google.common.collect.Maps;
import com.mojang.launcher.versions.ReleaseType;
import java.util.Map;

public enum MinecraftReleaseType implements ReleaseType {

    SNAPSHOT("SNAPSHOT", 0, "snapshot", "Enable experimental development versions (\"snapshots\")"),
    RELEASE("RELEASE", 1, "release", (String) null),
    OLD_BETA("OLD_BETA", 2, "old_beta", "Allow use of old \"Beta\" Minecraft versions (From 2010-2011)"),
    OLD_ALPHA("OLD_ALPHA", 3, "old_alpha", "Allow use of old \"Alpha\" Minecraft versions (From 2010)");
    private static final String POPUP_DEV_VERSIONS = "Are you sure you want to enable development builds?\nThey are not guaranteed to be stable and may corrupt your world.\nYou are advised to run this in a separate directory or run regular backups.";
    private static final String POPUP_OLD_VERSIONS = "These versions are very out of date and may be unstable. Any bugs, crashes, missing features or\nother nasties you may find will never be fixed in these versions.\nIt is strongly recommended you play these in separate directories to avoid corruption.\nWe are not responsible for the damage to your nostalgia or your save files!";
    private static final Map<String, MinecraftReleaseType> LOOKUP = Maps.newHashMap();
    private final String name;
    private final String description;
// $FF: synthetic field
    private static final MinecraftReleaseType[] $VALUES = new MinecraftReleaseType[]{SNAPSHOT, RELEASE, OLD_BETA, OLD_ALPHA};

    private MinecraftReleaseType(String var1, int var2, String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getName() {
        return this.name;
    }

    public String getDescription() {
        return this.description;
    }

    public String getPopupWarning() {
        return this.description == null ? null : (this == SNAPSHOT ? "Are you sure you want to enable development builds?\nThey are not guaranteed to be stable and may corrupt your world.\nYou are advised to run this in a separate directory or run regular backups." : (this == OLD_BETA ? "These versions are very out of date and may be unstable. Any bugs, crashes, missing features or\nother nasties you may find will never be fixed in these versions.\nIt is strongly recommended you play these in separate directories to avoid corruption.\nWe are not responsible for the damage to your nostalgia or your save files!" : (this == OLD_ALPHA ? "These versions are very out of date and may be unstable. Any bugs, crashes, missing features or\nother nasties you may find will never be fixed in these versions.\nIt is strongly recommended you play these in separate directories to avoid corruption.\nWe are not responsible for the damage to your nostalgia or your save files!" : null)));
    }

    public static MinecraftReleaseType getByName(String name) {
        return (MinecraftReleaseType) LOOKUP.get(name);
    }

    static {
        MinecraftReleaseType[] arr$ = values();
        int len$ = arr$.length;

        for (int i$ = 0; i$ < len$; ++i$) {
            MinecraftReleaseType type = arr$[i$];
            LOOKUP.put(type.getName(), type);
        }

    }
}
