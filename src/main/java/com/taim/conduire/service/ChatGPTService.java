package com.taim.conduire.service;

import com.taim.conduire.dto.LLMRequest;
import com.taim.conduire.dto.LLMResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
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
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();

        // Set the timeout in milliseconds
        requestFactory.setReadTimeout(300000);

        // Set the custom request factory to the RestTemplate
        restTemplate.setRequestFactory(requestFactory);

        // create a request
        LLMRequest request = new LLMRequest(model, prompt);
        // call the API
        LLMResponse response = restTemplate.postForObject(apiUrl, request, LLMResponse.class);

        // TODO --> implementation smell: complex condition
        if (response == null || response.getChoices() == null || response.getChoices().isEmpty()) {
            return "No response";
        }
        // return the first response
        return response.getChoices().get(0).getMessage().getContent();
    }
}
