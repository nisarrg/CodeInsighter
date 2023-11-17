package com.taim.conduire.service.impl;

import com.taim.conduire.domain.UserData;
import com.taim.conduire.repository.UserDataRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.*;

import static org.junit.jupiter.api.Assertions.*;
@ExtendWith(MockitoExtension.class)
public class UserDataServiceImplTest {

    @Mock
    private UserDataRepository repository;
    @InjectMocks
    private UserDataServiceImpl userDataService;

    @Test
    void getRepository() {
        JpaRepository<UserData, Integer> repo = userDataService.getRepository();
        assertNotNull(repo);
    }

    @Test
    void testFindByGithubUserId() {
        Integer githubUserId = 123;
        UserData expectedUserData = new UserData();
        when(repository.findByGithubUserId(githubUserId)).thenReturn(expectedUserData);

        UserData actualUserData = userDataService.findByGithubUserId(githubUserId);

        assertEquals(expectedUserData, actualUserData);
    }
}