package com.mojang.launcher.updater.download;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

public class ChecksummedDownloadable extends Downloadable {

    private String checksum;

    public ChecksummedDownloadable(Proxy proxy, URL remoteFile, File localFile, boolean forceDownload) {
        super(proxy, remoteFile, localFile, forceDownload);
    }

    public String download() throws IOException {
        ++this.numAttempts;
        this.ensureFileWritable(this.getTarget());
        File target = this.getTarget();
        File checksumFile = new File(target.getAbsolutePath() + ".sha");
        String localHash = null;
        if (target.isFile()) {
            localHash = getDigest(target, "SHA-1", 40);
        }

        if (target.isFile() && checksumFile.isFile()) {
            this.checksum = this.readFile(checksumFile, "");
            if (this.checksum.length() == 0 || this.checksum.trim().equalsIgnoreCase(localHash)) {
                return "Local file matches local checksum, using that";
            }

            this.checksum = null;
            FileUtils.deleteQuietly(checksumFile);
        }

        HttpURLConnection e;
        int status;
        if (this.checksum == null) {
            try {
                e = this.makeConnection(new URL(this.getUrl().toString() + ".sha1"));
                status = e.getResponseCode();
                if (status / 100 == 2) {
                    InputStream inputStream = e.getInputStream();

                    try {
                        this.checksum = IOUtils.toString(inputStream, Charsets.UTF_8);
                        FileUtils.writeStringToFile(checksumFile, this.checksum);
                    } catch (IOException var13) {
                        this.checksum = "";
                    } finally {
                        IOUtils.closeQuietly(inputStream);
                    }
                } else if (checksumFile.isFile()) {
                    this.checksum = this.readFile(checksumFile, "");
                } else {
                    this.checksum = "";
                }
            } catch (IOException var16) {
                if (!target.isFile()) {
                    throw var16;
                }

                this.checksum = this.readFile(checksumFile, "");
            }
        }

        try {
            e = this.makeConnection(this.getUrl());
            status = e.getResponseCode();
            if (status / 100 == 2) {
                this.updateExpectedSize(e);
                MonitoringInputStream inputStream1 = new MonitoringInputStream(e.getInputStream(), this.getMonitor());
                FileOutputStream outputStream = new FileOutputStream(this.getTarget());
                String digest = copyAndDigest(inputStream1, outputStream, "SHA", 40);
                if (this.checksum != null && this.checksum.length() != 0) {
                    if (this.checksum.trim().equalsIgnoreCase(digest)) {
                        return "Downloaded successfully and checksum matched";
                    } else {
                        throw new RuntimeException(String.format("Checksum did not match downloaded file (Checksum was %s, downloaded %s)", new Object[]{this.checksum, digest}));
                    }
                } else {
                    return "Didn\'t have checksum so assuming our copy is good";
                }
            } else if (this.getTarget().isFile()) {
                return "Couldn\'t connect to server (responded with " + status + ") but have local file, assuming it\'s good";
            } else {
                throw new RuntimeException("Server responded with " + status);
            }
        } catch (IOException var15) {
            if (this.getTarget().isFile() && (this.checksum == null || this.checksum.length() == 0)) {
                return "Couldn\'t connect to server (" + var15.getClass().getSimpleName() + ": \'" + var15.getMessage() + "\') but have local file, assuming it\'s good";
            } else {
                throw var15;
            }
        }
    }

    private String readFile(File file, String def) {
        try {
            return FileUtils.readFileToString(file);
        } catch (Throwable var4) {
            return def;
        }
    }
}
