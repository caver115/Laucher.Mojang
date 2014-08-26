package com.mojang.launcher.updater.download;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;

public class EtagDownloadable extends Downloadable {

    public EtagDownloadable(Proxy proxy, URL remoteFile, File localFile, boolean forceDownload) {
        super(proxy, remoteFile, localFile, forceDownload);
    }

    public String download() throws IOException {
        ++this.numAttempts;
        this.ensureFileWritable(this.getTarget());

        try {
            HttpURLConnection e = this.makeConnection(this.getUrl());
            int status = e.getResponseCode();
            if (status == 304) {
                return "Used own copy as it matched etag";
            } else if (status / 100 == 2) {
                this.updateExpectedSize(e);
                MonitoringInputStream inputStream = new MonitoringInputStream(e.getInputStream(), this.getMonitor());
                FileOutputStream outputStream = new FileOutputStream(this.getTarget());
                String md5 = copyAndDigest(inputStream, outputStream, "MD5", 32);
                String etag = getEtag(e.getHeaderField("ETag"));
                if (etag.contains("-")) {
                    return "Didn\'t have etag so assuming our copy is good";
                } else if (etag.equalsIgnoreCase(md5)) {
                    return "Downloaded successfully and etag matched";
                } else {
                    throw new RuntimeException(String.format("E-tag did not match downloaded MD5 (ETag was %s, downloaded %s)", new Object[]{etag, md5}));
                }
            } else if (this.getTarget().isFile()) {
                return "Couldn\'t connect to server (responded with " + status + ") but have local file, assuming it\'s good";
            } else {
                throw new RuntimeException("Server responded with " + status);
            }
        } catch (IOException var7) {
            if (this.getTarget().isFile()) {
                return "Couldn\'t connect to server (" + var7.getClass().getSimpleName() + ": \'" + var7.getMessage() + "\') but have local file, assuming it\'s good";
            } else {
                throw var7;
            }
        }
    }

    protected HttpURLConnection makeConnection(URL url) throws IOException {
        HttpURLConnection connection = super.makeConnection(url);
        if (!this.shouldIgnoreLocal() && this.getTarget().isFile()) {
            connection.setRequestProperty("If-None-Match", getDigest(this.getTarget(), "MD5", 32));
        }

        return connection;
    }

    public static String getEtag(String etag) {
        if (etag == null) {
            etag = "-";
        } else if (etag.startsWith("\"") && etag.endsWith("\"")) {
            etag = etag.substring(1, etag.length() - 1);
        }

        return etag;
    }
}
