package com.taim.conduire.controller;

import com.taim.conduire.service.ChatGPTService;
import com.taim.conduire.service.JSONUtils;
import com.taim.conduire.service.LLMService;
import org.apache.tomcat.util.http.fileupload.ByteArrayOutputStream;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;


// TODO --> Designite detected this class not being used --> unutilized abstraction.
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

    private static final String FILE_TYPE = "file";
    private static final String DIR_TYPE = "dir";

    private static final Logger logger = LoggerFactory.getLogger(LLMController.class);

    //private String[] fileTypes = {".jpg",".class",".png","svg", "docx", "exe", "dll", "jar", "gif"};

    @GetMapping("/repository/languages")
    public Map<String, Integer> getRepositoryLanguages() {
        return llmService.getRepositoryLanguages();
    }

    @GetMapping("/repository/code-frequency")
    public List<List<Integer>> getRepositoryCodeFrequency() throws IOException {

        return llmService.getRepositoryCodeFrequency();
    }

    @GetMapping("repository/punch")
    public List<List<Integer>> getRepositoryPunchCard() throws IOException {

        return llmService.getRepositoryPunchCard();
    }

    @GetMapping("repository/punchtest")
    public int[] getRepositoryPunchCardtest() throws IOException {

        return llmService.getRepositoryPunchCardtest("hello");
    }

    // TODO --> Design Smell : the method should be in LLMService.java
    @GetMapping("repository/chart")
    public ResponseEntity<byte[]> getLanguageChart() throws IOException {
        byte[] chartImage = llmService.generatePieChart(llmService.getRepositoryLanguages());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_PNG);

        return new ResponseEntity<>(chartImage, headers, HttpStatus.OK);

    }

    @GetMapping("/download/{owner}/{repoName}")
    public ResponseEntity<byte[]> downloadRepositoryCode(
            @PathVariable String owner,
            @PathVariable String repoName) {
        byte[] code = llmService.downloadRepositoryCode(owner, repoName);
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
    public String getTest() {
        return llmService.getHelloWorld();
    }

    @RequestMapping("/hello")
    public String hello() {
        return "Hello World";
    }

    private boolean isValidResponse(ResponseEntity<String> response) {
        return response != null && response.getBody() != null && response.getStatusCode().is2xxSuccessful();
    }

    private List<Map<String, Object>> parseJSONResponse(String responseBody) {
        return JSONUtils.parseJSONResponse(responseBody);
    }

    private boolean isValidFile(String path) {
        List<String> validExtensions = Arrays.asList("jpg", "png", "svg", "class", "docx", "exe", "dll", "jar", "gif", "css", "html");
        return !validExtensions.stream().anyMatch(extension -> path.toLowerCase().contains(extension));
    }

    private void processContentItem(Map<String, Object> item) {
        String basePath = "";
        String type = (String) item.get("type");
        String name = (String) item.get("name");
        String path = (String) item.get("path");

        if (FILE_TYPE.equals(type) && isValidFile(path)) {
            processFile(owner, repo, path, basePath);
        } else if (DIR_TYPE.equals(type)) {
            processDirectory(owner, repo, path, basePath);
        }
    }

    @GetMapping("/repository/content")
    public void getRepoContent() {
        ResponseEntity<String> response = llmService.getRepositoryContents(owner, repo);
        System.out.println(response);

        if (isValidResponse(response)) {
            List<Map<String, Object>> contents = parseJSONResponse(response.getBody());

            for (Map<String, Object> item : contents) {
                processContentItem(item);
            }
        }
    }


    /*public void getRepoContent() {
        String basePath = "";
        ResponseEntity<String> response = llmService.getRepositoryContents(owner, repo);
        System.out.println(response);

        // TODO: Get an early return. --> DONE
        if (response == null || response.getBody() == null) {
            return;
        }
        List<Map<String, Object>> contents = JSONUtils.parseJSONResponse(response.getBody());

        for (Map<String, Object> item : contents) {
            String type = (String) item.get("type");
            String name = (String) item.get("name");
            String path = (String) item.get("path");
            System.out.println(type + "***" + name + "***" + path);
            // TODO: split this logic or make a variable to help it understand --> DONE
            if ("file".equals(type) && !path.toLowerCase().contains("jpg") && !path.toLowerCase().contains("png") && !path.toLowerCase().contains("svg") && !path.toLowerCase().contains("class")
                    && !path.toLowerCase().contains("docx") && !path.toLowerCase().contains("exe") && !path.toLowerCase().contains("dll")
                    && !path.toLowerCase().contains("jar") && !path.toLowerCase().contains("gif") && !path.toLowerCase().contains("css") && !path.toLowerCase().contains("html")) {
                processFile(owner, repo, path, basePath);
            } else if ("dir".equals(type)) {
                processDirectory(owner, repo, path, basePath);
            }
        }
    }*/

    private void updateBasePath(String basePath, String dirPath) {
        basePath = StringUtils.hasText(basePath) ? basePath + File.separator + dirPath : dirPath;
    }

    private void processDirectory(String owner, String repo, String dirPath, String basePath) {
        logger.debug("Entering processDirectory");

        try {
            ResponseEntity<String> response = llmService.getRepositoryContents(owner, repo, dirPath);
            logger.debug("Response body: {}", response.getBody());
            List<Map<String, Object>> contents = JSONUtils.parseJSONResponse(response.getBody());
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
            // TODO: Handle exception --> NOT HANDLING ANYTHING FOR NOW.
            logger.error("Error processing directory: {}", e.getMessage(), e);
        }
        logger.debug("Exiting processDirectory");
    }

/*    private void processDirectory(String owner, String repo, String dirPath, String basePath) {
        System.out.println("WE IN PROCESSDIRECTORY");
        ResponseEntity<String> response = llmService.getRepositoryContents(owner, repo, dirPath);
        System.out.println(response.getBody());
        List<Map<String, Object>> contents = JSONUtils.parseJSONResponse(response.getBody());
        System.out.println(contents);

        if (StringUtils.hasText(basePath)) {
            basePath = basePath + File.separator + dirPath;
        } else {
            basePath = dirPath;
        }

        for (Map<String, Object> item : contents) {
            String type = (String) item.get("type");
            String path = (String) item.get("path");
            // TODO: Again make a variable to help understand what is happening. --> DONE
            if ("file".equals(type) && !path.toLowerCase().contains("jpg") && !path.toLowerCase().contains("png") && !path.toLowerCase().contains("svg") && !path.toLowerCase().contains("class")
                    && !path.toLowerCase().contains("docx") && !path.toLowerCase().contains("exe") && !path.toLowerCase().contains("dll")
                    && !path.toLowerCase().contains("jar") && !path.toLowerCase().contains("gif") && !path.toLowerCase().contains("css") && !path.toLowerCase().contains("html")) {
                processFile(owner, repo, path, basePath);
            } else if ("dir".equals(type)) {
                processDirectory(owner, repo, path, basePath);
            }
        }
        System.out.println("WE OUT PROCESSFILE");
    }*/

    private void processFile(String owner, String repo, String filePath, String basePath) {
        logger.debug("Processing file...");

        filePath = filePath.trim().replaceAll(" ", "%20");
        logger.debug("Modified filePath: " + filePath);

        String currentDirectory = System.getProperty("user.dir");
        logger.debug("Current Working Directory: " + currentDirectory);

        ResponseEntity<String> response = llmService.getFileContent(owner, repo, filePath);
        String content = response.getBody();

        String title = filePath;

        if (StringUtils.hasText(basePath)) {
            String fileName = basePath + File.separator + "output.txt"; // configurable or parameterized file name

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
        }

        logger.debug("File processing completed.");
    }


   /* private void processFile(String owner, String repo, String filePath, String basePath) {
        // TODO: why these print statements? --> DONE
        System.out.println("WE IN PROCESSFILE");
        filePath = filePath.trim().replaceAll(" ", "%20");
        System.out.println(filePath);
        String currentDirectory = System.getProperty("user.dir");
        System.out.println("Current Working Directory: " + currentDirectory);
        ResponseEntity<String> response = llmService.getFileContent(owner, repo, filePath);
        System.out.println(response.getBody());
        String content = response.getBody();
        String title = filePath;

        // TODO: Remove all useless comments. --> DONE
        //Map<String, Object> file = JSONUtils.parseJSONResponseAsMap(response.getBody());
        *//*System.out.println(file);

        String title = (String) file.get("name");
        String content = (String) file.get("content");
        System.out.println("Name : " + title);
        System.out.println("Content : " + content);*//*

        // TODO: Get an early statement. --> DONE
        if (StringUtils.hasText(basePath)) {
            *//*String outputDir = "output" + File.separator + basePath;
            new File(outputDir).mkdirs();
            try (FileWriter fileWriter = new FileWriter(outputDir + File.separator + title)) {
                fileWriter.write(title + "\n");
                fileWriter.write(new String(Base64.getDecoder().decode(content)));
            } catch (Exception e) {
                e.printStackTrace();
            }*//*

            String fileName = "output.txt"; // Replace with the desired file name

            try {
                // Create a FileWriter with append mode (true) to append to an existing file
                // If the file does not exist, it will be created
                FileWriter fileWriter = new FileWriter(fileName, true);

                // Create a PrintWriter for writing text to the file
                PrintWriter printWriter = new PrintWriter(fileWriter);

                // Write the content to the file
                printWriter.println("File Name: " + title);
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
    }*/

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

            //TODO --> remove magic numbers: 800, 400
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ChartUtilities.writeChartAsPNG(byteArrayOutputStream, chart, 800, 400);
            return byteArrayOutputStream.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            return new byte[0];
        }
    }
}
