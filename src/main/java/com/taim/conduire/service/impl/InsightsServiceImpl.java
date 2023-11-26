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
import java.util.regex.PatternSyntaxException;
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

    // Constant representing the file type received from gitHub API response for the source code.
    private static final String FILE_TYPE = "file";

    // Constant representing the directory type.
    private static final String DIR_TYPE = "dir";

    // Flag to indicate whether a pom.xml file has been found during processing.
    private boolean foundPomFlag = false;


    public static int countTokens(String input) {
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

        int llmTokenLimitWithPrompt = LLM_TOKEN_LIMIT - countTokens(llmInsightPrompt);
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

    /**
     * Processes the dependency file for a given repository, extracts relevant information, and generates insights.
     *
     * @param repoData The repository data containing information about the repository.
     * @return A string containing insights and recommendations in HTML format.
     * @throws IOException If an I/O error occurs during file reading or processing.
     */
    public String processDependencyFile(RepoData repoData) throws IOException {
        // We start with the assumption that we haven't found a pom.xml file yet for this repository.
        foundPomFlag = false;

        // Let's check if there's an existing temporary file from previous runs and clear it out if it exists.
        File tempFile = new File(TEMP_FILE_PATH);
        if (tempFile.exists()) {
            // If we can successfully delete the existing file, we print a message.
            if (tempFile.delete()) {
                System.out.println("Cleared the slate by removing the existing temporary file.");
            } else {
                // If deletion fails, we print an error message.
                System.err.println("Oops! Something went wrong. Couldn't delete the existing temporary file.");
            }
        }

        // Extract the owner and repository names from the RepoData object.
        String owner = repoData.getName().substring(0, repoData.getName().indexOf("/"));
        String repo = repoData.getName().substring(repoData.getName().indexOf("/"));

        // Fetch the content of the repository to inspect.
        System.out.println("Exploring repository - Owner: " + owner + " Repository: " + repo);
        ResponseEntity<String> response = llmService.getRepositoryContents(owner, repo);

        // Check if the response is valid and not null.
        if (response != null && isValidResponse(response)) {
            System.out.println("Successfully retrieved repository content.");
            System.out.println(response);

            // Parse the JSON response to get a list of items in the repository.
            List<Map<String, Object>> contents = jsonUtils.parseJSONResponse(response.getBody());

            // Iterate through the items in the repository to search for pom.xml files.
            for (Map<String, Object> item : contents) {
                processContentItem(item, owner, repo);
            }
        }

        // If we found a pom.xml file during exploration, proceed with further processing.
        if (foundPomFlag) {
            // Parse the pom.xml file and generate insights.
            String parsed = parsePOMintoMap(repoData);

            // Check if parsing was successful.
            if (parsed == null) {
                // If parsing fails, return null.
                return null;
            } else {
                // If parsing is successful, return the insights in HTML format.
                return parsed;
            }
        } else {
            // If no pom.xml file was found, return null.
            return null;
        }
    }

    /**
     * Processes a file within a repository, retrieves its content, and appends the content to a temporary file.
     *
     * @param owner    The owner of the repository.
     * @param repo     The name of the repository.
     * @param filePath The path of the file to be processed.
     * @param basePath The base path for the file.
     */
    private void processFile(String owner, String repo, String filePath, String basePath) {
        // Log that we're diving into processing a file.
        logger.debug("Processing a file.");

        // Ensure the filePath is URL-encoded and free of extra spaces.
        filePath = filePath.trim().replaceAll(" ", "%20");
        logger.debug("Modified filePath for URL encoding: " + filePath);

        // Get the current working directory.
        String currentDirectory = System.getProperty("user.dir");
        logger.debug("Our current working directory: " + currentDirectory);

        // Fetch the content of the file from the repository.
        ResponseEntity<String> response = llmService.getFileContent(owner, repo, filePath);
        String content = response.getBody();

        // Set the title of the file (used for logging).
        String title = filePath;

        // Attempt to append the content to a temporary file.
        try (PrintWriter printWriter = new PrintWriter(new FileWriter(TEMP_FILE_PATH, true))) {
            printWriter.println("File Name: " + title);
            printWriter.println();
            printWriter.println("Content:");
            printWriter.println(content);
            printWriter.println();

            // Log that the content has been successfully appended.
            logger.debug("Content successfully appended to the temporary file: " + TEMP_FILE_PATH);
        } catch (IOException e) {
            // Log an error message if appending to the file fails.
            logger.debug("Oops! An error occurred while trying to append to the file: " + e.getMessage());
        }

        // Log that the file processing is complete.
        logger.debug("File processing completed.");
    }

    /**
     * Parses the content of a POM file, extracts plugin/dependency information, and provides insights.
     *
     * @param repoData The repository data containing information about the repository.
     * @return A string containing insights and recommendations in HTML format.
     * @throws IOException If an I/O error occurs during file reading or processing.
     */
    public String parsePOMintoMap(RepoData repoData) throws IOException {
        // Calculate the initial number of tokens available for processing.
        Integer llmTokenLimitWithPrompt = LLM_TOKEN_LIMIT - countTokens(DEPENDENCY_VERSION_INSIGHT_PROMPT);
        System.out.println("In parsePOMintoMap llmTokenLimitWithPrompt tokens has:"+llmTokenLimitWithPrompt);
        System.out.println("In parsePOMintoMap countTokens(DEPENDENCY_VERSION_INSIGHT_PROMPT) tokens has:"+countTokens(DEPENDENCY_VERSION_INSIGHT_PROMPT));
        // StringBuilder to store the parsed result.
        StringBuilder resultBuilder = new StringBuilder();

        try {
            // Read content from the file and extract information.
            String content = new String(Files.readAllBytes(Paths.get(TEMP_FILE_PATH)));
            resultBuilder = extractContent(content);

            // Check if there was an issue extracting content.
            if (resultBuilder == null) {
                System.out.println("Oops! There seems to be an issue in extracting content.");
                return null;
            } else {
                // Append a message indicating the extraction of plugin/dependency information.
                resultBuilder.append("In the above, I've gathered plugin/dependency information from the pom.xml of a repository.").append("\n");

                // Adjust the available token limit based on the extracted content.
                if (countTokens(resultBuilder.toString()) <= llmTokenLimitWithPrompt) {
                    llmTokenLimitWithPrompt = llmTokenLimitWithPrompt - countTokens(resultBuilder.toString());
                }
            }

            // Get the diff string by comparing with Pull Requests.
            String diff = parsePRForPomFile(repoData, llmTokenLimitWithPrompt);

            // Append the diff to the resultBuilder if it's not null.
            if (diff != null) {
                resultBuilder.append("Below are the Open PRs that have changes to pom.xml, indicating upgrades/downgrades of project dependencies.");
                resultBuilder.append(DEPENDENCY_VERSION_INSIGHT_PROMPT).append(diff).append("\n");
            } else {
                // If there's no diff, append a placeholder message.
                resultBuilder.append(DEPENDENCY_VERSION_INSIGHT_PROMPT).append("\n");
            }

            // Return the final result as a string.
            return resultBuilder.toString();
        } catch (IOException e) {
            // Handle file reading exception and log an error message.
            System.err.println("Error reading file: " + e.getMessage());
            return null;
        }
    }

    /**
     * Parses Pull Requests for changes in "pom.xml" files and compiles a merged diff code.
     *
     * @param repoData              The repository data containing information about the repository.
     * @param llmTokenLimitWithPrompt The token limit for processing, adjusted based on previous content.
     * @return A string representing the merged diff code for "pom.xml" files in the Pull Requests.
     */
    public String parsePRForPomFile(RepoData repoData, Integer llmTokenLimitWithPrompt) {
        // Fetch developer and Pull Request code from the repository.
        Map<String, List<String>> devAndPRCode = getDevPRCode(repoData);

        // StringBuilder to store the merged diff code for all "pom.xml" occurrences.
        StringBuilder mergedPomXmlDiff = new StringBuilder();

        // Iterate through the entries in the map containing developer and PR code.
        for (Map.Entry<String, List<String>> entry : devAndPRCode.entrySet()) {
            // Check if there are more than 1 element in the list (indicating PR code is present).
            if (entry.getValue().size() > 1) {
                // Get the PR code from the list.
                String devPRCode = entry.getValue().get(1);

                // Check if "pom.xml" is present in the PR code.
                if (devPRCode != null && isPomXmlPresent(devPRCode)) {
                    // Get the entire diff patch relevant to "pom.xml".
                    String pomXmlDiff = getPomXmlDiff(devPRCode);

                    // Check if the token count in the "pom.xml" diff is within the adjusted limit.
                    if (countTokens(pomXmlDiff) <= llmTokenLimitWithPrompt) {
                        // Append the "pom.xml" diff to the merged diff.
                        mergedPomXmlDiff.append(pomXmlDiff).append("\n");

                        // Adjust the token limit based on the appended diff content.
                        llmTokenLimitWithPrompt = llmTokenLimitWithPrompt - countTokens(pomXmlDiff + "\n");
                    }
                }
            }
        }

        // Return the merged "pom.xml" diff code if at least one occurrence is found, otherwise return null.
        return !mergedPomXmlDiff.isEmpty() ? mergedPomXmlDiff.toString() : null;
    }

    /**
     * Checks if the "pom.xml" file is present in the provided diff code.
     *
     * @param diffCode The diff code to be examined for the presence of "pom.xml".
     * @return true if "pom.xml" is present, false otherwise.
     */
    private boolean isPomXmlPresent(String diffCode) {
        // Convert the diff code to lowercase for case-insensitive comparison.
        String lowercaseDiffCode = diffCode.toLowerCase();

        // Check if "pom.xml" is present in the diff code.
        if (lowercaseDiffCode.contains("pom.xml")) {
            // If present, log a message indicating its presence.
            System.out.println("The 'pom.xml' file is present in the provided diff code.");
            return true;
        } else {
            // If not present, log a message indicating its absence.
            System.out.println("The 'pom.xml' file is not found in the provided diff code.");
            return false;
        }
    }

    /**
     * Extracts the relevant "pom.xml" diff patch from the given diff code.
     *
     * @param diffCode The full diff code containing changes to various files.
     * @return The diff patch specific to the "pom.xml" file.
     */
    private String getPomXmlDiff(String diffCode) {
        // Split the diff code into lines for processing
        String[] lines = diffCode.split("\\n");

        // Flag to determine if we are inside the relevant "pom.xml" section
        boolean inPomXmlSection = false;

        // StringBuilder to store the relevant diff patch for "pom.xml"
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

        // Return the extracted "pom.xml" diff patch as a string
        return pomXmlDiff.toString();
    }

    /**
     * Processes a directory by retrieving its contents from the repository and recursively
     * handling each item in the directory.
     *
     * @param owner    The owner of the repository.
     * @param repo     The repository name.
     * @param dirPath  The path of the current directory.
     * @param basePath The base path for constructing file paths within the directory.
     */
    private void processDirectory(String owner, String repo, String dirPath, String basePath) {
        // Log a debug message to indicate the method entry
        logger.debug("Entering processDirectory");

        try {
            // Retrieve the contents of the current directory from the repository
            ResponseEntity<String> response = llmService.getRepositoryContents(owner, repo, dirPath);

            // Log the response body for debugging purposes
            logger.debug("Response body: {}", response.getBody());

            // Parse the JSON response to obtain a list of directory contents
            List<Map<String, Object>> contents = jsonUtils.parseJSONResponse(response.getBody());

            // Log the parsed contents for debugging purposes
            logger.debug("Contents: {}", contents);

            // Update the base path based on the current directory path
            updateBasePath(basePath, dirPath);

            // Iterate through each item in the directory contents
            for (Map<String, Object> item : contents) {
                String type = (String) item.get("type");
                String path = (String) item.get("path");
                String name = (String) item.get("name");

                // Check if the current item is a "pom.xml" file and it has not been found yet
                if (isPomFile(type, name) && !foundPomFlag) {
                    // Process the "pom.xml" file
                    processFile(owner, repo, path, basePath);
                    foundPomFlag = true;  // Set the flag to indicate that "pom.xml" has been found
                    break;  // Exit the loop since the file has been found
                } else if (isDirectory(type) && (!foundPomFlag)) {
                    // Recursively process subdirectories if "pom.xml" has not been found yet
                    processDirectory(owner, repo, path, basePath);
                }
            }
        } catch (Exception e) {
            // Log an error message if an exception occurs during directory processing
            logger.error("Error processing directory: {}", e.getMessage(), e);
        }

        // Log a debug message to indicate the method exit
        logger.debug("Exiting processDirectory");
    }

    /**
     * Extracts plugin and dependency information from the given input using a regular expression pattern.
     *
     * @param input The input string containing plugin and dependency information.
     * @return A StringBuilder containing information about plugin and dependency tags.
     *         Returns null if no valid information is found.
     */
    private static StringBuilder extractContent(String input) {

        // StringBuilder to store the extracted information.
        StringBuilder resultBuilder = new StringBuilder();

        // Regular expression pattern to match plugin and dependency tags with their artifactId and version.
        String patternString = "<(plugin|dependency)>.*?<artifactId>(.*?)</artifactId>.*?<version>(.*?)</version>.*?</(plugin|dependency)>";

        try {
            // Compile the regular expression pattern for matching.
            Pattern pattern = Pattern.compile(patternString, Pattern.DOTALL);

            // Create a matcher object to find matches in the input string.
            Matcher matcher = pattern.matcher(input);

            // Iterate through each match found by the pattern.
            while (matcher.find()) {
                // Extract matched groups for tagType, artifactId, and version.
                String tagType = matcher.group(1);
                String artifactId = matcher.group(2);
                String version = matcher.group(3);

                // Check if both version and artifactId are found.
                if (tagType != null && artifactId != null && version != null) {
                    // Append information to the result StringBuilder.
                    resultBuilder.append("Tag Type: ").append(tagType);
                    resultBuilder.append("  ArtifactId: ").append(artifactId);
                    resultBuilder.append("  Version: ").append(version).append("\n");
                }
            }
        } catch (PatternSyntaxException e) {
            // Handle the case where the regex pattern is invalid.
            System.err.println("Invalid regex pattern: " + e.getMessage());
        }

        // Check if any information was extracted.
        if (resultBuilder.isEmpty()) {
            // Log a message indicating that no valid information was found.
            System.out.println("No valid information found in the input.");
            return null;
        }

        // Return the StringBuilder containing extracted information.
        return resultBuilder;
    }

    /**
     * Updates the base path by appending the directory path if the base path is not empty.
     * If the base path is empty, sets the base path to the directory path.
     *
     * @param basePath The current base path.
     * @param dirPath  The directory path to be appended.
     */
    private void updateBasePath(String basePath, String dirPath) {
        // Log a message indicating the start of the base path update.
        System.out.println("Inside updateBasePath, current basePath: " + basePath + ", dirPath: " + dirPath);

        // Check if the base path is not empty.
        if (StringUtils.hasText(basePath)) {
            // Append the directory path to the base path using the appropriate separator.
            basePath = basePath + File.separator + dirPath;
        } else {
            // If the base path is empty, set it to the directory path.
            basePath = dirPath;
        }

        // Log a message indicating the updated base path.
        System.out.println("Updated basePath: " + basePath);
    }

    /**
     * Checks if a file with the given path is a valid file based on its extension.
     *
     * @param path The path of the file to be checked.
     * @return True if the file is valid, False otherwise.
     */
    private boolean isValidFile(String path) {
        // List of valid file extensions for images, documents, executables, and web-related files.
        List<String> validExtensions = Arrays.asList("jpg", "png", "svg", "class", "docx", "exe", "dll", "jar", "gif", "css", "html");

        // Convert the file path to lowercase for case-insensitive comparison.
        String lowercasePath = path.toLowerCase();

        // Check if the file path contains any of the valid extensions.
        boolean isValid = validExtensions.stream().noneMatch(extension -> lowercasePath.contains(extension));

        // Return the result indicating whether the file is valid or not.
        return isValid;
    }

    /**
     * Processes a content item based on its type, name, and the status of a flag.
     *
     * @param item  The map representing the content item.
     * @param owner The owner of the repository.
     * @param repo  The repository name.
     */
    private void processContentItem(Map<String, Object> item, String owner, String repo) {
        // Log a message indicating the start of processing the content item.
        System.out.println("Processing content item...");

        // Initialize the base path to an empty string.
        String basePath = "";

        // Extract information about the content item.
        String type = (String) item.get("type");
        String name = (String) item.get("name");
        String path = (String) item.get("path");

        // Check if the content item is a file, named "pom.xml", and the POM flag is not already found.
        if (isPomFile(type, name) && !foundPomFlag) {
            // Log a message indicating the discovery of a pom.xml file.
            System.out.println("Found POM.XML file\n");

            // Process the pom.xml file using the processFile method.
            processFile(owner, repo, path, basePath);

            // Set the POM flag to true, indicating the discovery of a pom.xml file.
            foundPomFlag = true;
        }
        // Check if the content item is a directory and the POM flag is not already found.
        else if (isDirectory(type) && (!foundPomFlag)) {
            // Process the directory using the processDirectory method.
            processDirectory(owner, repo, path, basePath);
        }

        // Log a message indicating the completion of processing the content item.
        System.out.println("Content item processing completed.");
    }

    /**
     * Checks if the content item represents a "pom.xml" file.
     *
     * @param type The type of the content item.
     * @param name The name of the content item.
     * @return {@code true} if the content item is a "pom.xml" file, {@code false} otherwise.
     */
    private boolean isPomFile(String type, String name) {
        return FILE_TYPE.equals(type) && "pom.xml".equals(name);
    }

    /**
     * Checks if the content item represents a directory.
     *
     * @param type The type of the content item.
     * @return {@code true} if the content item is a directory, {@code false} otherwise.
     */
    private boolean isDirectory(String type) {
        return DIR_TYPE.equals(type);
    }


    /**
     * Checks if the response from a service is valid.
     *
     * @param response The response entity received from the service.
     * @return {@code true} if the response is not null, has a non-null body,
     *         and the HTTP status code indicates success (2xx); otherwise, {@code false}.
     */
    private boolean isValidResponse(ResponseEntity<String> response) {
        // Check if the response is not null, has a non-null body, and the HTTP status code indicates success.
        return (response != null && response.getBody() != null && response.getStatusCode().is2xxSuccessful());
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
