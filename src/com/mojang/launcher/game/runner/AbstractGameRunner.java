package com.mojang.launcher.game.runner;

import com.google.common.collect.Lists;
import com.mojang.launcher.Launcher;
import com.mojang.launcher.game.GameInstanceStatus;
import com.mojang.launcher.updater.DownloadProgress;
import com.mojang.launcher.updater.VersionSyncInfo;
import com.mojang.launcher.updater.download.DownloadJob;
import com.mojang.launcher.updater.download.DownloadListener;
import com.mojang.launcher.updater.download.Downloadable;
import com.mojang.launcher.versions.CompleteVersion;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class AbstractGameRunner implements GameRunner, DownloadListener {

    protected static final Logger LOGGER = LogManager.getLogger();
    protected final Object lock = new Object();
    private final List<DownloadJob> jobs = new ArrayList();
    protected CompleteVersion version;
    private GameInstanceStatus status;
    private final List<GameRunnerListener> listeners;

    public AbstractGameRunner() {
        this.status = GameInstanceStatus.IDLE;
        this.listeners = Lists.newArrayList();
    }

    protected void setStatus(GameInstanceStatus status) {
        Object var2 = this.lock;
        synchronized (this.lock) {
            this.status = status;
            Iterator i$ = Lists.newArrayList((Iterable) this.listeners).iterator();

            while (i$.hasNext()) {
                GameRunnerListener listener = (GameRunnerListener) i$.next();
                listener.onGameInstanceChangedState(this, status);
            }

        }
    }

    protected abstract Launcher getLauncher();

    public GameInstanceStatus getStatus() {
        return this.status;
    }

    public void playGame(VersionSyncInfo syncInfo) {
        Object var2 = this.lock;
        synchronized (this.lock) {
            if (this.getStatus() != GameInstanceStatus.IDLE) {
                LOGGER.warn("Tried to play game but game is already starting!");
                return;
            }

            this.setStatus(GameInstanceStatus.PREPARING);
        }

        LOGGER.info("Getting syncinfo for selected version");
        if (syncInfo == null) {
            LOGGER.warn("Tried to launch a version without a version being selected...");
            this.setStatus(GameInstanceStatus.IDLE);
        } else {
            var2 = this.lock;
            synchronized (this.lock) {
                LOGGER.info("Queueing library & version downloads");

                try {
                    this.version = this.getLauncher().getVersionManager().getLatestCompleteVersion(syncInfo);
                } catch (IOException var7) {
                    LOGGER.error("Couldn\'t get complete version info for " + syncInfo.getLatestVersion(), (Throwable) var7);
                    this.setStatus(GameInstanceStatus.IDLE);
                    return;
                }

                if (syncInfo.getRemoteVersion() != null && syncInfo.getLatestSource() != VersionSyncInfo.VersionSource.REMOTE && !this.version.isSynced()) {
                    try {
                        syncInfo = this.getLauncher().getVersionManager().syncVersion(syncInfo);
                        this.version = this.getLauncher().getVersionManager().getLatestCompleteVersion(syncInfo);
                    } catch (IOException var6) {
                        LOGGER.error("Couldn\'t sync local and remote versions", (Throwable) var6);
                    }

                    this.version.setSynced(true);
                }

                if (!this.version.appliesToCurrentEnvironment()) {
                    String e = this.version.getIncompatibilityReason();
                    if (e == null) {
                        e = "This version is incompatible with your computer. Please try another one by going into Edit Profile and selecting one through the dropdown. Sorry!";
                    }

                    LOGGER.error("Version " + this.version.getId() + " is incompatible with current environment: " + e);
                    this.getLauncher().getUserInterface().gameLaunchFailure(e);
                    this.setStatus(GameInstanceStatus.IDLE);
                } else if (this.version.getMinimumLauncherVersion() > this.getLauncher().getLauncherFormatVersion()) {
                    LOGGER.error("An update to your launcher is available and is required to play " + this.version.getId() + ". Please restart your launcher.");
                    this.setStatus(GameInstanceStatus.IDLE);
                } else {
                    if (!syncInfo.isUpToDate()) {
                        try {
                            this.getLauncher().getVersionManager().installVersion(this.version);
                        } catch (IOException var5) {
                            LOGGER.error("Couldn\'t save version info to install " + syncInfo.getLatestVersion(), (Throwable) var5);
                            this.setStatus(GameInstanceStatus.IDLE);
                            return;
                        }
                    }

                    this.setStatus(GameInstanceStatus.DOWNLOADING);
                    this.downloadRequiredFiles(syncInfo);
                }
            }
        }
    }

    protected void downloadRequiredFiles(VersionSyncInfo syncInfo) {
        try {
            DownloadJob e = new DownloadJob("Version & Libraries", false, this);
            this.addJob(e);
            this.getLauncher().getVersionManager().downloadVersion(syncInfo, e);
            e.startDownloading(this.getLauncher().getDownloaderExecutorService());
            DownloadJob resourceJob = new DownloadJob("Resources", true, this);
            this.addJob(resourceJob);
            this.getLauncher().getVersionManager().downloadResources(resourceJob, this.version);
            resourceJob.startDownloading(this.getLauncher().getDownloaderExecutorService());
        } catch (IOException var4) {
            LOGGER.error("Couldn\'t get version info for " + syncInfo.getLatestVersion(), (Throwable) var4);
            this.setStatus(GameInstanceStatus.IDLE);
        }

    }

    protected void updateProgressBar() {
        if (this.hasRemainingJobs()) {
            Object var1 = this.lock;
            synchronized (this.lock) {
                long total = 0L;
                long current = 0L;
                Downloadable longestRunning = null;
                Iterator i$ = this.jobs.iterator();

                while (i$.hasNext()) {
                    DownloadJob job = (DownloadJob) i$.next();
                    Iterator i$1 = job.getAllFiles().iterator();

                    while (i$1.hasNext()) {
                        Downloadable file = (Downloadable) i$1.next();
                        total += file.getMonitor().getTotal();
                        current += file.getMonitor().getCurrent();
                        if (longestRunning == null || longestRunning.getEndTime() > 0L || file.getStartTime() < longestRunning.getStartTime() && file.getEndTime() == 0L) {
                            longestRunning = file;
                        }
                    }
                }

                this.getLauncher().getUserInterface().setDownloadProgress(new DownloadProgress(current, total, longestRunning == null ? null : longestRunning.getStatus()));
            }
        } else {
            this.getLauncher().getUserInterface().hideDownloadProgress();
        }

    }

    public boolean hasRemainingJobs() {
        Object var1 = this.lock;
        synchronized (this.lock) {
            Iterator i$ = this.jobs.iterator();

            DownloadJob job;
            do {
                if (!i$.hasNext()) {
                    return false;
                }

                job = (DownloadJob) i$.next();
            } while (job.isComplete());

            return true;
        }
    }

    public void addJob(DownloadJob job) {
        Object var2 = this.lock;
        synchronized (this.lock) {
            this.jobs.add(job);
        }
    }

    public void onDownloadJobFinished(DownloadJob job) {
        this.updateProgressBar();
        Object var2 = this.lock;
        synchronized (this.lock) {
            if (job.getFailures() > 0) {
                LOGGER.error("Job \'" + job.getName() + "\' finished with " + job.getFailures() + " failure(s)! (took " + job.getStopWatch().toString() + ")");
                this.setStatus(GameInstanceStatus.IDLE);
            } else {
                LOGGER.info("Job \'" + job.getName() + "\' finished successfully (took " + job.getStopWatch().toString() + ")");
                if (this.getStatus() != GameInstanceStatus.IDLE && !this.hasRemainingJobs()) {
                    try {
                        this.setStatus(GameInstanceStatus.LAUNCHING);
                        this.launchGame();
                    } catch (Throwable var5) {
                        LOGGER.fatal("Fatal error launching game. Report this to http://bugs.mojang.com please!", var5);
                    }
                }
            }

        }
    }

    protected abstract void launchGame() throws IOException;

    public void onDownloadJobProgressChanged(DownloadJob job) {
        this.updateProgressBar();
    }

    public void addListener(GameRunnerListener listener) {
        Object var2 = this.lock;
        synchronized (this.lock) {
            this.listeners.add(listener);
        }
    }

}
