package net.minecraft.launcher.profile;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.mojang.authlib.AuthenticationService;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.UserAuthentication;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import net.minecraft.launcher.Launcher;

public class AuthenticationDatabase {

    public static final String DEMO_UUID_PREFIX = "demo-";
    private final Map<String, UserAuthentication> authById;
    private final AuthenticationService authenticationService;

    public AuthenticationDatabase(AuthenticationService authenticationService) {
        this(new HashMap(), authenticationService);
    }

    public AuthenticationDatabase(Map<String, UserAuthentication> authById, AuthenticationService authenticationService) {
        this.authById = authById;
        this.authenticationService = authenticationService;
    }

    public UserAuthentication getByName(String name) {
        if (name == null) {
            return null;
        } else {
            Iterator i$ = this.authById.entrySet().iterator();

            Entry entry;
            GameProfile profile;
            do {
                if (!i$.hasNext()) {
                    return null;
                }

                entry = (Entry) i$.next();
                profile = ((UserAuthentication) entry.getValue()).getSelectedProfile();
                if (profile != null && profile.getName().equals(name)) {
                    return (UserAuthentication) entry.getValue();
                }
            } while (profile != null || !getUserFromDemoUUID((String) entry.getKey()).equals(name));

            return (UserAuthentication) entry.getValue();
        }
    }

    public UserAuthentication getByUUID(String uuid) {
        return (UserAuthentication) this.authById.get(uuid);
    }

    public Collection<String> getKnownNames() {
        ArrayList names = new ArrayList();
        Iterator i$ = this.authById.entrySet().iterator();

        while (i$.hasNext()) {
            Entry entry = (Entry) i$.next();
            GameProfile profile = ((UserAuthentication) entry.getValue()).getSelectedProfile();
            if (profile != null) {
                names.add(profile.getName());
            } else {
                names.add(getUserFromDemoUUID((String) entry.getKey()));
            }
        }

        return names;
    }

    public void register(String uuid, UserAuthentication authentication) {
        this.authById.put(uuid, authentication);
    }

    public Set<String> getknownUUIDs() {
        return this.authById.keySet();
    }

    public void removeUUID(String uuid) {
        this.authById.remove(uuid);
    }

    public AuthenticationService getAuthenticationService() {
        return this.authenticationService;
    }

    public static String getUserFromDemoUUID(String uuid) {
        return uuid.startsWith("demo-") && uuid.length() > "demo-".length() ? "Demo User " + uuid.substring("demo-".length()) : "Demo User";
    }

    public static class Serializer implements JsonDeserializer<AuthenticationDatabase>, JsonSerializer<AuthenticationDatabase> {

        private final Launcher launcher;

        public Serializer(Launcher launcher) {
            this.launcher = launcher;
        }

        public AuthenticationDatabase deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            HashMap services = new HashMap();
            Map credentials = this.deserializeCredentials((JsonObject) json, context);
            YggdrasilAuthenticationService authService = new YggdrasilAuthenticationService(this.launcher.getLauncher().getProxy(), this.launcher.getClientToken().toString());
            Iterator i$ = credentials.entrySet().iterator();

            while (i$.hasNext()) {
                Entry entry = (Entry) i$.next();
                UserAuthentication auth = authService.createUserAuthentication(this.launcher.getLauncher().getAgent());
                auth.loadFromStorage((Map) entry.getValue());
                services.put(entry.getKey(), auth);
            }

            return new AuthenticationDatabase(services, authService);
        }

        protected Map<String, Map<String, Object>> deserializeCredentials(JsonObject json, JsonDeserializationContext context) {
            LinkedHashMap result = new LinkedHashMap();
            Iterator i$ = json.entrySet().iterator();

            while (i$.hasNext()) {
                Entry authEntry = (Entry) i$.next();
                LinkedHashMap credentials = new LinkedHashMap();
                Iterator i$1 = ((JsonObject) authEntry.getValue()).entrySet().iterator();

                while (i$1.hasNext()) {
                    Entry credentialsEntry = (Entry) i$1.next();
                    credentials.put(credentialsEntry.getKey(), this.deserializeCredential((JsonElement) credentialsEntry.getValue()));
                }

                result.put(authEntry.getKey(), credentials);
            }

            return result;
        }

        private Object deserializeCredential(JsonElement element) {
            Iterator i$;
            if (element instanceof JsonObject) {
                LinkedHashMap result1 = new LinkedHashMap();
                i$ = ((JsonObject) element).entrySet().iterator();

                while (i$.hasNext()) {
                    Entry entry1 = (Entry) i$.next();
                    result1.put(entry1.getKey(), this.deserializeCredential((JsonElement) entry1.getValue()));
                }

                return result1;
            } else if (!(element instanceof JsonArray)) {
                return element.getAsString();
            } else {
                ArrayList result = new ArrayList();
                i$ = ((JsonArray) element).iterator();

                while (i$.hasNext()) {
                    JsonElement entry = (JsonElement) i$.next();
                    result.add(this.deserializeCredential(entry));
                }

                return result;
            }
        }

        public JsonElement serialize(AuthenticationDatabase src, Type typeOfSrc, JsonSerializationContext context) {
            Map services = src.authById;
            HashMap credentials = new HashMap();
            Iterator i$ = services.entrySet().iterator();

            while (i$.hasNext()) {
                Entry entry = (Entry) i$.next();
                credentials.put(entry.getKey(), ((UserAuthentication) entry.getValue()).saveForStorage());
            }

            return context.serialize(credentials);
        }

    }
}
