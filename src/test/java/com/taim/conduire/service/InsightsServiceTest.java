package com.taim.conduire.service;

import com.taim.conduire.constants.ConstantCodes;
import com.taim.conduire.domain.RepoData;
import com.taim.conduire.domain.UserData;
import com.taim.conduire.service.impl.ChatGPTServiceImpl;
import com.taim.conduire.service.impl.InsightsServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

public class InsightsServiceTest implements ConstantCodes {

    @Mock
    private RepoDataService repoDataService;

    @Mock
    private UserDataService userDataService;

    @Mock
    private ChatGPTServiceImpl chatGPTService;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private InsightsServiceImpl insightsService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testGetAllHeadersEntity() {
        // Test for private method getAllHeadersEntity
        // Invoke the method
        HttpHeaders headers = insightsService.getAllHeadersEntity("testToken").getHeaders();

        // Verify the headers set properly
        assertEquals("application/vnd.github+json", headers.getFirst("Accept"));
        assertEquals("Bearer testToken", headers.getFirst("Authorization"));
        assertEquals("2022-11-28", headers.getFirst("X-GitHub-Api-Version"));
    }

    @Test
    public void testShowAvailableAPIHits() {
        // Test for private method showAvailableAPIHits
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.set("X-RateLimit-Limit", "5000");
        responseHeaders.set("X-RateLimit-Remaining", "4999");

        // Capture console output
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputStream));

        insightsService.showAvailableAPIHits(responseHeaders);

        // Verify the printed output contains API hit information
        String expectedOutput = "GitHub API Hit Limit: 5000\nGitHub API Hit Limit Remaining: 4999\n";
        // Use contains instead of direct comparison
        assertTrue(outputStream.toString().contains("GitHub API Hit Limit: 5000"));
        assertTrue(outputStream.toString().contains("GitHub API Hit Limit Remaining: 4999"));
    }

    @Test
    void testGetRepositoryReviewComments_SuccessfulCall_ReturnsReviewerComments() {
        // Given
        String repoName = "testRepo";
        int userId = 1;
        String userAccessToken = "testAccessToken";
        String apiUrl = String.format("%s/repos/%s", GITHUB_API_URL, repoName);
        String prReviewCommentsURL = String.format(apiUrl + GITHUB_PULLS + GITHUB_COMMENTS);

        // Creating RepoData instance
        RepoData repoData = new RepoData();
        repoData.setId(1);
        repoData.setUserId(userId);
        repoData.setName(repoName);
        repoData.setIsFork(false);
        repoData.setRepoCreatedAt(new Date());

        // Creating UserData instance
        UserData userData = new UserData();
        userData.setId(userId);
        userData.setGithubUserId(123);
        userData.setName("John Doe");
        userData.setUserName("john_doe");
        userData.setUserAccessToken(userAccessToken);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", userAccessToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<String> repoResponse = new ResponseEntity<>("{ \"fork\": false }", HttpStatus.OK);
        ResponseEntity<String> prReviewResponse = new ResponseEntity<>("[{\"user\": {\"login\":\"reviewer1\"}, \"body\":\"comment1\"}, {\"user\": {\"login\":\"reviewer2\"}, \"body\":\"comment2\"}]", HttpStatus.OK);

        when(repoDataService.getOne(userId)).thenReturn(repoData);
        when(userDataService.getOne(userId)).thenReturn(userData);
        when(restTemplate.exchange(apiUrl, HttpMethod.GET, entity, String.class)).thenReturn(repoResponse);
        when(restTemplate.exchange(prReviewCommentsURL, HttpMethod.GET, entity, String.class)).thenReturn(prReviewResponse);

        // When
        Map<String, List<String>> reviewerComments = insightsService.getRepositoryReviewComments(repoData);

        // Then
        assertEquals(2, reviewerComments.size());
        assertEquals(List.of("comment1"), reviewerComments.get("reviewer1"));
        assertEquals(List.of("comment2"), reviewerComments.get("reviewer2"));
    }

    @Test
    void testGetRepositoryReviewComments_ForkedRepo_ReturnsReviewerCommentsFromSource() {
        // Given
        String repoName = "forkedRepo";
        int userId = 1;
        String userAccessToken = "testAccessToken";
        String apiUrl = String.format("%s/repos/%s", GITHUB_API_URL, repoName);
        String sourceRepoUrl = "http://sourceRepoUrl.com";
        String prReviewCommentsURL = String.format(sourceRepoUrl + GITHUB_PULLS + GITHUB_COMMENTS);

        // Creating RepoData instance
        RepoData repoData = new RepoData();
        repoData.setId(1);
        repoData.setUserId(userId);
        repoData.setName(repoName);
        repoData.setIsFork(true); // Forked repo
        repoData.setRepoCreatedAt(new Date());

        // Creating UserData instance
        UserData userData = new UserData();
        userData.setId(userId);
        userData.setGithubUserId(123);
        userData.setName("John Doe");
        userData.setUserName("john_doe");
        userData.setUserAccessToken(userAccessToken);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", userAccessToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<String> repoResponse = new ResponseEntity<>("{ \"fork\": true, \"source\": { \"url\": \"" + sourceRepoUrl + "\" } }", HttpStatus.OK);
        ResponseEntity<String> prReviewResponse = new ResponseEntity<>("[{\"user\": {\"login\":\"reviewer3\"}, \"body\":\"comment3\"}]", HttpStatus.OK);

        when(repoDataService.getOne(userId)).thenReturn(repoData);
        when(userDataService.getOne(userId)).thenReturn(userData);
        when(restTemplate.exchange(apiUrl, HttpMethod.GET, entity, String.class)).thenReturn(repoResponse);
        when(restTemplate.exchange(prReviewCommentsURL, HttpMethod.GET, entity, String.class)).thenReturn(prReviewResponse);

        // When
        Map<String, List<String>> reviewerComments = insightsService.getRepositoryReviewComments(repoData);

        // Then
        assertEquals(1, reviewerComments.size());
        assertEquals(List.of("comment3"), reviewerComments.get("reviewer3"));
    }

    @Test
    void testGetRepositoryReviewComments_EmptyResponse_ReturnsEmptyMap() {
        // Given
        String repoName = "emptyRepo";
        int userId = 1;
        String userAccessToken = "testAccessToken";
        String apiUrl = String.format("%s/repos/%s", GITHUB_API_URL, repoName);
        String prReviewCommentsURL = String.format(apiUrl + GITHUB_PULLS + GITHUB_COMMENTS);

        // Creating RepoData instance
        RepoData repoData = new RepoData();
        repoData.setId(1);
        repoData.setUserId(userId);
        repoData.setName(repoName);
        repoData.setIsFork(false); // Not a forked repo
        repoData.setRepoCreatedAt(new Date());

        // Creating UserData instance
        UserData userData = new UserData();
        userData.setId(userId);
        userData.setGithubUserId(123);
        userData.setName("John Doe");
        userData.setUserName("john_doe");
        userData.setUserAccessToken(userAccessToken);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", userAccessToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<String> repoResponse = new ResponseEntity<>("{ \"fork\": false }", HttpStatus.OK);
        ResponseEntity<String> prReviewResponse = new ResponseEntity<>("[]", HttpStatus.OK);

        when(repoDataService.getOne(userId)).thenReturn(repoData);
        when(userDataService.getOne(userId)).thenReturn(userData);
        when(restTemplate.exchange(apiUrl, HttpMethod.GET, entity, String.class)).thenReturn(repoResponse);
        when(restTemplate.exchange(prReviewCommentsURL, HttpMethod.GET, entity, String.class)).thenReturn(prReviewResponse);

        // When
        Map<String, List<String>> reviewerComments = insightsService.getRepositoryReviewComments(repoData);

        // Then
        assertEquals(0, reviewerComments.size());
    }

