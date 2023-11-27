package com.taim.conduire.service.impl;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.taim.conduire.constants.ConstantCodes;
import com.taim.conduire.controller.UserRepoInsightsController;
import com.taim.conduire.domain.RepoData;
import com.taim.conduire.domain.UserData;
import com.taim.conduire.repository.RepoDataRepository;
import com.taim.conduire.service.ChatGPTService;
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

    private UserRepoInsightsController userRepoInsightsController;

    @Autowired
    private ChatGPTService chatGPTService;

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

    /**
     * Creates an HTTP entity with headers for GitHub API requests.
     *
     * @param userAccessToken The user access token for authentication.
     * @return HttpEntity<String> containing the necessary headers for GitHub API requests.
     * @author Zeel Ravalani
     */
    public HttpEntity<String> getAllHeadersEntity(String userAccessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/vnd.github+json");
        headers.set("Authorization", "Bearer " + userAccessToken);
        headers.set("X-GitHub-Api-Version", "2022-11-28");
        HttpEntity<String> entity = new HttpEntity<>(headers);
        return entity;
    }

    /**
     * Displays information about available GitHub API hits based on the response headers.
     *
     * @param responseHeaders The HttpHeaders containing information about API rate limits.
     * @author Zeel Ravalani
     */
    @Override
    public void showAvailableAPIHits(HttpHeaders responseHeaders) {
        int limit = Integer.parseInt(responseHeaders.getFirst("X-RateLimit-Limit"));
        int remaining = Integer.parseInt(responseHeaders.getFirst("X-RateLimit-Remaining"));

        logger.debug("GitHub API Hit Limit: " + limit);
        logger.debug("GitHub API Hit Limit Remaining: " + remaining);
    }


    /**
     * Retrieves repository data for a user from the GitHub API.
     *
     * @param userData The UserData object representing the user.
     * @return String containing the repository data for the user.
     * @author Zeel Ravalani
     */
    @Override
    public String getRepoData(UserData userData) {
        String userRepoApiUrl = GITHUB_API_URL + GITHUB_USERS + "/" + userData.getUserName() + GITHUB_REPOS;
        logger.debug("userRepoApiUrl: " + userRepoApiUrl);

        ResponseEntity<String> response = restTemplate.exchange(userRepoApiUrl, HttpMethod.GET,
                getAllHeadersEntity(userData.getUserAccessToken()), String.class);

        showAvailableAPIHits(response.getHeaders());
        return response.getBody();
    }


    @Override
    public Map<String, Integer> getRepositoryLanguages(RepoData repoData) {
        String repoLanguagesAPIURL = String.format("%s/repos/%s/languages", GITHUB_API_URL, repoData.getName());
        logger.debug("Languages API: " + repoLanguagesAPIURL);

        UserData userData = userDataService.getOne(repoData.getUserId());
        ResponseEntity<Map> response = restTemplate.exchange(repoLanguagesAPIURL, HttpMethod.GET,
                getAllHeadersEntity(userData.getUserAccessToken()), Map.class);
        showAvailableAPIHits(response.getHeaders());
        return response.getBody();
    }

    @Override
    public Integer getRepositoryPRs(RepoData repoData) {
        String repoPRApiURL = String.format("%s/repos/%s/pulls", GITHUB_API_URL, getParentRepo(repoData));
        logger.debug("Repository PR API: " + repoPRApiURL);

        UserData userData = userDataService.getOne(repoData.getUserId());

        ResponseEntity<String> response = restTemplate.exchange(repoPRApiURL, HttpMethod.GET,
                getAllHeadersEntity(userData.getUserAccessToken()), String.class);
        showAvailableAPIHits(response.getHeaders());
        String jsonArrayString = response.getBody();
        Gson gson = new Gson();
        JsonArray jsonArray = gson.fromJson(jsonArrayString, JsonArray.class);
        return jsonArray.size();
    }

    @Override
    public Integer getRepositoryForksCount(RepoData repoData) {
        String parenRepoApiURL = String.format("%s/repos/%s", GITHUB_API_URL, getParentRepo(repoData));
        logger.debug("Parent Repo API: " + parenRepoApiURL);

        UserData userData = userDataService.getOne(repoData.getUserId());

        ResponseEntity<String> response = restTemplate.exchange(parenRepoApiURL, HttpMethod.GET,
                getAllHeadersEntity(userData.getUserAccessToken()), String.class);
        showAvailableAPIHits(response.getHeaders());
        String jsonArrayString = response.getBody();
        Gson gson = new Gson();
        JsonObject jsonObject = gson.fromJson(jsonArrayString, JsonObject.class);
        return jsonObject.get("forks_count").getAsInt();
    }

    @Override
    public String getParentRepo(RepoData repoData) {
        String parentRepository;
        Gson gson = new Gson();
        Boolean isFork = repoData.getIsFork();

        if (isFork != null && isFork) {
            String repoURL = GITHUB_API_URL + GITHUB_REPOS + "/" + repoData.getName();
            logger.debug("repoURL: " + repoURL);

            UserData userData = userDataService.getOne(repoData.getUserId());
            ResponseEntity<String> response = restTemplate.exchange(repoURL, HttpMethod.GET,
                    getAllHeadersEntity(userData.getUserAccessToken()), String.class);
            showAvailableAPIHits(response.getHeaders());

            String jsonRepoString = response.getBody();

            JsonObject jsonRepoObject = gson.fromJson(jsonRepoString, JsonObject.class);
            JsonObject sourceJsonObject = jsonRepoObject.get("source").getAsJsonObject();
            parentRepository = sourceJsonObject.get("full_name").getAsString();
        } else {
            parentRepository = repoData.getName();
        }
        return parentRepository;
    }

    @Override
    public String getRepoLOC(RepoData repoData) {
        String userRepoLocApiUrl = CODETABS_CLOC_API_URL + repoData.getName();
        logger.debug("Repo LOC API: " + userRepoLocApiUrl);
        boolean repoTooBig = false;
        List<Map<String, Object>> locArrMap = new ArrayList<>();
        try {
            locArrMap = restTemplate.getForObject(userRepoLocApiUrl, List.class);

        } catch (HttpClientErrorException.BadRequest e) {
            String responseBody = e.getResponseBodyAsString();
            if (responseBody.contains("too big")) {
                logger.debug("Repo Size > 500 MB: " + responseBody);
            } else {
                logger.debug("Other BadRequest Exception: " + responseBody);
            }
            // above TO-DO refers to this variable which was inside the loop earlier
            repoTooBig = true;

        } catch (Exception e) {
            logger.debug("An error occurred: " + e.getMessage());
            repoTooBig = true;
        }
        if (repoTooBig) {
            return "Repo > 500 MB";
        } else {

            Map<String, Integer> resultLoc = new HashMap<>();
            for (Map<String, Object> loc : locArrMap) {
                String language = (String) loc.get("language");
                Integer linesOfCode = (Integer) loc.get("linesOfCode");
                resultLoc.put(language, linesOfCode);
            }
            logger.debug("\nTotal Loc: " + resultLoc.get("Total"));

            return "" + resultLoc.get("Total");

        }
    }

    @Override
    public Map<String, Integer> getRepoContributors(RepoData repoData) {
        String apiUrl = String.format("%s/repos/%s/contributors", GITHUB_API_URL, repoData.getName());
        logger.debug("Contributors API: " + apiUrl);
        UserData userData = userDataService.getOne(repoData.getUserId());
        ResponseEntity<List> response = restTemplate.exchange(apiUrl, HttpMethod.GET,
                getAllHeadersEntity(userData.getUserAccessToken()), List.class);
        List<Map<String, Object>> contributors = response.getBody();
        Map<String, Integer> resultContributors = new HashMap<>();
        for (Map<String, Object> contributor : contributors) {
            String contributorName = (String) contributor.get("login");
            int contributions = (Integer) contributor.get("contributions");
            resultContributors.put(contributorName, contributions);
        }
        showAvailableAPIHits(response.getHeaders());
        return resultContributors;
    }

    /**
     * Retrieves and processes repository data for a user from the GitHub API and updates the database.
     *
     * @param userData The UserData object representing the user.
     * @return A String indicating the success of the repository data dump.
     * @throws ParseException If an error occurs while parsing date strings.
     * @author Zeel Ravalani
     */
    @Override
    public String dumpRepoData(UserData userData) {
        try {
            String jsonArrayString = getRepoData(userData);
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
            Gson gson = new Gson();
            JsonArray jsonArray = gson.fromJson(jsonArrayString, JsonArray.class);

            for (JsonElement element : jsonArray) {
                if (element.isJsonObject()) {
                    JsonObject repoObject = element.getAsJsonObject();
                    RepoData repoData = null;

                    if (findByGithubRepoId(repoObject.get("id").getAsInt()) != null) {
                        repoData = findByGithubRepoId(repoObject.get("id").getAsInt());
                    } else {
                        repoData = new RepoData();
                        repoData.setCreatedAt(new Date());
                    }
                    repoData.setGithubRepoId(repoObject.get("id").getAsInt());
                    repoData.setUserId(userData.getId());
                    repoData.setName(
                            !repoObject.get("full_name").isJsonNull() ? repoObject.get("full_name").getAsString() : "");
                    repoData.setDescription(
                            !repoObject.get("description").isJsonNull() ? repoObject.get("description").getAsString()
                                    : "");
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
                    repoData.setDefaultBranch(!repoObject.get("default_branch").isJsonNull()
                            ? repoObject.get("default_branch").getAsString()
                            : "");
                    repoData.setLanguage(
                            !repoObject.get("language").isJsonNull() ? repoObject.get("language").getAsString() : "");
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
