package com.mojang.launcher;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public enum OperatingSystem {

    LINUX("LINUX", 0, "linux", new String[]{"linux", "unix"}),
    WINDOWS("WINDOWS", 1, "windows", new String[]{"win"}),
    OSX("OSX", 2, "osx", new String[]{"mac"}),
    UNKNOWN("UNKNOWN", 3, "unknown", new String[0]);
    private static final Logger LOGGER = LogManager.getLogger();
    private final String name;
    private final String[] aliases;
// $FF: synthetic field
    private static final OperatingSystem[] $VALUES = new OperatingSystem[]{LINUX, WINDOWS, OSX, UNKNOWN};

    private OperatingSystem(String var1, int var2, String name, String... aliases) {
        this.name = name;
        this.aliases = aliases == null ? new String[0] : aliases;
    }

    public String getName() {
        return this.name;
    }

    public String[] getAliases() {
        return this.aliases;
    }

    public boolean isSupported() {
        return this != UNKNOWN;
    }

    public String getJavaDir() {
        String separator = System.getProperty("file.separator");
        String path = System.getProperty("java.home") + separator + "bin" + separator;
        return getCurrentPlatform() == WINDOWS && (new File(path + "javaw.exe")).isFile() ? path + "javaw.exe" : path + "java";
    }

    public static OperatingSystem getCurrentPlatform() {
        String osName = System.getProperty("os.name").toLowerCase();
        OperatingSystem[] arr$ = values();
        int len$ = arr$.length;

        for (int i$ = 0; i$ < len$; ++i$) {
            OperatingSystem os = arr$[i$];
            String[] arr$1 = os.getAliases();
            int len$1 = arr$1.length;

            for (int i$1 = 0; i$1 < len$1; ++i$1) {
                String alias = arr$1[i$1];
                if (osName.contains(alias)) {
                    return os;
                }
            }
        }

        return UNKNOWN;
    }

    public static void openLink(URI link) {
        try {
            Class e = Class.forName("java.awt.Desktop");
            Object e1 = e.getMethod("getDesktop", new Class[0]).invoke((Object) null, new Object[0]);
            e.getMethod("browse", new Class[]{URI.class}).invoke(e1, new Object[]{link});
        } catch (Throwable var4) {
            if (getCurrentPlatform() == OSX) {
                try {
                    Runtime.getRuntime().exec(new String[]{"/usr/bin/open", link.toString()});
                } catch (IOException var3) {
                    LOGGER.error("Failed to open link " + link.toString(), (Throwable) var3);
                }
            } else {
                LOGGER.error("Failed to open link " + link.toString(), var4);
            }
        }

    }

    public static void openFolder(File path) {
        String absolutePath = path.getAbsolutePath();
        OperatingSystem os = getCurrentPlatform();
        if (os == OSX) {
            try {
                Runtime.getRuntime().exec(new String[]{"/usr/bin/open", absolutePath});
                return;
            } catch (IOException var7) {
                LOGGER.error("Couldn\'t open " + path + " through /usr/bin/open", (Throwable) var7);
            }
        } else if (os == WINDOWS) {
            String e = String.format("cmd.exe /C start \"Open file\" \"%s\"", new Object[]{absolutePath});

            try {
                Runtime.getRuntime().exec(e);
                return;
            } catch (IOException var6) {
                LOGGER.error("Couldn\'t open " + path + " through cmd.exe", (Throwable) var6);
            }
        }

        try {
            Class e1 = Class.forName("java.awt.Desktop");
            Object desktop = e1.getMethod("getDesktop", new Class[0]).invoke((Object) null, new Object[0]);
            e1.getMethod("browse", new Class[]{URI.class}).invoke(desktop, new Object[]{path.toURI()});
        } catch (Throwable var5) {
            LOGGER.error("Couldn\'t open " + path + " through Desktop.browse()", var5);
        }

    }

}
