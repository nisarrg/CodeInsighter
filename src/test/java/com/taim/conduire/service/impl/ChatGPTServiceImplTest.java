package com.taim.conduire.service.impl;

import com.taim.conduire.dto.LLMRequest;
import com.taim.conduire.dto.LLMResponse;
import com.taim.conduire.dto.Message;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.web.client.RestTemplate;
import java.util.Collections;

public class ChatGPTServiceImplTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private ChatGPTServiceImpl chatGPTService;

    private final String model = "gpt-3";
    private final String apiUrl = "http://example.com/api";

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        chatGPTService.setModel(model);
        chatGPTService.setApiUrl(apiUrl);
    }

    @Test
    public void testChatWithValidResponse() {
        // Prepare
        LLMResponse mockResponse = new LLMResponse();
        LLMResponse.Choice choice = new LLMResponse.Choice();
        Message message = new Message();
        message.setContent("Test response");
        choice.setMessage(message);
        mockResponse.setChoices(Collections.singletonList(choice));
        Mockito.when(restTemplate.postForObject(apiUrl, new LLMRequest(model, "Test prompt"), LLMResponse.class))
                .thenReturn(mockResponse);

        // Execute
        String response = chatGPTService.chat("Test prompt");

        // Verify
        Assertions.assertEquals("Test response", response);
    }

    @Test
    public void testChatWithEmptyResponse() {
        // Prepare
        Mockito.when(restTemplate.postForObject(apiUrl, new LLMRequest(model, "Empty prompt"), LLMResponse.class))
                .thenReturn(null);

        // Execute
        String response = chatGPTService.chat("Empty prompt");

        // Verify
        Assertions.assertEquals("No response", response);
    }

    @Test
    public void testChatWithNoChoices() {
        // Prepare
        LLMResponse mockResponse = new LLMResponse();
        mockResponse.setChoices(Collections.emptyList());
        Mockito.when(restTemplate.postForObject(apiUrl, new LLMRequest(model, "No choices prompt"), LLMResponse.class))
                .thenReturn(mockResponse);

        // Execute
        String response = chatGPTService.chat("No choices prompt");

        // Verify
        Assertions.assertEquals("No response", response);
    }
}
