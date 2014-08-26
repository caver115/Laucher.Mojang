package com.mojang.launcher.updater.download.assets;

import com.mojang.launcher.updater.download.Downloadable;
import com.mojang.launcher.updater.download.MonitoringInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.util.zip.GZIPInputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AssetDownloadable extends Downloadable {

    private static final Logger LOGGER = LogManager.getLogger();
    private final String name;
    private final AssetIndex.AssetObject asset;
    private final String urlBase;
    private final File destination;
    private AssetDownloadable.Status status;

    public AssetDownloadable(Proxy proxy, String name, AssetIndex.AssetObject asset, String urlBase, File destination) throws MalformedURLException {
        super(proxy, new URL(urlBase + createPathFromHash(asset.getHash())), new File(destination, createPathFromHash(asset.getHash())), false);
        this.status = AssetDownloadable.Status.DOWNLOADING;
        this.name = name;
        this.asset = asset;
        this.urlBase = urlBase;
        this.destination = destination;
    }

    protected static String createPathFromHash(String hash) {
        return hash.substring(0, 2) + "/" + hash;
    }

    public String download() throws IOException {
        this.status = AssetDownloadable.Status.DOWNLOADING;
        ++this.numAttempts;
        File localAsset = this.getTarget();
        File localCompressed = this.asset.hasCompressedAlternative() ? new File(this.destination, createPathFromHash(this.asset.getCompressedHash())) : null;
        URL remoteAsset = this.getUrl();
        URL remoteCompressed = this.asset.hasCompressedAlternative() ? new URL(this.urlBase + createPathFromHash(this.asset.getCompressedHash())) : null;
        this.ensureFileWritable(localAsset);
        if (localCompressed != null) {
            this.ensureFileWritable(localCompressed);
        }

        if (localAsset.isFile()) {
            if (FileUtils.sizeOf(localAsset) == this.asset.getSize()) {
                return "Have local file and it\'s the same size; assuming it\'s okay!";
            }

            LOGGER.warn("Had local file but it was the wrong size... had {} but expected {}", new Object[]{Long.valueOf(FileUtils.sizeOf(localAsset)), Long.valueOf(this.asset.getSize())});
            FileUtils.deleteQuietly(localAsset);
            this.status = AssetDownloadable.Status.DOWNLOADING;
        }

        if (localCompressed != null && localCompressed.isFile()) {
            String connection = getDigest(localCompressed, "SHA", 40);
            if (connection.equalsIgnoreCase(this.asset.getCompressedHash())) {
                return this.decompressAsset(localAsset, localCompressed);
            }

            LOGGER.warn("Had local compressed but it was the wrong hash... expected {} but had {}", new Object[]{this.asset.getCompressedHash(), connection});
            FileUtils.deleteQuietly(localCompressed);
        }

        int status;
        MonitoringInputStream inputStream;
        FileOutputStream outputStream;
        String hash;
        HttpURLConnection connection1;
        if (remoteCompressed != null && localCompressed != null) {
            connection1 = this.makeConnection(remoteCompressed);
            status = connection1.getResponseCode();
            if (status / 100 == 2) {
                this.updateExpectedSize(connection1);
                inputStream = new MonitoringInputStream(connection1.getInputStream(), this.getMonitor());
                outputStream = new FileOutputStream(localCompressed);
                hash = copyAndDigest(inputStream, outputStream, "SHA", 40);
                if (hash.equalsIgnoreCase(this.asset.getCompressedHash())) {
                    return this.decompressAsset(localAsset, localCompressed);
                } else {
                    FileUtils.deleteQuietly(localCompressed);
                    throw new RuntimeException(String.format("Hash did not match downloaded compressed asset (Expected %s, downloaded %s)", new Object[]{this.asset.getCompressedHash(), hash}));
                }
            } else {
                throw new RuntimeException("Server responded with " + status);
            }
        } else {
            connection1 = this.makeConnection(remoteAsset);
            status = connection1.getResponseCode();
            if (status / 100 == 2) {
                this.updateExpectedSize(connection1);
                inputStream = new MonitoringInputStream(connection1.getInputStream(), this.getMonitor());
                outputStream = new FileOutputStream(localAsset);
                hash = copyAndDigest(inputStream, outputStream, "SHA", 40);
                if (hash.equalsIgnoreCase(this.asset.getHash())) {
                    return "Downloaded asset and hash matched successfully";
                } else {
                    FileUtils.deleteQuietly(localAsset);
                    throw new RuntimeException(String.format("Hash did not match downloaded asset (Expected %s, downloaded %s)", new Object[]{this.asset.getHash(), hash}));
                }
            } else {
                throw new RuntimeException("Server responded with " + status);
            }
        }
    }

    public String getStatus() {
        return this.status.name + " " + this.name;
    }

    protected String decompressAsset(File localAsset, File localCompressed) throws IOException {
        this.status = AssetDownloadable.Status.EXTRACTING;
        FileOutputStream outputStream = FileUtils.openOutputStream(localAsset);
        GZIPInputStream inputStream = new GZIPInputStream(FileUtils.openInputStream(localCompressed));

        String hash;
        try {
            hash = copyAndDigest(inputStream, outputStream, "SHA", 40);
        } finally {
            IOUtils.closeQuietly((OutputStream) outputStream);
            IOUtils.closeQuietly((InputStream) inputStream);
        }

        this.status = AssetDownloadable.Status.DOWNLOADING;
        if (hash.equalsIgnoreCase(this.asset.getHash())) {
            return "Had local compressed asset, unpacked successfully and hash matched";
        } else {
            FileUtils.deleteQuietly(localAsset);
            throw new RuntimeException("Had local compressed asset but unpacked hash did not match (expected " + this.asset.getHash() + " but had " + hash + ")");
        }
    }

    private static enum Status {

        DOWNLOADING("DOWNLOADING", 0, "Downloading"),
        EXTRACTING("EXTRACTING", 1, "Extracting");
        private final String name;
// $FF: synthetic field
        private static final AssetDownloadable.Status[] $VALUES = new AssetDownloadable.Status[]{DOWNLOADING, EXTRACTING};

        private Status(String var1, int var2, String name) {
            this.name = name;
        }

    }
}
