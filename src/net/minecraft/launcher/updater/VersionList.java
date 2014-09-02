package net.minecraft.launcher.updater;

import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.launcher.OperatingSystem;
import com.mojang.launcher.updater.DateTypeAdapter;
import com.mojang.launcher.updater.LowerCaseEnumTypeAdapterFactory;
import com.mojang.launcher.versions.CompleteVersion;
import com.mojang.launcher.versions.ReleaseType;
import com.mojang.launcher.versions.ReleaseTypeAdapterFactory;
import com.mojang.launcher.versions.Version;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.minecraft.launcher.game.MinecraftReleaseType;
import net.minecraft.launcher.game.MinecraftReleaseTypeFactory;

public abstract class VersionList {

    protected final Gson gson;
    private final Map<String, Version> versionsByName = new HashMap();
    private final List<Version> versions = new ArrayList();
    private final Map<MinecraftReleaseType, Version> latestVersions = Maps.newEnumMap(MinecraftReleaseType.class);

    public VersionList() {
        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapterFactory(new LowerCaseEnumTypeAdapterFactory());
        builder.registerTypeAdapter(Date.class, new DateTypeAdapter());
        builder.registerTypeAdapter(ReleaseType.class, new ReleaseTypeAdapterFactory(MinecraftReleaseTypeFactory.instance()));
        builder.enableComplexMapKeySerialization();
        builder.setPrettyPrinting();
        this.gson = builder.create();
    }

    public Collection<Version> getVersions() {
        return this.versions;
    }

    public Version getLatestVersion(MinecraftReleaseType type) {
        if (type == null) {
            throw new IllegalArgumentException("Type cannot be null");
        } else {
            return (Version) this.latestVersions.get(type);
        }
    }

    public Version getVersion(String name) {
        if (name != null && name.length() != 0) {
            return (Version) this.versionsByName.get(name);
        } else {
            throw new IllegalArgumentException("Name cannot be null or empty");
        }
    }

    public CompleteMinecraftVersion getCompleteVersion(String name) throws IOException {
        if (name != null && name.length() != 0) {
            Version version = this.getVersion(name);
            if (version == null) {
                throw new IllegalArgumentException("Unknown version - cannot get complete version of null");
            } else {
                return this.getCompleteVersion(version);
            }
        } else {
            throw new IllegalArgumentException("Name cannot be null or empty");
        }
    }

    public CompleteMinecraftVersion getCompleteVersion(Version version) throws IOException {
        if (version instanceof CompleteVersion) {
            return (CompleteMinecraftVersion) version;
        } else if (version == null) {
            throw new IllegalArgumentException("Version cannot be null");
        } else {
            CompleteMinecraftVersion complete = (CompleteMinecraftVersion) this.gson.fromJson(this.getContent("versions/" + version.getId() + "/" + version.getId() + ".json"), CompleteMinecraftVersion.class);
            MinecraftReleaseType type = (MinecraftReleaseType) version.getType();
            Collections.replaceAll(this.versions, version, complete);
            this.versionsByName.put(version.getId(), complete);
            if (this.latestVersions.get(type) == version) {
                this.latestVersions.put(type, complete);
            }

            return complete;
        }
    }

    protected void clearCache() {
        this.versionsByName.clear();
        this.versions.clear();
        this.latestVersions.clear();
    }

    public void refreshVersions() throws IOException {
        this.clearCache();
        VersionList.RawVersionList versionList = (VersionList.RawVersionList) this.gson.fromJson(this.getContent("versions/versions.json"), VersionList.RawVersionList.class);
        Iterator arr$ = versionList.getVersions().iterator();

        while (arr$.hasNext()) {
            Version len$ = (Version) arr$.next();
            this.versions.add(len$);
            this.versionsByName.put(len$.getId(), len$);
        }

        MinecraftReleaseType[] var6 = MinecraftReleaseType.values();
        int var7 = var6.length;

        for (int i$ = 0; i$ < var7; ++i$) {
            MinecraftReleaseType type = var6[i$];
            this.latestVersions.put(type, this.versionsByName.get(versionList.getLatestVersions().get(type)));
        }

    }

