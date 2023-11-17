package com.taim.conduire.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.tomcat.util.http.fileupload.ByteArrayOutputStream;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class LLMService {

    @Value("${github.api.url}")
    private String githubApiUrl;

    @Value("${github.repository.owner}")
    private String owner;

    @Value("${github.repository.name}")
    private String repo;

    // TODO: WTF. accessToken hardcoded.
    @Value("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")
    private String accessToken;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Autowired
    public LLMService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public Map<String, Integer> getRepositoryLanguages() {
        String apiUrl = String.format("%s/repos/%s/%s/languages", githubApiUrl, owner, repo);
        return restTemplate.getForObject(apiUrl, Map.class);
    }

    public List<List<Integer>> getRepositoryCodeFrequency() throws IOException {
        String apiUrl = String.format("%s/repos/%s/%s/stats/code_frequency", githubApiUrl, owner, repo);
        System.out.println(apiUrl);
        String apiResponse = restTemplate.getForObject(apiUrl, String.class);
        // TODO --> Implementation smell: long statement
        List<List<Integer>> codeFrequencyStats = objectMapper.readValue(apiResponse, objectMapper.getTypeFactory().constructCollectionType(List.class, List.class));
        return codeFrequencyStats;
    }

    public List<List<Integer>> getRepositoryPunchCard() throws IOException {
        String apiUrl = String.format("%s/repos/%s/%s/stats/punch_card", githubApiUrl, owner, repo);
        System.out.println(apiUrl);
        String apiResponse = restTemplate.getForObject(apiUrl, String.class);
        // TODO --> Implementation smell: long statement
        List<List<Integer>> repoPunchCard = objectMapper.readValue(apiResponse, objectMapper.getTypeFactory().constructCollectionType(List.class, List.class));
        computeWeeklyCommits(repoPunchCard);
        return repoPunchCard;
    }

    public int[] getRepositoryPunchCardtest(String name) throws IOException {
        String apiUrl = String.format("%s/repos/%s/stats/punch_card", githubApiUrl,name);
        System.out.println(apiUrl);
        String apiResponse = restTemplate.getForObject(apiUrl, String.class);
        // TODO --> Implementation smell: long statement
        List<List<Integer>> repoPunchCard = objectMapper.readValue(apiResponse, objectMapper.getTypeFactory().constructCollectionType(List.class, List.class));
        return computeWeeklyCommits(repoPunchCard);
    }

    public byte[] generatePieChart(Map<String,Integer> data) throws IOException{
        DefaultPieDataset dataset = new DefaultPieDataset();
        for (Map.Entry<String, Integer> entry : data.entrySet()) {
            dataset.setValue(entry.getKey(), entry.getValue());
        }

        // TODO: Why 8tab space instead of 4?
        JFreeChart chart = ChartFactory.createPieChart(
                "Your Pie Chart Title",
                dataset,
                true,
                true,
                false
        );

        PiePlot plot = (PiePlot) chart.getPlot();
        //plot.setSection(0000.35);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        // TODO --> Implementation smell: remove magic numbers: 400, 300
        ChartUtilities.writeChartAsPNG(outputStream, chart, 400, 300);
        return outputStream.toByteArray();
    }

    public JFreeChart generateBarChart(List<Map<String,Object>> contributors) throws IOException{
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        for (Map<String, Object> contributor : contributors) {
            String contributorName = (String) contributor.get("login");
            int contributions = (Integer) contributor.get("contributions");
            dataset.addValue(contributions, "Contributions", contributorName);
        }

        JFreeChart chart = ChartFactory.createBarChart(
                "Contributions of each developer",
                "Contributor",
                "Contributions",
                dataset,
                PlotOrientation.VERTICAL,
                false,
                true,
                false
        );
        return chart;
    }

    public int[] computeWeeklyCommits(List<List<Integer>> repoPunchCard){
        int[] weeklyCount = new int[7];
        int count = 0;
        int i=0,x=0;
        // TODO: Ask what is 23, 46, 69, ...???. remove magic numbers.
        // TODO: Make all those statements inside while loop common.
        // TODO: Remove all unnecessary comments. And add meaningful comments instead of sunday, monday.
        // TODO: Fix the indentation.
        //TODO: Complex method --> cyclomatic complexity is 9

        //Sunday
        while(i<=23)
        {
            List<Integer> tempList= repoPunchCard.get(i);
            count = count + tempList.get(2);
            i++;
        }
        weeklyCount[x]=count;
        x++;
        count=0;
        //System.out.println("Value of i is: " + i);
        //Monday
        while(i<=46)
        {
            List<Integer> tempList= repoPunchCard.get(i);
            count = count + tempList.get(2);
            i++;
        }
        weeklyCount[x]=count;
        x++;
        count=0;
        //System.out.println("Value of i is: " + i);
        //Tuesday
        while(i<=69)
        {
            List<Integer> tempList= repoPunchCard.get(i);
            count = count + tempList.get(2);
            i++;
        }
        weeklyCount[x]=count;
        x++;
        count=0;
        //Wednesday
        while(i<=92)
        {
            List<Integer> tempList= repoPunchCard.get(i);
            count = count + tempList.get(2);
            i++;
        }
        weeklyCount[x]=count;
        x++;
        count=0;
        //Thursday
        while(i<=115)
        {
            List<Integer> tempList= repoPunchCard.get(i);
            count = count + tempList.get(2);
            i++;
        }
        weeklyCount[x]=count;
        x++;
        count=0;
        //Friday
        while(i<=138)
        {
            List<Integer> tempList= repoPunchCard.get(i);
            count = count + tempList.get(2);
            i++;
        }
        weeklyCount[x]=count;
        x++;
        count=0;
        //Saturday
        while(i<=161)
        {
            List<Integer> tempList= repoPunchCard.get(i);
            count = count + tempList.get(2);
            i++;
        }
        weeklyCount[x]=count;
        for(int m=0; m<weeklyCount.length;m++)
            System.out.println("No. of Commits on day "+m+": " + weeklyCount[m]);

        return weeklyCount;
    }

    public byte[] downloadRepositoryCode(String owner, String repoName) {
        String downloadUrl = githubApiUrl + "/repos/" + owner + "/" + repoName + "/zipball";
        HttpHeaders headers = new HttpHeaders();
        //headers.setBearerAuth(apiToken);
        HttpEntity<String> requestEntity = new HttpEntity<>(headers);

        ResponseEntity<byte[]> responseEntity = restTemplate.exchange(
                downloadUrl,
                HttpMethod.GET,
                requestEntity,
                byte[].class);

        if (responseEntity.getStatusCode() == HttpStatus.OK) {
            return responseEntity.getBody();
        } else {
            // TODO: unnecessary else statement
            throw new RuntimeException("Failed to download repository code. Status code: " + responseEntity.getStatusCodeValue());
        }
    }

    public String getHelloWorld(){
        // TODO: Ask purpose of this method and why is URL hardcoded??
        String uri = "http://localhost:8080/hellooo";
        RestTemplate restTemplate1 = new RestTemplate();
        return restTemplate1.getForObject(uri,String.class);
    }

    public String getRepoData() {
        String apiUrl = String.format("%s/repos/%s/%s", githubApiUrl, owner, repo);
        //String apiUrl = githubApiUrl + "/repos/" + owner + "/" + repoName;
        return restTemplate.getForObject(apiUrl, String.class);
    }

    public List<Map<String,Object>> getRepoContributors(){
        String apiUrl = String.format("%s/repos/%s/%s/contributors", githubApiUrl, owner, repo);
        return restTemplate.getForObject(apiUrl, List.class);
    }

    public ResponseEntity<String> getRepositoryContents(String owner, String repo, String path) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        //headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        //String apiUrl = String.format("%s/repos/%s/%s", githubApiUrl, owner, repo);
        URI uri = URI.create(githubApiUrl + "/repos/" + owner + "/" + repo + "/contents" + "/" + path);

        RequestEntity<?> requestEntity = RequestEntity.get(uri).headers(headers).build();
        return restTemplate.exchange(requestEntity, String.class);
    }

    public ResponseEntity<String> getRepositoryContents(String owner, String repo) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        //headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        //String apiUrl = String.format("%s/repos/%s/%s", githubApiUrl, owner, repo);
        URI uri = URI.create(githubApiUrl + "/repos/" + owner + "/" + repo + "/contents");

        RequestEntity<?> requestEntity = RequestEntity.get(uri).headers(headers).build();
        return restTemplate.exchange(requestEntity, String.class);
    }

    public ResponseEntity<String> getFileContent(String owner, String repo, String filePath) {
        HttpHeaders headers = createHeaders();

        URI uri = URI.create(githubApiUrl + "/repos/" + owner + "/" + repo + "/contents/" + filePath);

        RequestEntity<?> requestEntity = RequestEntity.get(uri).headers(headers).build();
        ResponseEntity<String> response = restTemplate.exchange(requestEntity, String.class);
        // TODO: Flip the condition and get an early return
        if (response.getStatusCodeValue() == 200) {
            // The response contains base64-encoded content, so decode it
            String content = response.getBody();
            System.out.println("RESPONSE 200");
            System.out.println(content);
            //content = content.substring(content.indexOf("content")+9,content.indexOf("encoding")-3);
            content = JSONUtils.parseJSONResponseAsTree(content);
            // TODO: As the reason for double backslashes.
            content = content.replaceAll("\\s","");
            System.out.println(content);
            String decodedContent = new String(Base64.getDecoder().decode(content), StandardCharsets.UTF_8);
            // TODO: "At least"?
            System.out.println("WE REACHED HERE ATLEAST!!!" + decodedContent);
            response = ResponseEntity.ok(decodedContent);
        }

        return response;
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        //headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        return headers;
    }

}
