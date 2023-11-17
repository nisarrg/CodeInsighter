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


// TODO --> Designite detected this class not being used --> unutilized abstraction.
@Controller
@RequestMapping("/users")
@Validated
public class UserController {
    private static final Logger logger = LogManager.getLogger();

    @Autowired
    private UserDataService userDataService;

    @Autowired
    private RepoDataService repoDataService;

    @GetMapping("/{user_id}")
    public String view(@PathVariable("user_id") Integer userId, Model model) {
        UserData userData = userDataService.getOne(userId);
        System.out.println("userData: " + userData);
        String repoDump = repoDataService.dumpRepoData(userData);
        System.out.println(repoDump);
        model.addAttribute("userData", userData);
        return "user/index";
    }


    @RequestMapping(value = "/get-user-repos/{user-id}", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public ResponseEntity<List<RepoData>> getUserReposFor(@PathVariable("user-id") Integer userId) {
        System.out.println("userId: " + userId );
        List<RepoData> repoList = repoDataService.findByUserId(userId);

        return ResponseEntity.ok(repoList);
    }

}