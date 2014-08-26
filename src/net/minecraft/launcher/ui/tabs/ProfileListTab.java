package net.minecraft.launcher.ui.tabs;

import com.mojang.launcher.OperatingSystem;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.swing.Icon;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.table.AbstractTableModel;
import net.minecraft.launcher.Launcher;
import net.minecraft.launcher.LauncherConstants;
import net.minecraft.launcher.SwingUserInterface;
import net.minecraft.launcher.profile.AuthenticationDatabase;
import net.minecraft.launcher.profile.Profile;
import net.minecraft.launcher.profile.ProfileManager;
import net.minecraft.launcher.profile.RefreshedProfilesListener;
import net.minecraft.launcher.ui.popups.profile.ProfileEditorPopup;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ProfileListTab extends JScrollPane implements RefreshedProfilesListener {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final int COLUMN_NAME = 0;
    private static final int COLUMN_VERSION = 1;
    private static final int NUM_COLUMNS = 2;
    private final Launcher minecraftLauncher;
    private final ProfileListTab.ProfileTableModel dataModel = new ProfileListTab.ProfileTableModel(null);
    private final JTable table;
    private final JPopupMenu popupMenu;
    private final JMenuItem addProfileButton;
    private final JMenuItem copyProfileButton;
    private final JMenuItem deleteProfileButton;
    private final JMenuItem browseGameFolder;

    public ProfileListTab(Launcher minecraftLauncher) {
        this.table = new JTable(this.dataModel);
        this.popupMenu = new JPopupMenu();
        this.addProfileButton = new JMenuItem("Add Profile");
        this.copyProfileButton = new JMenuItem("Copy Profile");
        this.deleteProfileButton = new JMenuItem("Delete Profile");
        this.browseGameFolder = new JMenuItem("Open Game Folder");
        this.minecraftLauncher = minecraftLauncher;
        this.setViewportView(this.table);
        this.createInterface();
        minecraftLauncher.getProfileManager().addRefreshedProfilesListener(this);
    }

    protected void createInterface() {
        this.popupMenu.add(this.addProfileButton);
        this.popupMenu.add(this.copyProfileButton);
        this.popupMenu.add(this.deleteProfileButton);
        this.popupMenu.add(this.browseGameFolder);
        this.table.setFillsViewportHeight(true);
        this.table.setSelectionMode(0);
        this.popupMenu.addPopupMenuListener(new PopupMenuListener() {
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                int[] selection = ProfileListTab.this.table.getSelectedRows();
                boolean hasSelection = selection != null && selection.length > 0;
                ProfileListTab.this.copyProfileButton.setEnabled(hasSelection);
                ProfileListTab.this.deleteProfileButton.setEnabled(hasSelection);
                ProfileListTab.this.browseGameFolder.setEnabled(hasSelection);
            }

            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
            }

            public void popupMenuCanceled(PopupMenuEvent e) {
            }
        });
        this.addProfileButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Profile profile = new Profile();
                profile.setName("New Profile");

                while (ProfileListTab.this.minecraftLauncher.getProfileManager().getProfiles().containsKey(profile.getName())) {
                    profile.setName(profile.getName() + "_");
                }

                ProfileEditorPopup.showEditProfileDialog(ProfileListTab.this.getMinecraftLauncher(), profile);
            }
        });
        this.copyProfileButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int selection = ProfileListTab.this.table.getSelectedRow();
                if (selection >= 0 && selection < ProfileListTab.this.table.getRowCount()) {
                    Profile current = (Profile) ProfileListTab.this.dataModel.profiles.get(selection);
                    Profile copy = new Profile(current);
                    copy.setName("Copy of " + current.getName());

                    while (ProfileListTab.this.minecraftLauncher.getProfileManager().getProfiles().containsKey(copy.getName())) {
                        copy.setName(copy.getName() + "_");
                    }

                    ProfileEditorPopup.showEditProfileDialog(ProfileListTab.this.getMinecraftLauncher(), copy);
                }
            }
        });
        this.browseGameFolder.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int selection = ProfileListTab.this.table.getSelectedRow();
                if (selection >= 0 && selection < ProfileListTab.this.table.getRowCount()) {
                    Profile profile = (Profile) ProfileListTab.this.dataModel.profiles.get(selection);
                    OperatingSystem.openFolder(profile.getGameDir() == null ? ProfileListTab.this.minecraftLauncher.getLauncher().getWorkingDirectory() : profile.getGameDir());
                }
            }
        });
        this.deleteProfileButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int selection = ProfileListTab.this.table.getSelectedRow();
                if (selection >= 0 && selection < ProfileListTab.this.table.getRowCount()) {
                    Profile current = (Profile) ProfileListTab.this.dataModel.profiles.get(selection);
                    int result = JOptionPane.showOptionDialog(((SwingUserInterface) ProfileListTab.this.minecraftLauncher.getUserInterface()).getFrame(), "Are you sure you want to delete this profile?", "Profile Confirmation", 0, 2, (Icon) null, LauncherConstants.CONFIRM_PROFILE_DELETION_OPTIONS, LauncherConstants.CONFIRM_PROFILE_DELETION_OPTIONS[0]);
                    if (result == 0) {
                        ProfileListTab.this.minecraftLauncher.getProfileManager().getProfiles().remove(current.getName());

                        try {
                            ProfileListTab.this.minecraftLauncher.getProfileManager().saveProfiles();
                            ProfileListTab.this.minecraftLauncher.getProfileManager().fireRefreshEvent();
                        } catch (IOException var6) {
                            ProfileListTab.LOGGER.error("Couldn\'t save profiles whilst deleting \'" + current.getName() + "\'", (Throwable) var6);
                        }
                    }

                }
            }
        });
        this.table.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = ProfileListTab.this.table.getSelectedRow();
                    if (row >= 0 && row < ProfileListTab.this.dataModel.profiles.size()) {
                        ProfileEditorPopup.showEditProfileDialog(ProfileListTab.this.getMinecraftLauncher(), (Profile) ProfileListTab.this.dataModel.profiles.get(row));
                    }
                }

            }

            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger() && e.getComponent() instanceof JTable) {
                    int r = ProfileListTab.this.table.rowAtPoint(e.getPoint());
                    if (r >= 0 && r < ProfileListTab.this.table.getRowCount()) {
                        ProfileListTab.this.table.setRowSelectionInterval(r, r);
                    } else {
                        ProfileListTab.this.table.clearSelection();
                    }

                    ProfileListTab.this.popupMenu.show(e.getComponent(), e.getX(), e.getY());
                }

            }

            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger() && e.getComponent() instanceof JTable) {
                    int r = ProfileListTab.this.table.rowAtPoint(e.getPoint());
                    if (r >= 0 && r < ProfileListTab.this.table.getRowCount()) {
                        ProfileListTab.this.table.setRowSelectionInterval(r, r);
                    } else {
                        ProfileListTab.this.table.clearSelection();
                    }

                    ProfileListTab.this.popupMenu.show(e.getComponent(), e.getX(), e.getY());
                }

            }
        });
    }

    public Launcher getMinecraftLauncher() {
        return this.minecraftLauncher;
    }

    public void onProfilesRefreshed(final ProfileManager manager) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                ProfileListTab.this.dataModel.setProfiles(manager.getProfiles().values());
            }
        });
    }

    private class ProfileTableModel extends AbstractTableModel {

        private final List<Profile> profiles;

        private ProfileTableModel() {
            this.profiles = new ArrayList();
        }

        public int getRowCount() {
            return this.profiles.size();
        }

        public int getColumnCount() {
            return 2;
        }

        public Class<?> getColumnClass(int columnIndex) {
            return String.class;
        }

        public String getColumnName(int column) {
            switch (column) {
                case 0:
                    return "Version name";
                case 1:
                    return "Version";
                default:
                    return super.getColumnName(column);
            }
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            Profile profile = (Profile) this.profiles.get(rowIndex);
            AuthenticationDatabase authDatabase = ProfileListTab.this.minecraftLauncher.getProfileManager().getAuthDatabase();
            switch (columnIndex) {
                case 0:
                    return profile.getName();
                case 1:
                    if (profile.getLastVersionId() == null) {
                        return "(Latest version)";
                    }

                    return profile.getLastVersionId();
                default:
                    return null;
            }
        }

        public void setProfiles(Collection<Profile> profiles) {
            this.profiles.clear();
            this.profiles.addAll(profiles);
            Collections.sort(this.profiles);
            this.fireTableDataChanged();
        }

// $FF: synthetic method
        ProfileTableModel(Object x1) {
            this();
        }
    }
}
