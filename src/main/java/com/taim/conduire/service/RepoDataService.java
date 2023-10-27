package com.taim.conduire.service;

import com.taim.conduire.domain.RepoData;
import com.taim.conduire.domain.UserData;
import com.taim.conduire.service.common.AbstractService;

import java.util.List;

public interface RepoDataService extends AbstractService<RepoData, Integer> {

    String getRepoData(UserData userData);

    String dumpRepoData(UserData userData);

    RepoData findByGithubRepoId(Integer githubRepoId);

    List<RepoData> findByUserId(Integer userId);

}
