package com.taim.conduire.repository;

import com.taim.conduire.domain.UserData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

public interface UserDataRepository extends JpaRepository<UserData, Integer> {

    UserData findByGithubUserId(Integer githubUserId);
}
