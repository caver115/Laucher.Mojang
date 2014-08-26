package net.minecraft.launcher.ui.bottombar;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mojang.launcher.Http;
import com.mojang.launcher.updater.LowerCaseEnumTypeAdapterFactory;
import java.awt.GridBagConstraints;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.swing.JLabel;
import net.minecraft.launcher.Launcher;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class StatusPanelForm extends SidebarGridForm {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final String SERVER_SESSION = "session.minecraft.net";
    private static final String SERVER_LOGIN = "login.minecraft.net";
    private final Launcher minecraftLauncher;
    private final JLabel sessionStatus = new JLabel("???");
    private final JLabel loginStatus = new JLabel("???");
    private final Gson gson = (new GsonBuilder()).registerTypeAdapterFactory(new LowerCaseEnumTypeAdapterFactory()).create();

    public StatusPanelForm(Launcher minecraftLauncher) {
        this.minecraftLauncher = minecraftLauncher;
        this.createInterface();
        this.refreshStatuses();
    }

    protected void populateGrid(GridBagConstraints constraints) {
        this.add(new JLabel("Multiplayer:", 2), constraints, 0, 0, 0, 1, 17);
        this.add(this.sessionStatus, constraints, 1, 0, 1, 1);
        this.add(new JLabel("Login:", 2), constraints, 0, 1, 0, 1, 17);
        this.add(this.loginStatus, constraints, 1, 1, 1, 1);
    }

    public JLabel getSessionStatus() {
        return this.sessionStatus;
    }

    public JLabel getLoginStatus() {
        return this.loginStatus;
    }

    public void refreshStatuses() {
        this.minecraftLauncher.getLauncher().getVersionManager().getExecutorService().submit(new Runnable() {
            public void run() {
                try {
                    TypeToken e = new TypeToken() {
                    };
                    List statuses = (List) StatusPanelForm.this.gson.fromJson(Http.performGet(new URL("http://status.mojang.com/check"), StatusPanelForm.this.minecraftLauncher.getLauncher().getProxy()), e.getType());
                    Iterator i$ = statuses.iterator();

                    while (i$.hasNext()) {
                        Map serverStatusInformation = (Map) i$.next();
                        if (serverStatusInformation.containsKey("login.minecraft.net")) {
                            StatusPanelForm.this.loginStatus.setText(((StatusPanelForm.ServerStatus) serverStatusInformation.get("login.minecraft.net")).title);
                        } else if (serverStatusInformation.containsKey("session.minecraft.net")) {
                            StatusPanelForm.this.sessionStatus.setText(((StatusPanelForm.ServerStatus) serverStatusInformation.get("session.minecraft.net")).title);
                        }
                    }
                } catch (Exception var5) {
                    StatusPanelForm.LOGGER.error("Couldn\'t get server status", (Throwable) var5);
                }

            }
        });
    }

    public static enum ServerStatus {

        GREEN("GREEN", 0, "Online, no problems detected."),
        YELLOW("YELLOW", 1, "May be experiencing issues."),
        RED("RED", 2, "Offline, experiencing problems.");
        private final String title;
// $FF: synthetic field
        private static final StatusPanelForm.ServerStatus[] $VALUES = new StatusPanelForm.ServerStatus[]{GREEN, YELLOW, RED};

        private ServerStatus(String var1, int var2, String title) {
            this.title = title;
        }

    }
}
