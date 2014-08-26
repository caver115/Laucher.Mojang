package net.minecraft.launcher.ui.tabs;

import java.awt.BorderLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.beans.IntrospectionException;
import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import javax.swing.JPanel;
import net.minecraft.launcher.Launcher;
import net.minecraft.launcher.ui.tabs.website.Browser;
import net.minecraft.launcher.ui.tabs.website.JFXBrowser;
import net.minecraft.launcher.ui.tabs.website.LegacySwingBrowser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class WebsiteTab extends JPanel {

    private static final Logger LOGGER = LogManager.getLogger();
    private final Browser browser = this.selectBrowser();
    private final Launcher minecraftLauncher;

    public WebsiteTab(Launcher minecraftLauncher) {
        this.minecraftLauncher = minecraftLauncher;
        this.setLayout(new BorderLayout());
        this.add(this.browser.getComponent(), "Center");
        this.browser.resize(this.getSize());
        this.addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent e) {
                WebsiteTab.this.browser.resize(e.getComponent().getSize());
            }
        });
    }

    private Browser selectBrowser() {
        if (this.hasJFX()) {
            LOGGER.info("JFX is already initialized");
            return new JFXBrowser();
        } else {
            File jfxrt = new File(System.getProperty("java.home"), "lib/jfxrt.jar");
            if (jfxrt.isFile()) {
                LOGGER.debug("Attempting to load {}...", new Object[]{jfxrt});

                try {
                    addToSystemClassLoader(jfxrt);
                    LOGGER.info("JFX has been detected & successfully loaded");
                    return new JFXBrowser();
                } catch (Throwable var3) {
                    LOGGER.debug("JFX has been detected but unsuccessfully loaded", var3);
                    return new LegacySwingBrowser();
                }
            } else {
                LOGGER.debug("JFX was not found at {}", new Object[]{jfxrt});
                return new LegacySwingBrowser();
            }
        }
    }

    public void setPage(String url) {
        this.browser.loadUrl(url);
    }

    public Launcher getMinecraftLauncher() {
        return this.minecraftLauncher;
    }

    public static void addToSystemClassLoader(File file) throws IntrospectionException {
        if (ClassLoader.getSystemClassLoader() instanceof URLClassLoader) {
            URLClassLoader classLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();

            try {
                Method t = URLClassLoader.class.getDeclaredMethod("addURL", new Class[]{URL.class});
                t.setAccessible(true);
                t.invoke(classLoader, new Object[]{file.toURI().toURL()});
            } catch (Throwable var3) {
                LOGGER.warn("Couldn\'t add " + file + " to system classloader", var3);
            }
        }

    }

    public boolean hasJFX() {
        try {
            this.getClass().getClassLoader().loadClass("javafx.embed.swing.JFXPanel");
            return true;
        } catch (ClassNotFoundException var2) {
            return false;
        }
    }

}
