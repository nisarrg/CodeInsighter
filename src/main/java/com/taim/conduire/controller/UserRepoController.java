package com.taim.conduire.controller;

import com.taim.conduire.domain.RepoData;
import com.taim.conduire.domain.UserData;
import com.taim.conduire.service.RepoDataService;
import com.taim.conduire.service.UserDataService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;


@Controller
@RequestMapping("/users/repo")
@Validated
public class UserRepoController {
    private static final Logger logger = LogManager.getLogger();

    @Autowired
    private UserDataService userDataService;

    @Autowired
    private RepoDataService repoDataService;

    @GetMapping("/{repo_id}")
    public String view(@PathVariable("repo_id") Integer repoId, Model model) {
        RepoData repoData = repoDataService.getOne(repoId);
        System.out.println("repoData: " + repoData);
//        String repoLOCDump = repoLOCDataService.dumpRepoLOCData(repoData);
//        System.out.println("repoLOCDump: " + repoLOCDump);
        UserData userData = userDataService.getOne(repoData.getUserId());
        model.addAttribute("userData", userData);
        model.addAttribute("repoData", repoData);
        return "user/repo";
    }


    @RequestMapping(value = "/get-user-repos-lang/{repo_id}", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public ResponseEntity<Map<String, Integer>> getUserReposLangs(@PathVariable("repo_id") Integer repoID) {
        RepoData repoData = repoDataService.getOne(repoID);
        System.out.println("repoData: " + repoData);
        repoDataService.getRepositoryLanguages(repoData);
        System.out.println("repoID: " + repoID );

        return ResponseEntity.ok(repoDataService.getRepositoryLanguages(repoData));
    }

    @RequestMapping(value = "/get-user-repos-contibutors/{repo_id}", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public ResponseEntity<Map<String, Integer>> getUserReposContributors(@PathVariable("repo_id") Integer repoID) {
        RepoData repoData = repoDataService.getOne(repoID);
        System.out.println("repoData: " + repoData);
        repoDataService.getRepoContributors(repoData);
        System.out.println("Data Contri: " + repoDataService.getRepoContributors(repoData) );

        return ResponseEntity.ok(repoDataService.getRepoContributors(repoData));
    }




}