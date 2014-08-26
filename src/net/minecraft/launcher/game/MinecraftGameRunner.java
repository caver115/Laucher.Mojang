package net.minecraft.launcher.game;

import com.google.common.base.Objects;
import com.google.common.base.Predicate;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.authlib.UserAuthentication;
import com.mojang.authlib.UserType;
import com.mojang.authlib.properties.PropertyMap;
import com.mojang.authlib.yggdrasil.YggdrasilUserAuthentication;
import com.mojang.launcher.LegacyPropertyMapSerializer;
import com.mojang.launcher.OperatingSystem;
import com.mojang.launcher.game.GameInstanceStatus;
import com.mojang.launcher.game.process.GameProcess;
import com.mojang.launcher.game.process.GameProcessBuilder;
import com.mojang.launcher.game.process.GameProcessFactory;
import com.mojang.launcher.game.process.GameProcessRunnable;
import com.mojang.launcher.game.process.direct.DirectGameProcessFactory;
import com.mojang.launcher.game.runner.AbstractGameRunner;
import com.mojang.launcher.updater.DateTypeAdapter;
import com.mojang.launcher.updater.VersionSyncInfo;
import com.mojang.launcher.updater.download.Downloadable;
import com.mojang.launcher.updater.download.assets.AssetIndex;
import com.mojang.launcher.versions.ExtractRules;
import com.mojang.util.UUIDTypeAdapter;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeSet;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import net.minecraft.launcher.Launcher;
import net.minecraft.launcher.profile.LauncherVisibilityRule;
import net.minecraft.launcher.profile.Profile;
import net.minecraft.launcher.updater.CompleteMinecraftVersion;
import net.minecraft.launcher.updater.Library;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.StrSubstitutor;

public class MinecraftGameRunner extends AbstractGameRunner implements GameProcessRunnable {

    private static final String CRASH_IDENTIFIER_MAGIC = "#@!@#";
    private final Gson gson = new Gson();
    private final DateTypeAdapter dateAdapter = new DateTypeAdapter();
    private final Launcher minecraftLauncher;
    private final String[] additionalLaunchArgs;
    private final GameProcessFactory processFactory = new DirectGameProcessFactory();
    private File nativeDir;
    private LauncherVisibilityRule visibilityRule;
    private UserAuthentication auth;
    private Profile selectedProfile;

    public MinecraftGameRunner(Launcher minecraftLauncher, String[] additionalLaunchArgs) {
        this.visibilityRule = LauncherVisibilityRule.CLOSE_LAUNCHER;
        this.minecraftLauncher = minecraftLauncher;
        this.additionalLaunchArgs = additionalLaunchArgs;
    }

    protected void setStatus(GameInstanceStatus status) {
        Object var2 = this.lock;
        synchronized (this.lock) {
            if (this.nativeDir != null && status == GameInstanceStatus.IDLE) {
                LOGGER.info("Deleting " + this.nativeDir);
                if (this.nativeDir.isDirectory() && !FileUtils.deleteQuietly(this.nativeDir)) {
                    LOGGER.warn("Couldn\'t delete " + this.nativeDir + " - scheduling for deletion upon exit");

                    try {
                        FileUtils.forceDeleteOnExit(this.nativeDir);
                    } catch (Throwable var5) {
                        ;
                    }
                } else {
                    this.nativeDir = null;
                }
            }

            super.setStatus(status);
        }
    }

    protected com.mojang.launcher.Launcher getLauncher() {
        return this.minecraftLauncher.getLauncher();
    }

    protected void downloadRequiredFiles(VersionSyncInfo syncInfo) {
        this.migrateOldAssets();
        super.downloadRequiredFiles(syncInfo);
    }

