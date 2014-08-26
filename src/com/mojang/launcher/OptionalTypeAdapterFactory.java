package com.mojang.launcher;

import com.google.common.base.Optional;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public class OptionalTypeAdapterFactory implements TypeAdapterFactory {

    public <T extends Object> TypeAdapter<T> create(Gson gson, TypeToken<T> typeToken) {
        Type type = typeToken.getType();
        if (typeToken.getRawType() == Optional.class && type instanceof ParameterizedType) {
            Type valueType = ((ParameterizedType) type).getActualTypeArguments()[0];
            TypeAdapter valueAdapter = gson.getAdapter(TypeToken.get(valueType));
            return this.newOptionalTypeAdapter(valueAdapter);
        } else {
            return null;
        }
    }

    private <V extends Object> TypeAdapter<Optional<V>> newOptionalTypeAdapter(final TypeAdapter<V> valueAdapter) {
        return new TypeAdapter() {
            public void write(JsonWriter jsonWriter, Optional<V> vOptional) throws IOException {
                if (vOptional.isPresent()) {
                    valueAdapter.write(jsonWriter, vOptional.get());
                } else {
                    jsonWriter.nullValue();
                }

            }

            public Optional<V> read(JsonReader jsonReader) throws IOException {
                if (jsonReader.peek() == JsonToken.NULL) {
                    jsonReader.nextNull();
                    return Optional.absent();
                } else {
                    return Optional.of(valueAdapter.read(jsonReader));
                }
            }

            @Override
            public void write(JsonWriter writer, Object t) throws IOException {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }
        };
    }
}
