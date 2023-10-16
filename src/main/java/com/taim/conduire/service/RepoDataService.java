package com.taim.conduire.service;

import com.taim.conduire.domain.RepoData;
import com.taim.conduire.domain.UserData;
import com.taim.conduire.service.common.AbstractService;

public interface RepoDataService extends AbstractService<RepoData, Integer> {

    RepoData fetchUserRepos(Integer userGithubId);

}
