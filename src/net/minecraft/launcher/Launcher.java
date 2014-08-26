package net.minecraft.launcher;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.mojang.authlib.Agent;
import com.mojang.authlib.UserAuthentication;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.exceptions.InvalidCredentialsException;
import com.mojang.launcher.OperatingSystem;
import com.mojang.launcher.updater.DateTypeAdapter;
import com.mojang.launcher.updater.VersionSyncInfo;
import com.mojang.launcher.updater.download.assets.AssetIndex;
import com.mojang.launcher.versions.CompleteVersion;
import com.mojang.launcher.versions.Version;
import com.mojang.util.UUIDTypeAdapter;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.lang.Thread.UncaughtExceptionHandler;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import javax.swing.JFrame;
import joptsimple.ArgumentAcceptingOptionSpec;
import joptsimple.NonOptionArgumentSpec;
import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import net.minecraft.launcher.game.GameLaunchDispatcher;
import net.minecraft.launcher.game.MinecraftReleaseType;
import net.minecraft.launcher.game.MinecraftReleaseTypeFactory;
import net.minecraft.launcher.profile.AuthenticationDatabase;
import net.minecraft.launcher.profile.Profile;
import net.minecraft.launcher.profile.ProfileManager;
import net.minecraft.launcher.updater.CompleteMinecraftVersion;
import net.minecraft.launcher.updater.Library;
import net.minecraft.launcher.updater.LocalVersionList;
import net.minecraft.launcher.updater.MinecraftVersionManager;
import net.minecraft.launcher.updater.RemoteVersionList;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.AgeFileFilter;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.FileFileFilter;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.PrefixFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Launcher {

    private static final Logger LOGGER;
    private final com.mojang.launcher.Launcher launcher;
    private final Integer bootstrapVersion;
    private final MinecraftUserInterface userInterface;
    private final ProfileManager profileManager;
    private final Gson gson;
    private final GameLaunchDispatcher launchDispatcher;
    private UUID clientToken;
    private String requestedUser;

    public Launcher(JFrame frame, File workingDirectory, Proxy proxy, PasswordAuthentication proxyAuth, String[] args) {
        this(frame, workingDirectory, proxy, proxyAuth, args, Integer.valueOf(0));
    }

    public Launcher(JFrame frame, File workingDirectory, Proxy proxy, PasswordAuthentication proxyAuth, String[] args, Integer bootstrapVersion) {
        this.gson = new Gson();
        this.clientToken = UUID.randomUUID();
        this.setupErrorHandling();
        this.bootstrapVersion = bootstrapVersion;
        this.userInterface = this.selectUserInterface(frame);
        if (bootstrapVersion.intValue() < 4) {
            this.userInterface.showOutdatedNotice();
            throw new Error("Outdated bootstrap");
        } else {
            LOGGER.info(this.userInterface.getTitle() + " (through bootstrap " + bootstrapVersion + ") started on " + OperatingSystem.getCurrentPlatform().getName() + "...");
            LOGGER.info("Current time is " + DateFormat.getDateTimeInstance(2, 2, Locale.US).format(new Date()));
            if (!OperatingSystem.getCurrentPlatform().isSupported()) {
                LOGGER.fatal("This operating system is unknown or unsupported, we cannot guarantee that the game will launch successfully.");
            }

            LOGGER.info("System.getProperty(\'os.name\') == \'" + System.getProperty("os.name") + "\'");
            LOGGER.info("System.getProperty(\'os.version\') == \'" + System.getProperty("os.version") + "\'");
            LOGGER.info("System.getProperty(\'os.arch\') == \'" + System.getProperty("os.arch") + "\'");
            LOGGER.info("System.getProperty(\'java.version\') == \'" + System.getProperty("java.version") + "\'");
            LOGGER.info("System.getProperty(\'java.vendor\') == \'" + System.getProperty("java.vendor") + "\'");
            LOGGER.info("System.getProperty(\'sun.arch.data.model\') == \'" + System.getProperty("sun.arch.data.model") + "\'");
            this.launchDispatcher = new GameLaunchDispatcher(this, this.processArgs(args));
            this.launcher = new com.mojang.launcher.Launcher(this.userInterface, workingDirectory, proxy, proxyAuth, new MinecraftVersionManager(new LocalVersionList(workingDirectory), new RemoteVersionList("https://s3.amazonaws.com/Minecraft.Download/", proxy)), Agent.MINECRAFT, MinecraftReleaseTypeFactory.instance(), 17);
            this.profileManager = new ProfileManager(this);
            ((SwingUserInterface) this.userInterface).initializeFrame();
            this.refreshVersionsAndProfiles();
        }
    }

    private void setupErrorHandling() {
        Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler() {
            public void uncaughtException(Thread t, Throwable e) {
                Launcher.LOGGER.fatal("Unhandled exception in thread " + t, e);
            }
        });
    }

    private String[] processArgs(String[] args) {
        OptionParser optionParser = new OptionParser();
        optionParser.allowsUnrecognizedOptions();
        ArgumentAcceptingOptionSpec userOption = optionParser.accepts("user").withRequiredArg().ofType(String.class);
        NonOptionArgumentSpec nonOptions = optionParser.nonOptions();

        OptionSet optionSet;
        try {
            optionSet = optionParser.parse(args);
        } catch (OptionException var7) {
            return args;
        }

        if (optionSet.has((OptionSpec) userOption)) {
            this.requestedUser = (String) optionSet.valueOf((OptionSpec) userOption);
        }

        List remainingOptions = optionSet.valuesOf((OptionSpec) nonOptions);
        return (String[]) remainingOptions.toArray(new String[remainingOptions.size()]);
    }

    public void refreshVersionsAndProfiles() {
        this.getLauncher().getVersionManager().getExecutorService().submit(new Runnable() {
            public void run() {
                try {
                    Launcher.this.getLauncher().getVersionManager().refreshVersions();
                } catch (Throwable var7) {
                    Launcher.LOGGER.error("Unexpected exception refreshing version list", var7);
                }

                try {
                    Launcher.this.profileManager.loadProfiles();
                    Launcher.LOGGER.info("Loaded " + Launcher.this.profileManager.getProfiles().size() + " profile(s); selected \'" + Launcher.this.profileManager.getSelectedProfile().getName() + "\'");
                } catch (Throwable var6) {
                    Launcher.LOGGER.error("Unexpected exception refreshing profile list", var6);
                }

                if (Launcher.this.requestedUser != null) {
                    AuthenticationDatabase authDatabase = Launcher.this.profileManager.getAuthDatabase();
                    boolean loggedIn = false;

                    try {
                        String auth = UUIDTypeAdapter.fromUUID(UUIDTypeAdapter.fromString(Launcher.this.requestedUser));
                        UserAuthentication auth1 = authDatabase.getByUUID(auth);
                        if (auth1 != null) {
                            Launcher.this.profileManager.setSelectedUser(auth);
                            loggedIn = true;
                        }
                    } catch (RuntimeException var5) {
                        ;
                    }

                    if (!loggedIn && authDatabase.getByName(Launcher.this.requestedUser) != null) {
                        UserAuthentication auth2 = authDatabase.getByName(Launcher.this.requestedUser);
                        if (auth2.getSelectedProfile() != null) {
                            Launcher.this.profileManager.setSelectedUser(UUIDTypeAdapter.fromUUID(auth2.getSelectedProfile().getId()));
                        } else {
                            Launcher.this.profileManager.setSelectedUser("demo-" + auth2.getUserID());
                        }
                    }
                }

                Launcher.this.ensureLoggedIn();
            }
        });
    }

    private MinecraftUserInterface selectUserInterface(JFrame frame) {
        return new SwingUserInterface(this, frame);
    }

    public com.mojang.launcher.Launcher getLauncher() {
        return this.launcher;
    }

    public MinecraftUserInterface getUserInterface() {
        return this.userInterface;
    }

    public Integer getBootstrapVersion() {
        return this.bootstrapVersion;
    }

    public void ensureLoggedIn() {
        UserAuthentication auth = this.profileManager.getAuthDatabase().getByUUID(this.profileManager.getSelectedUser());
        if (auth == null) {
            this.getUserInterface().showLoginPrompt();
        } else if (!auth.isLoggedIn()) {
            if (auth.canLogIn()) {
                try {
                    auth.logIn();

                    try {
                        this.profileManager.saveProfiles();
                    } catch (IOException var6) {
                        LOGGER.error("Couldn\'t save profiles after refreshing auth!", (Throwable) var6);
                    }

                    this.profileManager.fireRefreshEvent();
                } catch (AuthenticationException var7) {
                    LOGGER.error("Exception whilst logging into profile", (Throwable) var7);
                    this.getUserInterface().showLoginPrompt();
                }
            } else {
                this.getUserInterface().showLoginPrompt();
            }
        } else if (!auth.canPlayOnline()) {
            try {
                LOGGER.info("Refreshing auth...");
                auth.logIn();

                try {
                    this.profileManager.saveProfiles();
                } catch (IOException var3) {
                    LOGGER.error("Couldn\'t save profiles after refreshing auth!", (Throwable) var3);
                }

                this.profileManager.fireRefreshEvent();
            } catch (InvalidCredentialsException var4) {
                LOGGER.error("Exception whilst logging into profile", (Throwable) var4);
                this.getUserInterface().showLoginPrompt();
            } catch (AuthenticationException var5) {
                LOGGER.error("Exception whilst logging into profile", (Throwable) var5);
            }
        }

    }

    public UUID getClientToken() {
        return this.clientToken;
    }

    public void setClientToken(UUID clientToken) {
        this.clientToken = clientToken;
    }

    public void cleanupOrphanedAssets() throws IOException {
        File assetsDir = new File(this.getLauncher().getWorkingDirectory(), "assets");
        File indexDir = new File(assetsDir, "indexes");
        File objectsDir = new File(assetsDir, "objects");
        HashSet referencedObjects = Sets.newHashSet();
        if (objectsDir.isDirectory()) {
            Iterator directories = this.getLauncher().getVersionManager().getInstalledVersions().iterator();

            File directory;
            while (directories.hasNext()) {
                VersionSyncInfo arr$ = (VersionSyncInfo) directories.next();
                if (arr$.getLocalVersion() instanceof CompleteMinecraftVersion) {
                    CompleteMinecraftVersion len$ = (CompleteMinecraftVersion) arr$.getLocalVersion();
                    String i$ = len$.getAssets() == null ? "legacy" : len$.getAssets();
                    directory = new File(indexDir, i$ + ".json");
                    AssetIndex files = (AssetIndex) this.gson.fromJson(FileUtils.readFileToString(directory, Charsets.UTF_8), AssetIndex.class);
                    Iterator arr$1 = files.getUniqueObjects().keySet().iterator();

                    while (arr$1.hasNext()) {
                        AssetIndex.AssetObject len$1 = (AssetIndex.AssetObject) arr$1.next();
                        referencedObjects.add(len$1.getHash().toLowerCase());
                    }
                }
            }

            File[] var15 = objectsDir.listFiles((FileFilter) DirectoryFileFilter.DIRECTORY);
            if (var15 != null) {
                File[] var16 = var15;
                int var17 = var15.length;

                for (int var18 = 0; var18 < var17; ++var18) {
                    directory = var16[var18];
                    File[] var20 = directory.listFiles((FileFilter) FileFileFilter.FILE);
                    if (var20 != null) {
                        File[] var19 = var20;
                        int var21 = var20.length;

                        for (int i$1 = 0; i$1 < var21; ++i$1) {
                            File file = var19[i$1];
                            if (!referencedObjects.contains(file.getName().toLowerCase())) {
                                LOGGER.info("Cleaning up orphaned object {}", new Object[]{file.getName()});
                                FileUtils.deleteQuietly(file);
                            }
                        }
                    }
                }
            }

            deleteEmptyDirectories(objectsDir);
        }
    }

    public void cleanupOrphanedLibraries() throws IOException {
        File librariesDir = new File(this.getLauncher().getWorkingDirectory(), "libraries");
        HashSet referencedLibraries = Sets.newHashSet();
        if (librariesDir.isDirectory()) {
            Iterator libraries = this.getLauncher().getVersionManager().getInstalledVersions().iterator();

            while (libraries.hasNext()) {
                VersionSyncInfo i$ = (VersionSyncInfo) libraries.next();
                if (i$.getLocalVersion() instanceof CompleteMinecraftVersion) {
                    CompleteMinecraftVersion file = (CompleteMinecraftVersion) i$.getLocalVersion();
                    Iterator i$1 = file.getRelevantLibraries().iterator();

                    while (i$1.hasNext()) {
                        Library library = (Library) i$1.next();
                        String file1 = null;
                        if (library.getNatives() != null) {
                            String natives = (String) library.getNatives().get(OperatingSystem.getCurrentPlatform());
                            if (natives != null) {
                                file1 = library.getArtifactPath(natives);
                            }
                        } else {
                            file1 = library.getArtifactPath();
                        }

                        if (file1 != null) {
                            referencedLibraries.add(new File(librariesDir, file1));
                            referencedLibraries.add(new File(librariesDir, file1 + ".sha"));
                        }
                    }
                }
            }

            Collection libraries1 = FileUtils.listFiles(librariesDir, TrueFileFilter.TRUE, TrueFileFilter.TRUE);
            if (libraries1 != null) {
                Iterator i$2 = libraries1.iterator();

                while (i$2.hasNext()) {
                    File file2 = (File) i$2.next();
                    if (!referencedLibraries.contains(file2)) {
                        LOGGER.info("Cleaning up orphaned library {}", new Object[]{file2});
                        FileUtils.deleteQuietly(file2);
                    }
                }
            }

            deleteEmptyDirectories(librariesDir);
        }
    }

    public void cleanupOldSkins() {
        File assetsDir = new File(this.getLauncher().getWorkingDirectory(), "assets");
        File skinsDir = new File(assetsDir, "skins");
        if (skinsDir.isDirectory()) {
            Collection files = FileUtils.listFiles(skinsDir, new AgeFileFilter(System.currentTimeMillis() - 604800000L), TrueFileFilter.TRUE);
            if (files != null) {
                Iterator i$ = files.iterator();

                while (i$.hasNext()) {
                    File file = (File) i$.next();
                    LOGGER.info("Cleaning up old skin {}", new Object[]{file.getName()});
                    FileUtils.deleteQuietly(file);
                }
            }

            deleteEmptyDirectories(skinsDir);
        }
    }

    public void cleanupOldVirtuals() throws IOException {
        File assetsDir = new File(this.getLauncher().getWorkingDirectory(), "assets");
        File virtualsDir = new File(assetsDir, "virtual");
        DateTypeAdapter dateAdapter = new DateTypeAdapter();
        Calendar calendar = Calendar.getInstance();
        calendar.add(5, -5);
        Date cutoff = calendar.getTime();
        if (virtualsDir.isDirectory()) {
            File[] directories = virtualsDir.listFiles((FileFilter) DirectoryFileFilter.DIRECTORY);
            if (directories != null) {
                File[] arr$ = directories;
                int len$ = directories.length;

                for (int i$ = 0; i$ < len$; ++i$) {
                    File directory = arr$[i$];
                    File lastUsedFile = new File(directory, ".lastused");
                    if (lastUsedFile.isFile()) {
                        Date lastUsed = dateAdapter.deserializeToDate(FileUtils.readFileToString(lastUsedFile));
                        if (cutoff.after(lastUsed)) {
                            LOGGER.info("Cleaning up old virtual directory {}", new Object[]{directory});
                            FileUtils.deleteQuietly(directory);
                        }
                    } else {
                        LOGGER.info("Cleaning up strange virtual directory {}", new Object[]{directory});
                        FileUtils.deleteQuietly(directory);
                    }
                }
            }

            deleteEmptyDirectories(virtualsDir);
        }
    }

    public void cleanupOldNatives() {
        File root = new File(this.launcher.getWorkingDirectory(), "versions/");
        LOGGER.info("Looking for old natives & assets to clean up...");
        AgeFileFilter ageFilter = new AgeFileFilter(System.currentTimeMillis() - 3600000L);
        if (root.isDirectory()) {
            File[] versions = root.listFiles((FileFilter) DirectoryFileFilter.DIRECTORY);
            if (versions != null) {
                File[] arr$ = versions;
                int len$ = versions.length;

                for (int i$ = 0; i$ < len$; ++i$) {
                    File version = arr$[i$];
                    File[] files = version.listFiles((FileFilter) FileFilterUtils.and(new IOFileFilter[]{new PrefixFileFilter(version.getName() + "-natives-"), ageFilter}));
                    if (files != null) {
                        File[] arr$1 = files;
                        int len$1 = files.length;

                        for (int i$1 = 0; i$1 < len$1; ++i$1) {
                            File folder = arr$1[i$1];
                            LOGGER.debug("Deleting " + folder);
                            FileUtils.deleteQuietly(folder);
                        }
                    }
                }
            }

        }
    }

    public void cleanupOrphanedVersions() {
        LOGGER.info("Looking for orphaned versions to clean up...");
        HashSet referencedVersions = Sets.newHashSet();
        Iterator calendar = this.getProfileManager().getProfiles().values().iterator();

        VersionSyncInfo versionSyncInfo;
        while (calendar.hasNext()) {
            Profile cutoff = (Profile) calendar.next();
            String i$ = cutoff.getLastVersionId();
            versionSyncInfo = null;
            if (i$ != null) {
                versionSyncInfo = this.getLauncher().getVersionManager().getVersionSyncInfo(i$);
            }

            if (versionSyncInfo == null || versionSyncInfo.getLatestVersion() == null) {
                versionSyncInfo = (VersionSyncInfo) this.getLauncher().getVersionManager().getVersions(cutoff.getVersionFilter()).get(0);
            }

            if (versionSyncInfo != null) {
                Version version = versionSyncInfo.getLatestVersion();
                referencedVersions.add(version.getId());
                if (version instanceof CompleteMinecraftVersion) {
                    CompleteMinecraftVersion e = (CompleteMinecraftVersion) version;
                    referencedVersions.add(e.getInheritsFrom());
                    referencedVersions.add(e.getJar());
                }
            }
        }

        Calendar calendar1 = Calendar.getInstance();
        calendar1.add(5, -7);
        Date cutoff1 = calendar1.getTime();
        Iterator i$1 = this.getLauncher().getVersionManager().getInstalledVersions().iterator();

        while (i$1.hasNext()) {
            versionSyncInfo = (VersionSyncInfo) i$1.next();
            if (versionSyncInfo.getLocalVersion() instanceof CompleteMinecraftVersion) {
                CompleteVersion version1 = (CompleteVersion) versionSyncInfo.getLocalVersion();
                if (!referencedVersions.contains(version1.getId()) && version1.getType() == MinecraftReleaseType.SNAPSHOT) {
                    if (versionSyncInfo.isOnRemote()) {
                        LOGGER.info("Deleting orphaned version {} because it\'s a snapshot available on remote", new Object[]{version1.getId()});

                        try {
                            this.getLauncher().getVersionManager().uninstallVersion(version1);
                        } catch (IOException var8) {
                            LOGGER.warn("Couldn\'t uninstall version " + version1.getId(), (Throwable) var8);
                        }
                    } else if (version1.getUpdatedTime().before(cutoff1)) {
                        LOGGER.info("Deleting orphaned version {} because it\'s an unsupported old snapshot", new Object[]{version1.getId()});

                        try {
                            this.getLauncher().getVersionManager().uninstallVersion(version1);
                        } catch (IOException var9) {
                            LOGGER.warn("Couldn\'t uninstall version " + version1.getId(), (Throwable) var9);
                        }
                    }
                }
            }
        }

    }

    private static Collection<File> listEmptyDirectories(File directory) {
        ArrayList result = Lists.newArrayList();
        File[] files = directory.listFiles();
        if (files != null) {
            File[] arr$ = files;
            int len$ = files.length;

            for (int i$ = 0; i$ < len$; ++i$) {
                File file = arr$[i$];
                if (file.isDirectory()) {
                    File[] subFiles = file.listFiles();
                    if (subFiles != null && subFiles.length != 0) {
                        result.addAll(listEmptyDirectories(file));
                    } else {
                        result.add(file);
                    }
                }
            }
        }

        return result;
    }

    private static void deleteEmptyDirectories(File directory) {
        while (true) {
            Collection files = listEmptyDirectories(directory);
            if (files.isEmpty()) {
                return;
            }

            Iterator i$ = files.iterator();

            while (i$.hasNext()) {
                File file = (File) i$.next();
                if (!FileUtils.deleteQuietly(file)) {
                    return;
                }

                LOGGER.info("Deleted empty directory {}", new Object[]{file});
            }
        }
    }

    public void performCleanups() throws IOException {
        this.cleanupOrphanedVersions();
        this.cleanupOrphanedAssets();
        this.cleanupOldSkins();
        this.cleanupOldNatives();
        this.cleanupOldVirtuals();
    }

    public ProfileManager getProfileManager() {
        return this.profileManager;
    }

    public GameLaunchDispatcher getLaunchDispatcher() {
        return this.launchDispatcher;
    }

    static {
        Thread.currentThread().setContextClassLoader(Launcher.class.getClassLoader());
        LOGGER = LogManager.getLogger();
    }
}
