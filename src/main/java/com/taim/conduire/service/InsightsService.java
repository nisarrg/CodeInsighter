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
import java.util.stream.Collectors;

@Service
public class InsightsService implements ConstantCodes {

    @Autowired
    private RepoDataService repoDataService;

    @Autowired
    private UserDataService userDataService;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ChatGPTService chatGPTService;

    HttpEntity<String> getAllHeadersEntity(String userAccessToken){
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/vnd.github+json");
        headers.set("Authorization", "Bearer " + userAccessToken);
        headers.set("X-GitHub-Api-Version", "2022-11-28");
        HttpEntity<String> entity = new HttpEntity<>(headers);
        return entity;
    }

    void showAvailableAPIHits(HttpHeaders responseHeaders){
        String limitHeader = responseHeaders.getFirst("X-RateLimit-Limit");
        String remainingHeader = responseHeaders.getFirst("X-RateLimit-Remaining");

        if (limitHeader != null && remainingHeader != null) {
            try {
                int limit = Integer.parseInt(limitHeader);
                int remaining = Integer.parseInt(remainingHeader);

                System.out.println("GitHub API Hit Limit: " + limit);
                System.out.println("GitHub API Hit Limit Remaining: " + remaining);
            } catch (NumberFormatException e) {
                System.err.println("Error parsing API hit limit or remaining headers: " + e.getMessage());
            }
        }
    }

    private static int countTokens(String input) {
        // Split the input string into tokens based on spaces and some common punctuation marks
        String[] tokens = input.split("\\s+|\\p{Punct}");
        return tokens.length;
    }

