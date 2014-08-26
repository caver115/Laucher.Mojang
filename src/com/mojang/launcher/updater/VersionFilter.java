package com.mojang.launcher.updater;

import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.mojang.launcher.versions.ReleaseType;
import com.mojang.launcher.versions.ReleaseTypeFactory;
import java.util.Collections;
import java.util.Set;

public class VersionFilter<T extends Object & ReleaseType> {

    private final Set<T> types = Sets.newHashSet();
    private int maxCount = 5;

    public VersionFilter(ReleaseTypeFactory<T> factory) {
        Iterables.addAll(this.types, factory);
    }

    public Set<T> getTypes() {
        return this.types;
    }

    public VersionFilter<T> onlyForTypes(T... types) {
        this.types.clear();
        this.includeTypes(types);
        return this;
    }

    public VersionFilter<T> includeTypes(T... types) {
        if (types != null) {
            Collections.addAll(this.types, types);
        }

        return this;
    }

    public VersionFilter<T> excludeTypes(T... types) {
        if (types != null) {
            ReleaseType[] arr$ = types;
            int len$ = types.length;

            for (int i$ = 0; i$ < len$; ++i$) {
                ReleaseType type = arr$[i$];
                this.types.remove(type);
            }
        }

        return this;
    }

    public int getMaxCount() {
        return this.maxCount;
    }

    public VersionFilter<T> setMaxCount(int maxCount) {
        this.maxCount = maxCount;
        return this;
    }
}
