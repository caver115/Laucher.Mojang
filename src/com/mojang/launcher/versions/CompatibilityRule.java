package com.mojang.launcher.versions;

import com.mojang.launcher.OperatingSystem;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CompatibilityRule {

    private CompatibilityRule.Action action;
    private CompatibilityRule.OSRestriction os;

    public CompatibilityRule() {
        this.action = CompatibilityRule.Action.ALLOW;
    }

    public CompatibilityRule(CompatibilityRule compatibilityRule) {
        this.action = CompatibilityRule.Action.ALLOW;
        this.action = compatibilityRule.action;
        if (compatibilityRule.os != null) {
            this.os = new CompatibilityRule.OSRestriction(compatibilityRule.os);
        }

    }

    public CompatibilityRule.Action getAppliedAction() {
        return this.os != null && !this.os.isCurrentOperatingSystem() ? null : this.action;
    }

    public CompatibilityRule.Action getAction() {
        return this.action;
    }

    public CompatibilityRule.OSRestriction getOs() {
        return this.os;
    }

    public String toString() {
        return "Rule{action=" + this.action + ", os=" + this.os + '}';
    }

    public static enum Action {

        ALLOW("ALLOW", 0),
        DISALLOW("DISALLOW", 1);
        // $FF: synthetic field
        private static final CompatibilityRule.Action[] $VALUES = new CompatibilityRule.Action[]{ALLOW, DISALLOW};

        private Action(String var1, int var2) {
        }

    }

    public class OSRestriction {

        private OperatingSystem name;
        private String version;
        private String arch;

        public OSRestriction() {
        }

        public OperatingSystem getName() {
            return this.name;
        }

        public String getVersion() {
            return this.version;
        }

        public String getArch() {
            return this.arch;
        }

        public OSRestriction(CompatibilityRule.OSRestriction osRestriction) {
            this.name = osRestriction.name;
            this.version = osRestriction.version;
            this.arch = osRestriction.arch;
        }

        public boolean isCurrentOperatingSystem() {
            if (this.name != null && this.name != OperatingSystem.getCurrentPlatform()) {
                return false;
            } else {
                Pattern ignored;
                Matcher matcher;
                if (this.version != null) {
                    try {
                        ignored = Pattern.compile(this.version);
                        matcher = ignored.matcher(System.getProperty("os.version"));
                        if (!matcher.matches()) {
                            return false;
                        }
                    } catch (Throwable var4) {
                        ;
                    }
                }

                if (this.arch != null) {
                    try {
                        ignored = Pattern.compile(this.arch);
                        matcher = ignored.matcher(System.getProperty("os.arch"));
                        if (!matcher.matches()) {
                            return false;
                        }
                    } catch (Throwable var3) {
                        ;
                    }
                }

                return true;
            }
        }

        public String toString() {
            return "OSRestriction{name=" + this.name + ", version=\'" + this.version + '\'' + ", arch=\'" + this.arch + '\'' + '}';
        }
    }
}
