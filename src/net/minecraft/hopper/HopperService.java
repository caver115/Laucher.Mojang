package net.minecraft.hopper;

import com.google.gson.Gson;
import java.io.IOException;
import java.net.Proxy;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public final class HopperService {

    private static final String BASE_URL = "http://hopper.minecraft.net/crashes/";
    private static final URL ROUTE_SUBMIT = Util.constantURL("http://hopper.minecraft.net/crashes/submit_report/");
    private static final URL ROUTE_PUBLISH = Util.constantURL("http://hopper.minecraft.net/crashes/publish_report/");
    private static final String[] INTERESTING_SYSTEM_PROPERTY_KEYS = new String[]{"os.version", "os.name", "os.arch", "java.version", "java.vendor", "sun.arch.data.model"};
    private static final Gson GSON = new Gson();

    public static SubmitResponse submitReport(Proxy proxy, String report, String product, String version) throws IOException {
        return submitReport(proxy, report, product, version, (Map) null);
    }

    public static SubmitResponse submitReport(Proxy proxy, String report, String product, String version, Map<String, String> env) throws IOException {
        HashMap environment = new HashMap();
        if (env != null) {
            environment.putAll(env);
        }

        String[] request = INTERESTING_SYSTEM_PROPERTY_KEYS;
        int len$ = request.length;

        for (int i$ = 0; i$ < len$; ++i$) {
            String key = request[i$];
            String value = System.getProperty(key);
            if (value != null) {
                environment.put(key, value);
            }
        }

        SubmitRequest var11 = new SubmitRequest(report, product, version, environment);
        return (SubmitResponse) makeRequest(proxy, ROUTE_SUBMIT, var11, SubmitResponse.class);
    }

    public static PublishResponse publishReport(Proxy proxy, Report report) throws IOException {
        PublishRequest request = new PublishRequest(report);
        return (PublishResponse) makeRequest(proxy, ROUTE_PUBLISH, request, PublishResponse.class);
    }

    private static <T extends Response> Response makeRequest(Proxy proxy, URL url, Object input, Class<T> classOfT) throws IOException {
        String jsonResult = Util.performPost(url, GSON.toJson(input), proxy, "application/json", true);
        Response result = (Response) GSON.fromJson(jsonResult, classOfT);
        if (result == null) {
            return null;
        } else if (result.getError() != null) {
            throw new IOException(result.getError());
        } else {
            return result;
        }
    }

}
