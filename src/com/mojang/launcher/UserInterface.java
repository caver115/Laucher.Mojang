package com.mojang.launcher;

import com.mojang.launcher.updater.DownloadProgress;
import com.mojang.launcher.versions.CompleteVersion;
import java.io.File;

public interface UserInterface {

    void showLoginPrompt();

    void setVisible(boolean var1);

    void shutdownLauncher();

    void hideDownloadProgress();

    void setDownloadProgress(DownloadProgress var1);

    void showCrashReport(CompleteVersion var1, File var2, String var3);

    void gameLaunchFailure(String var1);

    void updatePlayState();
}
