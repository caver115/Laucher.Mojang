package com.mojang.authlib.yggdrasil;

import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.mojang.authlib.Agent;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.HttpAuthenticationService;
import com.mojang.authlib.ProfileLookupCallback;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.yggdrasil.response.ProfileSearchResultsResponse;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class YggdrasilGameProfileRepository implements GameProfileRepository {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final String BASE_URL = "https://api.mojang.com/";
    private static final String SEARCH_PAGE_URL = "https://api.mojang.com/profiles/";
    private static final int ENTRIES_PER_PAGE = 2;
    private static final int MAX_FAIL_COUNT = 3;
    private static final int DELAY_BETWEEN_PAGES = 100;
    private static final int DELAY_BETWEEN_FAILURES = 750;
    private final YggdrasilAuthenticationService authenticationService;

    public YggdrasilGameProfileRepository(YggdrasilAuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    public void findProfilesByNames(String[] names, Agent agent, ProfileLookupCallback callback) {
        HashSet criteria = Sets.newHashSet();
        String[] page = names;
        int i$ = names.length;

        for (int request = 0; request < i$; ++request) {
            String failCount = page[request];
            if (!Strings.isNullOrEmpty(failCount)) {
                criteria.add(failCount.toLowerCase());
            }
        }

        byte var19 = 0;
        Iterator var20 = Iterables.partition(criteria, 2).iterator();

        while (var20.hasNext()) {
            List var21 = (List) var20.next();
            int var22 = 0;

            while (true) {
                boolean failed = false;

                try {
                    ProfileSearchResultsResponse var24 = (ProfileSearchResultsResponse) this.authenticationService.makeRequest(HttpAuthenticationService.constantURL("https://api.mojang.com/profiles/" + agent.getName().toLowerCase()), var21, ProfileSearchResultsResponse.class);
                    var22 = 0;
                    LOGGER.debug("Page {} returned {} results, parsing", new Object[]{Integer.valueOf(var19), Integer.valueOf(var24.getProfiles().length)});
                    HashSet var23 = Sets.newHashSet((Iterable) var21);
                    GameProfile[] var25 = var24.getProfiles();
                    int name1 = var25.length;

                    for (int i$1 = 0; i$1 < name1; ++i$1) {
                        GameProfile profile = var25[i$1];
                        LOGGER.debug("Successfully looked up profile {}", new Object[]{profile});
                        var23.remove(profile.getName().toLowerCase());
                        callback.onProfileLookupSucceeded(profile);
                    }

                    Iterator var26 = var23.iterator();

                    while (var26.hasNext()) {
                        String var27 = (String) var26.next();
                        LOGGER.debug("Couldn\'t find profile {}", new Object[]{var27});
                        callback.onProfileLookupFailed(new GameProfile((UUID) null, var27), new ProfileNotFoundException("Server did not find the requested profile"));
                    }

                    try {
                        Thread.sleep(100L);
                    } catch (InterruptedException var17) {
                        ;
                    }
                } catch (AuthenticationException var18) {
                    AuthenticationException e = var18;
                    ++var22;
                    if (var22 == 3) {
                        Iterator ignored = var21.iterator();

                        while (ignored.hasNext()) {
                            String name = (String) ignored.next();
                            LOGGER.debug("Couldn\'t find profile {} because of a server error", new Object[]{name});
                            callback.onProfileLookupFailed(new GameProfile((UUID) null, name), e);
                        }
                    } else {
                        try {
                            Thread.sleep(750L);
                        } catch (InterruptedException var16) {
                            ;
                        }

                        failed = true;
                    }
                }

                if (!failed) {
                    break;
                }
            }
        }

    }

}
