package com.taim.conduire.service.impl;

import com.taim.conduire.domain.RepoData;
import com.taim.conduire.domain.UserData;
import com.taim.conduire.repository.RepoDataRepository;
import com.taim.conduire.repository.UserDataRepository;
import com.taim.conduire.service.RepoDataService;
import com.taim.conduire.service.UserDataService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import com.taim.conduire.constants.ConstantCodes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

@Service
public class RepoDataServiceImpl implements RepoDataService {
	private static final Logger logger = LogManager.getLogger();

    @Autowired
    private RepoDataRepository repository;

    @Autowired
    private UserDataService userDataService;
    
    public JpaRepository<RepoData, Integer> getRepository() {
        return repository;
    }


    @Override
    public RepoData fetchUserRepos(Integer userGithubId) {
//        String apiUrl = String.format("%s/repos/%s/%s/stats/code_frequency", githubApiUrl, owner, repo);
//        UserData userData = userDataService.findByGithubUserId(userGithubId);
//
//        String userRepoApiUrl = ConstantCodes.GITHUB_API_URL + ConstantCodes.GITHUB_USERS + "/" + userData.getUserName() + ConstantCodes.GITHUB_REPOS;
//        System.out.println(userRepoApiUrl);
//        Map<String, Object> mapOfRepo;
//        ArrayList<mapOfRepo> listMapOfRepos = restTemplate.getForObject(apiUrl, String.class);
//        List<List<Integer>> codeFrequencyStats = objectMapper.readValue(apiResponse, objectMapper.getTypeFactory().constructCollectionType(List.class, List.class));
        return null;
    }
}
