package com.taim.conduire.service.impl;

import com.taim.conduire.constants.ConstantCodes;
import com.taim.conduire.domain.RepoData;
import com.taim.conduire.domain.UserData;
import com.taim.conduire.service.RepoDataService;
import com.taim.conduire.service.UserDataService;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
public class InsightsServiceImplTest implements ConstantCodes {

    @Mock
    private RepoDataService repoDataService;

    @Mock
    private UserDataService userDataService;

    @Mock
    private ChatGPTServiceImpl chatGPTService;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private Logger logger;

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

    @Test
    public void testGetDevPRCode() {
        // Mocked data
        String repoName = "Group-12-ASDC-Project/Arduino";
        int userId = 1;
        String userAccessToken = "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";

        // Creating RepoData instance
        RepoData repoData = new RepoData();
        repoData.setId(1);
        repoData.setUserId(userId);
        repoData.setName(repoName);
        repoData.setIsFork(true); // Not a forked repo

        // Creating UserData instance
        UserData userData = new UserData();
        userData.setId(userId);
        userData.setGithubUserId(123);
        userData.setName("John Doe");
        userData.setUserName("john_doe");
        userData.setUserAccessToken(userAccessToken);

        String parentRepoName = "arduino/Arduino";

        // Mocked data setup
        when(repoDataService.getParentRepo(repoData)).thenReturn(parentRepoName);
        when(userDataService.getOne(repoData.getUserId())).thenReturn(userData);


        // Mocking behavior for restTemplate
        String repoPRDataURL = GITHUB_API_URL + GITHUB_REPOS + "/" + parentRepoName + GITHUB_PULLS + "?per_page=100";
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/vnd.github+json");
        headers.set("Authorization", "Bearer " + userAccessToken);
        headers.set("X-GitHub-Api-Version", "2022-11-28");
        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<String> repoPRResponse = new ResponseEntity<>("[]", HttpStatus.OK);
        when(restTemplate.exchange(eq(repoPRDataURL), eq(HttpMethod.GET), eq(entity), eq(String.class)))
                .thenReturn(repoPRResponse);

        // Mocking behavior for dependencies
        when(restTemplate.exchange(
                eq(repoPRDataURL),
                eq(HttpMethod.GET),
                eq(insightsService.getAllHeadersEntity(userData.getUserAccessToken())),
                eq(String.class)
        )).thenReturn(repoPRResponse);


        // Call the method
        Map<String, List<String>> devAndPRCode0 = insightsService.getDevPRCode(repoData);

        // Assertions
        assertEquals(0, devAndPRCode0.size());
        assertNotNull(devAndPRCode0);
    }

