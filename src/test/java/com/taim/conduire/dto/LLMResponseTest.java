package com.taim.conduire.dto;

import org.junit.jupiter.api.Test;
import java.util.List;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class LLMResponseTest {

    @Test
    public void testLLMResponseInitialization() {
        // Given
        List<LLMResponse.Choice> choices = new ArrayList<>();
        choices.add(new LLMResponse.Choice(1, new Message("user", "Test Message")));

        // When
        LLMResponse llmResponse = new LLMResponse(choices);

        // Then
        assertEquals(choices, llmResponse.getChoices());
    }

    @Test
    public void testLLMResponseEmptyConstructor() {
        // When
        LLMResponse llmResponse = new LLMResponse();

        // Then
        assertNull(llmResponse.getChoices());
    }

    @Test
    public void testChoiceInitialization() {
        // Given
        Message message = new Message("user", "Test Message");

        // When
        LLMResponse.Choice choice = new LLMResponse.Choice(1, message);

        // Then
        assertEquals(1, choice.getIndex());
        assertEquals(message, choice.getMessage());
    }

    @Test
    public void testChoiceEmptyConstructor() {
        // When
        LLMResponse.Choice choice = new LLMResponse.Choice();

        // Then
        assertEquals(0, choice.getIndex());
        assertNull(choice.getMessage());
    }

    @Test
    public void testLLMResponseSetters() {
        // Given
        List<LLMResponse.Choice> choices = new ArrayList<>();
        choices.add(new LLMResponse.Choice(1, new Message("user", "Test Message")));

        LLMResponse llmResponse = new LLMResponse();

        // When
        llmResponse.setChoices(choices);

        // Then
        assertEquals(choices, llmResponse.getChoices());
    }

    @Test
    public void testChoiceSetters() {
        // Given
        Message message = new Message("user", "Test Message");

        LLMResponse.Choice choice = new LLMResponse.Choice();

        // When
        choice.setIndex(1);
        choice.setMessage(message);

        // Then
        assertEquals(1, choice.getIndex());
        assertEquals(message, choice.getMessage());
    }
}
