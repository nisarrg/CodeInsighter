package com.taim.conduire.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
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
        List<List<Integer>> codeFrequencyStats = objectMapper.readValue(apiResponse, objectMapper.getTypeFactory().constructCollectionType(List.class, List.class));
        return codeFrequencyStats;
    }

    public List<List<Integer>> getRepositoryPunchCard() throws IOException {
        String apiUrl = String.format("%s/repos/%s/%s/stats/punch_card", githubApiUrl, owner, repo);
        System.out.println(apiUrl);
        String apiResponse = restTemplate.getForObject(apiUrl, String.class);
        List<List<Integer>> repoPunchCard = objectMapper.readValue(apiResponse, objectMapper.getTypeFactory().constructCollectionType(List.class, List.class));
        computeWeeklyCommits(repoPunchCard);
        return repoPunchCard;
    }

    public int[] getRepositoryPunchCardtest(String name) throws IOException {
        String apiUrl = String.format("%s/repos/%s/stats/punch_card", githubApiUrl,name);
        System.out.println(apiUrl);
        String apiResponse = restTemplate.getForObject(apiUrl, String.class);
        List<List<Integer>> repoPunchCard = objectMapper.readValue(apiResponse, objectMapper.getTypeFactory().constructCollectionType(List.class, List.class));
        return computeWeeklyCommits(repoPunchCard);
    }

    @Getter
    public static class WeekCommitActivity {
        private String weekStartDate;
        private int commits;
        private int additions;
        private int deletions;

        public WeekCommitActivity(String weekStartDate, int commits, int additions, int deletions) {
            this.weekStartDate = weekStartDate;
            this.commits = commits;
            this.additions = additions;
            this.deletions = deletions;
        }

        public void setWeekStartDate(String weekStartDate) {
            this.weekStartDate = weekStartDate;
        }

        public void setCommits(int commits) {
            this.commits = commits;
        }

        public void setAdditions(int additions) {
            this.additions = additions;
        }

        public void setDeletions(int deletions) {
            this.deletions = deletions;
        }
    }

    public List<WeekCommitActivity> getAllContributorCommitActivity() throws IOException {
        String apiUrl = String.format("%s/repos/%s/%s/stats/contributors", githubApiUrl, owner, repo);
        System.out.println(apiUrl);
        ResponseEntity<String> responseEntity = restTemplate.getForEntity(apiUrl, String.class);
        String apiResponse = responseEntity.getBody();

        List<Map<String, Integer>> contributorData = objectMapper.readValue(apiResponse, new TypeReference<List<Map<String, Integer>>>() {});
        return getCommitActivityData(contributorData);
    }

    private List<WeekCommitActivity> getCommitActivityData(List<Map<String, Integer>> contributorData) {
        List<WeekCommitActivity> weeklyActivities = new ArrayList<>();

        for (Map<String, Integer> weekData : contributorData) {
            long weekStartTimestamp = weekData.get("w");
            int additions = weekData.get("a");
            int deletions = weekData.get("d");
            int commits = weekData.get("c");

            System.out.println("additions"+ additions);
            // Convert the Unix timestamp to a readable date
            String weekStartDate = convertUnixTimestampToDate(weekStartTimestamp);

            WeekCommitActivity weekActivity = new WeekCommitActivity(weekStartDate, commits, additions, deletions);
            weeklyActivities.add(weekActivity);
        }

        return weeklyActivities;
    }

    public List<Integer> getCommitCountsForNonOwners() {
        String apiUrl = String.format("%s/repos/%s/%s/stats/participation", githubApiUrl, owner, repo);
        System.out.println(apiUrl);
        ResponseEntity<GitHubParticipation> responseEntity = restTemplate.getForEntity(apiUrl, GitHubParticipation.class);

        GitHubParticipation participation = responseEntity.getBody();
        return calculateNonOwnerCommitCounts(participation);
    }

    private List<Integer> calculateNonOwnerCommitCounts(GitHubParticipation participation) {
        List<Integer> all = participation.getAll();
        List<Integer> owner = participation.getOwner();

        List<Integer> nonOwnerCommitCounts = new ArrayList<>();

        for (int i = 0; i < all.size(); i++) {
            int nonOwnerCommits = all.get(i) - owner.get(i);
            nonOwnerCommitCounts.add(nonOwnerCommits);
        }
        System.out.println("non owner commits:"+nonOwnerCommitCounts);
        return nonOwnerCommitCounts;
    }


