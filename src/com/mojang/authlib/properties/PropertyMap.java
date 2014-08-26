package com.mojang.authlib.properties;

import com.google.common.collect.ForwardingMultimap;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import java.lang.reflect.Type;
import java.util.Iterator;
import java.util.Map.Entry;

public class PropertyMap extends ForwardingMultimap<String, Property> {

    private final Multimap<String, Property> properties = LinkedHashMultimap.create();

    protected Multimap<String, Property> delegate() {
        return this.properties;
    }

    public static class Serializer implements JsonSerializer<PropertyMap>, JsonDeserializer<PropertyMap> {

        public PropertyMap deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            PropertyMap result = new PropertyMap();
            if (json instanceof JsonObject) {
                JsonObject i$ = (JsonObject) json;
                Iterator element = i$.entrySet().iterator();

                while (element.hasNext()) {
                    Entry object = (Entry) element.next();
                    if (object.getValue() instanceof JsonArray) {
                        Iterator name = ((JsonArray) object.getValue()).iterator();

                        while (name.hasNext()) {
                            JsonElement value = (JsonElement) name.next();
                            result.put(object.getKey().toString(), new Property((String) object.getKey(), value.getAsString()));
                        }
                    }
                }
            } else if (json instanceof JsonArray) {
                Iterator i$1 = ((JsonArray) json).iterator();

                while (i$1.hasNext()) {
                    JsonElement element1 = (JsonElement) i$1.next();
                    if (element1 instanceof JsonObject) {
                        JsonObject object1 = (JsonObject) element1;
                        String name1 = object1.getAsJsonPrimitive("name").getAsString();
                        String value1 = object1.getAsJsonPrimitive("value").getAsString();
                        if (object1.has("signature")) {
                            result.put(name1, new Property(name1, value1, object1.getAsJsonPrimitive("signature").getAsString()));
                        } else {
                            result.put(name1, new Property(name1, value1));
                        }
                    }
                }
            }

            return result;
        }

        public JsonElement serialize(PropertyMap src, Type typeOfSrc, JsonSerializationContext context) {
            JsonArray result = new JsonArray();

            JsonObject object;
            for (Iterator i$ = src.values().iterator(); i$.hasNext(); result.add(object)) {
                Property property = (Property) i$.next();
                object = new JsonObject();
                object.addProperty("name", property.getName());
                object.addProperty("value", property.getValue());
                if (property.hasSignature()) {
                    object.addProperty("signature", property.getSignature());
                }
            }

            return result;
        }

    }
}
