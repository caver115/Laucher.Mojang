package net.minecraft.launcher.ui.bottombar;

import com.google.common.collect.Lists;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicComboBoxRenderer;
import net.minecraft.launcher.Launcher;
import net.minecraft.launcher.profile.Profile;
import net.minecraft.launcher.profile.ProfileManager;
import net.minecraft.launcher.profile.RefreshedProfilesListener;
import net.minecraft.launcher.ui.popups.profile.ProfileEditorPopup;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ProfileSelectionPanel extends JPanel implements ActionListener, ItemListener, RefreshedProfilesListener {

    private static final Logger LOGGER = LogManager.getLogger();
    private final JComboBox profileList = new JComboBox();
    private final JButton newProfileButton = new JButton("New Profile");
    private final JButton editProfileButton = new JButton("Edit Profile");
    private final Launcher minecraftLauncher;
    private boolean skipSelectionUpdate;

    public ProfileSelectionPanel(Launcher minecraftLauncher) {
        this.minecraftLauncher = minecraftLauncher;
        this.profileList.setRenderer(new ProfileSelectionPanel.ProfileListRenderer(null));
        this.profileList.addItemListener(this);
        this.profileList.addItem("Loading profiles...");
        this.newProfileButton.addActionListener(this);
        this.editProfileButton.addActionListener(this);
        this.createInterface();
        minecraftLauncher.getProfileManager().addRefreshedProfilesListener(this);
    }

    protected void createInterface() {
        this.setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = 2;
        constraints.weightx = 0.0D;
        constraints.gridy = 0;
        this.add(new JLabel("Profile: "), constraints);
        constraints.gridx = 1;
        this.add(this.profileList, constraints);
        constraints.gridx = 0;
        ++constraints.gridy;
        JPanel buttonPanel = new JPanel(new GridLayout(1, 2));
        buttonPanel.setBorder(new EmptyBorder(2, 0, 0, 0));
        buttonPanel.add(this.newProfileButton);
        buttonPanel.add(this.editProfileButton);
        constraints.gridwidth = 2;
        this.add(buttonPanel, constraints);
        constraints.gridwidth = 1;
        ++constraints.gridy;
    }

    public void onProfilesRefreshed(ProfileManager manager) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                ProfileSelectionPanel.this.populateProfiles();
            }
        });
    }

    public void populateProfiles() {
        String previous = this.minecraftLauncher.getProfileManager().getSelectedProfile().getName();
        Profile selected = null;
        ArrayList profiles = Lists.newArrayList((Iterable) this.minecraftLauncher.getProfileManager().getProfiles().values());
        this.profileList.removeAllItems();
        Collections.sort(profiles);
        this.skipSelectionUpdate = true;

        Profile profile;
        for (Iterator i$ = profiles.iterator(); i$.hasNext(); this.profileList.addItem(profile)) {
            profile = (Profile) i$.next();
            if (previous.equals(profile.getName())) {
                selected = profile;
            }
        }

        if (selected == null) {
            if (profiles.isEmpty()) {
                selected = this.minecraftLauncher.getProfileManager().getSelectedProfile();
                this.profileList.addItem(selected);
            }

            selected = (Profile) profiles.iterator().next();
        }

        this.profileList.setSelectedItem(selected);
        this.skipSelectionUpdate = false;
    }

    public void itemStateChanged(ItemEvent e) {
        if (e.getStateChange() == 1) {
            if (!this.skipSelectionUpdate && e.getItem() instanceof Profile) {
                Profile profile = (Profile) e.getItem();
                this.minecraftLauncher.getProfileManager().setSelectedProfile(profile.getName());

                try {
                    this.minecraftLauncher.getProfileManager().saveProfiles();
                } catch (IOException var4) {
                    LOGGER.error("Couldn\'t save new selected profile", (Throwable) var4);
                }

                this.minecraftLauncher.ensureLoggedIn();
            }

        }
    }

    public void actionPerformed(ActionEvent e) {
        Profile profile;
        if (e.getSource() == this.newProfileButton) {
            profile = new Profile(this.minecraftLauncher.getProfileManager().getSelectedProfile());
            profile.setName("Copy of " + profile.getName());

            while (this.minecraftLauncher.getProfileManager().getProfiles().containsKey(profile.getName())) {
                profile.setName(profile.getName() + "_");
            }

            ProfileEditorPopup.showEditProfileDialog(this.getMinecraftLauncher(), profile);
            this.minecraftLauncher.getProfileManager().setSelectedProfile(profile.getName());
        } else if (e.getSource() == this.editProfileButton) {
            profile = this.minecraftLauncher.getProfileManager().getSelectedProfile();
            ProfileEditorPopup.showEditProfileDialog(this.getMinecraftLauncher(), profile);
        }

    }

    public Launcher getMinecraftLauncher() {
        return this.minecraftLauncher;
    }

    private static class ProfileListRenderer extends BasicComboBoxRenderer {

        private ProfileListRenderer() {
        }

        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            if (value instanceof Profile) {
                value = ((Profile) value).getName();
            }

            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            return this;
        }

        // $FF: synthetic method
        ProfileListRenderer(Object x0) {
            this();
        }
    }
}
