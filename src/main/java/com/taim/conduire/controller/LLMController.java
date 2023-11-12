package com.taim.conduire.controller;

import com.taim.conduire.service.ChatGPTService;
import com.taim.conduire.service.JSONUtils;
import com.taim.conduire.service.LLMService;
import org.apache.tomcat.util.http.fileupload.ByteArrayOutputStream;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

@RestController
public class LLMController {
    @Autowired
    private LLMService llmService;
    @Autowired
    private ChatGPTService chatGPTService;

    private JSONUtils jsonUtils;

    @Value("${github.api.url}")
    private String githubApiUrl;

    @Value("${github.repository.owner}")
    private String owner;

    @Value("${github.repository.name}")
    private String repo;

    //private String[] fileTypes = {".jpg",".class",".png","svg", "docx", "exe", "dll", "jar", "gif"};

    @GetMapping("/repository/languages")
    public Map<String, Integer> getRepositoryLanguages() {
        return llmService.getRepositoryLanguages();
    }

    @GetMapping("/repository/code-frequency")
    public List<List<Integer>>getRepositoryCodeFrequency() throws IOException {

        return llmService.getRepositoryCodeFrequency();
    }

    @GetMapping("repository/punch")
    public List<List<Integer>> getRepositoryPunchCard() throws IOException {

        return llmService.getRepositoryPunchCard();
    }

    /* Returns the total number of commits authored by the contributor.
    In addition, the response includes a Weekly Hash (weeks array) with the following information:
    w - Start of the week, given as a Unix timestamp.
    a - Number of additions
    d - Number of deletions
    c - Number of commits */
    @GetMapping("repository/contributors1")
    public List<LLMService.WeekCommitActivity> getAllContributorCommitActivity() throws IOException {

        return llmService.getAllContributorCommitActivity();
    }

    /* Returns the last year of commit activity grouped by week. The days
     array is a group of commits per day, starting on Sunday. */
    @GetMapping("repository/commit_activity")
    public List<LLMService.WeekCommitActivity> getLastYearCommitActivity() {
        return llmService.getLastYearCommitActivity();
    }

    /* Returns the total commit counts for the owner.
       The array order is the oldest week (index 0) to most recent week.
       The most recent week is seven days ago at UTC midnight to today at UTC midnight.
    @GetMapping("repository/participation")
    public List<Integer> getCommitCountsForOwner() {
        return llmService.getCommitCountsForOwner();
    } */

    @GetMapping("repository/participation1")
    public List<Integer> getCommitCountsForNonOwners() {
        return llmService.getCommitCountsForNonOwners();
    }


    @GetMapping("repository/punchtest")
    public int[] getRepositoryPunchCardtest() throws IOException {

        return llmService.getRepositoryPunchCardtest("hello");
    }

    @GetMapping("repository/chart")
    public ResponseEntity<byte[]> getLanguageChart() throws IOException{
        byte[] chartImage = llmService.generatePieChart(llmService.getRepositoryLanguages());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_PNG);

