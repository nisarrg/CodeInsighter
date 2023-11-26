package com.taim.conduire.service.impl;

import com.taim.conduire.dto.LLMRequest;
import com.taim.conduire.dto.LLMResponse;
import com.taim.conduire.service.ChatGPTService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class ChatGPTServiceImpl implements ChatGPTService {

    @Qualifier("openaiRestTemplate")
    @Autowired
    private RestTemplate restTemplate;

    @Value("${openai.model}")
    private String model;

    @Value("${openai.api.url}")
    private String apiUrl;

    /**
     * Initiates a chat conversation by sending a prompt to the LLM (Language Model) API.
     * @author Sameer Amesara
     * @param prompt The prompt to be sent to the LLM for generating a response.
     * @return The response message from the LLM API, or "No response" if the response or choices are null or empty.
     */
    @Override
    public String chat(String prompt) {
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

    public void setModel(String model) {
        this.model = model;
    }

    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }

}