    protected void launchGame() throws IOException {
        LOGGER.info("Launching game");
        this.selectedProfile = this.minecraftLauncher.getProfileManager().getSelectedProfile();
        this.auth = this.minecraftLauncher.getProfileManager().getAuthDatabase().getByUUID(this.minecraftLauncher.getProfileManager().getSelectedUser());
        if (this.getVersion() == null) {
            LOGGER.error("Aborting launch; version is null?");
        } else {
            this.nativeDir = new File(this.getLauncher().getWorkingDirectory(), "versions/" + this.getVersion().getId() + "/" + this.getVersion().getId() + "-natives-" + System.nanoTime());
            if (!this.nativeDir.isDirectory()) {
                this.nativeDir.mkdirs();
            }

            LOGGER.info("Unpacking natives to " + this.nativeDir);

            try {
                this.unpackNatives(this.nativeDir);
            } catch (IOException var11) {
                LOGGER.error("Couldn\'t unpack natives!", (Throwable) var11);
                return;
            }

            File assetsDir;
            try {
                assetsDir = this.reconstructAssets();
            } catch (IOException var10) {
                LOGGER.error("Couldn\'t unpack natives!", (Throwable) var10);
                return;
            }

            File gameDirectory = this.selectedProfile.getGameDir() == null ? this.getLauncher().getWorkingDirectory() : this.selectedProfile.getGameDir();
            LOGGER.info("Launching in " + gameDirectory);
            if (!gameDirectory.exists()) {
                if (!gameDirectory.mkdirs()) {
                    LOGGER.error("Aborting launch; couldn\'t create game directory");
                    return;
                }
            } else if (!gameDirectory.isDirectory()) {
                LOGGER.error("Aborting launch; game directory is not actually a directory");
                return;
            }

            GameProcessBuilder processBuilder = new GameProcessBuilder((String) Objects.firstNonNull(this.selectedProfile.getJavaPath(), OperatingSystem.getCurrentPlatform().getJavaDir()));
            processBuilder.withSysOutFilter(new Predicate() {
                public boolean apply(String input) {
                    return input.contains("#@!@#");
                }
// $FF: synthetic method
// $FF: bridge method

                public boolean apply(Object x0) {
                    return this.apply((String) x0);
                }
            });
            processBuilder.directory(gameDirectory);
            processBuilder.withLogProcessor(this.minecraftLauncher.getUserInterface().showGameOutputTab(this));
            OperatingSystem os = OperatingSystem.getCurrentPlatform();
            if (os.equals(OperatingSystem.OSX)) {
                processBuilder.withArguments(new String[]{"-Xdock:icon=" + this.getAssetObject("icons/minecraft.icns").getAbsolutePath(), "-Xdock:name=Minecraft"});
            } else if (os.equals(OperatingSystem.WINDOWS)) {
                processBuilder.withArguments(new String[]{"-XX:HeapDumpPath=MojangTricksIntelDriversForPerformance_javaw.exe_minecraft.exe.heapdump"});
            }

            String profileArgs = this.selectedProfile.getJavaArgs();
            if (profileArgs != null) {
                processBuilder.withArguments(profileArgs.split(" "));
            } else {
                boolean args = "32".equals(System.getProperty("sun.arch.data.model"));
                String proxy = args ? "-Xmx512M -XX:+UseConcMarkSweepGC -XX:+CMSIncrementalMode -XX:-UseAdaptiveSizePolicy -Xmn128M" : "-Xmx1G -XX:+UseConcMarkSweepGC -XX:+CMSIncrementalMode -XX:-UseAdaptiveSizePolicy -Xmn128M";
                processBuilder.withArguments(proxy.split(" "));
            }

            processBuilder.withArguments(new String[]{"-Djava.library.path=" + this.nativeDir.getAbsolutePath()});
            processBuilder.withArguments(new String[]{"-cp", this.constructClassPath(this.getVersion())});
            processBuilder.withArguments(new String[]{this.getVersion().getMainClass()});
            LOGGER.info("Half command: " + StringUtils.join((Iterable) processBuilder.getFullCommands(), " "));
            String[] args1 = this.getMinecraftArguments(this.getVersion(), this.selectedProfile, gameDirectory, assetsDir, this.auth);
            if (args1 != null) {
                processBuilder.withArguments(args1);
                Proxy proxy1 = this.getLauncher().getProxy();
                PasswordAuthentication proxyAuth = this.getLauncher().getProxyAuth();
                if (!proxy1.equals(Proxy.NO_PROXY)) {
                    InetSocketAddress e = (InetSocketAddress) proxy1.address();
                    processBuilder.withArguments(new String[]{"--proxyHost", e.getHostName()});
                    processBuilder.withArguments(new String[]{"--proxyPort", Integer.toString(e.getPort())});
                    if (proxyAuth != null) {
                        processBuilder.withArguments(new String[]{"--proxyUser", proxyAuth.getUserName()});
                        processBuilder.withArguments(new String[]{"--proxyPass", new String(proxyAuth.getPassword())});
                    }
                }

                processBuilder.withArguments(this.additionalLaunchArgs);
                if (this.auth == null || this.auth.getSelectedProfile() == null) {
                    processBuilder.withArguments(new String[]{"--demo"});
                }

                if (this.selectedProfile.getResolution() != null) {
                    processBuilder.withArguments(new String[]{"--width", String.valueOf(this.selectedProfile.getResolution().getWidth())});
                    processBuilder.withArguments(new String[]{"--height", String.valueOf(this.selectedProfile.getResolution().getHeight())});
                }

                try {
                    LOGGER.debug("Running " + StringUtils.join((Iterable) processBuilder.getFullCommands(), " "));
                    GameProcess e1 = this.processFactory.startGame(processBuilder);
                    e1.setExitRunnable(this);
                    this.setStatus(GameInstanceStatus.PLAYING);
                    if (this.visibilityRule != LauncherVisibilityRule.DO_NOTHING) {
                        this.minecraftLauncher.getUserInterface().setVisible(false);
                    }
                } catch (IOException var12) {
                    LOGGER.error("Couldn\'t launch game", (Throwable) var12);
                    this.setStatus(GameInstanceStatus.IDLE);
                    return;
                }

                this.minecraftLauncher.performCleanups();
            }
        }
    }

