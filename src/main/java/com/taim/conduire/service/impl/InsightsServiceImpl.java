package com.taim.conduire.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.taim.conduire.constants.ConstantCodes;
import com.taim.conduire.constants.InsightsPrompts;
import com.taim.conduire.domain.RepoData;
import com.taim.conduire.domain.UserData;
import com.taim.conduire.service.InsightsService;
import com.taim.conduire.service.RepoDataService;
import com.taim.conduire.service.UserDataService;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

@Service
public class InsightsServiceImpl implements InsightsService, ConstantCodes, InsightsPrompts  {

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

    @Override
    public HttpEntity<String> getAllHeadersEntity(String userAccessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/vnd.github+json");
        headers.set("Authorization", "Bearer " + userAccessToken);
        headers.set("X-GitHub-Api-Version", "2022-11-28");
        HttpEntity<String> entity = new HttpEntity<>(headers);
        return entity;
    }

    @Override
    public void showAvailableAPIHits(HttpHeaders responseHeaders) {
        String limitHeader = responseHeaders.getFirst("X-RateLimit-Limit");
        String remainingHeader = responseHeaders.getFirst("X-RateLimit-Remaining");

        if (limitHeader != null && remainingHeader != null) {
            try {
                int limit = Integer.parseInt(limitHeader);
                int remaining = Integer.parseInt(remainingHeader);

                logger.debug("GitHub API Hit Limit: " + limit);
                logger.debug("GitHub API Hit Limit Remaining: " + remaining);
            } catch (NumberFormatException e) {
                System.err.println("Error parsing API hit limit or remaining headers: " + e.getMessage());
            }
        }
    }

    @Override
    public Map<String, List<String>> getRepositoryReviewComments(RepoData repoData) {
        String apiUrl = GITHUB_API_URL + GITHUB_REPOS + "/" + repoData.getName();
        logger.debug("apiUrl: " + apiUrl);

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
        logger.debug("prReviewJsonArray: " + prReviewJsonArray.size());
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
        logger.debug("reviewerComments: " + reviewerComments.size());
        return reviewerComments;
    }

    @Override
    public String getCommonCodeMistakesInsights(RepoData repoData) {
        Map<String, List<String>> reviewerComments = getRepositoryReviewComments(repoData);
        String commonCodeMistakesPrompt = reviewerComments.toString() + COMMON_CODE_MISTAKES;
        String commonCodeMistakesInsight = chatGPTService.chat(commonCodeMistakesPrompt);
        Gson gson = new Gson();
        String jsonInsight = gson.toJson(commonCodeMistakesInsight);
        String finalResult = "{\"insights\":" + jsonInsight + "}";
        logger.debug("finalResult: " + finalResult);

        return finalResult;
    }

    @Override
    public Map<String, List<String>> getDevPRCode(RepoData repoData) {
        String parentRepoName = repoDataService.getParentRepo(repoData);
        Gson gson = new Gson();
        String repoPRDataURL = GITHUB_API_URL + GITHUB_REPOS + "/" + parentRepoName + GITHUB_PULLS + "?per_page=100";
        logger.debug("repoPRDataURL: " + repoPRDataURL);

        UserData userData = userDataService.getOne(repoData.getUserId());
        ResponseEntity<String> response = restTemplate.exchange(repoPRDataURL, HttpMethod.GET,
                getAllHeadersEntity(userData.getUserAccessToken()), String.class);
        showAvailableAPIHits(response.getHeaders());

        String jsonRepoPRsString = response.getBody();
        JsonArray jsonRepoPRsArray = gson.fromJson(jsonRepoPRsString, JsonArray.class);
        logger.debug("No: of Open PR: " + jsonRepoPRsArray.size());
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

        logger.debug("devAndPRCode Size: " + devAndPRCode.size());

        return devAndPRCode;
    }