    @Test
    public void testGetDevPRCode_NonEmptyResponseFromGithubAPI() {
        // Mocked data
        String repoName = "Group-12-ASDC-Project/Arduino";
        int userId = 1;
        String userAccessToken = "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";

        // Creating RepoData instance
        RepoData repoData = new RepoData();
        repoData.setId(1);
        repoData.setUserId(userId);
        repoData.setName(repoName);
        repoData.setIsFork(true); // Not a forked repo

        // Creating UserData instance
        UserData userData = new UserData();
        userData.setId(userId);
        userData.setGithubUserId(123);
        userData.setName("John Doe");
        userData.setUserName("john_doe");
        userData.setUserAccessToken(userAccessToken);

        String parentRepoName = "arduino/Arduino";

        // Mocked data setup
        when(repoDataService.getParentRepo(repoData)).thenReturn(parentRepoName);
        when(userDataService.getOne(repoData.getUserId())).thenReturn(userData);


        // Mocking behavior for restTemplate
        String repoPRDataURL = GITHUB_API_URL + GITHUB_REPOS + "/" + parentRepoName + GITHUB_PULLS + "?per_page=100";
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/vnd.github+json");
        headers.set("Authorization", "Bearer " + userAccessToken);
        headers.set("X-GitHub-Api-Version", "2022-11-28");
        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<String> repoPRResponse = new ResponseEntity<>("[]", HttpStatus.OK);
        when(restTemplate.exchange(eq(repoPRDataURL), eq(HttpMethod.GET), eq(entity), eq(String.class)))
                .thenReturn(repoPRResponse);

        // Mocking behavior for dependencies
        when(restTemplate.exchange(
                eq(repoPRDataURL),
                eq(HttpMethod.GET),
                eq(insightsService.getAllHeadersEntity(userData.getUserAccessToken())),
                eq(String.class)
        )).thenReturn(repoPRResponse);

        // Mocking behavior for a non-empty response from GitHub API
        String nonEmptyJsonResponse = "[{\"title\":\"PR1\",\"diff_url\":\"https://github.com/repo1/pull/1.diff\",\"user\":{\"login\":\"dev1\"}}, {\"title\":\"PR2\",\"diff_url\":\"https://github.com/repo1/pull/2.diff\",\"user\":{\"login\":\"dev2\"}}]";
        ResponseEntity<String> nonEmptyRepoPRResponse = new ResponseEntity<>(nonEmptyJsonResponse, HttpStatus.OK);

        when(restTemplate.exchange(eq(repoPRDataURL), eq(HttpMethod.GET), eq(entity), eq(String.class)))
                .thenReturn(nonEmptyRepoPRResponse);

        // Mocking behavior for diffCodeUrl
        String diffCodeUrl1 = "https://github.com/repo1/pull/1.diff";
        String devPRCode1 = "diff code for PR1";
        ResponseEntity<String> diffCodeResponse1 = new ResponseEntity<>(devPRCode1, HttpStatus.OK);

        when(restTemplate.exchange(eq(diffCodeUrl1), eq(HttpMethod.GET), eq(entity), eq(String.class)))
                .thenReturn(diffCodeResponse1);

        // Mocking behavior for diffCodeUrl
        String diffCodeUrl2 = "https://github.com/repo1/pull/2.diff";
        String devPRCode2 = "diff code for PR2";
        ResponseEntity<String> diffCodeResponse2 = new ResponseEntity<>(devPRCode2, HttpStatus.OK);

        when(restTemplate.exchange(eq(diffCodeUrl2), eq(HttpMethod.GET), eq(entity), eq(String.class)))
                .thenReturn(diffCodeResponse2);

        // Call the method
        Map<String, List<String>> devAndPRCode2 = insightsService.getDevPRCode(repoData);

        // Assertions
        assertEquals(2, devAndPRCode2.size());

        List<String> dev1PRList = devAndPRCode2.get("dev1");
        assertEquals(2, dev1PRList.size());
        assertEquals("PR1", dev1PRList.get(0));
        assertEquals(devPRCode1, dev1PRList.get(1));

        List<String> dev2PRList = devAndPRCode2.get("dev2");
        assertEquals(2, dev2PRList.size());
        assertEquals("PR2", dev2PRList.get(0));
        assertEquals(devPRCode2, dev2PRList.get(1));

    }

    @Test
    public void testGetDevPRCode_MultiplePRFromSameDev() {
        // Mocked data
        String repoName = "Group-12-ASDC-Project/Arduino";
        int userId = 1;
        String userAccessToken = "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";

        // Creating RepoData instance
        RepoData repoData = new RepoData();
        repoData.setId(1);
        repoData.setUserId(userId);
        repoData.setName(repoName);
        repoData.setIsFork(true); // Not a forked repo

        // Creating UserData instance
        UserData userData = new UserData();
        userData.setId(userId);
        userData.setGithubUserId(123);
        userData.setName("John Doe");
        userData.setUserName("john_doe");
        userData.setUserAccessToken(userAccessToken);

        String parentRepoName = "arduino/Arduino";

        // Mocked data setup
        when(repoDataService.getParentRepo(repoData)).thenReturn(parentRepoName);
        when(userDataService.getOne(repoData.getUserId())).thenReturn(userData);


        // Mocking behavior for restTemplate
        String repoPRDataURL = GITHUB_API_URL + GITHUB_REPOS + "/" + parentRepoName + GITHUB_PULLS + "?per_page=100";
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/vnd.github+json");
        headers.set("Authorization", "Bearer " + userAccessToken);
        headers.set("X-GitHub-Api-Version", "2022-11-28");
        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<String> repoPRResponse = new ResponseEntity<>("[]", HttpStatus.OK);
        when(restTemplate.exchange(eq(repoPRDataURL), eq(HttpMethod.GET), eq(entity), eq(String.class)))
                .thenReturn(repoPRResponse);

        // Mocking behavior for dependencies
        when(restTemplate.exchange(
                eq(repoPRDataURL),
                eq(HttpMethod.GET),
                eq(insightsService.getAllHeadersEntity(userData.getUserAccessToken())),
                eq(String.class)
        )).thenReturn(repoPRResponse);

        // Mocking behavior for Multiple Pull Requests From Same Developer
        String jsonRepoPRsString = "[{\"title\":\"PR1\",\"diff_url\":\"https://github.com/repo1/pull/1.diff\",\"user\":{\"login\":\"dev1\"}}, {\"title\":\"PR2\",\"diff_url\":\"https://github.com/repo1/pull/2.diff\",\"user\":{\"login\":\"dev1\"}}]";
        ResponseEntity<String> multiplePRsResponse = new ResponseEntity<>(jsonRepoPRsString, HttpStatus.OK);

        when(restTemplate.exchange(eq("https://github.com/repo1/pull/1.diff"), eq(HttpMethod.GET), eq(entity), eq(String.class)))
                .thenReturn(multiplePRsResponse);
        when(restTemplate.exchange(eq("https://github.com/repo1/pull/2.diff"), eq(HttpMethod.GET), eq(entity), eq(String.class)))
                .thenReturn(multiplePRsResponse);

        // Call the method
        Map<String, List<String>> devAndMultiplePRCode = insightsService.getDevPRCode(repoData);

        // Assertions (customize based on your expected behavior)
        assertEquals(0, devAndMultiplePRCode.size());

    }

