package com.taim.conduire.service.impl;

import com.taim.conduire.domain.RepoData;
import com.taim.conduire.domain.UserData;
import com.taim.conduire.repository.RepoDataRepository;
import com.taim.conduire.service.UserDataService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class RepoDataServiceImplTest {

    @Mock
    private RepoDataRepository repository;

    @Mock
    private UserDataService userDataService;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private RepoDataServiceImpl repoDataService;

    @Test
    public void testFindByGithubRepoId_Positive() {
        // Mock RepoData object with ID 1
        RepoData mockRepoData = new RepoData();
        mockRepoData.setGithubRepoId(1);

        when(repository.findByGithubRepoId(1)).thenReturn(mockRepoData);

        RepoData foundRepoData = repoDataService.findByGithubRepoId(1);

        assertEquals(mockRepoData, foundRepoData);
    }

    @Test
    public void testFindByGithubRepoId_Negative() {
        when(repository.findByGithubRepoId(2)).thenReturn(null);

        RepoData foundRepoData = repoDataService.findByGithubRepoId(2);

        assertNull(foundRepoData);
    }

    @Test
    public void testFindByUserId_Positive() {
        // Mocking user ID and creating a list of RepoData for that user
        int userId = 123;
        List<RepoData> repoList = new ArrayList<>();
        repoList.add(new RepoData());
        repoList.add(new RepoData());

        when(repository.findByUserId(userId)).thenReturn(repoList);

        List<RepoData> foundRepoList = repoDataService.findByUserId(userId);

        assertEquals(2, foundRepoList.size());
    }

    @Test
    public void testFindByUserId_Negative() {
        // Mocking user ID and providing an empty list of RepoData
        int userId = 456;
        List<RepoData> emptyRepoList = new ArrayList<>();

        when(repository.findByUserId(userId)).thenReturn(emptyRepoList);

        List<RepoData> foundRepoList = repoDataService.findByUserId(userId);

        assertTrue(foundRepoList.isEmpty());
    }

    @Test
    public void testGetRepoData_Positive() {
        // Mocking UserData object
        UserData userData = new UserData();
        userData.setUserName("exampleUser");
        userData.setUserAccessToken("exampleToken");

        // Mocking a response from GitHub API
        String responseBody = "[{\"id\":1,\"name\":\"repo1\"},{\"id\":2,\"name\":\"repo2\"}]";
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.set("X-RateLimit-Limit", "5000");
        responseHeaders.set("X-RateLimit-Remaining", "4999");
        ResponseEntity<String> responseEntity = ResponseEntity.ok().headers(responseHeaders).body(responseBody);

        when(restTemplate.exchange(anyString(), any(), any(), eq(String.class)))
                .thenReturn(responseEntity);

        String repoData = repoDataService.getRepoData(userData);

        assertNotNull(repoData);
        // Assertions based on the expected response from the GitHub API
        assertTrue(repoData.contains("repo1"));
        assertTrue(repoData.contains("repo2"));
    }

    @Test
    public void testGetRepoData_Negative() {
        // Mocking UserData object with missing access token
        UserData userData = new UserData();
        userData.setUserName("exampleUser");
        // No access token set

        // Simulating a failure in retrieving data from GitHub API by throwing an exception
        when(restTemplate.exchange(anyString(), any(), any(), eq(String.class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND)); // Simulate 404 Not Found

        String repoData;
        try {
            repoData = repoDataService.getRepoData(userData);
            fail("Expected an exception due to API call failure, but no exception was thrown.");
        } catch (HttpClientErrorException e) {
            assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
            // Add further assertions or handling based on the expected behavior when the API call fails
        } catch (Exception e) {
            fail("Unexpected exception type: " + e.getClass().getSimpleName());
        }
    }

    @Test
    public void testGetRepositoryLanguages_Positive() {
        // Mocking RepoData object
        RepoData repoData = new RepoData();
        repoData.setName("exampleRepo");
        repoData.setUserId(1);

        // Mocking UserData and ResponseEntity
        UserData userData = new UserData();
        userData.setUserAccessToken("dummyAccessToken");

        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.set("X-RateLimit-Limit", "5000");
        responseHeaders.set("X-RateLimit-Remaining", "4999");

        Map<String, Integer> languagesMap = Collections.singletonMap("Java", 100); // Example language map

        ResponseEntity<Map> responseEntity = ResponseEntity.ok()
                .headers(responseHeaders)
                .body(languagesMap);

        // Mocking UserDataService and RestTemplate
        when(userDataService.getOne(anyInt())).thenReturn(userData);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(responseEntity);

        // Call the method to test
        Map<String, Integer> result = repoDataService.getRepositoryLanguages(repoData);

        // Assertions
        assertNotNull(result);
        assertTrue(result.containsKey("Java"));
        assertEquals(100, result.get("Java"));
    }

    @Test
    public void testGetRepositoryLanguages_Negative() {
        // Mocking RepoData object for a non-existent repository
        RepoData repoData = new RepoData();
        repoData.setName("nonexistentRepo");
        repoData.setUserId(1);

        // Mocking a non-existent repository response from GitHub API
        String apiUrl = "https://api.github.com/repos/username/nonexistentRepo/languages";
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.set("X-RateLimit-Limit", "5000");
        responseHeaders.set("X-RateLimit-Remaining", "4999");
        ResponseEntity<Map> responseEntity = ResponseEntity.notFound().headers(responseHeaders).build();

        when(userDataService.getOne(anyInt())).thenReturn(new UserData());

        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(responseEntity);

        // Call the method to test
        Map<String, Integer> languages = repoDataService.getRepositoryLanguages(repoData);

        // Assertions
        assertNull(languages);
    }

    @Test
    public void testGetParentRepo_Positive() {
        // Mocking RepoData object for a forked repository
        RepoData forkedRepoData = new RepoData();
        forkedRepoData.setName("forkedRepo");
        forkedRepoData.setUserId(1);
        forkedRepoData.setIsFork(true);

        // Mocking a response from GitHub API for a forked repository's parent
        String apiUrl = "https://api.github.com/repos/username/forkedRepo";
        String responseBody = "{\"source\": {\"full_name\": \"username/parentRepo\"}}";
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.set("X-RateLimit-Limit", "5000");
        responseHeaders.set("X-RateLimit-Remaining", "4999");
        ResponseEntity<String> responseEntity = ResponseEntity.ok().headers(responseHeaders).body(responseBody);

        when(userDataService.getOne(1)).thenReturn(new UserData());
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(responseEntity);

        String parentRepo = repoDataService.getParentRepo(forkedRepoData);

        assertNotNull(parentRepo);
        assertEquals("username/parentRepo", parentRepo);
        // Add assertions based on the expected response from the GitHub API for a forked repository
    }

    @Test
    public void testGetParentRepo_Negative() {
        // Mocking RepoData object for a non-forked repository
        RepoData nonForkedRepoData = new RepoData();
        nonForkedRepoData.setName("nonForkedRepo");
        nonForkedRepoData.setUserId(1);
        nonForkedRepoData.setIsFork(false);

        // The method should return the same name as it's not a forked repository
        String parentRepo = repoDataService.getParentRepo(nonForkedRepoData);

        assertNotNull(parentRepo);
        assertEquals("nonForkedRepo", parentRepo);
        // Add assertions or handling for the scenario when the repository is not a fork
    }

    @Test
    public void testGetRepoLOC_Positive() {
        // Mocking RepoData object
        RepoData repoData = new RepoData();
        repoData.setName("exampleRepo");
        repoData.setUserId(1);

        // Mocking a response from the external API for repository lines of code (LOC)
        String apiUrl = "https://api.codetabs.com/v1/loc?github=exampleRepo";
        List<Map<String, Object>> locArrMap = new ArrayList<>();
        Map<String, Object> language1 = new HashMap<>();
        language1.put("language", "Total");
        language1.put("linesOfCode", 1000);
        locArrMap.add(language1);

        when(restTemplate.getForObject(apiUrl, List.class)).thenReturn(locArrMap);

        String repoLOC = repoDataService.getRepoLOC(repoData);

        assertNotNull(repoLOC);
        assertEquals("1000", repoLOC);
    }

    @Test
    public void testGetRepoLOC_Negative() {
        // Mocking RepoData object
        RepoData repoData = new RepoData();
        repoData.setName("largeRepo");
        repoData.setUserId(1);

        // Simulating an error response from the external API for a large repository
        String apiUrl = "https://api.codetabs.com/v1/loc?github=username/largeRepo";

        when(userDataService.getOne(1)).thenReturn(new UserData());
        when(restTemplate.getForObject(apiUrl, List.class)).thenThrow(new RuntimeException("Repository > 500 MB"));

        String repoLOC = repoDataService.getRepoLOC(repoData);

        assertEquals("Repo > 500 MB", repoLOC);
        // Add assertions or handling for the scenario when the repository size exceeds the limit or the API call fails
    }

    @Test
    public void testGetRepoContributors_Positive() {
        // Mocking RepoData object
        RepoData repoData = new RepoData();
        repoData.setName("exampleRepo");
        repoData.setUserId(1);

        // Mocking a response from GitHub API for repository contributors
        String apiUrl = "https://api.github.com/repos/username/exampleRepo/contributors";
        List<Map<String, Object>> contributors = Arrays.asList(
                createContributorMap("John", 10),
                createContributorMap("Alice", 15)
        );
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.set("X-RateLimit-Limit", "5000");
        responseHeaders.set("X-RateLimit-Remaining", "4999");
        ResponseEntity<List> responseEntity = ResponseEntity.ok().headers(responseHeaders).body(contributors);

        when(userDataService.getOne(1)).thenReturn(new UserData());
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(List.class)))
                .thenReturn(responseEntity);

        Map<String, Integer> repoContributors = repoDataService.getRepoContributors(repoData);

        assertNotNull(repoContributors);
        assertEquals(2, repoContributors.size());
        assertTrue(repoContributors.containsKey("John"));
        assertTrue(repoContributors.containsKey("Alice"));
        assertEquals(10, repoContributors.get("John"));
        assertEquals(15, repoContributors.get("Alice"));
    }

    @Test
    void getRepositoryPRsTest(){
        RepoData repoData = new RepoData();
    }

