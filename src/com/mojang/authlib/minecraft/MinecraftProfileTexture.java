package com.mojang.authlib.minecraft;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;

public class MinecraftProfileTexture {

    private final String url;

    public MinecraftProfileTexture(String url) {
        this.url = url;
    }

    public String getUrl() {
        return this.url;
    }

    public String getHash() {
        return FilenameUtils.getBaseName(this.url);
    }

    public String toString() {
        return (new ToStringBuilder(this)).append("url", (Object) this.url).append("hash", (Object) this.getHash()).toString();
    }

    public static enum Type {

        SKIN("SKIN", 0),
        CAPE("CAPE", 1);
// $FF: synthetic field
        private static final MinecraftProfileTexture.Type[] $VALUES = new MinecraftProfileTexture.Type[]{SKIN, CAPE};

        private Type(String var1, int var2) {
        }

    }
}