    protected CompleteMinecraftVersion getVersion() {
        return (CompleteMinecraftVersion) this.version;
    }

    private File getAssetObject(String name) throws IOException {
        File assetsDir = new File(this.getLauncher().getWorkingDirectory(), "assets");
        File indexDir = new File(assetsDir, "indexes");
        File objectsDir = new File(assetsDir, "objects");
        String assetVersion = this.getVersion().getAssets() == null ? "legacy" : this.getVersion().getAssets();
        File indexFile = new File(indexDir, assetVersion + ".json");
        AssetIndex index = (AssetIndex) this.gson.fromJson(FileUtils.readFileToString(indexFile, Charsets.UTF_8), AssetIndex.class);
        String hash = ((AssetIndex.AssetObject) index.getFileMap().get(name)).getHash();
        return new File(objectsDir, hash.substring(0, 2) + "/" + hash);
    }

    private File reconstructAssets() throws IOException {
        File assetsDir = new File(this.getLauncher().getWorkingDirectory(), "assets");
        File indexDir = new File(assetsDir, "indexes");
        File objectDir = new File(assetsDir, "objects");
        String assetVersion = this.getVersion().getAssets() == null ? "legacy" : this.getVersion().getAssets();
        File indexFile = new File(indexDir, assetVersion + ".json");
        File virtualRoot = new File(new File(assetsDir, "virtual"), assetVersion);
        if (!indexFile.isFile()) {
            LOGGER.warn("No assets index file " + virtualRoot + "; can\'t reconstruct assets");
            return virtualRoot;
        } else {
            AssetIndex index = (AssetIndex) this.gson.fromJson(FileUtils.readFileToString(indexFile, Charsets.UTF_8), AssetIndex.class);
            if (index.isVirtual()) {
                LOGGER.info("Reconstructing virtual assets folder at " + virtualRoot);
                Iterator i$ = index.getFileMap().entrySet().iterator();

                while (i$.hasNext()) {
                    Entry entry = (Entry) i$.next();
                    File target = new File(virtualRoot, (String) entry.getKey());
                    File original = new File(new File(objectDir, ((AssetIndex.AssetObject) entry.getValue()).getHash().substring(0, 2)), ((AssetIndex.AssetObject) entry.getValue()).getHash());
                    if (!target.isFile()) {
                        FileUtils.copyFile(original, target, false);
                    }
                }

                FileUtils.writeStringToFile(new File(virtualRoot, ".lastused"), this.dateAdapter.serializeToString(new Date()));
            }

            return virtualRoot;
        }
    }

