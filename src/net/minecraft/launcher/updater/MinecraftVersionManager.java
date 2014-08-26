package net.minecraft.launcher.updater;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.mojang.launcher.OperatingSystem;
import com.mojang.launcher.events.RefreshedVersionsListener;
import com.mojang.launcher.updater.ExceptionalThreadPoolExecutor;
import com.mojang.launcher.updater.VersionFilter;
import com.mojang.launcher.updater.VersionManager;
import com.mojang.launcher.updater.VersionSyncInfo;
import com.mojang.launcher.updater.download.DownloadJob;
import com.mojang.launcher.updater.download.Downloadable;
import com.mojang.launcher.updater.download.EtagDownloadable;
import com.mojang.launcher.updater.download.assets.AssetDownloadable;
import com.mojang.launcher.updater.download.assets.AssetIndex;
import com.mojang.launcher.versions.CompleteVersion;
import com.mojang.launcher.versions.ReleaseType;
import com.mojang.launcher.versions.Version;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.Proxy;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import net.minecraft.launcher.game.MinecraftReleaseType;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MinecraftVersionManager implements VersionManager {

    private static final Logger LOGGER = LogManager.getLogger();
    private final VersionList localVersionList;
    private final VersionList remoteVersionList;
    private final ThreadPoolExecutor executorService;
    private final List<RefreshedVersionsListener> refreshedVersionsListeners;
    private final Object refreshLock;
    private boolean isRefreshing;
    private final Gson gson;

    public MinecraftVersionManager(VersionList localVersionList, VersionList remoteVersionList) {
        this.executorService = new ExceptionalThreadPoolExecutor(4, 8, 30L, TimeUnit.SECONDS);
        this.refreshedVersionsListeners = Collections.synchronizedList(new ArrayList());
        this.refreshLock = new Object();
        this.gson = new Gson();
        this.localVersionList = localVersionList;
        this.remoteVersionList = remoteVersionList;
    }

    public void refreshVersions() throws IOException {
        Object i$ = this.refreshLock;
        synchronized (this.refreshLock) {
            this.isRefreshing = true;
        }

        try {
            LOGGER.info("Refreshing local version list...");
            this.localVersionList.refreshVersions();
            LOGGER.info("Refreshing remote version list...");
            this.remoteVersionList.refreshVersions();
        } catch (IOException var7) {
            Object listener = this.refreshLock;
            synchronized (this.refreshLock) {
                this.isRefreshing = false;
            }

            throw var7;
        }

        LOGGER.info("Refresh complete.");
        i$ = this.refreshLock;
        synchronized (this.refreshLock) {
            this.isRefreshing = false;
        }

        Iterator i$1 = Lists.newArrayList((Iterable) this.refreshedVersionsListeners).iterator();

        while (i$1.hasNext()) {
            RefreshedVersionsListener listener1 = (RefreshedVersionsListener) i$1.next();
            listener1.onVersionsRefreshed(this);
        }

    }

    public List<VersionSyncInfo> getVersions() {
        return this.getVersions((VersionFilter) null);
    }

    public List<VersionSyncInfo> getVersions(VersionFilter<? extends ReleaseType> filter) {
        Object result = this.refreshLock;
        synchronized (this.refreshLock) {
            if (this.isRefreshing) {
                return new ArrayList();
            }
        }

        ArrayList var10 = new ArrayList();
        HashMap lookup = new HashMap();
        EnumMap counts = Maps.newEnumMap(MinecraftReleaseType.class);
        MinecraftReleaseType[] i$ = MinecraftReleaseType.values();
        int version = i$.length;

        for (int syncInfo = 0; syncInfo < version; ++syncInfo) {
            MinecraftReleaseType syncInfo1 = i$[syncInfo];
            counts.put(syncInfo1, Integer.valueOf(0));
        }

        Iterator var11 = Lists.newArrayList((Iterable) this.localVersionList.getVersions()).iterator();

        Version var12;
        MinecraftReleaseType var13;
        VersionSyncInfo var14;
        while (var11.hasNext()) {
            var12 = (Version) var11.next();
            if (var12.getType() != null && var12.getUpdatedTime() != null) {
                var13 = (MinecraftReleaseType) var12.getType();
                if (filter == null || filter.getTypes().contains(var13) && ((Integer) counts.get(var13)).intValue() < filter.getMaxCount()) {
                    var14 = this.getVersionSyncInfo(var12, this.remoteVersionList.getVersion(var12.getId()));
                    lookup.put(var12.getId(), var14);
                    var10.add(var14);
                }
            }
        }

        var11 = this.remoteVersionList.getVersions().iterator();

        while (var11.hasNext()) {
            var12 = (Version) var11.next();
            if (var12.getType() != null && var12.getUpdatedTime() != null) {
                var13 = (MinecraftReleaseType) var12.getType();
                if (!lookup.containsKey(var12.getId()) && (filter == null || filter.getTypes().contains(var13) && ((Integer) counts.get(var13)).intValue() < filter.getMaxCount())) {
                    var14 = this.getVersionSyncInfo(this.localVersionList.getVersion(var12.getId()), var12);
                    lookup.put(var12.getId(), var14);
                    var10.add(var14);
                    if (filter != null) {
                        counts.put(var13, Integer.valueOf(((Integer) counts.get(var13)).intValue() + 1));
                    }
                }
            }
        }

        if (var10.isEmpty()) {
            var11 = this.localVersionList.getVersions().iterator();

            while (var11.hasNext()) {
                var12 = (Version) var11.next();
                if (var12.getType() != null && var12.getUpdatedTime() != null) {
                    VersionSyncInfo var15 = this.getVersionSyncInfo(var12, this.remoteVersionList.getVersion(var12.getId()));
                    lookup.put(var12.getId(), var15);
                    var10.add(var15);
                    break;
                }
            }
        }

        Collections.sort(var10, new Comparator() {
            public int compare(VersionSyncInfo a, VersionSyncInfo b) {
                Version aVer = a.getLatestVersion();
                Version bVer = b.getLatestVersion();
                return aVer.getReleaseTime() != null && bVer.getReleaseTime() != null ? bVer.getReleaseTime().compareTo(aVer.getReleaseTime()) : bVer.getUpdatedTime().compareTo(aVer.getUpdatedTime());
            }
// $FF: synthetic method
// $FF: bridge method

            public int compare(Object x0, Object x1) {
                return this.compare((VersionSyncInfo) x0, (VersionSyncInfo) x1);
            }
        });
        return var10;
    }

    public VersionSyncInfo getVersionSyncInfo(Version version) {
        return this.getVersionSyncInfo(version.getId());
    }

    public VersionSyncInfo getVersionSyncInfo(String name) {
        return this.getVersionSyncInfo(this.localVersionList.getVersion(name), this.remoteVersionList.getVersion(name));
    }

    public VersionSyncInfo getVersionSyncInfo(Version localVersion, Version remoteVersion) {
        boolean installed = localVersion != null;
        boolean upToDate = installed;
        CompleteMinecraftVersion resolved = null;
        if (installed && remoteVersion != null) {
            upToDate = !remoteVersion.getUpdatedTime().after(localVersion.getUpdatedTime());
        }

        if (localVersion instanceof CompleteVersion) {
            try {
                resolved = ((CompleteMinecraftVersion) localVersion).resolve(this);
            } catch (IOException var7) {
                LOGGER.error("Couldn\'t resolve version " + localVersion.getId(), (Throwable) var7);
                resolved = (CompleteMinecraftVersion) localVersion;
            }

            upToDate &= this.localVersionList.hasAllFiles(resolved, OperatingSystem.getCurrentPlatform());
        }

        return new VersionSyncInfo(resolved, remoteVersion, installed, upToDate);
    }

    public List<VersionSyncInfo> getInstalledVersions() {
        ArrayList result = new ArrayList();
        Iterator i$ = this.localVersionList.getVersions().iterator();

        while (i$.hasNext()) {
            Version version = (Version) i$.next();
            if (version.getType() != null && version.getUpdatedTime() != null) {
                VersionSyncInfo syncInfo = this.getVersionSyncInfo(version, this.remoteVersionList.getVersion(version.getId()));
                result.add(syncInfo);
            }
        }

        return result;
    }

    public VersionList getRemoteVersionList() {
        return this.remoteVersionList;
    }

    public VersionList getLocalVersionList() {
        return this.localVersionList;
    }

    public CompleteMinecraftVersion getLatestCompleteVersion(VersionSyncInfo syncInfo) throws IOException {
        if (syncInfo.getLatestSource() == VersionSyncInfo.VersionSource.REMOTE) {
            CompleteMinecraftVersion result = null;
            IOException exception = null;

            try {
                result = this.remoteVersionList.getCompleteVersion(syncInfo.getLatestVersion());
            } catch (IOException var7) {
                exception = var7;

                try {
                    result = this.localVersionList.getCompleteVersion(syncInfo.getLatestVersion());
                } catch (IOException var6) {
                    ;
                }
            }

            if (result != null) {
                return result;
            } else {
                throw exception;
            }
        } else {
            return this.localVersionList.getCompleteVersion(syncInfo.getLatestVersion());
        }
    }

    public DownloadJob downloadVersion(VersionSyncInfo syncInfo, DownloadJob job) throws IOException {
        if (!(this.localVersionList instanceof LocalVersionList)) {
            throw new IllegalArgumentException("Cannot download if local repo isn\'t a LocalVersionList");
        } else if (!(this.remoteVersionList instanceof RemoteVersionList)) {
            throw new IllegalArgumentException("Cannot download if local repo isn\'t a RemoteVersionList");
        } else {
            CompleteMinecraftVersion version = this.getLatestCompleteVersion(syncInfo);
            File baseDirectory = ((LocalVersionList) this.localVersionList).getBaseDirectory();
            Proxy proxy = ((RemoteVersionList) this.remoteVersionList).getProxy();
            job.addDownloadables((Collection) version.getRequiredDownloadables(OperatingSystem.getCurrentPlatform(), proxy, baseDirectory, false));
            String jarFile = "versions/" + version.getJar() + "/" + version.getJar() + ".jar";
            job.addDownloadables(new Downloadable[]{new EtagDownloadable(proxy, this.remoteVersionList.getUrl(jarFile), new File(baseDirectory, jarFile), false)});
            return job;
        }
    }

    public DownloadJob downloadResources(DownloadJob job, CompleteVersion version) throws IOException {
        File baseDirectory = ((LocalVersionList) this.localVersionList).getBaseDirectory();
        job.addDownloadables((Collection) this.getResourceFiles(((RemoteVersionList) this.remoteVersionList).getProxy(), baseDirectory, (CompleteMinecraftVersion) version));
        return job;
    }

    private Set<Downloadable> getResourceFiles(Proxy proxy, File baseDirectory, CompleteMinecraftVersion version) {
        HashSet result = new HashSet();
        InputStream inputStream = null;
        File assets = new File(baseDirectory, "assets");
        File objectsFolder = new File(assets, "objects");
        File indexesFolder = new File(assets, "indexes");
        String indexName = version.getAssets();
        long start = System.nanoTime();
        if (indexName == null) {
            indexName = "legacy";
        }

        File indexFile = new File(indexesFolder, indexName + ".json");

        try {
            URL ex = this.remoteVersionList.getUrl("indexes/" + indexName + ".json");
            inputStream = ex.openConnection(proxy).getInputStream();
            String json = IOUtils.toString(inputStream);
            FileUtils.writeStringToFile(indexFile, json);
            AssetIndex index = (AssetIndex) this.gson.fromJson(json, AssetIndex.class);
            Iterator end = index.getUniqueObjects().entrySet().iterator();

            while (end.hasNext()) {
                Entry entry = (Entry) end.next();
                AssetIndex.AssetObject delta = (AssetIndex.AssetObject) entry.getKey();
                String filename = delta.getHash().substring(0, 2) + "/" + delta.getHash();
                File file = new File(objectsFolder, filename);
                if (!file.isFile() || FileUtils.sizeOf(file) != delta.getSize()) {
                    AssetDownloadable downloadable = new AssetDownloadable(proxy, (String) entry.getValue(), delta, "http://resources.download.minecraft.net/", objectsFolder);
                    downloadable.setExpectedSize(delta.getSize());
                    result.add(downloadable);
                }
            }

            long end1 = System.nanoTime();
            long delta1 = end1 - start;
            LOGGER.debug("Delta time to compare resources: " + delta1 / 1000000L + " ms ");
        } catch (Exception var25) {
            LOGGER.error("Couldn\'t download resources", (Throwable) var25);
        } finally {
            IOUtils.closeQuietly(inputStream);
        }

        return result;
    }

    public ThreadPoolExecutor getExecutorService() {
        return this.executorService;
    }

    public void addRefreshedVersionsListener(RefreshedVersionsListener listener) {
        this.refreshedVersionsListeners.add(listener);
    }

    public void removeRefreshedVersionsListener(RefreshedVersionsListener listener) {
        this.refreshedVersionsListeners.remove(listener);
    }

    public VersionSyncInfo syncVersion(VersionSyncInfo syncInfo) throws IOException {
        CompleteMinecraftVersion remoteVersion = this.getRemoteVersionList().getCompleteVersion(syncInfo.getRemoteVersion());
        this.getLocalVersionList().removeVersion(syncInfo.getLocalVersion());
        this.getLocalVersionList().addVersion(remoteVersion);
        ((LocalVersionList) this.getLocalVersionList()).saveVersion(((CompleteMinecraftVersion) remoteVersion).getSavableVersion());
        return this.getVersionSyncInfo((Version) remoteVersion);
    }

    public void installVersion(CompleteVersion version) throws IOException {
        if (version instanceof CompleteMinecraftVersion) {
            version = ((CompleteMinecraftVersion) version).getSavableVersion();
        }

        VersionList localVersionList = this.getLocalVersionList();
        if (localVersionList.getVersion(((CompleteVersion) version).getId()) != null) {
            localVersionList.removeVersion(((CompleteVersion) version).getId());
        }

        localVersionList.addVersion((CompleteVersion) version);
        if (localVersionList instanceof LocalVersionList) {
            ((LocalVersionList) localVersionList).saveVersion((CompleteVersion) version);
        }

        LOGGER.info("Installed " + version);
    }

    public void uninstallVersion(CompleteVersion version) throws IOException {
        VersionList localVersionList = this.getLocalVersionList();
        if (localVersionList instanceof LocalVersionList) {
            localVersionList.uninstallVersion(version);
            LOGGER.info("Uninstalled " + version);
        }

    }

}
