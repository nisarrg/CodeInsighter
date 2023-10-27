package com.taim.conduire.controller;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.*;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import com.taim.conduire.constants.ConstantCodes;
import com.taim.conduire.domain.RepoData;
import com.taim.conduire.domain.UserData;
import com.taim.conduire.repository.RepoDataRepository;
import com.taim.conduire.service.RepoDataService;
import com.taim.conduire.service.common.CommonHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.datatables.mapping.DataTablesInput;
import org.springframework.data.jpa.datatables.mapping.DataTablesOutput;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.taim.conduire.service.UserDataService;

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