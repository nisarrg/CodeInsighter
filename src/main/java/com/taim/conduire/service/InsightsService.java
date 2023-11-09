package com.taim.conduire.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.taim.conduire.constants.ConstantCodes;
import com.taim.conduire.domain.RepoData;
import com.taim.conduire.domain.UserData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class InsightsService implements ConstantCodes {

    @Autowired
    private RepoDataService repoDataService;

    @Autowired
    private UserDataService userDataService;

    @Autowired
    private RestTemplate restTemplate;

    public Map<String, List<String>> getRepositoryReviewComments(RepoData repoData) {
        String apiUrl = String.format("%s/repos/%s", GITHUB_API_URL, repoData.getName());
        System.out.println("apiUrl: " + apiUrl);

        UserData userData = userDataService.getOne(repoData.getUserId());
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", userData.getUserAccessToken());
        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(apiUrl, HttpMethod.GET, entity, String.class);

        String jsonObjectString = response.getBody();
        System.out.println("jsonObjectString: " + jsonObjectString);
        Gson gson = new Gson();
        JsonObject repoJsonObject = gson.fromJson(jsonObjectString, JsonObject.class);
        System.out.println("repoJsonObject: " + repoJsonObject);
        ResponseEntity<String> prReviewResposne = null;

        if(repoJsonObject.get("fork").getAsBoolean()){
            JsonObject repoSourceJsonObject = repoJsonObject.get("source").getAsJsonObject();
            String repoSourceURL = repoSourceJsonObject.get("url").getAsString();
            String prReviewCommentsURL = String.format(repoSourceURL + GITHUB_PULLS + GITHUB_COMMENTS);
            prReviewResposne = restTemplate.exchange(prReviewCommentsURL, HttpMethod.GET, entity, String.class);
        } else {
            String prReviewCommentsURL = String.format(apiUrl + GITHUB_PULLS + GITHUB_COMMENTS);
            prReviewResposne = restTemplate.exchange(prReviewCommentsURL, HttpMethod.GET, entity, String.class);
        }
        String prReviewJsonArrayString = prReviewResposne.getBody();
        JsonArray prReviewJsonArray = gson.fromJson(prReviewJsonArrayString, JsonArray.class);
        System.out.println("prReviewJsonArray: " + prReviewJsonArray.size());
        Map<String, List<String>> reviewerComments = new HashMap<>();

        for (JsonElement element : prReviewJsonArray) {
            if (element.isJsonObject()) {
                JsonObject reviewCommentObject = element.getAsJsonObject();
                String reviewer = reviewCommentObject.get("user").getAsJsonObject().get("login").getAsString();
                String reviewerComment = reviewCommentObject.get("body").getAsString();
                reviewerComments.computeIfAbsent(reviewer, k -> new ArrayList<>()).add(reviewerComment);
            }
        }
        System.out.println("reviewerComments: " + reviewerComments.size());
        System.out.println("reviewerComments: " + reviewerComments.toString());
        return reviewerComments;
    }
}
