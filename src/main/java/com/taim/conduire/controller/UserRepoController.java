package com.taim.conduire.controller;

import com.taim.conduire.domain.RepoData;
import com.taim.conduire.domain.UserData;
import com.taim.conduire.service.LLMService;
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

import java.io.IOException;
import java.util.Map;

// TODO --> Designite detected this class not being used --> unutilized abstraction.
@Controller
@RequestMapping("/users/repo")
@Validated
public class UserRepoController {
    private static final Logger logger = LogManager.getLogger();

    @Autowired
    private UserDataService userDataService;

    @Autowired
    private RepoDataService repoDataService;

    @Autowired
    private LLMService llmService;

    @GetMapping("/{repo_id}")
    public String view(@PathVariable("repo_id") Integer repoId, Model model) {
        RepoData repoData = repoDataService.getOne(repoId);
        System.out.println("repoData: " + repoData);
        UserData userData = userDataService.getOne(repoData.getUserId());
        model.addAttribute("userData", userData);
        model.addAttribute("repoData", repoData);
        return "user/repo";
    }

    @RequestMapping(value = "/get-user-repos-lang/{repo_id}", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public ResponseEntity<Map<String, Integer>> getUserReposLangs(@PathVariable("repo_id") Integer repoID) {
         RepoData repoData = repoDataService.getOne(repoID);
         System.out.println(" repoData: " + repoData);
         repoDataService.getRepositoryLanguages(repoData);
         System.out.println(" repoID: " + repoID );

         return ResponseEntity.ok(repoDataService.getRepositoryLanguages(repoData));
    }

    @RequestMapping(value = "/get-user-repos-contibutors/{repo_id}", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public ResponseEntity<Map<String, Integer>> getUserReposContributors(@PathVariable("repo_id") Integer repoID) {
        RepoData repoData = repoDataService.getOne(repoID);
        System.out.println("repoData: " + repoData);
        repoDataService.getRepoContributors(repoData);
        System.out.println("Data Contri: " + repoDataService.getRepoContributors(repoData));

        return ResponseEntity.ok(repoDataService.getRepoContributors(repoData));
    }

    @RequestMapping(value = "/get-user-repos-punchcard/{repo_id}", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public ResponseEntity<int []> getUserReposPunchCard(@PathVariable("repo_id") Integer repoID) throws IOException {
        RepoData repoData = repoDataService.getOne(repoID);
        System.out.println("repoData: " + repoData.getName());
        return ResponseEntity.ok(llmService.getRepositoryPunchCardtest(repoData.getName()));
    }

    @RequestMapping(value = "/get-user-repo-loc/{repo_id}", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public ResponseEntity<String> getUserReposFor(@PathVariable("repo_id") Integer repoID) {
        RepoData repoData = repoDataService.getOne(repoID);
        System.out.println("repoData: " + repoData);

        return ResponseEntity.ok(repoDataService.getRepoLOC(repoData));
    }

    @RequestMapping(value = "/get-repo-prs/{repo_id}", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public ResponseEntity<Integer> getRepoPRsFor(@PathVariable("repo_id") Integer repoID) {
        RepoData repoData = repoDataService.getOne(repoID);
        System.out.println("repoData: " + repoData);
        return ResponseEntity.ok(repoDataService.getRepositoryPRs(repoData));
    }

    @RequestMapping(value = "/get-repo-forks-count/{repo_id}", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public ResponseEntity<Integer> getRepoForksCountFor(@PathVariable("repo_id") Integer repoID) {
        RepoData repoData = repoDataService.getOne(repoID);
        System.out.println("repoData: " + repoData);
        return ResponseEntity.ok(repoDataService.getRepositoryForksCount(repoData));
    }

}