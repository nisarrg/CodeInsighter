package com.taim.conduire.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class JSONUtils {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static List<Map<String, Object>> parseJSONResponse(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {});
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Map<String, Object> parseJSONResponseAsMap(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (IOException e) {
            e.printStackTrace();
            return Collections.emptyMap();
        }
    }

    public static String parseJSONResponseAsTree(String json){
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(json);

            // Extract the value from the "content" field
            String content = jsonNode.get("content").asText();

            // Decode the base64-encoded content
            /*byte[] decodedBytes = java.util.Base64.getDecoder().decode(content);
            String decodedContent = new String(decodedBytes);

            System.out.println("Content: " + decodedContent);*/

            return content;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
