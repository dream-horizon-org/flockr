package io.ascend.flockr.admin.util.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import io.vertx.core.json.JsonObject;
import java.io.IOException;

/**
 * Custom Jackson deserializer for Vert.x JsonObject.
 *
 * <p>Converts JSON nodes to Vert.x JsonObject instances during deserialization.
 *
 * @author Prithu Sharma
 * @since 1.0
 */
public class JsonObjectDeserializer extends JsonDeserializer<JsonObject> {

  @Override
  public JsonObject deserialize(
      JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
    JsonNode node = jsonParser.getCodec().readTree(jsonParser);
    return new JsonObject(node.toString());
  }
}
