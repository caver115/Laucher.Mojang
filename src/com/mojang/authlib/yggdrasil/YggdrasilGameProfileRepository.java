package com.mojang.authlib.yggdrasil;

import com.google.common.base.Strings;
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
import java.util.UUID;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class YggdrasilGameProfileRepository implements GameProfileRepository {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final String BASE_URL = "https://api.mojang.com/";
    private static final String SEARCH_PAGE_URL = "https://api.mojang.com/profiles/page/";
    private static final int MAX_FAIL_COUNT = 3;
    private static final int DELAY_BETWEEN_PAGES = 100;
    private static final int DELAY_BETWEEN_FAILURES = 750;
    private final YggdrasilAuthenticationService authenticationService;

    public YggdrasilGameProfileRepository(YggdrasilAuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    public void findProfilesByNames(String[] names, Agent agent, ProfileLookupCallback callback) {
        HashSet criteria = Sets.newHashSet();
        String[] exception = names;
        int request = names.length;

        int page;
        for (page = 0; page < request; ++page) {
            String failCount = exception[page];
            if (!Strings.isNullOrEmpty(failCount)) {
                criteria.add(new YggdrasilGameProfileRepository.ProfileCriteria(failCount, agent, (YggdrasilGameProfileRepository.NamelessClass1013858042) null));
            }
        }

        Object var17 = null;
        HashSet var18 = Sets.newHashSet((Iterable) criteria);
        page = 1;
        int var19 = 0;

        while (!criteria.isEmpty()) {
            try {
                ProfileSearchResultsResponse i$ = (ProfileSearchResultsResponse) this.authenticationService.makeRequest(HttpAuthenticationService.constantURL("https://api.mojang.com/profiles/page/" + page), var18, ProfileSearchResultsResponse.class);
                var19 = 0;
                var17 = null;
                if (i$.getSize() == 0 || i$.getProfiles().length == 0) {
                    LOGGER.debug("Page {} returned empty, aborting search", new Object[]{Integer.valueOf(page)});
                    break;
                }

                LOGGER.debug("Page {} returned {} results of {}, parsing", new Object[]{Integer.valueOf(page), Integer.valueOf(i$.getProfiles().length), Integer.valueOf(i$.getSize())});
                GameProfile[] profileCriteria = i$.getProfiles();
                int len$ = profileCriteria.length;

                for (int i$1 = 0; i$1 < len$; ++i$1) {
                    GameProfile profile = profileCriteria[i$1];
                    LOGGER.debug("Successfully looked up profile {}", new Object[]{profile});
                    criteria.remove(new YggdrasilGameProfileRepository.ProfileCriteria(profile.getName(), agent, (YggdrasilGameProfileRepository.NamelessClass1013858042) null));
                    callback.onProfileLookupSucceeded(profile);
                }

                LOGGER.debug("Page {} successfully parsed", new Object[]{Integer.valueOf(page)});
                ++page;

                try {
                    Thread.sleep(100L);
                } catch (InterruptedException var15) {
                    ;
                }
            } catch (AuthenticationException var16) {
                var17 = var16;
                ++var19;
                if (var19 == 3) {
                    break;
                }

                try {
                    Thread.sleep(750L);
                } catch (InterruptedException var14) {
                    ;
                }
            }
        }

        if (criteria.isEmpty()) {
            LOGGER.debug("Successfully found every profile requested");
        } else {
            LOGGER.debug("{} profiles were missing from search results", new Object[]{Integer.valueOf(criteria.size())});
            if (var17 == null) {
                var17 = new ProfileNotFoundException("Server did not find the requested profile");
            }

            Iterator var20 = criteria.iterator();

            while (var20.hasNext()) {
                YggdrasilGameProfileRepository.ProfileCriteria var21 = (YggdrasilGameProfileRepository.ProfileCriteria) var20.next();
                callback.onProfileLookupFailed(new GameProfile((UUID) null, var21.getName()), (Exception) var17);
            }
        }

    }

    private class ProfileCriteria {

        private final String name;
        private final String agent;

        private ProfileCriteria(String name, Agent agent) {
            this.name = name;
            this.agent = agent.getName();
        }

        public String getName() {
            return this.name;
        }

        public String getAgent() {
            return this.agent;
        }

        public boolean equals(Object o) {
            if (this == o) {
                return true;
            } else if (o != null && this.getClass() == o.getClass()) {
                YggdrasilGameProfileRepository.ProfileCriteria that = (YggdrasilGameProfileRepository.ProfileCriteria) o;
                return this.agent.equals(that.agent) && this.name.toLowerCase().equals(that.name.toLowerCase());
            } else {
                return false;
            }
        }

        public int hashCode() {
            return 31 * this.name.toLowerCase().hashCode() + this.agent.hashCode();
        }

        public String toString() {
            return (new ToStringBuilder(this)).append("agent", (Object) this.agent).append("name", (Object) this.name).toString();
        }

// $FF: synthetic method
        ProfileCriteria(String x1, Agent x2, YggdrasilGameProfileRepository.NamelessClass1013858042 x3) {
            this(x1, x2);
        }
    }

// $FF: synthetic class
    static class NamelessClass1013858042 {
    }
}
