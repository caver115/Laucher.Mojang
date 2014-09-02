package net.minecraft.launcher.game;

import com.google.common.base.Objects;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.mojang.authlib.UserAuthentication;
import com.mojang.launcher.game.GameInstanceStatus;
import com.mojang.launcher.game.runner.GameRunner;
import com.mojang.launcher.game.runner.GameRunnerListener;
import com.mojang.launcher.updater.VersionSyncInfo;
import java.io.File;
import java.util.Iterator;
import java.util.concurrent.locks.ReentrantLock;
import net.minecraft.launcher.Launcher;
import net.minecraft.launcher.profile.LauncherVisibilityRule;
import net.minecraft.launcher.profile.Profile;
import net.minecraft.launcher.profile.ProfileManager;

public class GameLaunchDispatcher implements GameRunnerListener {

    private final Launcher launcher;
    private final String[] additionalLaunchArgs;
    private final ReentrantLock lock = new ReentrantLock();
    private final BiMap<UserAuthentication, MinecraftGameRunner> instances = HashBiMap.create();
    private boolean downloadInProgress = false;

    public GameLaunchDispatcher(Launcher launcher, String[] additionalLaunchArgs) {
        this.launcher = launcher;
        this.additionalLaunchArgs = additionalLaunchArgs;
    }

    public GameLaunchDispatcher.PlayStatus getStatus() {
        ProfileManager profileManager = this.launcher.getProfileManager();
        Profile profile = profileManager.getProfiles().isEmpty() ? null : profileManager.getSelectedProfile();
        UserAuthentication user = profileManager.getSelectedUser() == null ? null : profileManager.getAuthDatabase().getByUUID(profileManager.getSelectedUser());
        if (user != null && user.isLoggedIn() && profile != null && !this.launcher.getLauncher().getVersionManager().getVersions(profile.getVersionFilter()).isEmpty()) {
            this.lock.lock();

            GameLaunchDispatcher.PlayStatus var4;
            try {
                if (!this.downloadInProgress) {
                    if (!this.instances.containsKey(user)) {
                        return user.getSelectedProfile() == null ? GameLaunchDispatcher.PlayStatus.CAN_PLAY_DEMO : (user.canPlayOnline() ? GameLaunchDispatcher.PlayStatus.CAN_PLAY_ONLINE : GameLaunchDispatcher.PlayStatus.CAN_PLAY_OFFLINE);
                    }

                    var4 = GameLaunchDispatcher.PlayStatus.ALREADY_PLAYING;
                    return var4;
                }

                var4 = GameLaunchDispatcher.PlayStatus.DOWNLOADING;
            } finally {
                this.lock.unlock();
            }

            return var4;
        } else {
            return GameLaunchDispatcher.PlayStatus.LOADING;
        }
    }

    public GameInstanceStatus getInstanceStatus() {
        ProfileManager profileManager = this.launcher.getProfileManager();
        UserAuthentication user = profileManager.getSelectedUser() == null ? null : profileManager.getAuthDatabase().getByUUID(profileManager.getSelectedUser());
        this.lock.lock();

        GameInstanceStatus var4;
        try {
            GameRunner gameRunner = (GameRunner) this.instances.get(user);
            if (gameRunner == null) {
                return GameInstanceStatus.IDLE;
            }

            var4 = gameRunner.getStatus();
        } finally {
            this.lock.unlock();
        }

        return var4;
    }