    public Map<String, List<String>> getRepositoryReviewComments(RepoData repoData) {
        String apiUrl = GITHUB_API_URL + GITHUB_REPOS + "/" + repoData.getName();
//        String apiUrl = String.format("%s/repos/%s", GITHUB_API_URL, repoData.getName());
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
    public String getCodeQualityEnhancementsInsights(RepoData repoData) {
        String parentRepoName;
        Gson gson = new Gson();
        Boolean isFork = repoData.getIsFork();
        if(isFork != null && isFork){
            String repoURL = GITHUB_API_URL + GITHUB_REPOS + "/" + repoData.getName();
            System.out.println("repoURL: " + repoURL);

            UserData userData = userDataService.getOne(repoData.getUserId());
            ResponseEntity<String> response = restTemplate.exchange(repoURL, HttpMethod.GET, getAllHeadersEntity(userData.getUserAccessToken()), String.class);
            showAvailableAPIHits(response.getHeaders());

            String jsonRepoString = response.getBody();;

            JsonObject jsonRepoObject = gson.fromJson(jsonRepoString, JsonObject.class);
            JsonObject sourceJsonObject = jsonRepoObject.get("source").getAsJsonObject();
            parentRepoName = sourceJsonObject.get("full_name").getAsString();
        } else {
            parentRepoName = repoData.getName();
        }
        String repoPRDataURL = GITHUB_API_URL + GITHUB_REPOS + "/" + parentRepoName + GITHUB_PULLS + "?per_page=100&sort=popularity";
        System.out.println("repoPRDataURL: " + repoPRDataURL);

        UserData userData = userDataService.getOne(repoData.getUserId());
        ResponseEntity<String> response = restTemplate.exchange(repoPRDataURL, HttpMethod.GET, getAllHeadersEntity(userData.getUserAccessToken()), String.class);
        showAvailableAPIHits(response.getHeaders());

        String jsonRepoPRsString = response.getBody();
        JsonArray jsonRepoPRsArray = gson.fromJson(jsonRepoPRsString, JsonArray.class);
        System.out.println("No: of Open PR: " + jsonRepoPRsArray.size());
        Map<String, List<String>> devAndPRCode = new HashMap<>();

        for (JsonElement element : jsonRepoPRsArray) {
            if (element.isJsonObject()) {
                JsonObject prItemJsonObject = element.getAsJsonObject();

                String prTitle = prItemJsonObject.get("title").getAsString();
                String diffCodeUrl = prItemJsonObject.get("diff_url").getAsString();
                ResponseEntity<String> responseDiffCode = restTemplate.exchange(diffCodeUrl, HttpMethod.GET, getAllHeadersEntity(userData.getUserAccessToken()), String.class);
                showAvailableAPIHits(responseDiffCode.getHeaders());
                String devPRCode = responseDiffCode.getBody();

                JsonObject sourceJsonObject = prItemJsonObject.get("user").getAsJsonObject();
                String prDev = sourceJsonObject.get("login").getAsString();

                List<String> devPRTitleCodeList = new ArrayList<>();
                devPRTitleCodeList.add(prTitle);
                devPRTitleCodeList.add(devPRCode);

                devAndPRCode.put(prDev,devPRTitleCodeList);
            }
        }

        System.out.println("devAndPRCode Size: " + devAndPRCode.size());

        String codeQualityEnhancementInsightPrompt = "The provided string is a map with \n" +
                "developers as key and value with list of 2 strings where\n" +
                "First string is the Title of the PR, and second string is the PR Code.\n" +
                "Based on different criteria: Readability, Performance, Correctness, Scalability\n" +
                "Can give a some Code improvements suggestions/comments and\n" +
                "A score for each criteria from 0 to 5 as I want to show it in a visual graph format\n" +
                "please mention for all 4 criteria (Readability, Performance, Correctness, Scalability) even if you don't find them you can score them as 0 if not found.\n" +
                "and make your response in JSON Array format\n" +
                "Generate a JSON array with the following pattern:\n" +
                "[\n" +
                "    {\n" +
                "        \"developer\": \"<developer_name>\",\n" +
                "        \"pr_title\": \"<title_string>\",\n" +
                "        \"code_improvements\": [<suggestion1>, <suggestion2>, <suggestion3>],\n" +
                "        \"score\": [<score1>, <score2>, <score3>, <score4>],\n" +
                "        \"criteria\": [\"<criterion1>\", \"<criterion2>\", \"<criterion3>\", \"<criterion4>\"]\n" +
                "    },\n" +
                "    {\n" +
                "        \"developer\": \"<developer_name>\",\n" +
                "        \"pr_title\": \"<title_string>\",\n" +
                "        \"code_improvements\": [<suggestion1>, <suggestion2>, <suggestion3>],\n" +
                "        \"score\": [<score1>, <score2>, <score3>, <score4>],\n" +
                "        \"criteria\": [\"<criterion1>\", \"<criterion2>\", \"<criterion3>\", \"<criterion4>\"]\n" +
                "    },\n" +
                "    {\n" +
                "        \"developer\": \"<developer_name>\",\n" +
                "        \"pr_title\": \"<title_string>\",\n" +
                "        \"code_improvements\": [<suggestion1>, <suggestion2>, <suggestion3>],\n" +
                "        \"score\": [<score1>, <score2>, <score3>, <score4>],\n" +
                "        \"criteria\": [\"<criterion1>\", \"<criterion2>\", \"<criterion3>\", \"<criterion4>\"]\n" +
                "    }\n" +
                "]\n"+
                "Keep the score and criteria in the same order so later on it can be fetched.\n\n" ;

        Integer llmTokenLimitWithPrompt = LLM_TOKEN_LIMIT - countTokens(codeQualityEnhancementInsightPrompt);
        Map<String, List<String>> devAndPRCodeWithLimit = new HashMap<>();

        for (Map.Entry<String, List<String>> entry : devAndPRCode.entrySet()) {

            String element = entry.getKey() + "=" + entry.getValue().toString() + ",";
            if (countTokens(devAndPRCodeWithLimit.toString() + element) <= llmTokenLimitWithPrompt) {
                devAndPRCodeWithLimit.put(entry.getKey(), entry.getValue());
                llmTokenLimitWithPrompt -= countTokens(devAndPRCodeWithLimit.toString() + element);
            }
        }
        System.out.println("devAndPRCodeWithLimit Size: " + devAndPRCodeWithLimit.size());
        System.out.println("Final code Token: " + countTokens(devAndPRCodeWithLimit.toString()));
        String codeQualityEnhancementInsightString;
        if(devAndPRCodeWithLimit.isEmpty()){
            System.out.println("devAndPRCodeWithLimit: " + devAndPRCodeWithLimit);
            codeQualityEnhancementInsightString = "{\"message\":\"There are no Open PR for this Repository\"}";
        } else {
            String devAndPRCodeWithLimitString;
            if(devAndPRCodeWithLimit.size() > 3){
                devAndPRCodeWithLimitString = devAndPRCode.entrySet()
                        .stream()
                        .limit(3)
                        .map(entry -> entry.getKey() + "=" + entry.getValue())
                        .collect(Collectors.joining(", ", "{", "}"));
            } else {
                devAndPRCodeWithLimitString = devAndPRCodeWithLimit.toString();
            }
            String promptAmdCode = codeQualityEnhancementInsightPrompt + devAndPRCodeWithLimitString;

            System.out.println("Final prompt + code Token: " + countTokens(promptAmdCode));
            codeQualityEnhancementInsightString = chatGPTService.chat(promptAmdCode);
            System.out.println(codeQualityEnhancementInsightString);
        }

        return codeQualityEnhancementInsightString;
    }
}
