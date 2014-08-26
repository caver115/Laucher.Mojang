package net.minecraft.launcher.updater;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.mojang.launcher.OperatingSystem;
import com.mojang.launcher.updater.VersionSyncInfo;
import com.mojang.launcher.updater.download.ChecksummedDownloadable;
import com.mojang.launcher.updater.download.Downloadable;
import com.mojang.launcher.versions.CompatibilityRule;
import com.mojang.launcher.versions.CompleteVersion;
import com.mojang.launcher.versions.ReleaseType;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CompleteMinecraftVersion implements CompleteVersion {

    private static final Logger LOGGER = LogManager.getLogger();
    private String inheritsFrom;
    private String id;
    private Date time;
    private Date releaseTime;
    private ReleaseType type;
    private String minecraftArguments;
    private List<Library> libraries;
    private String mainClass;
    private int minimumLauncherVersion;
    private String incompatibilityReason;
    private String assets;
    private List<CompatibilityRule> compatibilityRules;
    private String jar;
    private CompleteMinecraftVersion savableVersion;
    private transient boolean synced = false;

    public CompleteMinecraftVersion() {
    }

    public CompleteMinecraftVersion(CompleteMinecraftVersion version) {
        this.inheritsFrom = version.inheritsFrom;
        this.id = version.id;
        this.time = version.time;
        this.releaseTime = version.releaseTime;
        this.type = version.type;
        this.minecraftArguments = version.minecraftArguments;
        this.mainClass = version.mainClass;
        this.minimumLauncherVersion = version.minimumLauncherVersion;
        this.incompatibilityReason = version.incompatibilityReason;
        this.assets = version.assets;
        this.jar = version.jar;
        Iterator i$;
        if (version.libraries != null) {
            this.libraries = Lists.newArrayList();
            i$ = version.getLibraries().iterator();

            while (i$.hasNext()) {
                Library compatibilityRule = (Library) i$.next();
                this.libraries.add(new Library(compatibilityRule));
            }
        }

        if (version.compatibilityRules != null) {
            this.compatibilityRules = Lists.newArrayList();
            i$ = version.compatibilityRules.iterator();

            while (i$.hasNext()) {
                CompatibilityRule compatibilityRule1 = (CompatibilityRule) i$.next();
                this.compatibilityRules.add(new CompatibilityRule(compatibilityRule1));
            }
        }

    }

    public String getId() {
        return this.id;
    }

    public ReleaseType getType() {
        return this.type;
    }

    public Date getUpdatedTime() {
        return this.time;
    }

    public Date getReleaseTime() {
        return this.releaseTime;
    }

    public List<Library> getLibraries() {
        return this.libraries;
    }

    public String getMainClass() {
        return this.mainClass;
    }

    public String getJar() {
        return this.jar == null ? this.id : this.jar;
    }

    public void setType(ReleaseType type) {
        if (type == null) {
            throw new IllegalArgumentException("Release type cannot be null");
        } else {
            this.type = type;
        }
    }

    public Collection<Library> getRelevantLibraries() {
        ArrayList result = new ArrayList();
        Iterator i$ = this.libraries.iterator();

        while (i$.hasNext()) {
            Library library = (Library) i$.next();
            if (library.appliesToCurrentEnvironment()) {
                result.add(library);
            }
        }

        return result;
    }

    public Collection<File> getClassPath(OperatingSystem os, File base) {
        Collection libraries = this.getRelevantLibraries();
        ArrayList result = new ArrayList();
        Iterator i$ = libraries.iterator();

        while (i$.hasNext()) {
            Library library = (Library) i$.next();
            if (library.getNatives() == null) {
                result.add(new File(base, "libraries/" + library.getArtifactPath()));
            }
        }

        result.add(new File(base, "versions/" + this.getJar() + "/" + this.getJar() + ".jar"));
        return result;
    }

    public Set<String> getRequiredFiles(OperatingSystem os) {
        HashSet neededFiles = new HashSet();
        Iterator i$ = this.getRelevantLibraries().iterator();

        while (i$.hasNext()) {
            Library library = (Library) i$.next();
            if (library.getNatives() != null) {
                String natives = (String) library.getNatives().get(os);
                if (natives != null) {
                    neededFiles.add("libraries/" + library.getArtifactPath(natives));
                }
            } else {
                neededFiles.add("libraries/" + library.getArtifactPath());
            }
        }

        return neededFiles;
    }

    public Set<Downloadable> getRequiredDownloadables(OperatingSystem os, Proxy proxy, File targetDirectory, boolean ignoreLocalFiles) throws MalformedURLException {
        HashSet neededFiles = new HashSet();
        Iterator i$ = this.getRelevantLibraries().iterator();

        while (i$.hasNext()) {
            Library library = (Library) i$.next();
            String file = null;
            if (library.getNatives() != null) {
                String url = (String) library.getNatives().get(os);
                if (url != null) {
                    file = library.getArtifactPath(url);
                }
            } else {
                file = library.getArtifactPath();
            }

            if (file != null) {
                URL url1 = new URL(library.getDownloadUrl() + file);
                File local = new File(targetDirectory, "libraries/" + file);
                if (!local.isFile() || !library.hasCustomUrl()) {
                    neededFiles.add(new ChecksummedDownloadable(proxy, url1, local, ignoreLocalFiles));
                }
            }
        }

        return neededFiles;
    }

    public String toString() {
        return "CompleteVersion{id=\'" + this.id + '\'' + ", updatedTime=" + this.time + ", releasedTime=" + this.time + ", type=" + this.type + ", libraries=" + this.libraries + ", mainClass=\'" + this.mainClass + '\'' + ", jar=\'" + this.jar + '\'' + ", minimumLauncherVersion=" + this.minimumLauncherVersion + '}';
    }

    public String getMinecraftArguments() {
        return this.minecraftArguments;
    }

    public int getMinimumLauncherVersion() {
        return this.minimumLauncherVersion;
    }

    public boolean appliesToCurrentEnvironment() {
        if (this.compatibilityRules == null) {
            return true;
        } else {
            CompatibilityRule.Action lastAction = CompatibilityRule.Action.DISALLOW;
            Iterator i$ = this.compatibilityRules.iterator();

            while (i$.hasNext()) {
                CompatibilityRule compatibilityRule = (CompatibilityRule) i$.next();
                CompatibilityRule.Action action = compatibilityRule.getAppliedAction();
                if (action != null) {
                    lastAction = action;
                }
            }

            return lastAction == CompatibilityRule.Action.ALLOW;
        }
    }

    public String getIncompatibilityReason() {
        return this.incompatibilityReason;
    }

    public boolean isSynced() {
        return this.synced;
    }

    public void setSynced(boolean synced) {
        this.synced = synced;
    }

    public String getAssets() {
        return this.assets;
    }

    public String getInheritsFrom() {
        return this.inheritsFrom;
    }

    public CompleteMinecraftVersion resolve(MinecraftVersionManager versionManager) throws IOException {
        return this.resolve(versionManager, (Set) Sets.newHashSet());
    }

    protected CompleteMinecraftVersion resolve(MinecraftVersionManager versionManager, Set<String> resolvedSoFar) throws IOException {
        if (this.inheritsFrom == null) {
            return this;
        } else if (!resolvedSoFar.add(this.id)) {
            throw new IllegalStateException("Circular dependency detected");
        } else {
            VersionSyncInfo parentSync = versionManager.getVersionSyncInfo(this.inheritsFrom);
            CompleteMinecraftVersion parent = versionManager.getLatestCompleteVersion(parentSync).resolve(versionManager, resolvedSoFar);
            CompleteMinecraftVersion result = new CompleteMinecraftVersion(parent);
            if (!parentSync.isInstalled() || !parentSync.isUpToDate() || parentSync.getLatestSource() != VersionSyncInfo.VersionSource.LOCAL) {
                versionManager.installVersion(parent);
            }

            result.savableVersion = this;
            result.inheritsFrom = null;
            result.id = this.id;
            result.time = this.time;
            result.releaseTime = this.releaseTime;
            result.type = this.type;
            if (this.minecraftArguments != null) {
                result.minecraftArguments = this.minecraftArguments;
            }

            if (this.mainClass != null) {
                result.mainClass = this.mainClass;
            }

            if (this.incompatibilityReason != null) {
                result.incompatibilityReason = this.incompatibilityReason;
            }

            if (this.assets != null) {
                result.assets = this.assets;
            }

            if (this.jar != null) {
                result.jar = this.jar;
            }

            if (this.libraries != null) {
                ArrayList i$ = Lists.newArrayList();
                Iterator compatibilityRule = this.libraries.iterator();

                Library library;
                while (compatibilityRule.hasNext()) {
                    library = (Library) compatibilityRule.next();
                    i$.add(new Library(library));
                }

                compatibilityRule = result.libraries.iterator();

                while (compatibilityRule.hasNext()) {
                    library = (Library) compatibilityRule.next();
                    i$.add(library);
                }

                result.libraries = i$;
            }

            if (this.compatibilityRules != null) {
                Iterator i$1 = this.compatibilityRules.iterator();

                while (i$1.hasNext()) {
                    CompatibilityRule compatibilityRule1 = (CompatibilityRule) i$1.next();
                    result.compatibilityRules.add(new CompatibilityRule(compatibilityRule1));
                }
            }

            return result;
        }
    }

    public CompleteMinecraftVersion getSavableVersion() {
        return (CompleteMinecraftVersion) Objects.firstNonNull(this.savableVersion, this);
    }

}