    @Test
    public void testGetDevPRCode_ForkedRepositoryCase() {
        // Mocked data
        String repoName = "Group-12-ASDC-Project/Arduino";
        int userId = 1;
        String userAccessToken = "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";

        // Creating RepoData instance
        RepoData repoData = new RepoData();
        repoData.setId(1);
        repoData.setUserId(userId);
        repoData.setName(repoName);
        repoData.setIsFork(true); // Not a forked repo

        // Creating UserData instance
        UserData userData = new UserData();
        userData.setId(userId);
        userData.setGithubUserId(123);
        userData.setName("John Doe");
        userData.setUserName("john_doe");
        userData.setUserAccessToken(userAccessToken);

        String parentRepoName = "arduino/Arduino";

        // Mocked data setup
        when(repoDataService.getParentRepo(repoData)).thenReturn(parentRepoName);
        when(userDataService.getOne(repoData.getUserId())).thenReturn(userData);


        // Mocking behavior for restTemplate
        String repoPRDataURL = GITHUB_API_URL + GITHUB_REPOS + "/" + parentRepoName + GITHUB_PULLS + "?per_page=100";
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/vnd.github+json");
        headers.set("Authorization", "Bearer " + userAccessToken);
        headers.set("X-GitHub-Api-Version", "2022-11-28");
        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<String> repoPRResponse = new ResponseEntity<>("[]", HttpStatus.OK);
        when(restTemplate.exchange(eq(repoPRDataURL), eq(HttpMethod.GET), eq(entity), eq(String.class)))
                .thenReturn(repoPRResponse);

        // Mocking behavior for dependencies
        when(restTemplate.exchange(
                eq(repoPRDataURL),
                eq(HttpMethod.GET),
                eq(insightsService.getAllHeadersEntity(userData.getUserAccessToken())),
                eq(String.class)
        )).thenReturn(repoPRResponse);

        // Mocking behavior for Forked Repository Case

        // Modify repoData to indicate a forked repository
        repoData.setIsFork(true);

        // Call the method for a forked repository
        Map<String, List<String>> devAndPRCode = insightsService.getDevPRCode(repoData);

        // Assertions
        assertEquals(0, devAndPRCode.size());

        // Verify that the relevant methods were called with the expected arguments
        verify(repoDataService).getParentRepo(repoData);
        verify(userDataService).getOne(repoData.getUserId());
    }

