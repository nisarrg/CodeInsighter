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

// TODO --> Design Smell : Unutilized Abstraction
@Service
public class LLMServiceImpl implements LLMService {
    private static final Logger logger = LoggerFactory.getLogger(InsightsService.class);
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${github.api.url}")
    private String githubApiUrl;

    @Autowired
    private JSONUtils jsonUtils;

    @Autowired
    private UserDataService userDataService;

    @Autowired
    private InsightsService insightsService;

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
        // TODO: Ask what is 23, 46, 69, ...???. remove magic numbers.
        // TODO: Make all those statements inside while loop common.
        // TODO: Remove all unnecessary comments. And add meaningful comments instead of sunday, monday.
        //TODO: Complex method --> cyclomatic complexity is 9

        if (repoPunchCard == null || repoPunchCard.isEmpty()) {
            // Handle the case where repoPunchCard is null or empty
            return weeklyCount; // return an array of zeros or handle it as needed
        }

        //Sunday
        while (i <= 23) {
            List<Integer> tempList = repoPunchCard.get(i);
            count = count + tempList.get(2);
            i++;
        }
        weeklyCount[x] = count;
        x++;
        count = 0;

        //Monday
        while (i <= 46) {
            List<Integer> tempList = repoPunchCard.get(i);
            count = count + tempList.get(2);
            i++;
        }
        weeklyCount[x] = count;
        x++;
        count = 0;

        // Tuesday
        while (i <= 69) {
            List<Integer> tempList = repoPunchCard.get(i);
            count = count + tempList.get(2);
            i++;
        }
        weeklyCount[x] = count;
        x++;
        count = 0;
        // Wednesday
        while (i <= 92) {
            List<Integer> tempList = repoPunchCard.get(i);
            count = count + tempList.get(2);
            i++;
        }
        weeklyCount[x] = count;
        x++;
        count = 0;
        // Thursday
        while (i <= 115) {
            List<Integer> tempList = repoPunchCard.get(i);
            count = count + tempList.get(2);
            i++;
        }
        weeklyCount[x] = count;
        x++;
        count = 0;
        // Friday
        while (i <= 138) {
            List<Integer> tempList = repoPunchCard.get(i);
            count = count + tempList.get(2);
            i++;
        }
        weeklyCount[x] = count;
        x++;
        count = 0;
        // Saturday
        while (i <= 161) {
            List<Integer> tempList = repoPunchCard.get(i);
            count = count + tempList.get(2);
            i++;
        }
        weeklyCount[x] = count;
        for (int m = 0; m < weeklyCount.length; m++)
            logger.debug("No. of Commits on day " + m + ": " + weeklyCount[m]);

        return weeklyCount;
    }
}
