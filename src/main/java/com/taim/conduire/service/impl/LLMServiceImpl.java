package com.taim.conduire.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taim.conduire.service.InsightsService;
import com.taim.conduire.service.JSONUtils;
import com.taim.conduire.service.LLMService;
import com.taim.conduire.service.UserDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.List;

@Service
public class LLMServiceImpl implements LLMService {
    private static final Logger logger = LoggerFactory.getLogger(InsightsService.class);
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${github.api.url}")
    private String githubApiUrl;

    @Autowired
    public LLMServiceImpl(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public int[] getRepositoryPunchCardtest(String name) throws IOException {
        List<List<Integer>> repoPunchCard;
        String apiUrl = String.format("%s/repos/%s/stats/punch_card", githubApiUrl, name);
        logger.debug(apiUrl);
        String apiResponse = restTemplate.getForObject(apiUrl, String.class);
        repoPunchCard = objectMapper.readValue(apiResponse, objectMapper.getTypeFactory().constructCollectionType(List.class, List.class));
        return computeWeeklyCommits(repoPunchCard);
    }

    public int[] computeWeeklyCommits(List<List<Integer>> repoPunchCard) {
        int[] weeklyCount = new int[7];
        int count = 0;
        int i = 0, x = 0;

        if (repoPunchCard == null || repoPunchCard.isEmpty()) {
            return weeklyCount; // return an array of zeros or handle it as needed
        }

        int dayIndex = 0;
        int hourIndex = 0;

        while (hourIndex <= 161) {
            int endHourIndex = hourIndex + 23; // Each day has 24 hours

            while (hourIndex <= endHourIndex) {
                List<Integer> tempList = repoPunchCard.get(hourIndex);
                weeklyCount[dayIndex] += tempList.get(2);
                hourIndex++;
            }

            dayIndex++;
        }
        for (int m = 0; m < weeklyCount.length; m++) {
            logger.debug("No. of Commits on day " + m + ": " + weeklyCount[m]);
        }

        return weeklyCount;
    }
}
