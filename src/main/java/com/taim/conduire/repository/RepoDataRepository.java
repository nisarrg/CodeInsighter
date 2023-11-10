package com.taim.conduire.repository;

import com.taim.conduire.domain.RepoData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface RepoDataRepository extends JpaRepository<RepoData, Integer> {

    List<RepoData> findByUserId(Integer userId);
    RepoData findByGithubRepoId(Integer githubRepoId);

}
