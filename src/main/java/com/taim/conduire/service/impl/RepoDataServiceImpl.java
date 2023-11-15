package com.taim.conduire.service.impl;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.taim.conduire.constants.ConstantCodes;
import com.taim.conduire.domain.RepoData;
import com.taim.conduire.domain.UserData;
import com.taim.conduire.repository.RepoDataRepository;
import com.taim.conduire.service.RepoDataService;
import com.taim.conduire.service.UserDataService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class RepoDataServiceImpl implements RepoDataService, ConstantCodes {
    private static final Logger logger = LogManager.getLogger();

    @Autowired
    private RepoDataRepository repository;

    @Autowired
    private UserDataService userDataService;

    @Autowired
    private RestTemplate restTemplate;

    public JpaRepository<RepoData, Integer> getRepository() {
        return repository;
    }

    @Override
    public RepoData findByGithubRepoId(Integer githubRepoId) {
        return repository.findByGithubRepoId(githubRepoId);
    }

    @Override
    public List<RepoData> findByUserId(Integer userId) {
        return repository.findByUserId(userId);
    }

    private HttpEntity<String> getAllHeadersEntity(String userAccessToken){
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/vnd.github+json");
        headers.set("Authorization", "Bearer " + userAccessToken);
        headers.set("X-GitHub-Api-Version", "2022-11-28");
        HttpEntity<String> entity = new HttpEntity<>(headers);
        return entity;
    }

    private void showAvailableAPIHits(HttpHeaders responseHeaders){
        int limit = Integer.parseInt(responseHeaders.getFirst("X-RateLimit-Limit"));
        int remaining = Integer.parseInt(responseHeaders.getFirst("X-RateLimit-Remaining"));

        System.out.println("GitHub API Hit Limit: " + limit);
        System.out.println("GitHub API Hit Limit Remaining: " + remaining);
    }

    @Override
    public String getRepoData(UserData userData) {
        String userRepoApiUrl = GITHUB_API_URL + GITHUB_USERS + "/" + userData.getUserName() + GITHUB_REPOS;
        System.out.println("userRepoApiUrl: " + userRepoApiUrl);
        ResponseEntity<String> response = restTemplate.exchange(userRepoApiUrl, HttpMethod.GET, getAllHeadersEntity(userData.getUserAccessToken()), String.class);
        showAvailableAPIHits(response.getHeaders());
        return response.getBody();

    }

    public Map<String, Integer> getRepositoryLanguages(RepoData repoData) {
        String apiUrl = String.format("%s/repos/%s/languages", GITHUB_API_URL, repoData.getName());
        System.out.println("apiUrl: " + apiUrl);

        UserData userData = userDataService.getOne(repoData.getUserId());
        ResponseEntity<Map> response = restTemplate.exchange(apiUrl, HttpMethod.GET, getAllHeadersEntity(userData.getUserAccessToken()), Map.class);
        showAvailableAPIHits(response.getHeaders());
        return response.getBody();
    }

    public Integer getRepositoryPRs(RepoData repoData) {
        String apiUrl = String.format("%s/repos/%s/pulls", GITHUB_API_URL, repoData.getName());
        System.out.println("apiUrl: " + apiUrl);

        UserData userData = userDataService.getOne(repoData.getUserId());

        ResponseEntity<String> response = restTemplate.exchange(apiUrl, HttpMethod.GET, getAllHeadersEntity(userData.getUserAccessToken()), String.class);
        showAvailableAPIHits(response.getHeaders());
        String jsonArrayString = response.getBody();;
        System.out.println("jsonArrayString: " + jsonArrayString);

        Gson gson = new Gson();

        JsonArray jsonArray = gson.fromJson(jsonArrayString, JsonArray.class);
        System.out.println("jsonArray: " + jsonArray);

        return jsonArray.size();
    }

    public String getRepoLOC(RepoData repoData) {

        System.out.println("repoloc called: ");
        String userRepoLocApiUrl = CODETABS_CLOC_API_URL + repoData.getName();
        System.out.println("userRepoLocApiUrl: " + userRepoLocApiUrl);
        boolean repoTooBig = false;
        List<Map<String,Object>> locArrMap = new ArrayList<>();
        try {
            locArrMap = restTemplate.getForObject(userRepoLocApiUrl, List.class);
            System.out.println("\n\nlocArrMap: " + repoData.getName() + "\t" + locArrMap + "\n\n");

        } catch (HttpClientErrorException.BadRequest e) {
            String responseBody = e.getResponseBodyAsString();
            if (responseBody.contains("too big")) {
                System.out.println("Repo Size > 500 MB: " + responseBody);
                repoTooBig = true;
            } else {
                System.out.println("Other BadRequest Exception: " + responseBody);
                repoTooBig = true;
            }

        } catch (Exception e) {
            System.out.println("An error occurred: " + e.getMessage());
            repoTooBig = true;
        }
        if(repoTooBig){
            return "Repo > 500 MB";
        } else {

            Map<String, Integer> resultLoc = new HashMap<>();
            for (Map<String, Object> loc : locArrMap) {
                String language = (String) loc.get("language");
                int linesOfCode = (Integer) loc.get("linesOfCode");
                resultLoc.put(language, linesOfCode);
            }
            System.out.println("\nTotal Loc: " + resultLoc.get("Total"));

            return "" + resultLoc.get("Total");
        }
    }

    public Map<String, Integer> getRepoContributors(RepoData repoData){
        String apiUrl = String.format("%s/repos/%s/contributors", GITHUB_API_URL, repoData.getName());
        System.out.println("Contributors API: " + apiUrl);
        UserData userData = userDataService.getOne(repoData.getUserId());
        ResponseEntity<List> response = restTemplate.exchange(apiUrl, HttpMethod.GET, getAllHeadersEntity(userData.getUserAccessToken()), List.class);
        List<Map<String,Object>> contributors = response.getBody();
        Map<String, Integer> resultContributors = new HashMap<>();
        for (Map<String, Object> contributor : contributors) {
            String contributorName = (String) contributor.get("login");
            int contributions = (Integer) contributor.get("contributions");
            resultContributors.put(contributorName, contributions);
        }
        showAvailableAPIHits(response.getHeaders());

        return resultContributors;
    }

    public String dumpRepoData(UserData userData){
        try {
            String jsonArrayString = getRepoData(userData);
            System.out.println("jsonArrayString: " + jsonArrayString);
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

            Gson gson = new Gson();

            JsonArray jsonArray = gson.fromJson(jsonArrayString, JsonArray.class);
            System.out.println("jsonArray: " + jsonArray);

            for (JsonElement element : jsonArray) {
                if (element.isJsonObject()) {
                    JsonObject repoObject = element.getAsJsonObject();
                    RepoData repoData = null;

                    if(findByGithubRepoId(repoObject.get("id").getAsInt()) != null) {
                        repoData = findByGithubRepoId(repoObject.get("id").getAsInt());
                    }else {
                        repoData = new RepoData();
                        repoData.setCreatedAt(new Date());
                    }
                    repoData.setGithubRepoId(repoObject.get("id").getAsInt());
                    repoData.setUserId(userData.getId());
                    repoData.setName(!repoObject.get("full_name").isJsonNull() ? repoObject.get("full_name").getAsString() : "");
                    repoData.setDescription(!repoObject.get("description").isJsonNull() ? repoObject.get("description").getAsString() : "");
                    repoData.setIsPrivate(repoObject.get("private").getAsBoolean());
                    repoData.setIsFork(repoObject.get("fork").getAsBoolean());
                    repoData.setSize(repoObject.get("size").getAsInt());
                    repoData.setHasIssues(repoObject.get("has_issues").getAsBoolean());
                    repoData.setHasProjects(repoObject.get("has_projects").getAsBoolean());
                    repoData.setHasDownloads(repoObject.get("has_downloads").getAsBoolean());
                    repoData.setHasWiki(repoObject.get("has_wiki").getAsBoolean());
                    repoData.setForksCount(repoObject.get("forks_count").getAsInt());
                    repoData.setForks(repoObject.get("forks").getAsInt());
                    repoData.setOpenIssues(repoObject.get("open_issues").getAsInt());
                    repoData.setOpenIssuesCount(repoObject.get("open_issues_count").getAsInt());
                    repoData.setDefaultBranch(!repoObject.get("default_branch").isJsonNull() ? repoObject.get("default_branch").getAsString() : "");
                    repoData.setLanguage(!repoObject.get("language").isJsonNull() ? repoObject.get("language").getAsString() : "");
                    repoData.setRepoCreatedAt(dateFormat.parse(repoObject.get("created_at").getAsString()));
                    repoData.setRepoUpdatedAt(dateFormat.parse(repoObject.get("updated_at").getAsString()));
                    repoData.setUpdatedAt(new Date());
                    repoData = update(repoData);
                }
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return "dump success";
    }
}