//    @Test
//    public void testGetRepoContributors_Negative() {
//        // Mocking RepoData object
//        RepoData repoData = new RepoData();
//        repoData.setName("nonexistentRepo");
//        repoData.setUserId(1);
//
//        // Setting contributors to null to simulate the scenario when API response doesn't contain contributors
//        List<Map<String, Object>> contributors = null;
//
//        // Mocking a response from GitHub API with null contributors
//        String apiUrl = "https://api.github.com/repos/username/nonexistentRepo/contributors";
//        HttpHeaders responseHeaders = new HttpHeaders();
//        responseHeaders.set("X-RateLimit-Limit", "5000");
//        responseHeaders.set("X-RateLimit-Remaining", "4999");
//        ResponseEntity<List> responseEntity = ResponseEntity.ok().headers(responseHeaders).body(null);
//
//        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(List.class)))
//                .thenReturn(responseEntity);
//
//        Map<String, Integer> repoContributors = repoDataService.getRepoContributors(repoData);
//
//        assertNotNull(repoContributors);
//        assertTrue(repoContributors.isEmpty());
//        // Add assertions or handling for the scenario when the repository doesn't exist or the GitHub API call fails
//    }

    private Map<String, Object> createContributorMap(String name, int contributions) {
        return Map.of("login", name, "contributions", contributions);
    }
}
