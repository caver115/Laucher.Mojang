package com.mojang.launcher.updater.download.assets;

import com.google.common.collect.Maps;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

public class AssetIndex {

    public static final String DEFAULT_ASSET_NAME = "legacy";
    private Map<String, AssetIndex.AssetObject> objects = new LinkedHashMap();
    private boolean virtual;

    public Map<String, AssetIndex.AssetObject> getFileMap() {
        return this.objects;
    }

    public Map<AssetIndex.AssetObject, String> getUniqueObjects() {
        HashMap result = Maps.newHashMap();
        Iterator i$ = this.objects.entrySet().iterator();

        while (i$.hasNext()) {
            Entry objectEntry = (Entry) i$.next();
            result.put(objectEntry.getValue(), objectEntry.getKey());
        }

        return result;
    }

    public boolean isVirtual() {
        return this.virtual;
    }

    public class AssetObject {

        private String hash;
        private long size;
        private boolean reconstruct;
        private String compressedHash;
        private long compressedSize;

        public String getHash() {
            return this.hash;
        }

        public long getSize() {
            return this.size;
        }

        public boolean shouldReconstruct() {
            return this.reconstruct;
        }

        public boolean hasCompressedAlternative() {
            return this.compressedHash != null;
        }

        public String getCompressedHash() {
            return this.compressedHash;
        }

        public long getCompressedSize() {
            return this.compressedSize;
        }

        public boolean equals(Object o) {
            if (this == o) {
                return true;
            } else if (o != null && this.getClass() == o.getClass()) {
                AssetIndex.AssetObject that = (AssetIndex.AssetObject) o;
                if (this.compressedSize != that.compressedSize) {
                    return false;
                } else if (this.reconstruct != that.reconstruct) {
                    return false;
                } else if (this.size != that.size) {
                    return false;
                } else {
                    if (this.compressedHash != null) {
                        if (!this.compressedHash.equals(that.compressedHash)) {
                            return false;
                        }
                    } else if (that.compressedHash != null) {
                        return false;
                    }

                    if (this.hash != null) {
                        if (!this.hash.equals(that.hash)) {
                            return false;
                        }
                    } else if (that.hash != null) {
                        return false;
                    }

                    return true;
                }
            } else {
                return false;
            }
        }

        public int hashCode() {
            int result = this.hash != null ? this.hash.hashCode() : 0;
            result = 31 * result + (int) (this.size ^ this.size >>> 32);
            result = 31 * result + (this.reconstruct ? 1 : 0);
            result = 31 * result + (this.compressedHash != null ? this.compressedHash.hashCode() : 0);
            result = 31 * result + (int) (this.compressedSize ^ this.compressedSize >>> 32);
            return result;
        }
    }
}
