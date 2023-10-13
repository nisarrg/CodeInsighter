package com.taim.controller;

import com.taim.service.LLMService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class LLMController {

    private final LLMService llmService;

    public LLMController(LLMService llmService) {
        this.llmService = llmService;
    }

    @GetMapping("/repository/languages")
    public Map<String, Integer> getRepositoryLanguages() {
        return llmService.getRepositoryLanguages();
    }

    @GetMapping("/repository/code-frequency")
    public int[][] getRepositoryCodeFrequency() {
        return llmService.getRepositoryCodeFrequency();
    }
}
