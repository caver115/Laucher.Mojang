package com.mojang.launcher.updater.download;

public interface DownloadListener {

    void onDownloadJobFinished(DownloadJob var1);

    void onDownloadJobProgressChanged(DownloadJob var1);
}
