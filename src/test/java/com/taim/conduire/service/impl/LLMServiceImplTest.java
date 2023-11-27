package com.taim.conduire.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import com.fasterxml.jackson.core.type.TypeReference;

public class LLMServiceImplTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private LLMServiceImpl llmService;

    @Mock
    private TypeFactory typeFactory;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);
        llmService = new LLMServiceImpl(restTemplate, objectMapper);
        // Mock getTypeFactory() behavior
        when(objectMapper.getTypeFactory()).thenReturn(typeFactory);
    }
    
    @Test
    void testGetRepositoryPunchCardtest() throws IOException {
        // Mocked API response and object mapper behavior
        String apiUrl = "mocked_api_url";
        String name = "mocked_name";
        String mockedResponse = "[[1, 2, 3], [4, 5, 6], [7, 8, 9],[10, 11, 12],[13, 14, 15],[16, 17, 18],[19, 20, 21]]";
        List<List<Integer>> mockedPunchCard = Arrays.asList(Arrays.asList(1, 2, 3), Arrays.asList(4, 5, 6));
        int[] expectedWeeklyCommits = { 0, 0, 0, 0, 0, 0, 0 }; // Adjust according to your test case

        when(restTemplate.getForObject(apiUrl, String.class)).thenReturn(mockedResponse);
        when(objectMapper.readValue(any(String.class), any(TypeReference.class)))
                .thenReturn(mockedPunchCard);

        int[] actualWeeklyCommits = llmService.getRepositoryPunchCardtest(name);

        assertArrayEquals(expectedWeeklyCommits, actualWeeklyCommits);
    }

    @Test
    public void testComputeWeeklyCommitsWithNullInput() {
        int[] expectedResult = new int[7]; // All initialized to 0 by default

        int[] result = llmService.computeWeeklyCommits(null);

        assertArrayEquals(expectedResult, result);
    }

    @Test
    public void testComputeWeeklyCommitsWithEmptyInput() {
        List<List<Integer>> emptyRepoPunchCard = new ArrayList<>();
        int[] expectedResult = new int[7]; // All initialized to 0 by default

        int[] result = llmService.computeWeeklyCommits(emptyRepoPunchCard);

        assertArrayEquals(expectedResult, result);
    }

    @Test
    public void testComputeWeeklyCommitsWithData() {
        // Mock repository punch card data for testing
        List<List<Integer>> repoPunchCard = new ArrayList<>();
        for (int i = 0; i < 168; i++) { // 168 entries for 7 days (7 * 24)
            List<Integer> tempList = Arrays.asList(0, 0, i); // [0, 0, 0], [0, 0, 1], [0, 0, 2], ...
            repoPunchCard.add(tempList);
        }

        int[] result = llmService.computeWeeklyCommits(repoPunchCard);

        // Expected result: Total of all third elements (index 2) in each sublist for 7 days
        int[] expectedResult = {276, 852, 1428, 2004, 2580, 3156, 3732}; // Values are calculated manually

        assertArrayEquals(expectedResult, result);
    }
}
