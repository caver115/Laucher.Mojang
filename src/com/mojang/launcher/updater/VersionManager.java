package com.mojang.launcher.updater;

import com.mojang.launcher.events.RefreshedVersionsListener;
import com.mojang.launcher.updater.download.DownloadJob;
import com.mojang.launcher.versions.CompleteVersion;
import com.mojang.launcher.versions.ReleaseType;
import com.mojang.launcher.versions.Version;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

public interface VersionManager {

    void refreshVersions() throws IOException;

    List<VersionSyncInfo> getVersions();

    List<VersionSyncInfo> getVersions(VersionFilter<? extends ReleaseType> var1);

    VersionSyncInfo getVersionSyncInfo(Version var1);

    VersionSyncInfo getVersionSyncInfo(String var1);

    VersionSyncInfo getVersionSyncInfo(Version var1, Version var2);

    List<VersionSyncInfo> getInstalledVersions();

    CompleteVersion getLatestCompleteVersion(VersionSyncInfo var1) throws IOException;

    DownloadJob downloadVersion(VersionSyncInfo var1, DownloadJob var2) throws IOException;

    DownloadJob downloadResources(DownloadJob var1, CompleteVersion var2) throws IOException;

    ThreadPoolExecutor getExecutorService();

    void addRefreshedVersionsListener(RefreshedVersionsListener var1);

    void removeRefreshedVersionsListener(RefreshedVersionsListener var1);

    VersionSyncInfo syncVersion(VersionSyncInfo var1) throws IOException;

    void installVersion(CompleteVersion var1) throws IOException;

    void uninstallVersion(CompleteVersion var1) throws IOException;
}