    public String getInsightsFromPromptAndDevPRCode(Map<String, List<String>> devAndPRCode, String llmInsightPrompt) {

        String llmInsightString;

        Integer llmTokenLimitWithPrompt = LLM_TOKEN_LIMIT - countTokens(llmInsightPrompt);
        Map<String, List<String>> devAndPRCodeWithLimit = new HashMap<>();
        if(!devAndPRCode.isEmpty()){
            for (Map.Entry<String, List<String>> entry : devAndPRCode.entrySet()) {

                String element = entry.getKey() + "=" + entry.getValue().toString() + ",";
                if (countTokens(devAndPRCodeWithLimit + element) <= llmTokenLimitWithPrompt) {
                    devAndPRCodeWithLimit.put(entry.getKey(), entry.getValue());
                    llmTokenLimitWithPrompt -= countTokens(devAndPRCodeWithLimit + element);
                }
            }
            logger.debug("devAndPRCodeWithLimit Size: " + devAndPRCodeWithLimit.size());
            logger.debug("Final code Token: " + countTokens(devAndPRCodeWithLimit.toString()));
            if (devAndPRCodeWithLimit.isEmpty()) {
                logger.debug("devAndPRCodeWithLimit: " + devAndPRCodeWithLimit.size());
                llmInsightString = "{\"message\":\"There are no open PR for this Repository which can be processed by LLM. \"}";
            } else {
                String devAndPRCodeWithLimitString;
                if (devAndPRCodeWithLimit.size() > 3) {
                    devAndPRCodeWithLimitString = devAndPRCodeWithLimit.entrySet()
                            .stream()
                            .limit(3)
                            .map(entry -> entry.getKey() + "=" + entry.getValue())
                            .collect(Collectors.joining(", ", "{", "}"));
                } else {
                    devAndPRCodeWithLimitString = devAndPRCodeWithLimit.toString();
                }
                logger.debug("Final prompt Token: " + countTokens(llmInsightPrompt));
                logger.debug("Final devAndPRCodeWithLimitString Token: " + countTokens(devAndPRCodeWithLimitString));
                String promptAndCode = llmInsightPrompt + devAndPRCodeWithLimitString;

                logger.debug("Final prompt + code Token: " + countTokens(promptAndCode));
                llmInsightString = chatGPTService.chat(promptAndCode);
            }
        } else {
            llmInsightString = "{\"message\":\"There are no open PR for this Repository.\"}";
        }
        return llmInsightString;

    }

    @Override
    public String getCodeQualityEnhancementsInsights(RepoData repoData) {
        Map<String, List<String>> devAndPRCode = getDevPRCode(repoData);
        String codeQualityEnhancementInsightString = getInsightsFromPromptAndDevPRCode(devAndPRCode,
                CODE_QUALITY_ENHANCEMENTS);
        logger.debug(codeQualityEnhancementInsightString);
        return codeQualityEnhancementInsightString;
    }

    /**
     * Retrieves Dependency Version Control (DVC) insights for a given repository.
     *
     * @param repoData The repository data for which DVC insights are requested.
     * @return A string containing DVC insights or an informative message if not applicable.
     */
    @Override
     public String getDependencyVersionInsights(RepoData repoData) throws IOException {
        // Process the repository's dependency file to obtain version information
        String versions = processDependencyFile(repoData);

        // Check if version information is available
        if (versions == null) {
        // Return a message indicating that it's not a Java Maven repository
            return "This repository doesn't seem to be a Java Maven repository. Kindly retry with one!";
        } else {
        // Use a natural language processing service to generate insights based on version information
           return chatGPTService.chat(versions);
        }
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
                logger.debug("Cleared the slate by removing the existing temporary file.");
            } else {
                // If deletion fails, we print an error message.
                System.err.println("Oops! Something went wrong. Couldn't delete the existing temporary file.");
            }
        }

        // Extract the owner and repository names from the RepoData object.
