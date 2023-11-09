package com.taim.conduire.service.impl;

import com.taim.conduire.domain.UserData;
import com.taim.conduire.repository.UserDataRepository;
import com.taim.conduire.service.UserDataService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserDataServiceImpl implements UserDataService {
	private static final Logger logger = LogManager.getLogger();

    @Autowired
    private UserDataRepository repository;
    
    public JpaRepository<UserData, Integer> getRepository() {
        return repository;
    }

    @Override
    public UserData findByGithubUserId(Integer githubUserId) {
        return repository.findByGithubUserId(githubUserId);
    }
}
