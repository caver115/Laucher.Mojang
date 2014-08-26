package com.mojang.launcher;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Http {

    private static final Logger LOGGER = LogManager.getLogger();

    public static String buildQuery(Map<String, Object> query) {
        StringBuilder builder = new StringBuilder();
        Iterator i$ = query.entrySet().iterator();

        while (i$.hasNext()) {
            Entry entry = (Entry) i$.next();
            if (builder.length() > 0) {
                builder.append('&');
            }

            try {
                builder.append(URLEncoder.encode((String) entry.getKey(), "UTF-8"));
            } catch (UnsupportedEncodingException var6) {
                LOGGER.error("Unexpected exception building query", (Throwable) var6);
            }

            if (entry.getValue() != null) {
                builder.append('=');

                try {
                    builder.append(URLEncoder.encode(entry.getValue().toString(), "UTF-8"));
                } catch (UnsupportedEncodingException var5) {
                    LOGGER.error("Unexpected exception building query", (Throwable) var5);
                }
            }
        }

        return builder.toString();
    }

    public static String performGet(URL url, Proxy proxy) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection(proxy);
        connection.setConnectTimeout(15000);
        connection.setReadTimeout('\uea60');
        connection.setRequestMethod("GET");
        InputStream inputStream = connection.getInputStream();

        String var4;
        try {
            var4 = IOUtils.toString(inputStream);
        } finally {
            IOUtils.closeQuietly(inputStream);
        }

        return var4;
    }

}
