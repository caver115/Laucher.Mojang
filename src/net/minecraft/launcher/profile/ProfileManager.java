package net.minecraft.launcher.profile;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import com.mojang.launcher.updater.DateTypeAdapter;
import com.mojang.launcher.updater.FileTypeAdapter;
import com.mojang.launcher.updater.LowerCaseEnumTypeAdapterFactory;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.launcher.Launcher;
import org.apache.commons.io.FileUtils;

public class ProfileManager {

    public static final String DEFAULT_PROFILE_NAME = "(Default)";
    private final Launcher launcher;
    private final JsonParser parser = new JsonParser();
    private final Gson gson;
    private final Map<String, Profile> profiles = new HashMap();
    private final File profileFile;
    private final List<RefreshedProfilesListener> refreshedProfilesListeners = Collections.synchronizedList(new ArrayList());
    private final List<UserChangedListener> userChangedListeners = Collections.synchronizedList(new ArrayList());
    private String selectedProfile;
    private String selectedUser;
    private AuthenticationDatabase authDatabase;

    public ProfileManager(Launcher launcher) {
        this.launcher = launcher;
        this.profileFile = new File(launcher.getLauncher().getWorkingDirectory(), "launcher_profiles.json");
        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapterFactory(new LowerCaseEnumTypeAdapterFactory());
        builder.registerTypeAdapter(Date.class, new DateTypeAdapter());
        builder.registerTypeAdapter(File.class, new FileTypeAdapter());
        builder.registerTypeAdapter(AuthenticationDatabase.class, new AuthenticationDatabase.Serializer(launcher));
        builder.registerTypeAdapter(ProfileManager.RawProfileList.class, new ProfileManager.RawProfileList.Serializer(launcher));
        builder.setPrettyPrinting();
        this.gson = builder.create();
        this.authDatabase = new AuthenticationDatabase(new YggdrasilAuthenticationService(launcher.getLauncher().getProxy(), launcher.getClientToken().toString()));
    }

    public void saveProfiles() throws IOException {
        ProfileManager.RawProfileList rawProfileList = new ProfileManager.RawProfileList(this.profiles, this.getSelectedProfile().getName(), this.selectedUser, this.launcher.getClientToken(), this.authDatabase, (ProfileManager.NamelessClass2121708221) null);
        FileUtils.writeStringToFile(this.profileFile, this.gson.toJson((Object) rawProfileList));
    }

    public boolean loadProfiles() throws IOException {
        this.profiles.clear();
        this.selectedProfile = null;
        this.selectedUser = null;
        if (this.profileFile.isFile()) {
            JsonObject object = this.parser.parse(FileUtils.readFileToString(this.profileFile)).getAsJsonObject();
            if (object.has("clientToken")) {
                this.launcher.setClientToken((UUID) this.gson.fromJson(object.get("clientToken"), UUID.class));
            }

            ProfileManager.RawProfileList rawProfileList = (ProfileManager.RawProfileList) this.gson.fromJson((JsonElement) object, ProfileManager.RawProfileList.class);
            this.profiles.putAll(rawProfileList.profiles);
            this.selectedProfile = rawProfileList.selectedProfile;
            this.selectedUser = rawProfileList.selectedUser;
            this.authDatabase = rawProfileList.authenticationDatabase;
            this.fireRefreshEvent();
            this.fireUserChangedEvent();
            return true;
        } else {
            this.fireRefreshEvent();
            this.fireUserChangedEvent();
            return false;
        }
    }

    public void fireRefreshEvent() {
        Iterator i$ = Lists.newArrayList((Iterable) this.refreshedProfilesListeners).iterator();

        while (i$.hasNext()) {
            RefreshedProfilesListener listener = (RefreshedProfilesListener) i$.next();
            listener.onProfilesRefreshed(this);
        }

    }

    public void fireUserChangedEvent() {
        Iterator i$ = Lists.newArrayList((Iterable) this.userChangedListeners).iterator();

        while (i$.hasNext()) {
            UserChangedListener listener = (UserChangedListener) i$.next();
            listener.onUserChanged(this);
        }

    }

    public Profile getSelectedProfile() {
        if (this.selectedProfile == null || !this.profiles.containsKey(this.selectedProfile)) {
            if (this.profiles.get("(Default)") != null) {
                this.selectedProfile = "(Default)";
            } else if (this.profiles.size() > 0) {
                this.selectedProfile = ((Profile) this.profiles.values().iterator().next()).getName();
            } else {
                this.selectedProfile = "(Default)";
                this.profiles.put("(Default)", new Profile(this.selectedProfile));
            }
        }

        return (Profile) this.profiles.get(this.selectedProfile);
    }

    public Map<String, Profile> getProfiles() {
        return this.profiles;
    }

    public void addRefreshedProfilesListener(RefreshedProfilesListener listener) {
        this.refreshedProfilesListeners.add(listener);
    }

