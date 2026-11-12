package lk.ac.kln.hallsync.web;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.time.*;
import java.util.function.Function;

/** JSON boundary with explicit ISO date/time adapters. */
public final class JsonCodec {
    private JsonCodec() { }
    public static final Gson GSON = new GsonBuilder().serializeNulls()
            .registerTypeAdapter(LocalDate.class, adapter(LocalDate::parse))
            .registerTypeAdapter(LocalTime.class, adapter(LocalTime::parse))
            .registerTypeAdapter(LocalDateTime.class, adapter(LocalDateTime::parse))
            .create();
    private static <T> TypeAdapter<T> adapter(Function<String,T> parse) {
        return new TypeAdapter<T>() {
            @Override public void write(JsonWriter out,T value) throws IOException { out.value(value.toString()); }
            @Override public T read(JsonReader in) throws IOException { return parse.apply(in.nextString()); }
        }.nullSafe();
    }
}