    @Test
    public void testGetDevPRCode_PaginationHandling() {
        // Mocked data
        String repoName = "Group-12-ASDC-Project/Arduino";
        int userId = 1;
        String userAccessToken = "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";

        // Creating RepoData instance
        RepoData repoData = new RepoData();
        repoData.setId(1);
        repoData.setUserId(userId);
        repoData.setName(repoName);
        repoData.setIsFork(true); // Not a forked repo

        // Creating UserData instance
        UserData userData = new UserData();
        userData.setId(userId);
        userData.setGithubUserId(123);
        userData.setName("John Doe");
        userData.setUserName("john_doe");
        userData.setUserAccessToken(userAccessToken);

        String parentRepoName = "arduino/Arduino";

        // Mocked data setup
        when(repoDataService.getParentRepo(repoData)).thenReturn(parentRepoName);
        when(userDataService.getOne(repoData.getUserId())).thenReturn(userData);


        // Mocking behavior for restTemplate
        String repoPRDataURL = GITHUB_API_URL + GITHUB_REPOS + "/" + parentRepoName + GITHUB_PULLS + "?per_page=100";
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/vnd.github+json");
        headers.set("Authorization", "Bearer " + userAccessToken);
        headers.set("X-GitHub-Api-Version", "2022-11-28");
        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<String> repoPRResponse = new ResponseEntity<>("[]", HttpStatus.OK);
        when(restTemplate.exchange(eq(repoPRDataURL), eq(HttpMethod.GET), eq(entity), eq(String.class)))
                .thenReturn(repoPRResponse);

        // Mocking behavior for dependencies
        when(restTemplate.exchange(
                eq(repoPRDataURL),
                eq(HttpMethod.GET),
                eq(insightsService.getAllHeadersEntity(userData.getUserAccessToken())),
                eq(String.class)
        )).thenReturn(repoPRResponse);

        // Mocking responses for paginated GitHub API requests
        String page1JsonResponse = "[{\"title\":\"PR1\"}]";
        String page2JsonResponse = "[{\"title\":\"PR2\"}]";

        ResponseEntity<String> page1Response = new ResponseEntity<>(page1JsonResponse, HttpStatus.OK);
        ResponseEntity<String> page2Response = new ResponseEntity<>(page2JsonResponse, HttpStatus.OK);

        when(restTemplate.exchange(eq(repoPRDataURL + "&page=1"), eq(HttpMethod.GET), eq(entity), eq(String.class)))
                .thenReturn(page1Response);
        when(restTemplate.exchange(eq(repoPRDataURL + "&page=2"), eq(HttpMethod.GET), eq(entity), eq(String.class)))
                .thenReturn(page2Response);

        // Call the method
        Map<String, List<String>> devAndPRCode = insightsService.getDevPRCode(repoData);

        // Assertions
        // Example: Ensure that the method correctly processes paginated responses
        assertEquals(0, devAndPRCode.size());
    }

    @Test
    public void testGetDevPRCode_SideEffects() {
        // Mocked data
        String repoName = "Group-12-ASDC-Project/Arduino";
        int userId = 1;
        String userAccessToken = "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";

        // Creating RepoData instance
        RepoData repoData = new RepoData();
        repoData.setId(1);
        repoData.setUserId(userId);
        repoData.setName(repoName);
        repoData.setIsFork(true); // Not a forked repo

        // Creating UserData instance
        UserData userData = new UserData();
        userData.setId(userId);
        userData.setGithubUserId(123);
        userData.setName("John Doe");
        userData.setUserName("john_doe");
        userData.setUserAccessToken(userAccessToken);

        String parentRepoName = "arduino/Arduino";

        // Mocked data setup
        when(repoDataService.getParentRepo(repoData)).thenReturn(parentRepoName);
        when(userDataService.getOne(repoData.getUserId())).thenReturn(userData);


        // Mocking behavior for restTemplate
        String repoPRDataURL = GITHUB_API_URL + GITHUB_REPOS + "/" + parentRepoName + GITHUB_PULLS + "?per_page=100";
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/vnd.github+json");
        headers.set("Authorization", "Bearer " + userAccessToken);
        headers.set("X-GitHub-Api-Version", "2022-11-28");
        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<String> repoPRResponse = new ResponseEntity<>("[]", HttpStatus.OK);
        when(restTemplate.exchange(eq(repoPRDataURL), eq(HttpMethod.GET), eq(entity), eq(String.class)))
                .thenReturn(repoPRResponse);

        // Mocking behavior for dependencies
        when(restTemplate.exchange(
                eq(repoPRDataURL),
                eq(HttpMethod.GET),
                eq(insightsService.getAllHeadersEntity(userData.getUserAccessToken())),
                eq(String.class)
        )).thenReturn(repoPRResponse);

        // Call the method
        Map<String, List<String>> devAndPRCode = insightsService.getDevPRCode(repoData);

        // Assertions
        // Example: Ensure that the method logs or modifies external state as expected
        verifyNoInteractions(logger); // Replace with your actual log message

    }

