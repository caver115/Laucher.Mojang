package com.mojang.authlib.yggdrasil;

import com.google.common.collect.Iterables;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.HttpAuthenticationService;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.exceptions.AuthenticationUnavailableException;
import com.mojang.authlib.minecraft.HttpMinecraftSessionService;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.yggdrasil.request.JoinMinecraftServerRequest;
import com.mojang.authlib.yggdrasil.response.HasJoinedMinecraftServerResponse;
import com.mojang.authlib.yggdrasil.response.MinecraftProfilePropertiesResponse;
import com.mojang.authlib.yggdrasil.response.MinecraftTexturesPayload;
import com.mojang.authlib.yggdrasil.response.Response;
import com.mojang.util.UUIDTypeAdapter;
import java.net.URL;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.apache.commons.codec.Charsets;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class YggdrasilMinecraftSessionService extends HttpMinecraftSessionService {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final String BASE_URL = "https://sessionserver.mojang.com/session/minecraft/";
    private static final URL JOIN_URL = HttpAuthenticationService.constantURL("https://sessionserver.mojang.com/session/minecraft/join");
    private static final URL CHECK_URL = HttpAuthenticationService.constantURL("https://sessionserver.mojang.com/session/minecraft/hasJoined");
    private final PublicKey publicKey;
    private final Gson gson = (new GsonBuilder()).registerTypeAdapter(UUID.class, new UUIDTypeAdapter()).create();

    protected YggdrasilMinecraftSessionService(YggdrasilAuthenticationService authenticationService) {
        super(authenticationService);

        try {
            X509EncodedKeySpec e = new X509EncodedKeySpec(IOUtils.toByteArray(YggdrasilMinecraftSessionService.class.getResourceAsStream("/yggdrasil_session_pubkey.der")));
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            this.publicKey = keyFactory.generatePublic(e);
        } catch (Exception var4) {
            throw new Error("Missing/invalid yggdrasil public key!");
        }
    }

    public void joinServer(GameProfile profile, String authenticationToken, String serverId) throws AuthenticationException {
        JoinMinecraftServerRequest request = new JoinMinecraftServerRequest();
        request.accessToken = authenticationToken;
        request.selectedProfile = profile.getId();
        request.serverId = serverId;
        this.getAuthenticationService().makeRequest(JOIN_URL, request, Response.class);
    }

    public GameProfile hasJoinedServer(GameProfile user, String serverId) throws AuthenticationUnavailableException {
        HashMap arguments = new HashMap();
        arguments.put("username", user.getName());
        arguments.put("serverId", serverId);
        URL url = HttpAuthenticationService.concatenateURL(CHECK_URL, HttpAuthenticationService.buildQuery(arguments));

        try {
            HasJoinedMinecraftServerResponse e = (HasJoinedMinecraftServerResponse) this.getAuthenticationService().makeRequest(url, (Object) null, HasJoinedMinecraftServerResponse.class);
            if (e != null && e.getId() != null) {
                GameProfile result = new GameProfile(e.getId(), user.getName());
                if (e.getProperties() != null) {
                    result.getProperties().putAll(e.getProperties());
                }

                return result;
            } else {
                return null;
            }
        } catch (AuthenticationUnavailableException var7) {
            throw var7;
        } catch (AuthenticationException var8) {
            return null;
        }
    }

    public Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> getTextures(GameProfile profile, boolean requireSecure) {
        Property textureProperty = (Property) Iterables.getFirst(profile.getProperties().get("textures"), (Object) null);
        if (textureProperty == null) {
            return new HashMap();
        } else if (!textureProperty.hasSignature()) {
            LOGGER.error("Signature is missing from textures payload");
            return new HashMap();
        } else if (!textureProperty.isSignatureValid(this.publicKey)) {
            LOGGER.error("Textures payload has been tampered with (signature invalid)");
            return new HashMap();
        } else {
            MinecraftTexturesPayload result;
            try {
                String limit = new String(Base64.decodeBase64(textureProperty.getValue()), Charsets.UTF_8);
                result = (MinecraftTexturesPayload) this.gson.fromJson(limit, MinecraftTexturesPayload.class);
            } catch (JsonParseException var7) {
                LOGGER.error("Could not decode textures payload", (Throwable) var7);
                return new HashMap();
            }

            if (result.getProfileId() != null && result.getProfileId().equals(profile.getId())) {
                if (result.getProfileName() != null && result.getProfileName().equals(profile.getName())) {
                    if (requireSecure) {
                        if (result.isPublic()) {
                            LOGGER.error("Decrypted textures payload was public but we require secure data");
                            return new HashMap();
                        }

                        Calendar limit1 = Calendar.getInstance();
                        limit1.add(5, -1);
                        Date validFrom = new Date(result.getTimestamp());
                        if (validFrom.before(limit1.getTime())) {
                            LOGGER.error("Decrypted textures payload is too old ({0}, but we need it to be at least {1})", new Object[]{validFrom, limit1});
                            return new HashMap();
                        }
                    }

                    return (Map) (result.getTextures() == null ? new HashMap() : result.getTextures());
                } else {
                    LOGGER.error("Decrypted textures payload was for another user (expected name {} but was for {})", new Object[]{profile.getName(), result.getProfileName()});
                    return new HashMap();
                }
            } else {
                LOGGER.error("Decrypted textures payload was for another user (expected id {} but was for {})", new Object[]{profile.getId(), result.getProfileId()});
                return new HashMap();
            }
        }
    }

    public GameProfile fillProfileProperties(GameProfile profile) {
        if (profile.getId() == null) {
            return profile;
        } else {
            try {
                URL e = HttpAuthenticationService.constantURL("https://sessionserver.mojang.com/session/minecraft/profile/" + UUIDTypeAdapter.fromUUID(profile.getId()));
                MinecraftProfilePropertiesResponse response = (MinecraftProfilePropertiesResponse) this.getAuthenticationService().makeRequest(e, (Object) null, MinecraftProfilePropertiesResponse.class);
                if (response == null) {
                    LOGGER.debug("Couldn\'t fetch profile properties for " + profile + " as the profile does not exist");
                    return profile;
                } else {
                    LOGGER.debug("Successfully fetched profile properties for " + profile);
                    GameProfile result = new GameProfile(response.getId(), response.getName());
                    result.getProperties().putAll(response.getProperties());
                    profile.getProperties().putAll(response.getProperties());
                    return result;
                }
            } catch (AuthenticationException var5) {
                LOGGER.warn("Couldn\'t look up profile properties for " + profile, (Throwable) var5);
                return profile;
            }
        }
    }

    public YggdrasilAuthenticationService getAuthenticationService() {
        return (YggdrasilAuthenticationService) super.getAuthenticationService();
    }

}
