package com.taim.conduire.service;

import com.taim.conduire.dto.LLMRequest;
import com.taim.conduire.dto.LLMResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class ChatGPTService {

    @Qualifier("openaiRestTemplate")
    @Autowired
    private RestTemplate restTemplate;

    @Value("${openai.model}")
    private String model;

    @Value("${openai.api.url}")
    private String apiUrl;
    public String chat(String prompt) {
        // create a request
        LLMRequest request = new LLMRequest(model, prompt);
        // call the API
        LLMResponse response = restTemplate.postForObject(apiUrl, request, LLMResponse.class);
        if (response == null || response.getChoices() == null || response.getChoices().isEmpty()) {
            return "No response";
        }
        // return the first response
        return response.getChoices().get(0).getMessage().getContent();
    }
}
