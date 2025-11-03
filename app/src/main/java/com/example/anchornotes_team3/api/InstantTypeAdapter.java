package com.example.anchornotes_team3.api;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

/**
 * Gson TypeAdapter for java.time.Instant
 * Handles ISO-8601 timestamp strings from the backend
 */
public class InstantTypeAdapter extends TypeAdapter<Instant> {
    
    @Override
    public void write(JsonWriter out, Instant value) throws IOException {
        if (value == null) {
            out.nullValue();
        } else {
            out.value(value.toString());
        }
    }
    
    @Override
    public Instant read(JsonReader in) throws IOException {
        if (in.peek() == JsonToken.NULL) {
            in.nextNull();
            return null;
        }
        
        String timestamp = in.nextString();
        if (timestamp == null || timestamp.isEmpty()) {
            return null;
        }
        
        try {
            return Instant.parse(timestamp);
        } catch (Exception e) {
            // Try parsing with DateTimeFormatter as fallback
            try {
                return Instant.from(DateTimeFormatter.ISO_INSTANT.parse(timestamp));
            } catch (Exception e2) {
                throw new IOException("Failed to parse Instant from: " + timestamp, e2);
            }
        }
    }
}

