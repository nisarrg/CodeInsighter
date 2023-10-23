package com.taim.conduire.service.impl;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.taim.conduire.domain.RepoData;
import com.taim.conduire.domain.UserData;
import com.taim.conduire.repository.RepoDataRepository;
import com.taim.conduire.service.RepoDataService;
import com.taim.conduire.service.UserDataService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import com.taim.conduire.constants.ConstantCodes;
import org.springframework.web.client.RestTemplate;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

@Service
public class RepoDataServiceImpl implements RepoDataService {
	private static final Logger logger = LogManager.getLogger();

    @Autowired
    private RepoDataRepository repository;

    @Autowired
    private UserDataService userDataService;

    private final RestTemplate restTemplate;

    public RepoDataServiceImpl(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public JpaRepository<RepoData, Integer> getRepository() {
        return repository;
    }

    @Override
    public RepoData findByGithubRepoId(Integer githubRepoId) {
        return repository.findByGithubRepoId(githubRepoId);
    }


    @Override
    public String getRepoData(UserData userData) {
        String userRepoApiUrl = ConstantCodes.GITHUB_API_URL + ConstantCodes.GITHUB_USERS + "/" + userData.getUserName() + ConstantCodes.GITHUB_REPOS;
        System.out.println("userRepoApiUrl: " + userRepoApiUrl);
        return restTemplate.getForObject(userRepoApiUrl, String.class);
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
