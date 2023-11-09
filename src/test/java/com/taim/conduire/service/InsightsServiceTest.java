package com.taim.conduire.service;

import com.taim.conduire.constants.ConstantCodes;
import com.taim.conduire.domain.RepoData;
import com.taim.conduire.domain.UserData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class InsightsServiceTest implements ConstantCodes {

    @Mock
    private RepoDataService repoDataService;

    @Mock
    private UserDataService userDataService;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private InsightsService insightsService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void getRepositoryReviewComments_SuccessfulCall_ReturnsReviewerComments() {
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
        assertEquals(Arrays.asList("comment1"), reviewerComments.get("reviewer1"));
        assertEquals(Arrays.asList("comment2"), reviewerComments.get("reviewer2"));
    }

    @Test
    void getRepositoryReviewComments_ForkedRepo_ReturnsReviewerCommentsFromSource() {
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
        assertEquals(Arrays.asList("comment3"), reviewerComments.get("reviewer3"));
    }

    @Test
    void getRepositoryReviewComments_EmptyResponse_ReturnsEmptyMap() {
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

}
