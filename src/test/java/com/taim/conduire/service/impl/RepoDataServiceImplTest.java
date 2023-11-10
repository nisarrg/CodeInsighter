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
import org.springframework.web.client.RestTemplate;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    void testFindByGithubRepoId() {
        // Arrange
        Integer githubRepoId = 123;
        RepoData expectedRepoData = new RepoData();
        when(repository.findByGithubRepoId(githubRepoId)).thenReturn(expectedRepoData);

        // Act
        RepoData actualRepoData = repoDataService.findByGithubRepoId(githubRepoId);

        // Assert
        assertEquals(expectedRepoData, actualRepoData);
    }

    @Test
    void testFindByUserId() {
        // Arrange
        Integer userId = 456;
        List<RepoData> expectedRepoDataList = new ArrayList<>();
        when(repository.findByUserId(userId)).thenReturn(expectedRepoDataList);

        // Act
        List<RepoData> actualRepoDataList = repoDataService.findByUserId(userId);

        // Assert
        assertEquals(expectedRepoDataList, actualRepoDataList);
    }

    @Test
    void testGetRepoData() {
        // Arrange
        UserData userData = new UserData();
        userData.setUserName("testUser");
        userData.setUserAccessToken("testAccessToken");
        String expectedResponseBody = "testResponseBody";

        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.set("X-RateLimit-Limit", "100");
        responseHeaders.set("X-RateLimit-Remaining", "50");

        ResponseEntity<String> mockResponseEntity = new ResponseEntity<>(expectedResponseBody, responseHeaders, HttpStatus.OK);
        when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(String.class)))
                .thenReturn(mockResponseEntity);

        // Act
        String result = repoDataService.getRepoData(userData);

        // Assert
        assertEquals(expectedResponseBody, result);
    }

    @Test
    void testGetRepositoryLanguages() {
        // Arrange
        RepoData repoData = new RepoData();
        repoData.setName("testRepo");

        UserData userData = new UserData();
        userData.setUserAccessToken("testAccessToken");
        repoData.setUserId(1); // Set a valid user ID for the repoData

        Map<String, Integer> expectedLanguages = new HashMap<>();
        expectedLanguages.put("Java", 1000);
        expectedLanguages.put("Python", 500);

        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.set("X-RateLimit-Limit", "100");
        responseHeaders.set("X-RateLimit-Remaining", "50");

        ResponseEntity<Map> mockResponseEntity = new ResponseEntity<>(expectedLanguages, responseHeaders, HttpStatus.OK);
        when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(mockResponseEntity);

        // Adjust the user ID in the following line
        when(userDataService.getOne(eq(1))).thenReturn(userData); // Mocking userDataService

        // Act
        Map<String, Integer> result = repoDataService.getRepositoryLanguages(repoData);

        // Assert
        assertEquals(expectedLanguages, result);
    }

//    @Test
//    void testGetRepoLOC() {
//        // Arrange
//        RepoData repoData = new RepoData();
//        repoData.setName("testRepo");
//
//        List<Map<String, Object>> mockLocArrMap = new ArrayList<>();
//        Map<String, Object> mockLocMap1 = new HashMap<>();
//        mockLocMap1.put("Java", "Java");
//        mockLocMap1.put("Total", 1000);
//        mockLocArrMap.add(mockLocMap1);
//
//        // Adjust the return type to match the actual implementation
//        when(restTemplate.getForObject(anyString(), eq(List.class))).thenReturn(mockLocArrMap);
//
//        // Act
//        String result = repoDataService.getRepoLOC(repoData);
//
//        // Assert
//        assertEquals("1000", result);
//    }

    @Test
    void testGetRepoContributors() {
        // Arrange
        RepoData repoData = new RepoData();
        repoData.setName("testRepo");
        repoData.setUserId(1); // Set a valid user ID

        UserData userData = new UserData();
        userData.setUserAccessToken("testAccessToken");

        List<Map<String, Object>> mockContributors = new ArrayList<>();
        Map<String, Object> mockContributor1 = new HashMap<>();
        mockContributor1.put("login", "contributor1");
        mockContributor1.put("contributions", 10);
        mockContributors.add(mockContributor1);

        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.set("X-RateLimit-Limit", "100");
        responseHeaders.set("X-RateLimit-Remaining", "50");

        ResponseEntity<List> mockResponseEntity = new ResponseEntity<>(mockContributors, responseHeaders, HttpStatus.OK);
        when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(List.class)))
                .thenReturn(mockResponseEntity);

        when(userDataService.getOne(eq(1))).thenReturn(userData);

        // Act
        Map<String, Integer> result = repoDataService.getRepoContributors(repoData);

        // Assert
        assertEquals(mockContributors.size(), result.size());
        assertEquals(10, result.get("contributor1"));
    }

