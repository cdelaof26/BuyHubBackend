/*
  GsonBase64Serializer.java
  Adaptador GSON para serializar/deserializar byte[] como base 64
  Ver: https://sites.google.com/site/gson/gson-user-guide
  Carlos Pineda Guerrero, septiembre 2024
*/

package buyhub;

import java.lang.reflect.Type;
import java.util.Base64;
import com.google.gson.JsonSerializer;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonElement;

public class GsonBase64Serializer implements JsonSerializer<byte[]>, JsonDeserializer<byte[]> {
    @Override
    public JsonElement serialize(byte[] src, Type typeOfSrc, JsonSerializationContext context) {
        return new JsonPrimitive(Base64.getEncoder().encodeToString(src));
    }

    @Override
    public byte[] deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
        // jax-rs reemplaza cada "+" por " ", pero el decodificador Base64 no reconoce " "
        return Base64.getDecoder().decode(json.getAsString().replaceAll("\\ ", "+"));
    }
}
