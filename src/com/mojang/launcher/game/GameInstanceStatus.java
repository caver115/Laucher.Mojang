package com.mojang.launcher.game;

public enum GameInstanceStatus {

    PREPARING("PREPARING", 0, "Preparing..."),
    DOWNLOADING("DOWNLOADING", 1, "Downloading..."),
    INSTALLING("INSTALLING", 2, "Installing..."),
    LAUNCHING("LAUNCHING", 3, "Launching..."),
    PLAYING("PLAYING", 4, "Playing..."),
    IDLE("IDLE", 5, "Idle");
    private final String name;
// $FF: synthetic field
    private static final GameInstanceStatus[] $VALUES = new GameInstanceStatus[]{PREPARING, DOWNLOADING, INSTALLING, LAUNCHING, PLAYING, IDLE};

    private GameInstanceStatus(String var1, int var2, String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public String toString() {
        return this.name;
    }

}
