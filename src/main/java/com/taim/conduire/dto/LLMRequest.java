package com.taim.conduire.dto;

import lombok.Data;
import java.util.*;
@Data
public class LLMRequest {
    private String model;
    private List<Message> messages;

    public LLMRequest(String model, String prompt) {
        this.model = model;
        this.messages = new ArrayList<>();
        this.messages.add(new Message("user", prompt));
    }

}
