package com.taim.conduire.repository;

import com.taim.conduire.domain.RepoData;
import com.taim.conduire.domain.UserData;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RepoDataRepository extends JpaRepository<RepoData, Integer> {

}