@Getter
    public static class GitHubParticipation {
        private List<Integer> all;
        private List<Integer> owner;

        public void setAll(List<Integer> all) {
            this.all = all;
        }

        public void setOwner(List<Integer> owner) {
            this.owner = owner;
        }

    public List<Integer> getAll() {
        return all;
    }

    public List<Integer> getOwner() {
        return owner;
    }
    }

    public List<Integer> getCommitCountsForOwner(String name) {
        String apiUrl = String.format("%s/repos/%s/stats/participation", githubApiUrl, name);
        System.out.println(apiUrl);

        //String apiResponse = restTemplate.getForObject(apiUrl, String.class);
        ResponseEntity<GitHubParticipation> responseEntity = restTemplate.getForEntity(apiUrl, GitHubParticipation.class);

        GitHubParticipation participation = responseEntity.getBody();
        return participation.getOwner();
    }

    public List<WeekCommitActivity> getLastYearCommitActivity() {
        String apiUrl = String.format("%s/repos/%s/%s/stats/commit_activity", githubApiUrl, owner, repo);
        System.out.println(apiUrl);
        ResponseEntity<WeekCommitActivity[]> responseEntity = restTemplate.getForEntity(apiUrl, WeekCommitActivity[].class);

        if (responseEntity.getStatusCode().is2xxSuccessful()) {
            return Arrays.asList(Objects.requireNonNull(responseEntity.getBody()));
        } else {
            // Handle the case where the request was not successful.
            return Collections.emptyList(); // or handle the error as needed
        }
    }

    public List<WeekCommitActivity> getComputeContributorActivity(List<Map<String, Integer>> contributorData) {
        List<WeekCommitActivity> weeklyActivities = new ArrayList<>();

        for (Map<String, Integer> weekData : contributorData) {
            long weekStartTimestamp = weekData.get("w");
            int additions = weekData.get("a");
            int deletions = weekData.get("d");
            int commits = weekData.get("c");

            // Convert the Unix timestamp to a readable date
            String weekStartDate = convertUnixTimestampToDate(weekStartTimestamp);

            WeekCommitActivity weekActivity = new WeekCommitActivity(weekStartDate, commits, additions, deletions);
            weeklyActivities.add(weekActivity);
        }

        return weeklyActivities;
    }

    private static String convertUnixTimestampToDate(long timestamp) {
        // Adjust the timestamp to the start of the week (assuming Sunday as the first day)
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timestamp * 1000L); // Convert to milliseconds
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK); // Get the day of the week
        if (dayOfWeek != Calendar.SUNDAY) {
            calendar.add(Calendar.DAY_OF_WEEK, -dayOfWeek + Calendar.SUNDAY); // Adjust to the previous Sunday
        }

        Date startDate = calendar.getTime();

        // Format the date in a readable format
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        return sdf.format(startDate);
    }

    public byte[] generatePieChart(Map<String,Integer> data) throws IOException{
        DefaultPieDataset dataset = new DefaultPieDataset();
        for (Map.Entry<String, Integer> entry : data.entrySet()) {
            dataset.setValue(entry.getKey(), entry.getValue());
        }

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
            throw new RuntimeException("Failed to download repository code. Status code: " + responseEntity.getStatusCodeValue());
        }
    }

    public String getHelloWorld(){
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

        if (response.getStatusCodeValue() == 200) {
            // The response contains base64-encoded content, so decode it
            String content = response.getBody();
            System.out.println("RESPONSE 200");
            System.out.println(content);
            //content = content.substring(content.indexOf("content")+9,content.indexOf("encoding")-3);
            content = JSONUtils.parseJSONResponseAsTree(content);
            content = content.replaceAll("\\s","");
            System.out.println(content);
            String decodedContent = new String(Base64.getDecoder().decode(content), StandardCharsets.UTF_8);
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