//    @Test
//    public void testGetCodeQualityEnhancementsInsights() {
//        // Mock necessary dependencies: RepoData, UserData, HttpHeaders, ResponseEntity, etc.
//        RepoData repoData = new RepoData();
//        repoData.setName("testRepo");
//        repoData.setUserId(123);
//
//        UserData userData = new UserData();
//        userData.setUserAccessToken("testToken");
//
//        when(userDataService.getOne(anyInt())).thenReturn(userData);
//
//        // Mock ResponseEntity for exchange calls
//        String prResponseJson = "[{\"title\":\"PR Title 1\",\"diff_url\":\"https://diffurl.com\"}]";
//        ResponseEntity<String> mockPRResponseEntity = new ResponseEntity<>(prResponseJson, HttpStatus.OK);
//        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
//                .thenReturn(mockPRResponseEntity);
//
//        String diffCode = "Sample diff code for the PR";
//        ResponseEntity<String> mockDiffCodeResponseEntity = new ResponseEntity<>(diffCode, HttpStatus.OK);
//        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
//                .thenReturn(mockDiffCodeResponseEntity);
//
//        // Mock chatGPTService response
//        String chatGPTResponse = "[{\"developer\":\"user1\",\"pr_title\":\"PR Title 1\","
//                + "\"code_improvements\":[\"Improvement 1\",\"Improvement 2\",\"Improvement 3\"],"
//                + "\"score\":[4,5,3,2],"
//                + "\"criteria\":[\"Readability\",\"Performance\",\"Correctness\",\"Scalability\"]}]";
//        when(chatGPTService.chat(anyString())).thenReturn(chatGPTResponse);
//
//        // Invoke the method
//        String insights = insightsService.getCodeQualityEnhancementsInsights(repoData);
//
//        // Verify the behavior
//        assertNotNull(insights);
//        assertTrue(insights.contains("user1"));
//        assertTrue(insights.contains("PR Title 1"));
//        assertTrue(insights.contains("Improvement 1"));
//        assertTrue(insights.contains("Improvement 2"));
//        assertTrue(insights.contains("Improvement 3"));
//        assertTrue(insights.contains("Readability"));
//        assertTrue(insights.contains("Performance"));
//        assertTrue(insights.contains("Correctness"));
//        assertTrue(insights.contains("Scalability"));
//        // Add more assertions based on the expected behavior
//    }

}
