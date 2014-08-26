package com.mojang.launcher;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import java.lang.reflect.Type;
import java.util.Iterator;

public class LegacyPropertyMapSerializer implements JsonSerializer<PropertyMap> {

    public JsonElement serialize(PropertyMap src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject result = new JsonObject();
        Iterator i$ = src.keySet().iterator();

        while (i$.hasNext()) {
            String key = (String) i$.next();
            JsonArray values = new JsonArray();
            Iterator i$1 = src.get(key).iterator();

            while (i$1.hasNext()) {
                Property property = (Property) i$1.next();
                values.add(new JsonPrimitive(property.getValue()));
            }

            result.add(key, values);
        }

        return result;
    }

}