    private String[] getMinecraftArguments(CompleteMinecraftVersion version, Profile selectedProfile, File gameDirectory, File assetsDirectory, UserAuthentication authentication) {
        if (version.getMinecraftArguments() == null) {
            LOGGER.error("Can\'t run version, missing minecraftArguments");
            this.setStatus(GameInstanceStatus.IDLE);
            return null;
        } else {
            HashMap map = new HashMap();
            StrSubstitutor substitutor = new StrSubstitutor(map);
            String[] split = version.getMinecraftArguments().split(" ");
            map.put("auth_access_token", authentication.getAuthenticatedToken());
            map.put("user_properties", (new GsonBuilder()).registerTypeAdapter(PropertyMap.class, new LegacyPropertyMapSerializer()).create().toJson((Object) authentication.getUserProperties()));
            map.put("user_property_map", (new GsonBuilder()).registerTypeAdapter(PropertyMap.class, new PropertyMap.Serializer()).create().toJson((Object) authentication.getUserProperties()));
            if (authentication.isLoggedIn() && authentication.canPlayOnline()) {
                if (authentication instanceof YggdrasilUserAuthentication) {
                    map.put("auth_session", String.format("token:%s:%s", new Object[]{authentication.getAuthenticatedToken(), UUIDTypeAdapter.fromUUID(authentication.getSelectedProfile().getId())}));
                } else {
                    map.put("auth_session", authentication.getAuthenticatedToken());
                }
            } else {
                map.put("auth_session", "-");
            }

            if (authentication.getSelectedProfile() != null) {
                map.put("auth_player_name", authentication.getSelectedProfile().getName());
                map.put("auth_uuid", UUIDTypeAdapter.fromUUID(authentication.getSelectedProfile().getId()));
                map.put("user_type", authentication.getUserType().getName());
            } else {
                map.put("auth_player_name", "Player");
                map.put("auth_uuid", (new UUID(0L, 0L)).toString());
                map.put("user_type", UserType.LEGACY.getName());
            }

            map.put("profile_name", selectedProfile.getName());
            map.put("version_name", version.getId());
            map.put("game_directory", gameDirectory.getAbsolutePath());
            map.put("game_assets", assetsDirectory.getAbsolutePath());
            map.put("assets_root", (new File(this.getLauncher().getWorkingDirectory(), "assets")).getAbsolutePath());
            map.put("assets_index_name", version.getAssets() == null ? "legacy" : version.getAssets());

            for (int i = 0; i < split.length; ++i) {
                split[i] = substitutor.replace(split[i]);
            }

            return split;
        }
    }

    private void migrateOldAssets() {
        File sourceDir = new File(this.getLauncher().getWorkingDirectory(), "assets");
        File objectsDir = new File(sourceDir, "objects");
        if (sourceDir.isDirectory()) {
            IOFileFilter migratableFilter = FileFilterUtils.notFileFilter(FileFilterUtils.or(new IOFileFilter[]{FileFilterUtils.nameFileFilter("indexes"), FileFilterUtils.nameFileFilter("objects"), FileFilterUtils.nameFileFilter("virtual"), FileFilterUtils.nameFileFilter("skins")}));

            File arr$;
            for (Iterator assets = (new TreeSet(FileUtils.listFiles(sourceDir, TrueFileFilter.TRUE, migratableFilter))).iterator(); assets.hasNext(); FileUtils.deleteQuietly(arr$)) {
                arr$ = (File) assets.next();
                String len$ = Downloadable.getDigest(arr$, "SHA-1", 40);
                File i$ = new File(objectsDir, len$.substring(0, 2) + "/" + len$);
                if (!i$.exists()) {
                    LOGGER.info("Migrated old asset {} into {}", new Object[]{arr$, i$});

                    try {
                        FileUtils.copyFile(arr$, i$);
                    } catch (IOException var9) {
                        LOGGER.error("Couldn\'t migrate old asset", (Throwable) var9);
                    }
                }
            }

            File[] var10 = sourceDir.listFiles();
            if (var10 != null) {
                File[] var11 = var10;
                int var12 = var10.length;

                for (int var13 = 0; var13 < var12; ++var13) {
                    File file = var11[var13];
                    if (!file.getName().equals("indexes") && !file.getName().equals("objects") && !file.getName().equals("virtual") && !file.getName().equals("skins")) {
                        LOGGER.info("Cleaning up old assets directory {} after migration", new Object[]{file});
                        FileUtils.deleteQuietly(file);
                    }
                }
            }

        }
    }

