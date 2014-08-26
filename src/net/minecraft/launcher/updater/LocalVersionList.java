package net.minecraft.launcher.updater;

import com.mojang.launcher.OperatingSystem;
import com.mojang.launcher.versions.CompleteVersion;
import com.mojang.launcher.versions.Version;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.Set;
import net.minecraft.launcher.game.MinecraftReleaseType;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LocalVersionList extends FileBasedVersionList {

    private static final Logger LOGGER = LogManager.getLogger();
    private final File baseDirectory;
    private final File baseVersionsDir;

    public LocalVersionList(File baseDirectory) {
        if (baseDirectory != null && baseDirectory.isDirectory()) {
            this.baseDirectory = baseDirectory;
            this.baseVersionsDir = new File(this.baseDirectory, "versions");
            if (!this.baseVersionsDir.isDirectory()) {
                this.baseVersionsDir.mkdirs();
            }

        } else {
            throw new IllegalArgumentException("Base directory is not a folder!");
        }
    }

    protected InputStream getFileInputStream(String path) throws FileNotFoundException {
        return new FileInputStream(new File(this.baseDirectory, path));
    }

    public void refreshVersions() throws IOException {
        this.clearCache();
        File[] files = this.baseVersionsDir.listFiles();
        if (files != null) {
            File[] i$ = files;
            int version = files.length;

            for (int type = 0; type < version; ++type) {
                File directory = i$[type];
                String id = directory.getName();
                File jsonFile = new File(directory, id + ".json");
                if (directory.isDirectory() && jsonFile.exists()) {
                    try {
                        String ex = "versions/" + id + "/" + id + ".json";
                        CompleteVersion version1 = (CompleteVersion) this.gson.fromJson(this.getContent(ex), CompleteMinecraftVersion.class);
                        if (version1.getType() == null) {
                            LOGGER.warn("Ignoring: " + ex + "; it has an invalid version specified");
                            return;
                        }

                        if (version1.getId().equals(id)) {
                            this.addVersion(version1);
                        } else {
                            LOGGER.warn("Ignoring: " + ex + "; it contains id: \'" + version1.getId() + "\' expected \'" + id + "\'");
                        }
                    } catch (RuntimeException var10) {
                        LOGGER.error("Couldn\'t load local version " + jsonFile.getAbsolutePath(), (Throwable) var10);
                    }
                }
            }

            Iterator var11 = this.getVersions().iterator();

            while (var11.hasNext()) {
                Version var12 = (Version) var11.next();
                MinecraftReleaseType var13 = (MinecraftReleaseType) var12.getType();
                if (this.getLatestVersion(var13) == null || this.getLatestVersion(var13).getUpdatedTime().before(var12.getUpdatedTime())) {
                    this.setLatestVersion(var12);
                }
            }

        }
    }

    public void saveVersionList() throws IOException {
        String text = this.serializeVersionList();
        PrintWriter writer = new PrintWriter(new File(this.baseVersionsDir, "versions.json"));
        writer.print(text);
        writer.close();
    }

    public void saveVersion(CompleteVersion version) throws IOException {
        String text = this.serializeVersion(version);
        File target = new File(this.baseVersionsDir, version.getId() + "/" + version.getId() + ".json");
        if (target.getParentFile() != null) {
            target.getParentFile().mkdirs();
        }

        PrintWriter writer = new PrintWriter(target);
        writer.print(text);
        writer.close();
    }

    public File getBaseDirectory() {
        return this.baseDirectory;
    }

    public boolean hasAllFiles(CompleteMinecraftVersion version, OperatingSystem os) {
        Set files = version.getRequiredFiles(os);
        Iterator i$ = files.iterator();

        String file;
        do {
            if (!i$.hasNext()) {
                return true;
            }

            file = (String) i$.next();
        } while ((new File(this.baseDirectory, file)).isFile());

        return false;
    }

    public void uninstallVersion(Version version) {
        super.uninstallVersion(version);
        File dir = new File(this.baseVersionsDir, version.getId());
        if (dir.isDirectory()) {
            FileUtils.deleteQuietly(dir);
        }

    }

}
