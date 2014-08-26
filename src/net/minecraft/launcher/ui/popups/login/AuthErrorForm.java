package net.minecraft.launcher.ui.popups.login;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mojang.launcher.Http;
import com.mojang.launcher.updater.LowerCaseEnumTypeAdapterFactory;
import java.net.URL;
import java.util.Map;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import org.apache.commons.lang3.exception.ExceptionUtils;

public class AuthErrorForm extends JPanel {

    private final LogInPopup popup;
    private final JLabel errorLabel = new JLabel();
    private final Gson gson = (new GsonBuilder()).registerTypeAdapterFactory(new LowerCaseEnumTypeAdapterFactory()).create();

    public AuthErrorForm(LogInPopup popup) {
        this.popup = popup;
        this.createInterface();
        this.clear();
    }

    protected void createInterface() {
        this.setBorder(new EmptyBorder(0, 0, 15, 0));
        this.errorLabel.setFont(this.errorLabel.getFont().deriveFont(1));
        this.add(this.errorLabel);
    }

    public void clear() {
        this.setVisible(false);
    }

    public void setVisible(boolean value) {
        super.setVisible(value);
        this.popup.repack();
    }

    public void displayError(final Throwable throwable, final String... lines) {
        if (SwingUtilities.isEventDispatchThread()) {
            String error = "";
            String[] arr$ = lines;
            int len$ = lines.length;

            for (int i$ = 0; i$ < len$; ++i$) {
                String line = arr$[i$];
                error = error + "<p>" + line + "</p>";
            }

            if (throwable != null) {
                error = error + "<p style=\'font-size: 0.9em; font-style: italic;\'>(" + ExceptionUtils.getRootCauseMessage(throwable) + ")</p>";
            }

            this.errorLabel.setText("<html><div style=\'text-align: center;\'>" + error + " </div></html>");
            if (!this.isVisible()) {
                this.refreshStatuses();
            }

            this.setVisible(true);
        } else {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    AuthErrorForm.this.displayError(throwable, lines);
                }
            });
        }

    }

    public void refreshStatuses() {
        this.popup.getMinecraftLauncher().getLauncher().getVersionManager().getExecutorService().submit(new Runnable() {
            public void run() {
                try {
                    TypeToken token = new TypeToken() {
                    };
                    Map statuses = (Map) AuthErrorForm.this.gson.fromJson(Http.performGet(new URL("http://status.mojang.com/check?service=authserver.mojang.com"), AuthErrorForm.this.popup.getMinecraftLauncher().getLauncher().getProxy()), token.getType());
                    if (statuses.get("authserver.mojang.com") == AuthErrorForm.ServerStatus.RED) {
                        AuthErrorForm.this.displayError((Throwable) null, new String[]{"It looks like our servers are down right now. Sorry!", "We\'re already working on the problem and will have it fixed soon.", "Please try again later!"});
                    }
                } catch (Exception var3) {
                    ;
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
        private static final AuthErrorForm.ServerStatus[] $VALUES = new AuthErrorForm.ServerStatus[]{GREEN, YELLOW, RED};

        private ServerStatus(String var1, int var2, String title) {
            this.title = title;
        }

    }
}
