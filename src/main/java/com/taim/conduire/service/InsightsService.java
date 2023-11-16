package com.taim.conduire.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.taim.conduire.constants.ConstantCodes;
import com.taim.conduire.domain.RepoData;
import com.taim.conduire.domain.UserData;
import com.taim.conduire.service.impl.RepoDataServiceImpl;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
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

    @Autowired
    private RepoDataServiceImpl repoDataServiceImpl;

    @Autowired
    private ChatGPTService chatGPTService;

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
        System.out.println("reviewerComments: " + reviewerComments.toString());
        return reviewerComments;
    }

    public String getRepositoryPRsCollab(RepoData repoData) throws IOException, InterruptedException {
        //Call getRepoContributors to get the top 3 contributors who commit the most
        Map<String,Integer> collabCommit = repoDataServiceImpl.getRepoContributors(repoData);
        Map<String,Integer> top3Contributors = new HashMap<>();

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

        //Create a Map to store the diff url against the contributor
        Map<String,String> contributorDiff = new HashMap<>();

        for(Map.Entry<String,Integer> entry : top3Contributors.entrySet()){

            //Get owner name from top 3 contributors
            String owner = entry.getKey();

            //Get Repo from variable repoData
            String repoName = repoData.getName().substring(repoData.getName().indexOf("/"));
            System.out.println("repo name for collab: owner: " + owner + " reponame: " + repoName);

            String apiUrl = String.format("%s/repos/%s%s/pulls?state=all&sort=created&direction=desc&per_page=1&page=1", GITHUB_API_URL, owner, repoName);
            System.out.println("apiUrl for collab-analysis story: " + apiUrl);

            UserData userData = userDataService.getOne(repoData.getUserId());

            try {
                ResponseEntity<String> response = restTemplate.exchange(apiUrl, HttpMethod.GET, repoDataServiceImpl.getAllHeadersEntity(userData.getUserAccessToken()), String.class);
                repoDataServiceImpl.showAvailableAPIHits(response.getHeaders());
                String jsonArrayString = response.getBody();
                System.out.println("jsonArrayString: " + jsonArrayString);

                if(response.getStatusCode().value() == 200) {
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
                    }
                    else {
                        contributorDiff.put(owner, "No PR found");
                    }
                }
            }
            catch (Exception e){
                contributorDiff.put(owner, "No PR found");
                e.printStackTrace();
            }
        }

        //Print the map of contributor against it's respective diff url
        for(Map.Entry<String,String> test : contributorDiff.entrySet()) {
            System.out.println("Contributor: " + test.getKey() + "\nDiff Url: " + test.getValue());
        }

        //get the code for the respective diff urls and store them in 3 separate text files
        getCodefromDiffUrl(contributorDiff,repoData,sortedMap);

        //Calculate the commit count and the smells using LLM and get insight after that
        String response = getSmellRating();
        return response;
    }

    //This method would hit the 3 diff urls in the map and store them in three text file: diff1.txt, diff2.txt, diff3.txt
    public void getCodefromDiffUrl(Map<String,String> contributorDiff, RepoData repoData, Map<String,Integer> sortedMap) throws IOException {
        int count = 1;
        for( Map.Entry<String,String> entry : contributorDiff.entrySet()){
            System.out.println("Individual: " + entry.getKey());
            int commitCount = sortedMap.get(entry.getKey());
            String fileName = "diff" + String.valueOf(count) + ".txt";
            count++;
            FileWriter fw = new FileWriter(fileName);
            try{
                if(entry.getValue().equals("No PR found")) {
                    fw.write("Determine the smells of the code in the .diff file below. Rate the overall smells on a scale of 1-10, " +
                            "1 being the least amount of code smells and 10 being the most code smells found. " +
                            "Give me the in the following output format: \"Contributor; Rating; Commit-Count\"\n "+"\n\nContributor: "  + entry.getKey() +
                            "\nCommit Count: "+ commitCount +
                            "\n\nDiff File:\nIgnore the code, the Code Smell Rating is 10." +
                            "\n\nModify this prompt to receive the output in a semi-colon separated string. For Example: Contributor; Commit-Count; Smell-Rating");
                    System.out.println("The Code Smell Rating is 10.");
                }
                else
                {
                    System.out.println("Inside else: " + entry.getValue());
                    downloadURLToFile(entry.getValue(), fileName, entry.getKey(), commitCount);
                    System.out.println("Content downloaded successfully.");
                }
                fw.close();
            }
            catch(Exception e){
                e.printStackTrace();
            }
        }
    }

    private static void downloadURLToFile(String url, String filePath, String contributor, Integer commitCount) throws IOException {
        Document document = Jsoup.parse(new URL(url), 3000);

        // Replace this CSS selector with the appropriate selector for your code snippet
        Elements codeElements = document.select("body");

        // Extract the code content
        String codeSnippet = codeElements.text();

        // Save the code snippet to a file
        try (FileWriter writer = new FileWriter(filePath)) {
            writer.write("Determine the smells of the code in the .diff file below. Rate the overall smells on a scale of 1-10, " +
                    "1 being the least amount of code smells and 10 being the most code smells found. " +
                    "Give me the in the following output format: \"Contributor; Rating; Commit-Count\"\n "+"\n\nContributor: " +
                    contributor + "\nCommit Count: "+ commitCount + "\n\nDiff File:\n");
            writer.write(codeSnippet);
            writer.write("\n\nModify this prompt to receive the output in a semi-colon separated string. For Example: Contributor; Commit-Count; Smell-Rating");
            writer.close();
        }
    }

    public String getSmellRating() throws IOException, InterruptedException {
        Map<String, String> roleInsights = new HashMap<>();

        System.out.println("In Smell Rating Method inside RepoDataServiceImpl");

        FileWriter fw = new FileWriter("SmellRatingPrompt.txt");
        fw.write("For a specific GitHub repository, you have information about the top 3 contributors based on commit count and their respective code smells ratings. " +
                "The code smells ratings are on a scale of 1-10, where 1 represents the least number of code smells, " +
                "and 10 represents a high number of code smells.\n\nTop 3 Contributors:\n\n");

        for(int i = 1 ; i<=3 ; i++){
            String fileName = "diff" + String.valueOf(i) + ".txt";
            String prompt = new String(Files.readAllBytes(Paths.get(fileName)));
            System.out.println("Checking individual file read: " + prompt);
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
            int commitCount  = Integer.parseInt(parts[1]);
            System.out.println(commitCount);
            int rating = (int)Double.parseDouble(parts[2]);
            System.out.println(rating);

            fw.write(contributor+"\nCommit Count: " + String.valueOf(commitCount) + "\nCode Smells Rating: " + String.valueOf(rating) + "\n\n");

        }

        fw.write("Your task is to determine which two contributors, when collaborating together, " +
                "would be the most productive. Productivity is defined as a combination of commit count and code smells rating, " +
                "where lower code smells ratings are preferable.\n\n" +
                "Provide the names of the two contributors and a brief explanation of why you consider them to be the most productive collaborators based on the given criteria.");
        fw.close();
        String finalPrompt = new String(Files.readAllBytes(Paths.get("SmellRatingPrompt.txt")));
        Thread.sleep(30000);
        String finalResponse = chatGPTService.chat(finalPrompt);
        System.out.println("Collab Analysis GPT Response:\n" + finalResponse);

        return finalResponse;
    }
}
