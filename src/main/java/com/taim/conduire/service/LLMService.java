package com.taim.conduire.service;

import org.jfree.chart.JFreeChart;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface LLMService {

    Map<String, Integer> getRepositoryLanguages();

    List<List<Integer>> getRepositoryCodeFrequency() throws IOException;

    List<List<Integer>> getRepositoryPunchCard() throws IOException;

    int[] getRepositoryPunchCardtest(String name) throws IOException;

    byte[] generatePieChart(Map<String, Integer> data) throws IOException;

    JFreeChart generateBarChart(List<Map<String, Object>> contributors) throws IOException;

    int[] computeWeeklyCommits(List<List<Integer>> repoPunchCard);

    byte[] downloadRepositoryCode(String owner, String repoName);

    String getHelloWorld();

    String getRepoData();

    List<Map<String, Object>> getRepoContributors();

    ResponseEntity<String> getRepositoryContents(String owner, String repo, String path);

    ResponseEntity<String> getRepositoryContents(String owner, String repo);

    ResponseEntity<String> getFileContent(String owner, String repo, String filePath);

}