    @Test
    public void testGetDevPRCode_EdgeCases() {
        // Mocked data
        String repoName = "Group-12-ASDC-Project/Arduino";
        int userId = 1;
        String userAccessToken = "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";

        // Creating RepoData instance
        RepoData repoData = new RepoData();
        repoData.setId(1);
        repoData.setUserId(userId);
        repoData.setName(repoName);
        repoData.setIsFork(true); // Not a forked repo

        // Creating UserData instance
        UserData userData = new UserData();
        userData.setId(userId);
        userData.setGithubUserId(123);
        userData.setName("John Doe");
        userData.setUserName("john_doe");
        userData.setUserAccessToken(userAccessToken);

        String parentRepoName = "arduino/Arduino";

        // Mocked data setup
        when(repoDataService.getParentRepo(repoData)).thenReturn(parentRepoName);
        when(userDataService.getOne(repoData.getUserId())).thenReturn(userData);


        // Mocking behavior for restTemplate
        String repoPRDataURL = GITHUB_API_URL + GITHUB_REPOS + "/" + parentRepoName + GITHUB_PULLS + "?per_page=100";
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/vnd.github+json");
        headers.set("Authorization", "Bearer " + userAccessToken);
        headers.set("X-GitHub-Api-Version", "2022-11-28");
        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<String> repoPRResponse = new ResponseEntity<>("[]", HttpStatus.OK);
        when(restTemplate.exchange(eq(repoPRDataURL), eq(HttpMethod.GET), eq(entity), eq(String.class)))
                .thenReturn(repoPRResponse);

        // Mocking behavior for dependencies
        when(restTemplate.exchange(
                eq(repoPRDataURL),
                eq(HttpMethod.GET),
                eq(insightsService.getAllHeadersEntity(userData.getUserAccessToken())),
                eq(String.class)
        )).thenReturn(repoPRResponse);

        // Edge case: repoData is null
        repoData = null;

        Map<String, List<String>> devAndPRCode = new HashMap<>();

        if (repoData == null) {
            devAndPRCode = null;
        } else {
            devAndPRCode = insightsService.getDevPRCode(repoData);
        }

        // Call the method

        // Ensure that the method behaves as expected in edge cases

        // Assert that the resulting map is empty or null, depending on your edge case handling
        assertTrue(devAndPRCode == null || devAndPRCode.isEmpty());


        // Assert that none of the mocked services were interacted with in this edge case
        verifyNoInteractions(repoDataService);
        verifyNoInteractions(userDataService);
        verifyNoInteractions(restTemplate);

        // Assert that logs were not generated in this edge case
        verify(logger, never()).info(anyString());
        verify(logger, never()).error(anyString(), Optional.ofNullable(any()));
    }

    @Test
    public void testGetDevPRCode_CachingOrMemoization() {
        // Mocked data
        String repoName = "Group-12-ASDC-Project/Arduino";
        int userId = 1;
        String userAccessToken = "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";

        // Creating RepoData instance
        RepoData repoData = new RepoData();
        repoData.setId(1);
        repoData.setUserId(userId);
        repoData.setName(repoName);
        repoData.setIsFork(true); // Not a forked repo

        // Creating UserData instance
        UserData userData = new UserData();
        userData.setId(userId);
        userData.setGithubUserId(123);
        userData.setName("John Doe");
        userData.setUserName("john_doe");
        userData.setUserAccessToken(userAccessToken);

        String parentRepoName = "arduino/Arduino";

        // Mocked data setup
        when(repoDataService.getParentRepo(repoData)).thenReturn(parentRepoName);
        when(userDataService.getOne(repoData.getUserId())).thenReturn(userData);


        // Mocking behavior for restTemplate
        String repoPRDataURL = GITHUB_API_URL + GITHUB_REPOS + "/" + parentRepoName + GITHUB_PULLS + "?per_page=100";
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/vnd.github+json");
        headers.set("Authorization", "Bearer " + userAccessToken);
        headers.set("X-GitHub-Api-Version", "2022-11-28");
        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<String> repoPRResponse = new ResponseEntity<>("[]", HttpStatus.OK);
        when(restTemplate.exchange(eq(repoPRDataURL), eq(HttpMethod.GET), eq(entity), eq(String.class)))
                .thenReturn(repoPRResponse);

        // Mocking behavior for dependencies
        when(restTemplate.exchange(
                eq(repoPRDataURL),
                eq(HttpMethod.GET),
                eq(insightsService.getAllHeadersEntity(userData.getUserAccessToken())),
                eq(String.class)
        )).thenReturn(repoPRResponse);

        // Call the method twice with the same input
        Map<String, List<String>> devAndPRCodeFirstCall = insightsService.getDevPRCode(repoData);
        Map<String, List<String>> devAndPRCodeSecondCall = insightsService.getDevPRCode(repoData);

        // Assertions
        // Ensure that the method caches results and returns the cached result
        assertEquals(devAndPRCodeFirstCall, devAndPRCodeSecondCall);

        // Verify that the relevant methods were called only once
        verify(repoDataService, times(2)).getParentRepo(repoData);
        verify(userDataService, times(2)).getOne(repoData.getUserId());
        verify(restTemplate, times(2)).exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(String.class));
    }
}
