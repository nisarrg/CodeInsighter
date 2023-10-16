package com.taim.conduire.service;

import com.taim.conduire.domain.UserData;
import com.taim.conduire.service.common.AbstractService;

import java.util.List;

public interface UserDataService extends AbstractService<UserData, Integer> {

    UserData findByGithubUserId(Integer githubUserId);
}
