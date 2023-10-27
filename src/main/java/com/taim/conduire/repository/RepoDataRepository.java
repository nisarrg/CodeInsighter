package com.taim.conduire.repository;

import com.taim.conduire.domain.RepoData;
import com.taim.conduire.domain.UserData;
import org.springframework.data.jpa.datatables.mapping.DataTablesOutput;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

public interface RepoDataRepository extends JpaRepository<RepoData, Integer> {

    List<RepoData> findByUserId(Integer userId);
    RepoData findByGithubRepoId(Integer githubRepoId);

}
