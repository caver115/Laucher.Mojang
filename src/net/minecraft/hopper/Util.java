package net.minecraft.hopper;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.nio.charset.Charset;
import org.apache.commons.io.IOUtils;

public class Util {

    public static String performPost(URL url, String parameters, Proxy proxy, String contentType, boolean returnErrorPage) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection(proxy);
        byte[] paramAsBytes = parameters.getBytes(Charset.forName("UTF-8"));
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(15000);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", contentType + "; charset=utf-8");
        connection.setRequestProperty("Content-Length", "" + paramAsBytes.length);
        connection.setRequestProperty("Content-Language", "en-US");
        connection.setUseCaches(false);
        connection.setDoInput(true);
        connection.setDoOutput(true);
        DataOutputStream writer = new DataOutputStream(connection.getOutputStream());
        writer.write(paramAsBytes);
        writer.flush();
        writer.close();
        InputStream stream = null;

        try {
            stream = connection.getInputStream();
        } catch (IOException var10) {
            if (!returnErrorPage) {
                throw var10;
            }

            stream = connection.getErrorStream();
            if (stream == null) {
                throw var10;
            }
        }

        return IOUtils.toString(stream);
    }

    public static URL constantURL(String input) {
        try {
            return new URL(input);
        } catch (MalformedURLException var2) {
            throw new Error(var2);
        }
    }
}