    private void unpackNatives(File targetDir) throws IOException {
        OperatingSystem os = OperatingSystem.getCurrentPlatform();
        Collection libraries = this.getVersion().getRelevantLibraries();
        Iterator i$ = libraries.iterator();

        while (i$.hasNext()) {
            Library library = (Library) i$.next();
            Map nativesPerOs = library.getNatives();
            if (nativesPerOs != null && nativesPerOs.get(os) != null) {
                File file = new File(this.getLauncher().getWorkingDirectory(), "libraries/" + library.getArtifactPath((String) nativesPerOs.get(os)));
                ZipFile zip = new ZipFile(file);
                ExtractRules extractRules = library.getExtractRules();

                try {
                    Enumeration entries = zip.entries();

                    while (entries.hasMoreElements()) {
                        ZipEntry entry = (ZipEntry) entries.nextElement();
                        if (extractRules == null || extractRules.shouldExtract(entry.getName())) {
                            File targetFile = new File(targetDir, entry.getName());
                            if (targetFile.getParentFile() != null) {
                                targetFile.getParentFile().mkdirs();
                            }

                            if (!entry.isDirectory()) {
                                BufferedInputStream inputStream = new BufferedInputStream(zip.getInputStream(entry));
                                byte[] buffer = new byte[2048];
                                FileOutputStream outputStream = new FileOutputStream(targetFile);
                                BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream);

                                int length;
                                try {
                                    while ((length = inputStream.read(buffer, 0, buffer.length)) != -1) {
                                        bufferedOutputStream.write(buffer, 0, length);
                                    }
                                } finally {
                                    Downloadable.closeSilently(bufferedOutputStream);
                                    Downloadable.closeSilently(outputStream);
                                    Downloadable.closeSilently(inputStream);
                                }
                            }
                        }
                    }
                } finally {
                    zip.close();
                }
            }
        }

    }

    private String constructClassPath(CompleteMinecraftVersion version) {
        StringBuilder result = new StringBuilder();
        Collection classPath = version.getClassPath(OperatingSystem.getCurrentPlatform(), this.getLauncher().getWorkingDirectory());
        String separator = System.getProperty("path.separator");

        File file;
        for (Iterator i$ = classPath.iterator(); i$.hasNext(); result.append(file.getAbsolutePath())) {
            file = (File) i$.next();
            if (!file.isFile()) {
                throw new RuntimeException("Classpath file not found: " + file);
            }

            if (result.length() > 0) {
                result.append(separator);
            }
        }

        return result.toString();
    }

    public void onGameProcessEnded(GameProcess process) {
        int exitCode = process.getExitCode();
        if (exitCode == 0) {
            LOGGER.info("Game ended with no troubles detected (exit code " + exitCode + ")");
            if (this.visibilityRule == LauncherVisibilityRule.CLOSE_LAUNCHER) {
                LOGGER.info("Following visibility rule and exiting launcher as the game has ended");
                this.getLauncher().shutdownLauncher();
            } else if (this.visibilityRule == LauncherVisibilityRule.HIDE_LAUNCHER) {
                LOGGER.info("Following visibility rule and showing launcher as the game has ended");
                this.minecraftLauncher.getUserInterface().setVisible(true);
            }
        } else {
            LOGGER.error("Game ended with bad state (exit code " + exitCode + ")");
            LOGGER.info("Ignoring visibility rule and showing launcher due to a game crash");
            this.minecraftLauncher.getUserInterface().setVisible(true);
            String errorText = null;
            Collection sysOutLines = process.getSysOutLines();
            String[] sysOut = (String[]) sysOutLines.toArray(new String[sysOutLines.size()]);

            for (int file = sysOut.length - 1; file >= 0; --file) {
                String inputStream = sysOut[file];
                int e = inputStream.lastIndexOf("#@!@#");
                if (e >= 0 && e < inputStream.length() - "#@!@#".length() - 1) {
                    errorText = inputStream.substring(e + "#@!@#".length()).trim();
                    break;
                }
            }

            if (errorText != null) {
                File var16 = new File(errorText);
                if (var16.isFile()) {
                    LOGGER.info("Crash report detected, opening: " + errorText);
                    FileInputStream var17 = null;

                    try {
                        var17 = new FileInputStream(var16);
                        BufferedReader var18 = new BufferedReader(new InputStreamReader(var17));

                        StringBuilder result;
                        String line;
                        for (result = new StringBuilder(); (line = var18.readLine()) != null; result.append(line)) {
                            if (result.length() > 0) {
                                result.append("\n");
                            }
                        }

                        var18.close();
                        this.minecraftLauncher.getUserInterface().showCrashReport(this.getVersion(), var16, result.toString());
                    } catch (IOException var14) {
                        LOGGER.error("Couldn\'t open crash report", (Throwable) var14);
                    } finally {
                        Downloadable.closeSilently(var17);
                    }
                } else {
                    LOGGER.error("Crash report detected, but unknown format: " + errorText);
                }
            }
        }

        this.setStatus(GameInstanceStatus.IDLE);
    }

    public void setVisibility(LauncherVisibilityRule visibility) {
        this.visibilityRule = visibility;
    }

    public UserAuthentication getAuth() {
        return this.auth;
    }

    public Profile getSelectedProfile() {
        return this.selectedProfile;
    }
}
