package com.mojang.launcher.versions;

public interface ReleaseTypeFactory<T extends Object & ReleaseType> extends Iterable<T> {

    T getTypeByName(String var1);

    T[] getAllTypes();

    Class<T> getTypeClass();
}