    public CompleteVersion addVersion(CompleteVersion version) {
        if (version.getId() == null) {
            throw new IllegalArgumentException("Cannot add blank version");
        } else if (this.getVersion(version.getId()) != null) {
            throw new IllegalArgumentException("Version \'" + version.getId() + "\' is already tracked");
        } else {
            this.versions.add(version);
            this.versionsByName.put(version.getId(), version);
            return version;
        }
    }

    public void removeVersion(String name) {
        if (name != null && name.length() != 0) {
            Version version = this.getVersion(name);
            if (version == null) {
                throw new IllegalArgumentException("Unknown version - cannot remove null");
            } else {
                this.removeVersion(version);
            }
        } else {
            throw new IllegalArgumentException("Name cannot be null or empty");
        }
    }

    public void removeVersion(Version version) {
        if (version == null) {
            throw new IllegalArgumentException("Cannot remove null version");
        } else {
            this.versions.remove(version);
            this.versionsByName.remove(version.getId());
            MinecraftReleaseType[] arr$ = MinecraftReleaseType.values();
            int len$ = arr$.length;

            for (int i$ = 0; i$ < len$; ++i$) {
                MinecraftReleaseType type = arr$[i$];
                if (this.getLatestVersion(type) == version) {
                    this.latestVersions.remove(type);
                }
            }

        }
    }

    public void setLatestVersion(Version version) {
        if (version == null) {
            throw new IllegalArgumentException("Cannot set latest version to null");
        } else {
            this.latestVersions.put((MinecraftReleaseType) version.getType(), version);
        }
    }

    public void setLatestVersion(String name) {
        if (name != null && name.length() != 0) {
            Version version = this.getVersion(name);
            if (version == null) {
                throw new IllegalArgumentException("Unknown version - cannot set latest version to null");
            } else {
                this.setLatestVersion(version);
            }
        } else {
            throw new IllegalArgumentException("Name cannot be null or empty");
        }
    }

    public String serializeVersionList() {
        VersionList.RawVersionList list = new VersionList.RawVersionList((VersionList.NamelessClass425749475) null);
        MinecraftReleaseType[] i$ = MinecraftReleaseType.values();
        int version = i$.length;

        for (int partial = 0; partial < version; ++partial) {
            MinecraftReleaseType type = i$[partial];
            Version latest = this.getLatestVersion(type);
            if (latest != null) {
                list.getLatestVersions().put(type, latest.getId());
            }
        }

        PartialVersion var9;
        for (Iterator var7 = this.getVersions().iterator(); var7.hasNext(); list.getVersions().add(var9)) {
            Version var8 = (Version) var7.next();
            var9 = null;
            if (var8 instanceof PartialVersion) {
                var9 = (PartialVersion) var8;
            } else {
                var9 = new PartialVersion(var8);
            }
        }

        return this.gson.toJson((Object) list);
    }

    public String serializeVersion(CompleteVersion version) {
        if (version == null) {
            throw new IllegalArgumentException("Cannot serialize null!");
        } else {
            return this.gson.toJson((Object) version);
        }
    }

    public abstract boolean hasAllFiles(CompleteMinecraftVersion var1, OperatingSystem var2);

    public abstract String getContent(String var1) throws IOException;

    public abstract URL getUrl(String var1) throws MalformedURLException;

    public void uninstallVersion(Version version) {
        this.removeVersion(version);
    }

    // $FF: synthetic class
    static class NamelessClass425749475 {
    }

    private static class RawVersionList {

        private List<PartialVersion> versions;
        private Map<MinecraftReleaseType, String> latest;

        private RawVersionList() {
            this.versions = new ArrayList();
            this.latest = Maps.newEnumMap(MinecraftReleaseType.class);
        }

        public List<PartialVersion> getVersions() {
            return this.versions;
        }

        public Map<MinecraftReleaseType, String> getLatestVersions() {
            return this.latest;
        }

        // $FF: synthetic method
        RawVersionList(VersionList.NamelessClass425749475 x0) {
            this();
        }
    }
}
