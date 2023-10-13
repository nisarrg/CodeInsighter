package com.taim.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class LLMService {

    @Value("${github.api.url}")
    private String githubApiUrl;

    @Value("${github.repository.owner}")
    private String owner;

    @Value("${github.repository.name}")
    private String repo;

    private final RestTemplate restTemplate;

    @Autowired
    public LLMService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public Map<String, Integer> getRepositoryLanguages() {
        String apiUrl = String.format("%s/repos/%s/%s/languages", githubApiUrl, owner, repo);
        return restTemplate.getForObject(apiUrl, Map.class);
    }

    public int[][] getRepositoryCodeFrequency() {
        String apiUrl = String.format("%s/repos/%s/%s/stats/code_frequency", githubApiUrl, owner, repo);
        return restTemplate.getForObject(apiUrl, int[][].class);
    }
}
