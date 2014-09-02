package net.minecraft.launcher.profile;

public enum LauncherVisibilityRule {

    HIDE_LAUNCHER("HIDE_LAUNCHER", 0, "Hide launcher and re-open when game closes"),
    CLOSE_LAUNCHER("CLOSE_LAUNCHER", 1, "Close launcher when game starts"),
    DO_NOTHING("DO_NOTHING", 2, "Keep the launcher open");
    private final String name;
    // $FF: synthetic field
    private static final LauncherVisibilityRule[] $VALUES = new LauncherVisibilityRule[]{HIDE_LAUNCHER, CLOSE_LAUNCHER, DO_NOTHING};

    private LauncherVisibilityRule(String var1, int var2, String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public String toString() {
        return this.name;
    }

}
