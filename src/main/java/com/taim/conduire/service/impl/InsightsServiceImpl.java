package com.taim.conduire.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.taim.conduire.constants.ConstantCodes;
import com.taim.conduire.domain.RepoData;
import com.taim.conduire.domain.UserData;
import com.taim.conduire.service.InsightsService;
import com.taim.conduire.service.LLMService;
import com.taim.conduire.service.RepoDataService;
import com.taim.conduire.service.UserDataService;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class InsightsServiceImpl implements InsightsService, ConstantCodes  {

    @Autowired
    private RepoDataService repoDataService;

    @Autowired
    private UserDataService userDataService;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ChatGPTServiceImpl chatGPTService;

    @Autowired
    private JSONUtilsImpl jsonUtils;

    @Autowired
    private LLMService llmService;

    private static final Logger logger = LoggerFactory.getLogger(InsightsService.class);

    private static final String FILE_TYPE = "file";
    private static final String DIR_TYPE = "dir";

    private boolean found_pom_flag = false;


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

    public String getCommonCodeMistakesInsights(RepoData repoData) {
        Map<String, List<String>> reviewerComments = getRepositoryReviewComments(repoData);
        String commonCodeMistakesPrompt = "These are open PR review comments by the reviewer:"
                + reviewerComments.toString() + "\n." +
                "Can you give me some insights of Common code mistakes based upon these comments.\n" +
                "Please consider yourself as a Business Analyst and write in Technical English.\n" +
                "And please frame it as if you are writing this response in <p></p> tag of html so to make sure its properly formatted "
                +
                "using html and shown to user. Make sure you break it into most important points and limit it to only 5 points "
                +
                "and highlight your reasoning. And Format the response in HTML tags and use Bootstrap classes for better readability";
        String commonCodeMistakesInsight = chatGPTService.chat(commonCodeMistakesPrompt);
        Gson gson = new Gson();
        String jsonInsight = gson.toJson(commonCodeMistakesInsight);
        String finalResult = "{\"insights\":" + jsonInsight + "}";
        System.out.println("finalResult: " + finalResult);

        return finalResult;
    }

    public Map<String, List<String>> getDevPRCode(RepoData repoData) {
        String parentRepoName = repoDataService.getParentRepo(repoData);
        Gson gson = new Gson();
        String repoPRDataURL = GITHUB_API_URL + GITHUB_REPOS + "/" + parentRepoName + GITHUB_PULLS + "?per_page=100";
        System.out.println("repoPRDataURL: " + repoPRDataURL);

        UserData userData = userDataService.getOne(repoData.getUserId());
        ResponseEntity<String> response = restTemplate.exchange(repoPRDataURL, HttpMethod.GET,
                getAllHeadersEntity(userData.getUserAccessToken()), String.class);
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
                ResponseEntity<String> responseDiffCode = restTemplate.exchange(diffCodeUrl, HttpMethod.GET,
                        getAllHeadersEntity(userData.getUserAccessToken()), String.class);
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
        String codeQualityEnhancementInsightString = getInsightsFromPromptAndDevPRCode(devAndPRCode,
                codeQualityEnhancementInsightPrompt);
        return codeQualityEnhancementInsightString;
    }

    private String getCodeQualityEnhancementInsightLLMPrompt() {

        String codeQualityEnhancementInsightPrompt = "The provided string is a map with \n" +
                "developers as key and value with list of 2 strings where\n" +
                "First string is the Title of the PR, and second string is the PR Code.\n" +
                "Based on different criteria: Readability, Performance, Correctness, Scalability\n" +
                "Can give a some Code improvements suggestions/comments and\n" +
                "A score for each criteria from 0 to 5 as I want to show it in a visual graph format\n" +
                "please mention for all 4 criteria (Readability, Performance, Correctness, Scalability) even if you don't find them you can score them as 0 if not found.\n"
                +
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

    public String processDependencyFile(RepoData repoData) throws IOException {

        String owner = repoData.getName().substring(0,repoData.getName().indexOf("/"));
        String repo = repoData.getName().substring(repoData.getName().indexOf("/"));

        //Repo Content Code
        System.out.println("Owner is: "+owner+" Repo is: "+ repo);
        ResponseEntity<String> response = llmService.getRepositoryContents(owner, repo);
        System.out.println(response);
        if (isValidResponse(response)) {
            List<Map<String, Object>> contents = jsonUtils.parseJSONResponse(response.getBody());

            // Searching for pom.xml
            for (Map<String, Object> item : contents) {
                processContentItem(item, owner, repo);
            }
        }
        if (found_pom_flag) {
            return parsePOMintoMap(repoData);
        } else {
            return null;
        }
    }


    private void processFile(String owner, String repo, String filePath, String basePath) {
        logger.debug("Processing file...");

        filePath = filePath.trim().replaceAll(" ", "%20");
        logger.debug("Modified filePath: " + filePath);

        String currentDirectory = System.getProperty("user.dir");
        logger.debug("Current Working Directory: " + currentDirectory);

        ResponseEntity<String> response = llmService.getFileContent(owner, repo, filePath);
        String content = response.getBody();

        String title = filePath;

        String fileName = "output.txt"; // configurable or parameterized file name
        System.out.println("File name where the output is saved is at: "+ fileName);

        // Check if the file exists
        File file = new File(fileName);
        if (file.exists()) {
            // If the file exists, delete it
            if (file.delete()) {
                System.out.println("Existing file deleted.");
            } else {
                System.err.println("Unable to delete existing file.");
            }
        }
            // Writing content to the file
            try (PrintWriter printWriter = new PrintWriter(new FileWriter(fileName, true))) {
                printWriter.println("File Name: " + title);
                printWriter.println();
                printWriter.println("Content:");
                printWriter.println(content);
                printWriter.println();

                logger.debug("Content has been written to the file: " + fileName);
            } catch (IOException e) {
                logger.debug("An error occurred while writing to the file: " + e.getMessage());
            }
        found_pom_flag=true;
        logger.debug("File processing completed.");
    }

  public String parsePOMintoMap(RepoData repoData) throws IOException {
        System.out.println("Inside parsePOMintoMap()\n");
        StringBuilder resultBuilder = new StringBuilder();
        String content = new String(Files.readAllBytes(Paths.get("output.txt")));
        resultBuilder =  extractContent(content);

        String diff = parsePRForPomFile(repoData);
        if (diff != null)
        System.out.println("diff is:"+ diff);
        else
            System.out.println("diff is null");

        return resultBuilder.toString();
  }

    public String parsePRForPomFile(RepoData repoData) {
        Map<String, List<String>> devAndPRCode = getDevPRCode(repoData);

        // StringBuilder to store the merged diff code for all "pom.xml" occurrences
        StringBuilder mergedPomXmlDiff = new StringBuilder();

        for (Map.Entry<String, List<String>> entry : devAndPRCode.entrySet()) {
            String diffCodeUrl = entry.getValue().get(1);

            // Get the diff code for the PR
            UserData userData = userDataService.getOne(repoData.getUserId());
            ResponseEntity<String> responseDiffCode = restTemplate.exchange(diffCodeUrl, HttpMethod.GET,
                    getAllHeadersEntity(userData.getUserAccessToken()), String.class);
            showAvailableAPIHits(responseDiffCode.getHeaders());
            String devPRCode = responseDiffCode.getBody();

            // Check if pom.xml is present in the diff code
            if (isPomXmlPresent(devPRCode)) {
                // Get the entire diff patch relevant to "pom.xml"
                String pomXmlDiff = getPomXmlDiff(devPRCode);

                // Append the "pom.xml" diff to the merged diff
                mergedPomXmlDiff.append(pomXmlDiff).append("\n");
            }
        }

        // Return the merged "pom.xml" diff code if at least one occurrence is found, otherwise return null
        return mergedPomXmlDiff.length() > 0 ? mergedPomXmlDiff.toString() : null;
    }

    private boolean isPomXmlPresent(String diffCode) {
        // Check if "pom.xml" is present in the diff code (case-insensitive)
        return diffCode.toLowerCase().contains("pom.xml");
    }

    private String getPomXmlDiff(String diffCode) {
        // Split the diff code into lines
        String[] lines = diffCode.split("\\n");

        // Flag to determine if we are inside the relevant "pom.xml" section
        boolean inPomXmlSection = false;

        // StringBuilder to store the relevant diff patch
        StringBuilder pomXmlDiff = new StringBuilder();

        // Iterate through each line of the diff code
        for (String line : lines) {
            // Check if the line starts with "diff --git" to identify the beginning of a new file
            if (line.startsWith("diff --git")) {
                // Check if the new file is "pom.xml"
                inPomXmlSection = line.contains("pom.xml");
            }

            // If we are inside the relevant "pom.xml" section, append the line to the StringBuilder
            if (inPomXmlSection) {
                pomXmlDiff.append(line).append("\n");
            }
        }

        return pomXmlDiff.toString();
    }

    private void processDirectory(String owner, String repo, String dirPath, String basePath) {
        logger.debug("Entering processDirectory");

        try {
            ResponseEntity<String> response = llmService.getRepositoryContents(owner, repo, dirPath);
            logger.debug("Response body: {}", response.getBody());
            List<Map<String, Object>> contents = jsonUtils.parseJSONResponse(response.getBody());
            logger.debug("Contents: {}", contents);

            updateBasePath(basePath, dirPath);

            for (Map<String, Object> item : contents) {
                String type = (String) item.get("type");
                String path = (String) item.get("path");

                if (FILE_TYPE.equals(type) && isValidFile(path)) {
                    processFile(owner, repo, path, basePath);
                    break;
                } else if (DIR_TYPE.equals(type)) {
                    processDirectory(owner, repo, path, basePath);
                }
            }
        } catch (Exception e) {
            logger.error("Error processing directory: {}", e.getMessage(), e);
        }
        logger.debug("Exiting processDirectory");
    }

    private static StringBuilder extractContent(String input) {
        System.out.println("inside extractContent and the input is: "+input);
        StringBuilder resultBuilder = new StringBuilder();
        String patternString = "<(plugin|dependency)>.*?<artifactId>(.*?)</artifactId>.*?<version>(.*?)</version>.*?</(plugin|dependency)>";
        Pattern pattern = Pattern.compile(patternString, Pattern.DOTALL);
        Matcher matcher = pattern.matcher(input);

        while (matcher.find()) {
            String tagType = matcher.group(1);
            String artifactId = matcher.group(2);
            String version = matcher.group(3);

            resultBuilder.append("Tag Type: ").append(tagType).append("\n");
            resultBuilder.append("ArtifactId: ").append(artifactId).append("\n");
            resultBuilder.append("Version: ").append(version).append("\n\n");
        }

        if (resultBuilder.isEmpty()) {
            return null;
        }

        return resultBuilder;
    }

    private void updateBasePath(String basePath, String dirPath) {
        basePath = StringUtils.hasText(basePath) ? basePath + File.separator + dirPath : dirPath;
    }

    private boolean isValidFile(String path) {
        List<String> validExtensions = Arrays.asList("jpg", "png", "svg", "class", "docx", "exe", "dll", "jar", "gif", "css", "html");
        return validExtensions.stream().noneMatch(extension -> path.toLowerCase().contains(extension));
    }

    private void processContentItem(Map<String, Object> item, String owner, String repo) {
        String basePath = "";
        String type = (String) item.get("type");
        String name = (String) item.get("name");
        String path = (String) item.get("path");

        if (FILE_TYPE.equals(type) && name.equals("pom.xml") && !found_pom_flag) {
            System.out.println("Found POM.XML file\n");
            processFile(owner, repo, path, basePath);
            found_pom_flag=true;
        } else if (DIR_TYPE.equals(type) && (!found_pom_flag)){
            processDirectory(owner, repo, path, basePath);
        }
    }

    private boolean isValidResponse(ResponseEntity<String> response) {
        return response != null && response.getBody() != null && response.getStatusCode().is2xxSuccessful();
    }

    private List<Map<String, Object>> parseJSONResponse(String responseBody) {
        return jsonUtils.parseJSONResponse(responseBody);
    }

    public String getBugDetectionInApplicationFlowInsights(RepoData repoData) {
        Map<String, List<String>> devAndPRCode = getDevPRCode(repoData);
        String bugDetectionInApplicationFlowInsightPrompt = getBugDetectionInApplicationFlowInsightLLMPrompt();
        String bugDetectionInApplicationFlowInsightString = getInsightsFromPromptAndDevPRCode(devAndPRCode,
                bugDetectionInApplicationFlowInsightPrompt);
        System.out.println(bugDetectionInApplicationFlowInsightString);
        return bugDetectionInApplicationFlowInsightString;
    }

    private String getBugDetectionInApplicationFlowInsightLLMPrompt() {

        String bugDetectionInApplicationFlowInsightPrompt = "The provided string is a map with \n" +
                "developers as key and value with list of 2 strings where\n" +
                "First string is the Title of the PR, and second string is the PR Code.\n" +
                "I want you to conduct bug detection to find unexpected bugs being introduced by pushed code in the application flows.\n"
                +
                "and I want you to display actionable recommendations for resolving these bugs.\n" +
                "Also, I want you to display alerts if this PR is introducing any bug in the application's major flows."
                +
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
                "        \"recommendation\": [\"<recommendation1>\", \"<recommendation2>\", \"<recommendation3>\", \"<recommendation4>\"]\n"
                +
                "      }\n" +
                "    ],\n" +
                "    \"alerts\": [\"<alert1>\", \"<alert2>\", \"<alert3>\", \"<alert4>\"],\n" +
                "    \"general_recommendation\": [\"<general_recommendation1>\", \"<general_recommendation2>\", \"<general_recommendation3>\", \"<general_recommendation4>\"]\n"
                +
                "  }\n" +
                "]";

        return bugDetectionInApplicationFlowInsightPrompt;
    }

    public String getCustomCodeLintingInsights(RepoData repoData) {
        Map<String, List<String>> devAndPRCode = getDevPRCode(repoData);
        String getCustomCodeLintingInsightPrompt = getCustomCodeLintingInsightLLMPrompt();
        String getCustomCodeLintingInsightString = getInsightsFromPromptAndDevPRCode(devAndPRCode,
                getCustomCodeLintingInsightPrompt);
        System.out.println(getCustomCodeLintingInsightString);
        return getCustomCodeLintingInsightString;
    }

    private String getCustomCodeLintingInsightLLMPrompt() {

        String getCustomCodeLintingInsightPrompt = "The provided string is a map with \n" +
                "developers as key and value with list of 2 strings where\n" +
                "First string is the Title of the PR, and second string is the PR Code.\n" +
                "Linting Check Criteria: Syntax Errors, Code Standards Adherence, Code Smells, Security Checks.\n" +
                "I want you to conduct linting check based on the above mentioned criteria to find out whether the Linting rules are followed by pushed code.\n"+
                "and I want you to display actionable recommendations for improving the Linting Standards.\n" +
                "and make your response in JSON Array format\n" +
                "Generate a JSON Array with the following pattern:\n" +
                "[\n" +
                "  {\n" +
                "    \"developer\": \"<developer_name>\",\n" +
                "    \"pr_title\": \"<title_string>\",\n" +
                "    \"follows_linting\": \"<true or false>\",\n" +
                "    \"linting_comments\": [<linting_comment1>, <linting_comment2>, <linting_comment3>],\n" +
                "  }\n" +
                "]";

        return getCustomCodeLintingInsightPrompt;
    }

    public String getRepositoryPRsCollab(RepoData repoData) throws IOException, InterruptedException {
        // Call getRepoContributors to get the top 3 contributors who commit the most
        Map<String, Integer> collabCommit = repoDataService.getRepoContributors(repoData);
        Map<String, Integer> top3Contributors = new HashMap<>();

        // Convert the map to a list of entries
        List<Map.Entry<String, Integer>> entryList = new ArrayList<>(collabCommit.entrySet());

        // Sort the list using a custom comparator
        entryList.sort((entry1, entry2) -> entry2.getValue().compareTo(entry1.getValue()));

        // Create a new LinkedHashMap to store the sorted entries
        Map<String, Integer> sortedMap = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : entryList) {
            sortedMap.put(entry.getKey(), entry.getValue());
        }

        // Display and store only the top 3 entries in top3Contributors map
        int count = 0;
        for (Map.Entry<String, Integer> entry : sortedMap.entrySet()) {
            if (count < 3) {
                System.out.println(entry.getKey() + ": " + entry.getValue());
                top3Contributors.put(entry.getKey(), entry.getValue());
                count++;
            } else {
                break;
            }
        }

        // Create a Map to store the diff url against the contributor
        Map<String, String> contributorDiff = new HashMap<>();

        for (Map.Entry<String, Integer> entry : top3Contributors.entrySet()) {

            // Get owner name from top 3 contributors
            String owner = entry.getKey();

            // Get Repo from variable repoData
            String repoName = repoData.getName().substring(repoData.getName().indexOf("/"));
            System.out.println("repo name for collab: owner: " + owner + " reponame: " + repoName);

            String apiUrl = String.format("%s/repos/%s%s/pulls?state=all&sort=created&direction=desc&per_page=1&page=1",
                    GITHUB_API_URL, owner, repoName);
            System.out.println("apiUrl for collab-analysis story: " + apiUrl);

            UserData userData = userDataService.getOne(repoData.getUserId());

            try {
                ResponseEntity<String> response = restTemplate.exchange(apiUrl, HttpMethod.GET,
                        repoDataService.getAllHeadersEntity(userData.getUserAccessToken()), String.class);
                repoDataService.showAvailableAPIHits(response.getHeaders());
                String jsonArrayString = response.getBody();

                if (response.getStatusCode().value() == 200) {
                    if (!jsonArrayString.equals("[]")) {
                        try {
                            // Create an ObjectMapper instance
                            ObjectMapper objectMapper = new ObjectMapper();

                            // Parse the JSON array string
                            JsonNode jsonNodeArray = objectMapper.readTree(jsonArrayString);

                            // Iterate through each element in the array
                            for (JsonNode jsonNode : jsonNodeArray) {
                                // Extract required values
                                String diffUrl = jsonNode.get("diff_url").asText();

                                // Print and store in map
                                System.out.println("diffUrl for user: " + owner + " is: " + diffUrl);
                                contributorDiff.put(owner, diffUrl);
                            }

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {
                        contributorDiff.put(owner, "No PR found");
                    }
                } else
                    contributorDiff.put(owner, "No PR found");
            } catch (Exception e) {
                contributorDiff.put(owner, "No PR found");
                System.out.println("dcontributorDiff: " + contributorDiff);
                e.printStackTrace();
            }
        }

        // Print the map of contributor against it's respective diff url
        for (Map.Entry<String, String> test : contributorDiff.entrySet()) {
            System.out.println("Contributor: " + test.getKey() + "\nDiff Url: " + test.getValue());
        }

        // get the code for the respective diff urls and store them in 3 separate text
        // files
        getCodefromDiffUrl(contributorDiff, repoData, sortedMap);

        // Calculate the commit count and the smells using LLM and get insight after
        // that
        String response = getSmellRating();
        return response;
    }

    // This method would hit the 3 diff urls in the map and store them in three text
    // file: diff1.txt, diff2.txt, diff3.txt
    public void getCodefromDiffUrl(Map<String, String> contributorDiff, RepoData repoData,
            Map<String, Integer> sortedMap) throws IOException {
        int count = 1;
        for (Map.Entry<String, String> entry : contributorDiff.entrySet()) {
            System.out.println("Individual: " + entry.getKey());
            int commitCount = sortedMap.get(entry.getKey());
            Files.createDirectories(Paths.get(COLLAB_ANALYSIS_FILES_PATH));
            String fileName = COLLAB_ANALYSIS_FILES_PATH + "diff" + count + ".txt";
            count++;
            FileWriter fw = new FileWriter(fileName);
            try {
                if (entry.getValue().equals("No PR found")) {
                    fw.write(
                            "Determine the smells of the code in the .diff file below. Rate the overall smells on a scale of 1-10, "
                                    +
                                    "1 being the least amount of code smells and 10 being the most code smells found. "
                                    +
                                    "Give me the in the following output format: \"Contributor; Rating; Commit-Count\"\n "
                                    + "\n\nContributor: " + entry.getKey() +
                                    "\nCommit Count: " + commitCount +
                                    "\n\nDiff File:\nIgnore the code, the Code Smell Rating is 10." +
                                    "\n\nModify this prompt to receive the output in a semi-colon separated string. For Example: Contributor; Commit-Count; Smell-Rating");
                    System.out.println("The Code Smell Rating is 10.");
                } else {
                    System.out.println("Inside else: " + entry.getValue());
                    downloadURLToFile(entry.getValue(), fileName, entry.getKey(), commitCount);
                    System.out.println("Content downloaded successfully.");
                }
                fw.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static void downloadURLToFile(String url, String filePath, String contributor, Integer commitCount)
            throws IOException {
        Document document = Jsoup.parse(new URL(url), 3000);

        // Replace this CSS selector with the appropriate selector for your code snippet
        Elements codeElements = document.select("body");

        // Extract the code content
        String codeSnippet = codeElements.text();

        // Save the code snippet to a file
        try (FileWriter writer = new FileWriter(filePath)) {
            writer.write(
                    "Determine the smells of the code in the .diff file below. Rate the overall smells on a scale of 1-10, "
                            +
                            "1 being the least amount of code smells and 10 being the most code smells found. " +
                            "Give me the in the following output format: \"Contributor; Rating; Commit-Count\"\n "
                            + "\n\nContributor: " +
                            contributor + "\nCommit Count: " + commitCount + "\n\nDiff File:\n");
            writer.write(codeSnippet);
            writer.write(
                    "\n\nModify this prompt to receive the output in a semi-colon separated string. For Example: Contributor; Commit-Count; Smell-Rating");
            writer.close();
        }
    }

    public String getSmellRating() throws IOException, InterruptedException {
        Map<String, String> roleInsights = new HashMap<>();

        System.out.println("In Smell Rating Method inside RepoDataServiceImpl");
        Files.createDirectories(Paths.get(COLLAB_ANALYSIS_FILES_PATH));
        FileWriter fw = new FileWriter(COLLAB_ANALYSIS_FILES_PATH + "SmellRatingPrompt.txt");
        fw.write(
                "For a specific GitHub repository, you have information about the top 3 contributors based on commit count and their respective code smells ratings. "
                        +
                        "The code smells ratings are on a scale of 1-10, where 1 represents the least number of code smells, "
                        +
                        "and 10 represents a high number of code smells.\n\nTop 3 Contributors:\n\n");

        for (int i = 1; i <= 3; i++) {
            String fileName = COLLAB_ANALYSIS_FILES_PATH + "diff" + i + ".txt";
            String prompt = new String(Files.readAllBytes(Paths.get(fileName)));
            String response = chatGPTService.chat(prompt);
            System.out.println(response);

            // Split the string using the ; delimiter
            String[] parts = response.split(";");

            // Trim leading and trailing whitespaces from each part
            for (int j = 0; j < parts.length; j++) {
                parts[j] = parts[j].trim();
            }

            // Access the individual parts
            String contributor = parts[0];
            System.out.println(contributor);
            int commitCount = Integer.parseInt(parts[1]);
            System.out.println(commitCount);
            int rating = (int) Double.parseDouble(parts[2]);
            System.out.println(rating);

            fw.write(contributor + "\nCommit Count: " + commitCount + "\nCode Smells Rating: "
                    + rating + "\n\n");

        }

        fw.write("Your task is to determine which two contributors, when collaborating together, " +
                "would be the most productive. Productivity is defined as a combination of commit count and code smells rating, "
                +
                "where lower code smells ratings are preferable.\n\n" +
                "Provide the names of the two contributors and a brief explanation of why you consider them to be the most productive collaborators based on the given criteria.");
        fw.close();
        String finalPrompt = new String(
                Files.readAllBytes(Paths.get(COLLAB_ANALYSIS_FILES_PATH + "SmellRatingPrompt.txt")));
        Thread.sleep(30000);
        String finalResponse = chatGPTService.chat(finalPrompt);
        System.out.println("Collab Analysis GPT Response:\n" + finalResponse);

        return finalResponse;
    }

}
