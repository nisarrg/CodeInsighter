package com.taim.conduire.dto;

import com.taim.conduire.dto.LLMRequest;
import com.taim.conduire.dto.Message;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;


import static org.junit.jupiter.api.Assertions.assertEquals;

public class LLMRequestTest {

    @Test
    public void testLLMRequestInitialization() {
        // Given
        String model = "TestModel";
        String prompt = "TestPrompt";

        // When
        LLMRequest llmRequest = new LLMRequest(model, prompt);

        // Then
        assertEquals(model, llmRequest.getModel());
        List<Message> messages = llmRequest.getMessages();
        assertEquals(1, messages.size());
        Message message = messages.get(0);
        assertEquals("user", message.getRole());
        assertEquals(prompt, message.getContent());
    }

    @Test
    public void testAddMessage() {
        // Given
        LLMRequest llmRequest = new LLMRequest("TestModel", "TestPrompt");
        String newMessageContent = "NewMessage";

        // When
        llmRequest.getMessages().add(new Message("system", newMessageContent));

        // Then
        List<Message> messages = llmRequest.getMessages();
        assertEquals(2, messages.size()); // One initial message + the new message
        Message newMessage = messages.get(1);
        assertEquals("system", newMessage.getRole());
        assertEquals(newMessageContent, newMessage.getContent());
    }
}
