package com.taim.conduire.service.impl;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.taim.conduire.constants.ConstantCodes;
import com.taim.conduire.domain.RepoData;
import com.taim.conduire.domain.UserData;
import com.taim.conduire.service.InsightsService;
import com.taim.conduire.service.RepoDataService;
import com.taim.conduire.service.UserDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class InsightsServiceImpl implements InsightsService, ConstantCodes {

    @Autowired
    private RepoDataService repoDataService;

    @Autowired
    private UserDataService userDataService;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ChatGPTServiceImpl chatGPTService;

    private static int countTokens(String input) {
        String[] tokens = input.split("\\s+|\\p{Punct}");
        return tokens.length;
    }

    public HttpEntity<String> getAllHeadersEntity(String userAccessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/vnd.github+json");
        headers.set("Authorization", "Bearer " + userAccessToken);
        headers.set("X-GitHub-Api-Version", "2022-11-28");
        HttpEntity<String> entity = new HttpEntity<>(headers);
        return entity;
    }

    public void showAvailableAPIHits(HttpHeaders responseHeaders) {
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

    public Map<String, List<String>> getRepositoryReviewComments(RepoData repoData) {
        String apiUrl = GITHUB_API_URL + GITHUB_REPOS + "/" + repoData.getName();
        System.out.println("apiUrl: " + apiUrl);

        UserData userData = userDataService.getOne(repoData.getUserId());
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", userData.getUserAccessToken());
        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(apiUrl, HttpMethod.GET, entity, String.class);

        String jsonObjectString = response.getBody();
        Gson gson = new Gson();
        JsonObject repoJsonObject = gson.fromJson(jsonObjectString, JsonObject.class);
        ResponseEntity<String> prReviewResposne = null;

        if (repoJsonObject.get("fork").getAsBoolean()) {
            JsonObject repoSourceJsonObject = repoJsonObject.get("source").getAsJsonObject();
            String repoSourceURL = repoSourceJsonObject.get("url").getAsString();
            String prReviewCommentsURL = String.format(repoSourceURL + GITHUB_PULLS + GITHUB_COMMENTS);
            // TODO: extract the same part form if-else
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
            // TODO: Early return
            if (element.isJsonObject()) {
                JsonObject reviewCommentObject = element.getAsJsonObject();
                String reviewer;
                if (reviewCommentObject.get("user").isJsonNull()) {
                    reviewer = "Bot";
                } else {
                    reviewer = reviewCommentObject.get("user").getAsJsonObject().get("login").getAsString();
                }
                String reviewerComment = reviewCommentObject.get("body").getAsString();
                reviewerComments.computeIfAbsent(reviewer, k -> new ArrayList<>()).add(reviewerComment);
            }
        }
        System.out.println("reviewerComments: " + reviewerComments.size());
        return reviewerComments;
    }

    public Map<String, List<String>> getDevPRCode(RepoData repoData) {
        String parentRepoName = repoDataService.getParentRepo(repoData);
        Gson gson = new Gson();
        String repoPRDataURL = GITHUB_API_URL + GITHUB_REPOS + "/" + parentRepoName + GITHUB_PULLS + "?per_page=100";
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

                // Check if the key already exists in the map
                if (!devAndPRCode.containsKey(prDev)) {
                    List<String> devPRTitleCodeList = new ArrayList<>();
                    devPRTitleCodeList.add(prTitle);
                    devPRTitleCodeList.add(devPRCode);

                    // Add the new entry to the map
                    devAndPRCode.put(prDev, devPRTitleCodeList);
                }
            }
        }

        System.out.println("devAndPRCode Size: " + devAndPRCode.size());

        return devAndPRCode;
    }

    public String getInsightsFromPromptAndDevPRCode(Map<String, List<String>> devAndPRCode, String llmInsightPrompt) {

        String llmInsightString;

        Integer llmTokenLimitWithPrompt = LLM_TOKEN_LIMIT - countTokens(llmInsightPrompt);
        Map<String, List<String>> devAndPRCodeWithLimit = new HashMap<>();

        for (Map.Entry<String, List<String>> entry : devAndPRCode.entrySet()) {

            String element = entry.getKey() + "=" + entry.getValue().toString() + ",";
            if (countTokens(devAndPRCodeWithLimit + element) <= llmTokenLimitWithPrompt) {
                devAndPRCodeWithLimit.put(entry.getKey(), entry.getValue());
                llmTokenLimitWithPrompt -= countTokens(devAndPRCodeWithLimit + element);
            }
        }
        System.out.println("devAndPRCodeWithLimit Size: " + devAndPRCodeWithLimit.size());
        System.out.println("Final code Token: " + countTokens(devAndPRCodeWithLimit.toString()));
        if (devAndPRCodeWithLimit.isEmpty()) {
            System.out.println("devAndPRCodeWithLimit: " + devAndPRCodeWithLimit.size());
            llmInsightString = "{\"message\":\"There are no Open PR for this Repository\"}";
        } else {
            String devAndPRCodeWithLimitString;
            if (devAndPRCodeWithLimit.size() > 3) {
                devAndPRCodeWithLimitString = devAndPRCode.entrySet()
                        .stream()
                        .limit(3)
                        .map(entry -> entry.getKey() + "=" + entry.getValue())
                        .collect(Collectors.joining(", ", "{", "}"));
            } else {
                devAndPRCodeWithLimitString = devAndPRCodeWithLimit.toString();
            }
            System.out.println("Final prompt Token: " + countTokens(llmInsightPrompt));
            System.out.println("Final devAndPRCodeWithLimitString Token: " + countTokens(devAndPRCodeWithLimitString));
            String promptAndCode = llmInsightPrompt + devAndPRCodeWithLimitString;

            System.out.println("Final prompt + code Token: " + countTokens(promptAndCode));
            llmInsightString = chatGPTService.chat(promptAndCode);
        }

        return llmInsightString;

    }

    public String getCodeQualityEnhancementsInsights(RepoData repoData) {
        Map<String, List<String>> devAndPRCode = getDevPRCode(repoData);
        String codeQualityEnhancementInsightPrompt = getCodeQualityEnhancementInsightLLMPrompt();
        String codeQualityEnhancementInsightString = getInsightsFromPromptAndDevPRCode(devAndPRCode, codeQualityEnhancementInsightPrompt);
        return codeQualityEnhancementInsightString;
    }

    private String getCodeQualityEnhancementInsightLLMPrompt() {

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
                "        \"developer\": \"<developer name>\",\n" +
                "        \"pr_title\": \"<pr title>\",\n" +
                "        \"code_improvements\": [<suggestion1>, <suggestion2>, <suggestion3>],\n" +
                "        \"score\": [<score1>, <score2>, <score3>, <score4>],\n" +
                "        \"criteria\": [\"<criterion1>\", \"<criterion2>\", \"<criterion3>\", \"<criterion4>\"]\n" +
                "    },\n" +
                "]\n" +
                "Keep the score and criteria in the same order so later on it can be fetched.\n\n";

        return codeQualityEnhancementInsightPrompt;
    }

    public String getBugDetectionInApplicationFlowInsights(RepoData repoData) {
        Map<String, List<String>> devAndPRCode = getDevPRCode(repoData);
        String bugDetectionInApplicationFlowInsightPrompt = getBugDetectionInApplicationFlowInsightLLMPrompt();
        String bugDetectionInApplicationFlowInsightString = getInsightsFromPromptAndDevPRCode(devAndPRCode, bugDetectionInApplicationFlowInsightPrompt);
        System.out.println(bugDetectionInApplicationFlowInsightString);
        return bugDetectionInApplicationFlowInsightString;
    }

    private String getBugDetectionInApplicationFlowInsightLLMPrompt() {

        String bugDetectionInApplicationFlowInsightPrompt = "The provided string is a map with \n" +
                "developers as key and value with list of 2 strings where\n" +
                "First string is the Title of the PR, and second string is the PR Code.\n" +
                "I want you to conduct bug detection to find unexpected bugs being introduced by pushed code in the application flows.\n" +
                "and I want you to display actionable recommendations for resolving these bugs.\n" +
                "Also, I want you to display alerts if this PR is introducing any bug in the application's major flows." +
                "and make your response in JSON Array format\n" +
                "Generate a JSON Array with the following pattern:\n" +
                "[\n" +
                "  {\n" +
                "    \"developer\": \"<developer_name>\",\n" +
                "    \"pr_title\": \"<title_string>\",\n" +
                "    \"bugs\": [\n" +
                "      {\n" +
                "        \"file_location\": \"<file_name_with_extension>\",\n" +
                "        \"code_in_file\": \"<code_string>\",\n" +
                "        \"issue\":  \"<issue_string>\",\n" +
                "        \"recommendation\": [\"<recommendation1>\", \"<recommendation2>\", \"<recommendation3>\", \"<recommendation4>\"]\n" +
                "      }\n" +
                "    ],\n" +
                "    \"alerts\": [\"<alert1>\", \"<alert2>\", \"<alert3>\", \"<alert4>\"],\n" +
                "    \"general_recommendation\": [\"<general_recommendation1>\", \"<general_recommendation2>\", \"<general_recommendation3>\", \"<general_recommendation4>\"]\n" +
                "  }\n" +
                "]";


        return bugDetectionInApplicationFlowInsightPrompt;
    }

}
