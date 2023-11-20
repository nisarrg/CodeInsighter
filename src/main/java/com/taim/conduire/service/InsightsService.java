package com.taim.conduire.service;

import com.taim.conduire.domain.RepoData;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Map.Entry.comparingByValue;
import java.util.List;
import java.util.Map;

@Service
public interface InsightsService {

    HttpEntity<String> getAllHeadersEntity(String userAccessToken);

    void showAvailableAPIHits(HttpHeaders responseHeaders);

    Map<String, List<String>> getRepositoryReviewComments(RepoData repoData);

    String getCodeQualityEnhancementsInsights(RepoData repoData);

    String getRepositoryPRsCollab(RepoData repoData) throws IOException, InterruptedException;
}
