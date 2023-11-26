package com.taim.conduire.service.impl;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;

public class JSONUtilsImplTest {

    private final JSONUtilsImpl jsonUtils = new JSONUtilsImpl();

    @Test
    public void testParseJSONResponse() {
        // Prepare
        String jsonString = "[{\"id\": 1, \"name\": \"John\"}, {\"id\": 2, \"name\": \"Alice\"}]";

        // Execute
        List<Map<String, Object>> result = jsonUtils.parseJSONResponse(jsonString);

        // Verify
        Assertions.assertNotNull(result);
        Assertions.assertEquals(2, result.size());
        Assertions.assertEquals(1, result.get(0).get("id"));
        Assertions.assertEquals("Alice", result.get(1).get("name"));
    }

    @Test
    public void testParseJSONResponseAsMap() {
        // Prepare
        String jsonString = "{\"key1\": \"value1\", \"key2\": 123}";

        // Execute
        Map<String, Object> result = jsonUtils.parseJSONResponseAsMap(jsonString);

        // Verify
        Assertions.assertNotNull(result);
        Assertions.assertEquals("value1", result.get("key1"));
        Assertions.assertEquals(123, result.get("key2"));
    }

    @Test
    public void testParseJSONResponseAsTree() {
        // Prepare
        String jsonString = "{\"content\": \"Hello World!\"}";

        // Execute
        String result = jsonUtils.parseJSONResponseAsTree(jsonString);

        // Verify
        Assertions.assertNotNull(result);
        Assertions.assertEquals("Hello World!", result);
    }

    @Test
    public void testParseJSONResponseWithInvalidJson() {
        // Prepare
        String invalidJsonString = "invalid json";

        // Execute
        List<Map<String, Object>> result = jsonUtils.parseJSONResponse(invalidJsonString);

        // Verify
        Assertions.assertNull(result);
    }

    @Test
    public void testParseJSONResponseAsMapWithInvalidJson() {
        // Prepare
        String invalidJsonString = "invalid json";

        // Execute
        Map<String, Object> result = jsonUtils.parseJSONResponseAsMap(invalidJsonString);

        // Verify
        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    public void testParseJSONResponseAsTreeWithInvalidJson() {
        // Prepare
        String invalidJsonString = "invalid json";

        // Execute
        String result = jsonUtils.parseJSONResponseAsTree(invalidJsonString);

        // Verify
        Assertions.assertNull(result);
    }
}