//        String owner = repoData.getName().substring(0, repoData.getName().indexOf("/"));
//        String repo = repoData.getName().substring(repoData.getName().indexOf("/"));

        // Fetch the content of the repository to inspect.
        ResponseEntity<String> response = getRepositoryContents(repoData);

        // Check if the response is valid and not null.
        if (isValidResponse(response)) {
            logger.debug("Successfully retrieved repository content.");

            // Parse the JSON response to get a list of items in the repository.
            List<Map<String, Object>> contents = jsonUtils.parseJSONResponse(response.getBody());

            // Iterate through the items in the repository to search for pom.xml files.
            for (Map<String, Object> item : contents) {
                processContentItem(item, repoData);
            }
        }

        // If we found a pom.xml file during exploration, proceed with further processing.
        if (foundPomFlag) {
            // Parse the pom.xml file and generate insights.
            String parsed = parsePOMintoMap(repoData);

            // Check if parsing was successful.
            // If parsing fails, return null.
            // If parsing is successful, return the insights in HTML format.
            return parsed;
        } else {
            // If no pom.xml file was found, return null.
            return null;
        }
    }

    /**
     * Processes a file within a repository, retrieves its content, and appends the content to a temporary file.
     *
     * @param filePath The path of the file to be processed.
     * @param basePath The base path for the file.
     */
    private void processFile(RepoData repoData, String filePath, String basePath) {
        // Ensure the filePath is URL-encoded and free of extra spaces.
        filePath = filePath.trim().replaceAll(" ", "%20");

        // Get the current working directory.
        String currentDirectory = System.getProperty("user.dir");

        // Fetch the content of the file from the repository.
        ResponseEntity<String> response = getFileContent(repoData, filePath);
        String content = response.getBody();

        // Set the title of the file (used for logging).
        String title = filePath;

        // Attempt to append the content to a temporary file.
        try (PrintWriter printWriter = new PrintWriter(new FileWriter(TEMP_FILE_PATH, true))) {
            printWriter.println("File Name: " + title);
            printWriter.println("Content:");
            printWriter.println(content);

        } catch (IOException e) {
            // Log an error message if appending to the file fails.
            logger.debug("Oops! An error occurred while trying to append to the file: " + e.getMessage());
        }
    }

    /**
     * Parses the content of a POM file, extracts plugin/dependency information, and provides insights.
     *
     * @param repoData The repository data containing information about the repository.
     * @return A string containing insights and recommendations in HTML format.
     */
    public String parsePOMintoMap(RepoData repoData) {
        try {
            // Read content from the file and extract information.
            String content = new String(Files.readAllBytes(Paths.get(TEMP_FILE_PATH)));

            // Extracted content from the pom.xml file.
            StringBuilder resultBuilder = extractContent(content);

            // Check if there was an issue extracting content.
            if (resultBuilder == null) {
                logger.debug("Oops! There seems to be an issue in extracting content.");
                return null;
            }

            // Append a message indicating the extraction of plugin/dependency information.
            resultBuilder.append("In the above, I've gathered plugin/dependency information from the pom.xml of a repository.")
                    .append("\n");

            // Calculate the initial number of tokens available for processing.
            int llmTokenLimitWithPrompt = LLM_TOKEN_LIMIT - countTokens(DEPENDENCY_VERSION_INSIGHT_PROMPT);

            // Get the diff string by comparing with Pull Requests.
            // Return the final result as a string.
            return parsePRForPomFile(repoData, llmTokenLimitWithPrompt);
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
        boolean isDiffHeaderSet = false;
        boolean isPomPresentInDiff = false;

        // Fetch developer and Pull Request code from the repository.
        Map<String, List<String>> devAndPRCode = getDevPRCode(repoData);

        // StringBuilder to store the merged diff code for all "pom.xml" occurrences.
        StringBuilder mergedPomXmlDiff = new StringBuilder();

        // Iterate through the entries in the map containing developer and PR code.
        for (Map.Entry<String, List<String>> entry : devAndPRCode.entrySet()) {
            // Check if there are more than 1 element in the list (indicating PR code is present).
            if ((entry.getValue().size() > 1) && (llmTokenLimitWithPrompt > 0)){
                // Get the PR code from the list.
                String devPRCode = entry.getValue().get(1);

                // Check if "pom.xml" is present in the PR code.
                if (devPRCode != null && isPomXmlPresent(devPRCode)) {
                    isPomPresentInDiff = true;
                    // Get the entire diff patch relevant to "pom.xml".
                    String pomXmlDiff = getPomXmlDiff(devPRCode);

                    // Check if the token count in the "pom.xml" diff and prompt is within the adjusted limit.
                    if (countTokens(pomXmlDiff+DEPENDENCY_VERSION_INSIGHT_PR_PROMPT) <= llmTokenLimitWithPrompt) {
                        if (!isDiffHeaderSet) {
                            mergedPomXmlDiff.append(DEPENDENCY_VERSION_INSIGHT_PR_PROMPT).append(DEPENDENCY_VERSION_INSIGHT_PROMPT).append("\n");
                            isDiffHeaderSet = true;
                        }

                        // Append the "pom.xml" diff to the merged diff.
                        mergedPomXmlDiff.append(pomXmlDiff);

                        // Adjust the token limit based on the appended diff content.
                        llmTokenLimitWithPrompt = llmTokenLimitWithPrompt - countTokens(pomXmlDiff + "\n");
                    }
                }
            }
        }

        if (!isPomPresentInDiff)
        {
            // If there's no diff, append a placeholder message.
            mergedPomXmlDiff.append(DEPENDENCY_VERSION_INSIGHT_PROMPT).append("\n");
        }

        // Return the merged "pom.xml" diff code if at least one occurrence is found, otherwise return null.
        return mergedPomXmlDiff.toString();
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
        return lowercaseDiffCode.contains("pom.xml");
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
     * @param dirPath  The path of the current directory.
     * @param basePath The base path for constructing file paths within the directory.
     */
    private void processDirectory(RepoData repoData, String dirPath, String basePath) {

        try {
            // Retrieve the contents of the current directory from the repository
            ResponseEntity<String> response = getRepositoryContents(repoData, dirPath);

            // Parse the JSON response to obtain a list of directory contents
            List<Map<String, Object>> contents = jsonUtils.parseJSONResponse(response.getBody());

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
                    processFile(repoData, path, basePath);
                    foundPomFlag = true;  // Set the flag to indicate that "pom.xml" has been found
                    break;  // Exit the loop since the file has been found
                } else if (isDirectory(type) && (!foundPomFlag)) {
                    // Recursively process subdirectories if "pom.xml" has not been found yet
                    processDirectory(repoData, path, basePath);
                }
            }
        } catch (Exception e) {
            // Log an error message if an exception occurs during directory processing
            logger.error("Error processing directory: {}", e.getMessage(), e);
        }
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
                String tagType = matcher.group(TAG_TYPE_GROUP_INDEX);
                String artifactId = matcher.group(ARTIFACT_ID_GROUP_INDEX);
                String version = matcher.group(VERSION_GROUP_INDEX);

                // Check if both version and artifactId are found.
                if (isValid(tagType, artifactId, version)) {
                    // Append information to the result StringBuilder.
                    appendToResult(resultBuilder, tagType, artifactId, version);
                }
            }
        } catch (PatternSyntaxException e) {
            // Handle the case where the regex pattern is invalid.
            handleInvalidRegex(e);
        }
        // Check if any information was extracted.
        if (resultBuilder.isEmpty()) {
            return null;
        }
        // Return the StringBuilder containing extracted information.
        return resultBuilder;
    }

    /**
     * Checks if the provided tagType, artifactId, and version are valid.
     *
     * @param tagType    The tag type extracted from the regex match.
     * @param artifactId The artifactId extracted from the regex match.
     * @param version    The version extracted from the regex match.
     * @return True if both version and artifactId are found and not null; false otherwise.
     */
    private static boolean isValid(String tagType, String artifactId, String version) {
        return tagType != null && artifactId != null && version != null;
    }

    /**
     * Appends information to the provided StringBuilder.
     *
     * @param resultBuilder The StringBuilder to which information is appended.
     * @param tagType       The tag type extracted from the regex match.
     * @param artifactId    The artifactId extracted from the regex match.
     * @param version       The version extracted from the regex match.
     */
    private static void appendToResult(StringBuilder resultBuilder, String tagType, String artifactId, String version) {
        resultBuilder.append("Tag Type: ").append(tagType);
        resultBuilder.append("  ArtifactId: ").append(artifactId);
        resultBuilder.append("  Version: ").append(version).append("\n");
    }

    /**
     * Handles the case where the regex pattern is invalid.
     *
     * @param e The PatternSyntaxException thrown during regex pattern compilation.
     */
    private static void handleInvalidRegex(PatternSyntaxException e) {
        // Log an error message indicating an invalid regex pattern.
        System.err.println("Invalid regex pattern: " + e.getMessage());
    }

    /**
     * Updates the base path by appending the directory path if the base path is not empty.
     * If the base path is empty, sets the base path to the directory path.
     *
     * @param basePath The current base path.
     * @param dirPath  The directory path to be appended.
     */
    private void updateBasePath(String basePath, String dirPath) {
        // Check if the base path is not empty.
        if (StringUtils.hasText(basePath)) {
            // Append the directory path to the base path using the appropriate separator.
            basePath = basePath + File.separator + dirPath;
        } else {
            // If the base path is empty, set it to the directory path.
            basePath = dirPath;
        }
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
        // Return the result indicating whether the file is valid or not.
        return validExtensions.stream().noneMatch(extension -> lowercasePath.contains(extension));
    }

    /**
     * Processes a content item based on its type, name, and the status of a flag.
     *
     * @param item  The map representing the content item.
     */
    private void processContentItem(Map<String, Object> item, RepoData repoData) {
        // Initialize the base path to an empty string.
        String basePath = "";

        // Extract information about the content item.
        String type = (String) item.get("type");
        String name = (String) item.get("name");
        String path = (String) item.get("path");

        // Check if the content item is a file, named "pom.xml", and the POM flag is not already found.
        if (isPomFile(type, name) && !foundPomFlag) {
            // Log a message indicating the discovery of a pom.xml file.
            logger.debug("Found POM.XML file\n");

            // Process the pom.xml file using the processFile method.
            processFile(repoData, path, basePath);

            // Set the POM flag to true, indicating the discovery of a pom.xml file.
            foundPomFlag = true;
        }
        // Check if the content item is a directory and the POM flag is not already found.
        else if (isDirectory(type) && (!foundPomFlag)) {
            // Process the directory using the processDirectory method.
            processDirectory(repoData, path, basePath);
        }
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
        String bugDetectionInApplicationFlowInsightString = getInsightsFromPromptAndDevPRCode(devAndPRCode,
                BUG_DETECTION_IN_APPLICATION_FLOW);
        logger.debug(bugDetectionInApplicationFlowInsightString);
        return bugDetectionInApplicationFlowInsightString;
    }

    /**
     * Retrieves custom code linting insights based on the provided repository data.
     * @author Sameer Amesara
     * @param repoData The repository data used to generate insights.
     * @return A string containing insights related to custom code linting.
     */
    @Override
    public String getCustomCodeLintingInsights(RepoData repoData) {
        Map<String, List<String>> devAndPRCode = getDevPRCode(repoData);
        String getCustomCodeLintingInsightString = getInsightsFromPromptAndDevPRCode(devAndPRCode,
                CUSTOM_CODE_LINTING);
        logger.debug(getCustomCodeLintingInsightString);
        return getCustomCodeLintingInsightString;
    }

    @Override
    public String getTestCaseMinimizationInsights(RepoData repoData) {

        Map<String, List<String>> devAndPRCode = getDevPRCode(repoData);
        String testCaseMinimizationInsightString = getInsightsFromPromptAndDevPRCode(devAndPRCode,
                TEST_CASE_MINIMIZATION);
        logger.debug(testCaseMinimizationInsightString);
        return testCaseMinimizationInsightString;
    }

    @Override
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
                logger.debug(entry.getKey() + ": " + entry.getValue());
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
            logger.debug("repo name for collab: owner: " + owner + " reponame: " + repoName);

            String apiUrl = String.format("%s/repos/%s%s/pulls?state=all&sort=created&direction=desc&per_page=1&page=1",
                    GITHUB_API_URL, owner, repoName);
            logger.debug("apiUrl for collab-analysis story: " + apiUrl);

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
                                logger.debug("diffUrl for user: " + owner + " is: " + diffUrl);
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
                logger.debug("dcontributorDiff: " + contributorDiff);
                e.printStackTrace();
            }
        }

        // Print the map of contributor against it's respective diff url
        for (Map.Entry<String, String> test : contributorDiff.entrySet()) {
            logger.debug("Contributor: " + test.getKey() + "\nDiff Url: " + test.getValue());
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
            logger.debug("Individual: " + entry.getKey());
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
                    logger.debug("The Code Smell Rating is 10.");
                } else {
                    logger.debug("Inside else: " + entry.getValue());
                    downloadURLToFile(entry.getValue(), fileName, entry.getKey(), commitCount);
                    logger.debug("Content downloaded successfully.");
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

        logger.debug("In Smell Rating Method inside RepoDataServiceImpl");
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
            logger.debug(response);

            // Split the string using the ; delimiter
            String[] parts = response.split(";");

            // Trim leading and trailing whitespaces from each part
            for (int j = 0; j < parts.length; j++) {
                parts[j] = parts[j].trim();
            }

            // Access the individual parts
            String contributor = parts[0];
            logger.debug(contributor);
            int commitCount = Integer.parseInt(parts[1]);
            logger.debug(String.valueOf(commitCount));
            int rating = (int) Double.parseDouble(parts[2]);
            logger.debug(String.valueOf(rating));

            fw.write(contributor + "\nCommit Count: " + commitCount + "\nCode Smells Rating: "
                    + rating + "\n\n");

        }

        fw.write(COLLAB_ANALYSIS_FINAL_PART);
        fw.close();
        String finalPrompt = new String(
                Files.readAllBytes(Paths.get(COLLAB_ANALYSIS_FILES_PATH + "SmellRatingPrompt.txt")));
        Thread.sleep(30000);
        String finalResponse = chatGPTService.chat(finalPrompt);
        logger.debug("Collab Analysis GPT Response:\n" + finalResponse);

        return finalResponse;
    }

    @Override
    public String getAdvancedCodeSearchInsight(RepoData repoData, String input) {
        Map<String, List<String>> devAndPRCode = getDevPRCode(repoData);
        String AdvancedCodeSearchPrompt = "Check if there is/are any " + input + ADVANCED_CODE_SEARCH;
        String AdvancedCodeSearchString = getInsightsFromPromptAndDevPRCode(devAndPRCode, AdvancedCodeSearchPrompt);
        logger.debug(AdvancedCodeSearchString);
        return AdvancedCodeSearchString;
    }

    public ResponseEntity<String> getRepositoryContents(RepoData repoData) {

        UserData userData = userDataService.getOne(repoData.getUserId());
        String apiURL = GITHUB_API_URL + GITHUB_REPOS + "/" + repoData.getName() + "/contents";
        logger.debug(apiURL);
        ResponseEntity<String> response = restTemplate.exchange(apiURL, HttpMethod.GET,
                getAllHeadersEntity(userData.getUserAccessToken()), String.class);
        return response;
    }

    public ResponseEntity<String> getFileContent(RepoData repoData, String filePath) {
        UserData userData = userDataService.getOne(repoData.getUserId());
        String apiURL = GITHUB_API_URL + GITHUB_REPOS + "/" + repoData.getName() + "/contents/" + filePath;
        logger.debug(apiURL);
        ResponseEntity<String> response = restTemplate.exchange(apiURL, HttpMethod.GET,
                getAllHeadersEntity(userData.getUserAccessToken()), String.class);

        // TODO: Flip the condition and get an early return
        if (response.getStatusCodeValue() == 200) {
            // The response contains base64-encoded content, so decode it
            String content = response.getBody();
            logger.debug("RESPONSE 200");
            logger.debug(content);
            // content =
            // content.substring(content.indexOf("content")+9,content.indexOf("encoding")-3);
            content = jsonUtils.parseJSONResponseAsTree(content);
            // TODO: As the reason for double backslashes.
            content = content.replaceAll("\\s", "");
            logger.debug(content);
            String decodedContent = new String(Base64.getDecoder().decode(content), StandardCharsets.UTF_8);
            response = ResponseEntity.ok(decodedContent);
        }
        return response;
    }

    public ResponseEntity<String> getRepositoryContents(RepoData repoData, String path) {
        UserData userData = userDataService.getOne(repoData.getUserId());
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/vnd.github+json");
        headers.set("Authorization", "Bearer " + userData.getUserAccessToken());
        headers.set("X-GitHub-Api-Version", "2022-11-28");
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        URI uri = URI.create(GITHUB_API_URL + "/repos/" + repoData.getName() + "/contents" + "/" + path);

        RequestEntity<?> requestEntity = RequestEntity.get(uri).headers(headers).build();
        return restTemplate.exchange(requestEntity, String.class);
    }

}