//    @Test
//    void testDumpRepoData() throws ParseException {
//        // Arrange
//        UserData userData = new UserData();
//        userData.setId(789);
//        userData.setUserName("testUser");
//        userData.setUserAccessToken("testAccessToken");
//
//        String jsonArrayString = "[{\"id\": 123, \"full_name\": \"testUser/testRepo\", \"description\": \"Test Repo\", \"private\": false, \"fork\": false, \"size\": 100, \"has_issues\": true, \"has_projects\": true, \"has_downloads\": true, \"has_wiki\": true, \"forks_count\": 5, \"forks\": 3, \"open_issues\": 2, \"open_issues_count\": 2, \"default_branch\": \"main\", \"language\": \"Java\", \"created_at\": \"2023-01-01T12:00:00Z\", \"updated_at\": \"2023-01-02T12:00:00Z\"}]";
//
//        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
//        Date repoCreatedAt = dateFormat.parse("2023-01-01T12:00:00Z");
//        Date repoUpdatedAt = dateFormat.parse("2023-01-02T12:00:00Z");
//
//        RepoData expectedRepoData = new RepoData();
//        expectedRepoData.setGithubRepoId(123);
//        expectedRepoData.setUserId(789);
//        expectedRepoData.setName("testUser/testRepo");
//        expectedRepoData.setDescription("Test Repo");
//        expectedRepoData.setIsPrivate(false);
//        expectedRepoData.setIsFork(false);
//        expectedRepoData.setSize(100);
//        expectedRepoData.setHasIssues(true);
//        expectedRepoData.setHasProjects(true);
//        expectedRepoData.setHasDownloads(true);
//        expectedRepoData.setHasWiki(true);
//        expectedRepoData.setForksCount(5);
//        expectedRepoData.setForks(3);
//        expectedRepoData.setOpenIssues(2);
//        expectedRepoData.setOpenIssuesCount(2);
//        expectedRepoData.setDefaultBranch("main");
//        expectedRepoData.setLanguage("Java");
//        expectedRepoData.setRepoCreatedAt(repoCreatedAt);
//        expectedRepoData.setRepoUpdatedAt(repoUpdatedAt);
//        expectedRepoData.setCreatedAt(new Date());
//        expectedRepoData.setUpdatedAt(new Date());
//
//        when(repoDataService.getRepoData(userData)).thenReturn(jsonArrayString);
//        when(repository.findByGithubRepoId(123)).thenReturn(null);
//        when(repository.save(any(RepoData.class))).thenReturn(expectedRepoData);
//
//        // Mock the response for restTemplate.exchange
//        HttpHeaders responseHeaders = new HttpHeaders();
//        responseHeaders.set("X-RateLimit-Limit", "100");
//        responseHeaders.set("X-RateLimit-Remaining", "50");
//
//        // Adjust the response entity type to match the method signature (String.class)
//        ResponseEntity<String> mockResponseEntity = new ResponseEntity<>(jsonArrayString, responseHeaders, HttpStatus.OK);
//
//        // Mock the behavior of restTemplate.exchange more precisely
//        when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(String.class)))
//                .thenReturn(mockResponseEntity);
//
//        // Act
//        String result = repoDataService.dumpRepoData(userData);
//
//        // Assert
//        assertEquals("dump success", result);
//        Mockito.verify(repository, Mockito.times(1)).save(any(RepoData.class));
//    }


}
