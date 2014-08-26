package com.mojang.launcher.versions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class ExtractRules {

    private List<String> exclude = new ArrayList();

    public ExtractRules() {
    }

    public ExtractRules(String... exclude) {
        if (exclude != null) {
            Collections.addAll(this.exclude, exclude);
        }

    }

    public ExtractRules(ExtractRules rules) {
        Iterator i$ = rules.exclude.iterator();

        while (i$.hasNext()) {
            String exclude = (String) i$.next();
            this.exclude.add(exclude);
        }

    }

    public List<String> getExcludes() {
        return this.exclude;
    }

    public boolean shouldExtract(String path) {
        if (this.exclude != null) {
            Iterator i$ = this.exclude.iterator();

            while (i$.hasNext()) {
                String rule = (String) i$.next();
                if (path.startsWith(rule)) {
                    return false;
                }
            }
        }

        return true;
    }
}
