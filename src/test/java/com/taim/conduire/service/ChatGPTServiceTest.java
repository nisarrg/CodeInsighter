package com.taim.conduire.service;

import com.taim.conduire.dto.LLMRequest;
import com.taim.conduire.dto.LLMResponse;
import com.taim.conduire.dto.Message;
import com.taim.conduire.service.ChatGPTService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
public class ChatGPTServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private ChatGPTService chatGPTService;

    @Test
    void chat_WithValidInput_ReturnsResponse() {
        // Given
        String prompt = "Test prompt";
        String expectedResponse = "Test response";
        String apiUrl = "http://example.com/api";
        String model = "testModel";

        chatGPTService = new ChatGPTService(restTemplate, model, apiUrl);

        // Create a Choice instance with a default role
        String defaultRole = "defaultRole";
        LLMResponse.Choice choice = new LLMResponse.Choice(1, new Message(defaultRole, expectedResponse));


        // Create an LLMResponse instance with a list of choices
        LLMResponse response = new LLMResponse(Collections.singletonList(choice));

        when(restTemplate.postForObject(apiUrl, new LLMRequest(model, prompt), LLMResponse.class))
                .thenReturn(response);

        // When
        String actualResponse = chatGPTService.chat(prompt);

        // Then
        assertEquals(expectedResponse, actualResponse);
        verify(restTemplate, times(1)).postForObject(apiUrl, new LLMRequest(model, prompt), LLMResponse.class);
    }

    @Test
    void chat_WithNoResponse_ReturnsNoResponseMessage() {
        // Given
        String prompt = "Test prompt";
        String apiUrl = "http://example.com/api";
        String model = "testModel";

        chatGPTService = new ChatGPTService(restTemplate, model, apiUrl);

        when(restTemplate.postForObject(apiUrl, new LLMRequest(model, prompt), LLMResponse.class))
                .thenReturn(null);

        // When
        String actualResponse = chatGPTService.chat(prompt);

        // Then
        assertEquals("No response", actualResponse);
        verify(restTemplate, times(1)).postForObject(apiUrl, new LLMRequest(model, prompt), LLMResponse.class);
    }

    @Test
    void chat_WithEmptyChoices_ReturnsNoResponseMessage() {
        // Given
        String prompt = "Test prompt";
        String apiUrl = "http://example.com/api";
        String model = "testModel";

        chatGPTService = new ChatGPTService(restTemplate, model, apiUrl);

        when(restTemplate.postForObject(apiUrl, new LLMRequest(model, prompt), LLMResponse.class))
                .thenReturn(new LLMResponse());

        // When
        String actualResponse = chatGPTService.chat(prompt);

        // Then
        assertEquals("No response", actualResponse);
        verify(restTemplate, times(1)).postForObject(apiUrl, new LLMRequest(model, prompt), LLMResponse.class);
    }
}