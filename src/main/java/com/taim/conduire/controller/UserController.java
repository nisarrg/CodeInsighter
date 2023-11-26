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

@Controller
@RequestMapping("/users")
@Validated
public class UserController {
    private static final Logger logger = LogManager.getLogger();

    @Autowired
    private UserDataService userDataService;

    @Autowired
    private RepoDataService repoDataService;

    /**
     * Controller method for viewing user data by user ID.
     *
     * @param userId The unique identifier for the user.
     * @param model  The Spring MVC model for passing data to the view.
     * @return The view name for displaying user data.
     * @author Zeel Ravalani
     */
    @GetMapping("/{user_id}")
    public String view(@PathVariable("user_id") Integer userId, Model model) {
        // Retrieving user data by user ID
        UserData userData = userDataService.getOne(userId);
        logger.debug("userData: " + userData);

        // Dumping repository data for the user
        String repoDump = repoDataService.dumpRepoData(userData);
        logger.debug(repoDump);

        // Adding user data to the model
        model.addAttribute("userData", userData);

        // Returning the view name
        return "user/index";
    }

    /**
     * Controller method for getting user repositories by user ID.
     *
     * @author Zeel Ravalani
     * @param userId The unique identifier for the user.
     * @return ResponseEntity with a list of RepoData or an empty list if none found.
     */
    @RequestMapping(value = "/get-user-repos/{user-id}", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public ResponseEntity<List<RepoData>> getUserReposFor(@PathVariable("user-id") Integer userId) {
        logger.debug("userId: " + userId);

        // Retrieving repositories for the user by user ID
        List<RepoData> repoList = repoDataService.findByUserId(userId);

        // Returning ResponseEntity with the list of repositories
        return ResponseEntity.ok(repoList);
    }
}