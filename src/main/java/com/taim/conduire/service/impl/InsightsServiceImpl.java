package com.taim.conduire.service.impl;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.taim.conduire.constants.ConstantCodes;
import com.taim.conduire.domain.RepoData;
import com.taim.conduire.domain.UserData;
import com.taim.conduire.service.*;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import javax.xml.bind.annotation.*;
import java.io.*;
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

    private boolean flag = false;


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
                String reviewer = reviewCommentObject.get("user").getAsJsonObject().get("login").getAsString();
                String reviewerComment = reviewCommentObject.get("body").getAsString();
                reviewerComments.computeIfAbsent(reviewer, k -> new ArrayList<>()).add(reviewerComment);
            }
        }
        System.out.println("reviewerComments: " + reviewerComments.size());
        return reviewerComments;
    }

    public String getCodeQualityEnhancementsInsights(RepoData repoData) {
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

        String codeQualityEnhancementInsightPrompt = getCodeQualityEnhancementInsightLLMPrompt();

        Integer llmTokenLimitWithPrompt = LLM_TOKEN_LIMIT - countTokens(codeQualityEnhancementInsightPrompt);
        Map<String, List<String>> devAndPRCodeWithLimit = new HashMap<>();

        for (Map.Entry<String, List<String>> entry : devAndPRCode.entrySet()) {

            String element = entry.getKey() + "=" + entry.getValue().toString() + ",";
            if (countTokens(devAndPRCodeWithLimit + element) <= llmTokenLimitWithPrompt) {
                devAndPRCodeWithLimit.put(entry.getKey(), entry.getValue());
                llmTokenLimitWithPrompt -= countTokens(devAndPRCodeWithLimit.toString());
            }
        }
        System.out.println("devAndPRCodeWithLimit Size: " + devAndPRCodeWithLimit.size());
        System.out.println("Final code Token: " + countTokens(devAndPRCodeWithLimit.toString()));
        String codeQualityEnhancementInsightString;
        if (devAndPRCodeWithLimit.isEmpty()) {
            System.out.println("devAndPRCodeWithLimit: " + devAndPRCodeWithLimit.size());
            codeQualityEnhancementInsightString = "{\"message\":\"There are no Open PR for this Repository\"}";
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
            String promptAndCode = codeQualityEnhancementInsightPrompt + devAndPRCodeWithLimitString;

            System.out.println("Final prompt + code Token: " + countTokens(promptAndCode));
            codeQualityEnhancementInsightString = chatGPTService.chat(promptAndCode);
        }

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
                "]\n" +
                "Keep the score and criteria in the same order so later on it can be fetched.\n\n";

        return codeQualityEnhancementInsightPrompt;
    }

    public StringBuilder processPomXMLFile(RepoData repoData) throws IOException {

        String owner = repoData.getName().substring(0,repoData.getName().indexOf("/"));
        String repo = repoData.getName().substring(repoData.getName().indexOf("/"));

        //Repo Content Code
        System.out.println("Owner is: "+owner+" Repo is: "+ repo);
        ResponseEntity<String> response = llmService.getRepositoryContents(owner, repo);
        System.out.println(response);
        if (isValidResponse(response)) {
            List<Map<String, Object>> contents = jsonUtils.parseJSONResponse(response.getBody());

            for (Map<String, Object> item : contents) {
                processContentItem(item, owner, repo);
            }
        }

        return parsePOMintoMap();
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

        System.out.println("hello ji]\n");
        System.out.println("Basepath is:\n"+basePath);

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
        flag=true;
        logger.debug("File processing completed.");
    }

  public StringBuilder parsePOMintoMap() throws IOException {
        System.out.println("Inside parsePOMintoMap()\n");
        StringBuilder resultBuilder = new StringBuilder();
        String content = new String(Files.readAllBytes(Paths.get("output.txt")));
        resultBuilder =  extractContent(content);

        return resultBuilder;
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

        if (FILE_TYPE.equals(type) && name.equals("pom.xml") && !flag) {
            System.out.println("Found POM.XML file\n");
            processFile(owner, repo, path, basePath);
            flag=true;
        } else if (DIR_TYPE.equals(type) && (!flag)){
            processDirectory(owner, repo, path, basePath);
        }
    }

    private boolean isValidResponse(ResponseEntity<String> response) {
        return response != null && response.getBody() != null && response.getStatusCode().is2xxSuccessful();
    }


    private List<Map<String, Object>> parseJSONResponse(String responseBody) {
        return jsonUtils.parseJSONResponse(responseBody);
    }

}
