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

    /**
     * Controller method for viewing repository details by repository ID.
     *
     * @param repoId The unique identifier for the repository.
     * @param model  The Spring MVC model for passing data to the view.
     * @return The view name for displaying repository and user data.
     * @author Zeel Ravalani
     */
    @GetMapping("/{repo_id}")
    public String view(@PathVariable("repo_id") Integer repoId, Model model) {
        // Retrieving repository data by repository ID
        RepoData repoData = repoDataService.getOne(repoId);
        logger.debug("repoData: " + repoData);

        // Retrieving user data associated with the repository
        UserData userData = userDataService.getOne(repoData.getUserId());

        // Adding user and repository data to the model
        model.addAttribute("userData", userData);
        model.addAttribute("repoData", repoData);

        // Returning the view name
        return "user/repo";
    }

    /**
     * Controller method for getting programming languages used in a repository by repository ID.
     *
     * @author Zeel Ravalani
     * @param repoID The unique identifier for the repository.
     * @return ResponseEntity with a map of programming languages and their usage counts.
     */
    @RequestMapping(value = "/get-user-repos-lang/{repo_id}", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public ResponseEntity<Map<String, Integer>> getUserReposLangs(@PathVariable("repo_id") Integer repoID) {
        // Retrieving repository data by repository ID
        RepoData repoData = repoDataService.getOne(repoID);
        logger.debug("repoData: " + repoData);

        // Retrieving and returning programming languages and their usage counts
        return ResponseEntity.ok(repoDataService.getRepositoryLanguages(repoData));
    }

    /**
     * Controller method for getting contributors of a repository by repository ID.
     *
     * @author Zeel Ravalani
     * @param repoID The unique identifier for the repository.
     * @return ResponseEntity with a map of contributors and their commit counts.
     */
    @RequestMapping(value = "/get-user-repos-contibutors/{repo_id}", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public ResponseEntity<Map<String, Integer>> getUserReposContributors(@PathVariable("repo_id") Integer repoID) {
        // Retrieving repository data by repository ID
        RepoData repoData = repoDataService.getOne(repoID);
        logger.debug("repoData: " + repoData);

        // Retrieving and returning repository contributors and their commit counts
        return ResponseEntity.ok(repoDataService.getRepoContributors(repoData));
    }

    @RequestMapping(value = "/get-user-repos-punchcard/{repo_id}", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public ResponseEntity<int []> getUserReposPunchCard(@PathVariable("repo_id") Integer repoID) throws IOException {
        RepoData repoData = repoDataService.getOne(repoID);
        logger.debug("repoData: " + repoData.getName());
        return ResponseEntity.ok(llmService.getRepositoryPunchCardtest(repoData.getName()));
    }

    /**
     * Controller method for getting lines of code (LOC) for a repository by repository ID.
     *
     * @author Zeel Ravalani
     * @param repoID The unique identifier for the repository.
     * @return ResponseEntity with a string representation of the lines of code for the repository.
     */
    @RequestMapping(value = "/get-user-repo-loc/{repo_id}", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public ResponseEntity<String> getUserReposFor(@PathVariable("repo_id") Integer repoID) {
        // Retrieving repository data by repository ID
        RepoData repoData = repoDataService.getOne(repoID);
        logger.debug("repoData: " + repoData);

        // Retrieving and returning lines of code (LOC) for the repository
        return ResponseEntity.ok(repoDataService.getRepoLOC(repoData));
    }


    @RequestMapping(value = "/get-repo-prs/{repo_id}", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public ResponseEntity<Integer> getRepoPRsFor(@PathVariable("repo_id") Integer repoID) {
        RepoData repoData = repoDataService.getOne(repoID);
        logger.debug("repoData: " + repoData);
        return ResponseEntity.ok(repoDataService.getRepositoryPRs(repoData));
    }

    @RequestMapping(value = "/get-repo-forks-count/{repo_id}", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public ResponseEntity<Integer> getRepoForksCountFor(@PathVariable("repo_id") Integer repoID) {
        RepoData repoData = repoDataService.getOne(repoID);
        logger.debug("repoData: " + repoData);
        return ResponseEntity.ok(repoDataService.getRepositoryForksCount(repoData));
    }

}