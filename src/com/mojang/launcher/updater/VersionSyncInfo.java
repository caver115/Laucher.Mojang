package com.mojang.launcher.updater;

import com.mojang.launcher.versions.Version;

public class VersionSyncInfo {

    private final Version localVersion;
    private final Version remoteVersion;
    private final boolean isInstalled;
    private final boolean isUpToDate;

    public VersionSyncInfo(Version localVersion, Version remoteVersion, boolean installed, boolean upToDate) {
        this.localVersion = localVersion;
        this.remoteVersion = remoteVersion;
        this.isInstalled = installed;
        this.isUpToDate = upToDate;
    }

    public Version getLocalVersion() {
        return this.localVersion;
    }

    public Version getRemoteVersion() {
        return this.remoteVersion;
    }

    public Version getLatestVersion() {
        return this.getLatestSource() == VersionSyncInfo.VersionSource.REMOTE ? this.remoteVersion : this.localVersion;
    }

    public VersionSyncInfo.VersionSource getLatestSource() {
        return this.getLocalVersion() == null ? VersionSyncInfo.VersionSource.REMOTE : (this.getRemoteVersion() == null ? VersionSyncInfo.VersionSource.LOCAL : (this.getRemoteVersion().getUpdatedTime().after(this.getLocalVersion().getUpdatedTime()) ? VersionSyncInfo.VersionSource.REMOTE : VersionSyncInfo.VersionSource.LOCAL));
    }

    public boolean isInstalled() {
        return this.isInstalled;
    }

    public boolean isOnRemote() {
        return this.remoteVersion != null;
    }

    public boolean isUpToDate() {
        return this.isUpToDate;
    }

    public String toString() {
        return "VersionSyncInfo{localVersion=" + this.localVersion + ", remoteVersion=" + this.remoteVersion + ", isInstalled=" + this.isInstalled + ", isUpToDate=" + this.isUpToDate + '}';
    }

    public static enum VersionSource {

        REMOTE("REMOTE", 0),
        LOCAL("LOCAL", 1);
// $FF: synthetic field
        private static final VersionSyncInfo.VersionSource[] $VALUES = new VersionSyncInfo.VersionSource[]{REMOTE, LOCAL};

        private VersionSource(String var1, int var2) {
        }

    }
}
