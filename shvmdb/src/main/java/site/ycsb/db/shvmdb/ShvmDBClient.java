/**
 * ShvmDB YCSB Binding
 *
 * This binding communicates with shvm-db via its DynamoDB-compatible HTTP API.
 * shvm-db uses x-amz-target headers and DynamoDB JSON format (AttributeValue maps).
 *
 * Supported operations:
 *   - PutItem  (insert + update)
 *   - GetItem  (read)
 *   - DeleteItem (delete)
 *   - Scan is NOT supported in shvm-db MVP – returns NOT_IMPLEMENTED
 *
 * Properties:
 *   shvmdb.endpoint  – HTTP endpoint (default: http://localhost:8787)
 *   shvmdb.table     – Table name (default: usertable)
 *   shvmdb.debug     – Enable debug logging (default: false)
 */
package site.ycsb.db.shvmdb;

import site.ycsb.ByteIterator;
import site.ycsb.DB;
import site.ycsb.DBException;
import site.ycsb.Status;
import site.ycsb.StringByteIterator;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ShvmDBClient extends DB {

  private static final Logger LOGGER = Logger.getLogger(ShvmDBClient.class.getName());
  private static final String DEFAULT_ENDPOINT = "http://localhost:8787";
  private static final String DEFAULT_TABLE = "usertable";
  private static final String DYNAMO_SERVICE = "DynamoDB_20120810";

  private String endpoint;
  private String tableName;
  private boolean debug;
  private final Gson gson = new Gson();

  @Override
  public void init() throws DBException {
    endpoint = getProperties().getProperty("shvmdb.endpoint", DEFAULT_ENDPOINT);
    // If user provides a table, use it. Otherwise generate a unique one for isolation.
    String userTable = getProperties().getProperty("shvmdb.table");
    if (userTable != null) {
        tableName = userTable;
    } else {
        tableName = "bench_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 1000);
        createTable(tableName);
    }
    debug = Boolean.parseBoolean(getProperties().getProperty("shvmdb.debug", "false"));

    if (debug) {
      LOGGER.setLevel(Level.ALL);
    }

    LOGGER.info("ShvmDBClient initialized: endpoint=" + endpoint + ", table=" + tableName);

    // Verify connectivity
    try {
      HttpURLConnection conn = (HttpURLConnection) new URL(endpoint).openConnection();
      conn.setRequestMethod("GET");
      conn.setConnectTimeout(5000);
      conn.setReadTimeout(5000);
      int rc = conn.getResponseCode();
      conn.disconnect();
      LOGGER.info("ShvmDB connectivity check: HTTP " + rc);
    } catch (Exception e) {
      throw new DBException("Cannot connect to ShvmDB at " + endpoint + ": " + e.getMessage(), e);
    }
  }

  @Override
  public Status read(String table, String key, Set<String> fields, Map<String, ByteIterator> result) {
    try {
      // Build GetItem request
      // Key: { PK: { S: key }, SK: { S: key } }
      JsonObject keyObj = new JsonObject();
      JsonObject pkAttr = new JsonObject();
      pkAttr.addProperty("S", key);
      keyObj.add("PK", pkAttr);

      JsonObject skAttr = new JsonObject();
      skAttr.addProperty("S", key);
      keyObj.add("SK", skAttr);

      JsonObject requestBody = new JsonObject();
      requestBody.addProperty("TableName", table);
      requestBody.add("Key", keyObj);

      String response = doRequest("GetItem", requestBody.toString());

      if (response == null || response.isEmpty()) {
        return Status.ERROR;
      }

      JsonObject responseObj = JsonParser.parseString(response).getAsJsonObject();

      if (responseObj.has("Item")) {
        JsonObject item = responseObj.getAsJsonObject("Item");
        // Extract fields from the DynamoDB-format item
        for (Map.Entry<String, JsonElement> entry : item.entrySet()) {
          String fieldName = entry.getKey();
          // Skip PK and SK from results
          if ("PK".equals(fieldName) || "SK".equals(fieldName)) {
            continue;
          }
          if (fields != null && !fields.contains(fieldName)) {
            continue;
          }
          // Extract the string value from { S: "..." }
          if (entry.getValue().isJsonObject()) {
            JsonObject attrValue = entry.getValue().getAsJsonObject();
            if (attrValue.has("S")) {
              result.put(fieldName, new StringByteIterator(attrValue.get("S").getAsString()));
            }
          }
        }
      }

      if (debug) {
        LOGGER.info("READ " + key + " -> " + result.size() + " fields");
      }

      return Status.OK;
    } catch (Exception e) {
      LOGGER.log(Level.WARNING, "Error reading key " + key, e);
      return Status.ERROR;
    }
  }

  @Override
  public Status scan(String table, String startkey, int recordcount,
                     Set<String> fields, Vector<HashMap<String, ByteIterator>> result) {
    // shvm-db MVP does not support Query/Scan
    LOGGER.warning("SCAN not supported in shvm-db MVP");
    return Status.NOT_IMPLEMENTED;
  }

  @Override
  public Status update(String table, String key, Map<String, ByteIterator> values) {
    // shvm-db uses PutItem for both insert and update (INSERT OR REPLACE)
    return insert(table, key, values);
  }

  @Override
  public Status insert(String table, String key, Map<String, ByteIterator> values) {
    try {
      // Build PutItem request
      JsonObject item = new JsonObject();

      // Add PK
      JsonObject pkAttr = new JsonObject();
      pkAttr.addProperty("S", key);
      item.add("PK", pkAttr);

      // Add SK (using key as SK for YCSB's flat key model)
      JsonObject skAttr = new JsonObject();
      skAttr.addProperty("S", key);
      item.add("SK", skAttr);

      // Add all field values
      for (Map.Entry<String, ByteIterator> entry : values.entrySet()) {
        JsonObject fieldAttr = new JsonObject();
        fieldAttr.addProperty("S", entry.getValue().toString());
        item.add(entry.getKey(), fieldAttr);
      }

      JsonObject requestBody = new JsonObject();
      requestBody.addProperty("TableName", table);
      requestBody.add("Item", item);

      String response = doRequest("PutItem", requestBody.toString());

      if (debug) {
        LOGGER.info("INSERT " + key + " -> " + values.size() + " fields, response: " + response);
      }

      return Status.OK;
    } catch (Exception e) {
      LOGGER.log(Level.WARNING, "Error inserting key " + key, e);
      return Status.ERROR;
    }
  }

  @Override
  public Status delete(String table, String key) {
    try {
      // Build DeleteItem request
      JsonObject keyObj = new JsonObject();
      JsonObject pkAttr = new JsonObject();
      pkAttr.addProperty("S", key);
      keyObj.add("PK", pkAttr);

      JsonObject skAttr = new JsonObject();
      skAttr.addProperty("S", key);
      keyObj.add("SK", skAttr);

      JsonObject requestBody = new JsonObject();
      requestBody.addProperty("TableName", table);
      requestBody.add("Key", keyObj);

      doRequest("DeleteItem", requestBody.toString());

      if (debug) {
        LOGGER.info("DELETE " + key);
      }

      return Status.OK;
    } catch (Exception e) {
      LOGGER.log(Level.WARNING, "Error deleting key " + key, e);
      return Status.ERROR;
    }
  }

  /**
   * Execute an HTTP request to the shvm-db endpoint.
   * Uses DynamoDB-compatible x-amz-target headers.
   */
  private String doRequest(String operation, String body) throws Exception {
    URL url = new URL(endpoint + "/api");
    HttpURLConnection conn = (HttpURLConnection) url.openConnection();

    try {
      conn.setRequestMethod("POST");
      conn.setRequestProperty("Content-Type", "application/x-amz-json-1.0");
      conn.setRequestProperty("x-amz-target", DYNAMO_SERVICE + "." + operation);
      conn.setConnectTimeout(30000);
      conn.setReadTimeout(30000);
      conn.setDoOutput(true);

      // Write request body
      try (OutputStream os = conn.getOutputStream()) {
        os.write(body.getBytes(StandardCharsets.UTF_8));
        os.flush();
      }

      int responseCode = conn.getResponseCode();

      if (responseCode >= 200 && responseCode < 300) {
        // Read success response
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
            new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
          String line;
          while ((line = br.readLine()) != null) {
            sb.append(line);
          }
        }
        return sb.toString();
      } else {
        // Read error response
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
            new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8))) {
          String line;
          while ((line = br.readLine()) != null) {
            sb.append(line);
          }
        }
        LOGGER.warning(operation + " failed with HTTP " + responseCode + ": " + sb.toString());
        throw new Exception("HTTP " + responseCode + ": " + sb.toString());
      }
    } finally {
      conn.disconnect();
    }
  }

  // --- Table Management ---

  private void createTable(String name) throws DBException {
    LOGGER.info("Creating table: " + name);
    try {
      JsonObject tableDef = new JsonObject();
      tableDef.addProperty("TableName", name);
      
      // KeySchema
      com.google.gson.JsonArray keySchema = new com.google.gson.JsonArray();
      JsonObject pk = new JsonObject(); pk.addProperty("AttributeName", "PK"); pk.addProperty("KeyType", "HASH");
      JsonObject sk = new JsonObject(); sk.addProperty("AttributeName", "SK"); sk.addProperty("KeyType", "RANGE");
      keySchema.add(pk);
      keySchema.add(sk);
      tableDef.add("KeySchema", keySchema);

      // AttributeDefinitions
      com.google.gson.JsonArray attrDefs = new com.google.gson.JsonArray();
      JsonObject pkDef = new JsonObject(); pkDef.addProperty("AttributeName", "PK"); pkDef.addProperty("AttributeType", "S");
      JsonObject skDef = new JsonObject(); skDef.addProperty("AttributeName", "SK"); skDef.addProperty("AttributeType", "S");
      attrDefs.add(pkDef);
      attrDefs.add(skDef);
      tableDef.add("AttributeDefinitions", attrDefs);

      doRequest("CreateTable", tableDef.toString());
    } catch (Exception e) {
      throw new DBException("Failed to create table " + name, e);
    }
  }

  private void deleteTable(String name) throws DBException {
    LOGGER.info("Deleting table: " + name);
    try {
      JsonObject req = new JsonObject();
      req.addProperty("TableName", name);
      doRequest("DeleteTable", req.toString());
    } catch (Exception e) {
      LOGGER.warning("Failed to delete table " + name + ": " + e.getMessage());
      // Don't fail the benchmark if cleanup fails
    }
  }

  @Override
  public void cleanup() throws DBException {
     if (this.tableName != null && !this.tableName.equals(DEFAULT_TABLE)) {
         deleteTable(this.tableName);
     }
  }
}
