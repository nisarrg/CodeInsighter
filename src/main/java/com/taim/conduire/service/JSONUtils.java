package com.taim.conduire.service;

import java.util.List;
import java.util.Map;

public interface JSONUtils {

    List<Map<String, Object>> parseJSONResponse(String json);

    Map<String, Object> parseJSONResponseAsMap(String json);

    String parseJSONResponseAsTree(String json);
}