        return new ResponseEntity<>(chartImage, headers, HttpStatus.OK);

    }
    @GetMapping("/download/{owner}/{repoName}")
    public ResponseEntity<byte[]> downloadRepositoryCode(
            @PathVariable String owner,
            @PathVariable String repoName) {
        byte[] code = llmService .downloadRepositoryCode(owner, repoName);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", repoName + ".zip");
        return new ResponseEntity<>(code, headers, HttpStatus.OK);
    }

    @GetMapping("/repo")
    public String getRepoData() {
        return llmService.getRepoData();
    }

    @GetMapping("/test")
    public String getTest(){
        return llmService.getHelloWorld();
    }

    @RequestMapping("/hello")
    public String hello(){
        return "Hello World";
    }

    @GetMapping("/repository/content")
    public void getRepoContent(){
        String basePath = "";
        ResponseEntity<String> response = llmService.getRepositoryContents(owner,repo);
        System.out.println(response);

        if(response!=null && response.getBody()!=null) {
            List<Map<String, Object>> contents = JSONUtils.parseJSONResponse(response.getBody());

            assert contents != null;
            for (Map<String, Object> item : contents) {
                String type = (String) item.get("type");
                String name = (String) item.get("name");
                String path = (String) item.get("path");
                System.out.println(type + "***" + name + "***" + path);

                if ("file".equals(type) && !path.toLowerCase().contains("jpg") && !path.toLowerCase().contains("png") && !path.toLowerCase().contains("svg") && !path.toLowerCase().contains("class")
                        && !path.toLowerCase().contains("docx") && !path.toLowerCase().contains("exe") && !path.toLowerCase().contains("dll")
                        && !path.toLowerCase().contains("jar") && !path.toLowerCase().contains("gif") && !path.toLowerCase().contains("css") && !path.toLowerCase().contains("html")) {
                    processFile(owner, repo, path, basePath);
                }
                else if ("dir".equals(type)) {
                    processDirectory(owner, repo, path, basePath);
                }
            }
        }
    }

    private void processDirectory(String owner, String repo, String dirPath, String basePath) {
        System.out.println("WE IN PROCESSDIRECTORY");
        ResponseEntity<String> response = llmService.getRepositoryContents(owner,repo,dirPath);
        System.out.println(response.getBody());
        List<Map<String, Object>> contents = JSONUtils.parseJSONResponse(response.getBody());
        System.out.println(contents);

        if (StringUtils.hasText(basePath)) {
            basePath = basePath + File.separator + dirPath;
        } else {
            basePath = dirPath;
        }

        assert contents != null;
        for (Map<String, Object> item : contents) {
            String type = (String) item.get("type");
            String path = (String) item.get("path");

            if ("file".equals(type) && !path.toLowerCase().contains("jpg") && !path.toLowerCase().contains("png") && !path.toLowerCase().contains("svg") && !path.toLowerCase().contains("class")
                    && !path.toLowerCase().contains("docx") && !path.toLowerCase().contains("exe") && !path.toLowerCase().contains("dll")
                    && !path.toLowerCase().contains("jar") && !path.toLowerCase().contains("gif") && !path.toLowerCase().contains("css") && !path.toLowerCase().contains("html")) {
                processFile(owner, repo, path, basePath);
            } else if ("dir".equals(type)) {
                processDirectory(owner, repo, path, basePath);
            }
        }
        System.out.println("WE OUT PROCESSFILE");
    }

    private void processFile(String owner, String repo, String filePath, String basePath) {
        System.out.println("WE IN PROCESSFILE");
        filePath = filePath.trim().replaceAll(" ","%20");
        System.out.println(filePath);
        String currentDirectory = System.getProperty("user.dir");
        System.out.println("Current Working Directory: " + currentDirectory);
        ResponseEntity<String> response = llmService.getFileContent(owner, repo, filePath);
        System.out.println(response.getBody());
        String content = response.getBody();
        String title = filePath;
        //Map<String, Object> file = JSONUtils.parseJSONResponseAsMap(response.getBody());
        /*System.out.println(file);

        String title = (String) file.get("name");
        String content = (String) file.get("content");
        System.out.println("Name : " + title);
        System.out.println("Content : " + content);*/

        if (StringUtils.hasText(basePath)) {
            /*String outputDir = "output" + File.separator + basePath;
            new File(outputDir).mkdirs();
            try (FileWriter fileWriter = new FileWriter(outputDir + File.separator + title)) {
                fileWriter.write(title + "\n");
                fileWriter.write(new String(Base64.getDecoder().decode(content)));
            } catch (Exception e) {
                e.printStackTrace();
            }*/

            String fileName = "output.txt"; // Replace with the desired file name

            try {
                // Create a FileWriter with append mode (true) to append to an existing file
                // If the file does not exist, it will be created
                FileWriter fileWriter = new FileWriter(fileName, true);

                // Create a PrintWriter for writing text to the file
                PrintWriter printWriter = new PrintWriter(fileWriter);

                // Write the content to the file
                printWriter.println("File Name: "+title);
                printWriter.println();
                printWriter.println("Content:");
                printWriter.println(content);
                printWriter.println();

                // Close the PrintWriter and FileWriter
                printWriter.close();
                fileWriter.close();

                System.out.println("Content has been written to the file.");
            } catch (IOException e) {
                System.err.println("An error occurred: " + e.getMessage());
            }
        }
        System.out.println("WE OUT PROCESSFILE");
    }

    @GetMapping("repository/contributors")
    public ResponseEntity<byte[]> getContributors() throws IOException {

        List<Map<String, Object>> contributors = llmService.getRepoContributors();
        // Generate the bar chart
        JFreeChart barChart = llmService.generateBarChart(contributors);

        // Generate chart image as a byte array
        byte[] chartImage = generateBarChartImage(barChart);

        // Return the chart image as a response with content type 'image/png'
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(chartImage);
    }

    private byte[] generateBarChartImage(JFreeChart chart) {
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ChartUtilities.writeChartAsPNG(byteArrayOutputStream, chart, 800, 400);
            return byteArrayOutputStream.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            return new byte[0];
        }
    }
}
