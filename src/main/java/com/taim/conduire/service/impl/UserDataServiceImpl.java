package com.taim.conduire.service.impl;

import com.taim.conduire.domain.UserData;
import com.taim.conduire.repository.UserDataRepository;
import com.taim.conduire.service.UserDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

@Service
public class UserDataServiceImpl implements UserDataService {

    @Autowired
    private UserDataRepository repository;

    @Override
    public JpaRepository<UserData, Integer> getRepository() {
        return repository;
    }

    /**
     * Retrieves user data by GitHub user ID.
     *
     * @param githubUserId The GitHub user ID to search for.
     * @return UserData object representing the user data if found, or null if not found.
     * @author Zeel Ravalani
     */
    @Override
    public UserData findByGithubUserId(Integer githubUserId) {
        return repository.findByGithubUserId(githubUserId);
    }
}