    public void addUserChangedListener(UserChangedListener listener) {
        this.userChangedListeners.add(listener);
    }

    public void setSelectedProfile(String selectedProfile) {
        boolean update = !this.selectedProfile.equals(selectedProfile);
        this.selectedProfile = selectedProfile;
        if (update) {
            this.fireRefreshEvent();
        }

    }

    public String getSelectedUser() {
        return this.selectedUser;
    }

    public void setSelectedUser(String selectedUser) {
        boolean update = !Objects.equal(this.selectedUser, selectedUser);
        if (update) {
            this.selectedUser = selectedUser;
            this.fireUserChangedEvent();
        }

    }

    public AuthenticationDatabase getAuthDatabase() {
        return this.authDatabase;
    }

    private static class RawProfileList {

        public Map<String, Profile> profiles;
        public String selectedProfile;
        public String selectedUser;
        public UUID clientToken;
        public AuthenticationDatabase authenticationDatabase;

        private RawProfileList(Map<String, Profile> profiles, String selectedProfile, String selectedUser, UUID clientToken, AuthenticationDatabase authenticationDatabase) {
            this.profiles = new HashMap();
            this.clientToken = UUID.randomUUID();
            this.profiles = profiles;
            this.selectedProfile = selectedProfile;
            this.selectedUser = selectedUser;
            this.clientToken = clientToken;
            this.authenticationDatabase = authenticationDatabase;
        }

        // $FF: synthetic method
        RawProfileList(Map x0, String x1, String x2, UUID x3, AuthenticationDatabase x4, ProfileManager.NamelessClass2121708221 x5) {
            this(x0, x1, x2, x3, x4);
        }

        public static class Serializer implements JsonDeserializer<ProfileManager.RawProfileList>, JsonSerializer<ProfileManager.RawProfileList> {

            private final Launcher launcher;

            public Serializer(Launcher launcher) {
                this.launcher = launcher;
            }

            public ProfileManager.RawProfileList deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
                JsonObject object = (JsonObject) json;
                Map<String, Profile> profiles = Maps.newHashMap();
                if (object.has("profiles")) {
                    profiles = (Map) context.deserialize(object.get("profiles"), (new TypeToken<Map<String, Profile>>() {
                    }).getType());
                }

                String selectedProfile = null;
                if (object.has("selectedProfile")) {
                    selectedProfile = object.getAsJsonPrimitive("selectedProfile").getAsString();
                }

                UUID clientToken = UUID.randomUUID();
                if (object.has("clientToken")) {
                    clientToken = (UUID) context.deserialize(object.get("clientToken"), UUID.class);
                }

                AuthenticationDatabase database = new AuthenticationDatabase(new YggdrasilAuthenticationService(this.launcher.getLauncher().getProxy(), this.launcher.getClientToken().toString()));
                if (object.has("authenticationDatabase")) {
                    database = (AuthenticationDatabase) context.deserialize(object.get("authenticationDatabase"), AuthenticationDatabase.class);
                }

                String selectedUser = null;
                if (object.has("selectedUser")) {
                    selectedUser = object.getAsJsonPrimitive("selectedUser").getAsString();
                } else if (selectedProfile != null && ((Map) profiles).containsKey(selectedProfile) && ((Profile) ((Map) profiles).get(selectedProfile)).getPlayerUUID() != null) {
                    selectedUser = ((Profile) ((Map) profiles).get(selectedProfile)).getPlayerUUID();
                } else if (!database.getknownUUIDs().isEmpty()) {
                    selectedUser = (String) database.getknownUUIDs().iterator().next();
                }

                Iterator i$ = ((Map<String, Profile>) profiles).values().iterator();

                while (i$.hasNext()) {
                    Profile profile = (Profile) i$.next();
                    profile.setPlayerUUID((String) null);
                }

                return new ProfileManager.RawProfileList((Map) profiles, selectedProfile, selectedUser, clientToken, database, (ProfileManager.NamelessClass2121708221) null);
            }

            public JsonElement serialize(ProfileManager.RawProfileList src, Type typeOfSrc, JsonSerializationContext context) {
                JsonObject version = new JsonObject();
                version.addProperty("name", "1.5.3");
                version.addProperty("format", (Number) Integer.valueOf(17));
                JsonObject object = new JsonObject();
                object.add("profiles", context.serialize(src.profiles));
                object.add("selectedProfile", context.serialize(src.selectedProfile));
                object.add("clientToken", context.serialize(src.clientToken));
                object.add("authenticationDatabase", context.serialize(src.authenticationDatabase));
                object.add("selectedUser", context.serialize(src.selectedUser));
                object.add("launcherVersion", version);
                return object;
            }

        }
    }

    // $FF: synthetic class
    static class NamelessClass2121708221 {
    }
}
