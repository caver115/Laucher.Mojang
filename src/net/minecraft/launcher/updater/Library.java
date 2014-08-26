package net.minecraft.launcher.updater;

import com.mojang.launcher.OperatingSystem;
import com.mojang.launcher.versions.CompatibilityRule;
import com.mojang.launcher.versions.ExtractRules;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.StrSubstitutor;

public class Library {

    private static final StrSubstitutor SUBSTITUTOR = new StrSubstitutor(new HashMap() {
        {
            this.put("arch", System.getProperty("os.arch").contains("64") ? "64" : "32");
        }
    });
    private String name;
    private List<CompatibilityRule> rules;
    private Map<OperatingSystem, String> natives;
    private ExtractRules extract;
    private String url;

    public Library() {
    }

    public Library(String name) {
        if (name != null && name.length() != 0) {
            this.name = name;
        } else {
            throw new IllegalArgumentException("Library name cannot be null or empty");
        }
    }

    public Library(Library library) {
        this.name = library.name;
        this.url = library.url;
        if (library.extract != null) {
            this.extract = new ExtractRules(library.extract);
        }

        Iterator i$;
        if (library.rules != null) {
            this.rules = new ArrayList();
            i$ = library.rules.iterator();

            while (i$.hasNext()) {
                CompatibilityRule entry = (CompatibilityRule) i$.next();
                this.rules.add(new CompatibilityRule(entry));
            }
        }

        if (library.natives != null) {
            this.natives = new LinkedHashMap();
            i$ = library.getNatives().entrySet().iterator();

            while (i$.hasNext()) {
                Entry entry1 = (Entry) i$.next();
                this.natives.put((OperatingSystem) entry1.getKey(), entry1.getValue().toString());
            }
        }

    }

    public String getName() {
        return this.name;
    }

    public Library addNative(OperatingSystem operatingSystem, String name) {
        if (operatingSystem != null && operatingSystem.isSupported()) {
            if (name != null && name.length() != 0) {
                if (this.natives == null) {
                    this.natives = new EnumMap(OperatingSystem.class);
                }

                this.natives.put(operatingSystem, name);
                return this;
            } else {
                throw new IllegalArgumentException("Cannot add native for null or empty name");
            }
        } else {
            throw new IllegalArgumentException("Cannot add native for unsupported OS");
        }
    }

    public List<CompatibilityRule> getCompatibilityRules() {
        return this.rules;
    }

    public boolean appliesToCurrentEnvironment() {
        if (this.rules == null) {
            return true;
        } else {
            CompatibilityRule.Action lastAction = CompatibilityRule.Action.DISALLOW;
            Iterator i$ = this.rules.iterator();

            while (i$.hasNext()) {
                CompatibilityRule compatibilityRule = (CompatibilityRule) i$.next();
                CompatibilityRule.Action action = compatibilityRule.getAppliedAction();
                if (action != null) {
                    lastAction = action;
                }
            }

            return lastAction == CompatibilityRule.Action.ALLOW;
        }
    }

    public Map<OperatingSystem, String> getNatives() {
        return this.natives;
    }

    public ExtractRules getExtractRules() {
        return this.extract;
    }

    public Library setExtractRules(ExtractRules rules) {
        this.extract = rules;
        return this;
    }

    public String getArtifactBaseDir() {
        if (this.name == null) {
            throw new IllegalStateException("Cannot get artifact dir of empty/blank artifact");
        } else {
            String[] parts = this.name.split(":", 3);
            return String.format("%s/%s/%s", new Object[]{parts[0].replaceAll("\\.", "/"), parts[1], parts[2]});
        }
    }

    public String getArtifactPath() {
        return this.getArtifactPath((String) null);
    }

    public String getArtifactPath(String classifier) {
        if (this.name == null) {
            throw new IllegalStateException("Cannot get artifact path of empty/blank artifact");
        } else {
            return String.format("%s/%s", new Object[]{this.getArtifactBaseDir(), this.getArtifactFilename(classifier)});
        }
    }

    public String getArtifactFilename(String classifier) {
        if (this.name == null) {
            throw new IllegalStateException("Cannot get artifact filename of empty/blank artifact");
        } else {
            String[] parts = this.name.split(":", 3);
            String result = String.format("%s-%s%s.jar", new Object[]{parts[1], parts[2], StringUtils.isEmpty(classifier) ? "" : "-" + classifier});
            return SUBSTITUTOR.replace(result);
        }
    }

    public String toString() {
        return "Library{name=\'" + this.name + '\'' + ", rules=" + this.rules + ", natives=" + this.natives + ", extract=" + this.extract + '}';
    }

    public boolean hasCustomUrl() {
        return this.url != null;
    }

    public String getDownloadUrl() {
        return this.url != null ? this.url : "https://libraries.minecraft.net/";
    }

}