    public void play() {
        ProfileManager profileManager = this.launcher.getProfileManager();
        final Profile profile = profileManager.getSelectedProfile();
        UserAuthentication user = profileManager.getSelectedUser() == null ? null : profileManager.getAuthDatabase().getByUUID(profileManager.getSelectedUser());
        final String lastVersionId = profile.getLastVersionId();
        final MinecraftGameRunner gameRunner = new MinecraftGameRunner(this.launcher, this.additionalLaunchArgs);
        gameRunner.setStatus(GameInstanceStatus.PREPARING);
        this.lock.lock();

        try {
            if (this.instances.containsKey(user) || this.downloadInProgress) {
                return;
            }

            this.instances.put(user, gameRunner);
            this.downloadInProgress = true;
        } finally {
            this.lock.unlock();
        }

        this.launcher.getLauncher().getVersionManager().getExecutorService().execute(new Runnable() {
            public void run() {
                gameRunner.setVisibility((LauncherVisibilityRule) Objects.firstNonNull(profile.getLauncherVisibilityOnGameClose(), Profile.DEFAULT_LAUNCHER_VISIBILITY));
                VersionSyncInfo syncInfo = null;
                if (lastVersionId != null) {
                    syncInfo = GameLaunchDispatcher.this.launcher.getLauncher().getVersionManager().getVersionSyncInfo(lastVersionId);
                }

                if (syncInfo == null || syncInfo.getLatestVersion() == null) {
                    syncInfo = (VersionSyncInfo) GameLaunchDispatcher.this.launcher.getLauncher().getVersionManager().getVersions(profile.getVersionFilter()).get(0);
                }

                gameRunner.setStatus(GameInstanceStatus.IDLE);
                gameRunner.addListener(GameLaunchDispatcher.this);
                gameRunner.playGame(syncInfo);
            }
        });
    }

    public void onGameInstanceChangedState(GameRunner runner, GameInstanceStatus status) {
        this.lock.lock();

        try {
            if (status == GameInstanceStatus.IDLE) {
                this.instances.inverse().remove(runner);
            }

            this.downloadInProgress = false;
            Iterator i$ = this.instances.values().iterator();

            while (true) {
                if (i$.hasNext()) {
                    GameRunner instance = (GameRunner) i$.next();
                    if (instance.getStatus() == GameInstanceStatus.PLAYING) {
                        continue;
                    }

                    this.downloadInProgress = true;
                }

                this.launcher.getUserInterface().updatePlayState();
                return;
            }
        } finally {
            this.lock.unlock();
        }
    }

    public boolean isRunningInSameFolder() {
        this.lock.lock();

        try {
            File currentGameDir = (File) Objects.firstNonNull(this.launcher.getProfileManager().getSelectedProfile().getGameDir(), this.launcher.getLauncher().getWorkingDirectory());
            Iterator i$ = this.instances.values().iterator();

            while (i$.hasNext()) {
                MinecraftGameRunner runner = (MinecraftGameRunner) i$.next();
                Profile profile = runner.getSelectedProfile();
                if (profile != null) {
                    File otherGameDir = (File) Objects.firstNonNull(profile.getGameDir(), this.launcher.getLauncher().getWorkingDirectory());
                    if (currentGameDir.equals(otherGameDir)) {
                        boolean var6 = true;
                        return var6;
                    }
                }
            }
        } finally {
            this.lock.unlock();
        }

        return false;
    }

    public static enum PlayStatus {

        LOADING("LOADING", 0, "Loading...", false),
        CAN_PLAY_DEMO("CAN_PLAY_DEMO", 1, "Play Demo", true),
        CAN_PLAY_ONLINE("CAN_PLAY_ONLINE", 2, "Play", true),
        CAN_PLAY_OFFLINE("CAN_PLAY_OFFLINE", 3, "Play Offline", true),
        ALREADY_PLAYING("ALREADY_PLAYING", 4, "Already Playing...", false),
        DOWNLOADING("DOWNLOADING", 5, "Installing...", false);
        private final String name;
        private final boolean canPlay;
        // $FF: synthetic field
        private static final GameLaunchDispatcher.PlayStatus[] $VALUES = new GameLaunchDispatcher.PlayStatus[]{LOADING, CAN_PLAY_DEMO, CAN_PLAY_ONLINE, CAN_PLAY_OFFLINE, ALREADY_PLAYING, DOWNLOADING};

        private PlayStatus(String var1, int var2, String name, boolean canPlay) {
            this.name = name;
            this.canPlay = canPlay;
        }

        public String getName() {
            return this.name;
        }

        public boolean canPlay() {
            return this.canPlay;
        }

    }
}
