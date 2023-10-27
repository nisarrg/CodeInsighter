package com.taim.conduire.controller;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.*;

import com.taim.conduire.domain.RepoData;
import com.taim.conduire.domain.UserData;
import com.taim.conduire.repository.RepoDataRepository;
import com.taim.conduire.service.RepoDataService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.taim.conduire.service.UserDataService;

@RestController
@RequestMapping("/")
@Validated
public class LoginController {
    private static final Logger logger = LogManager.getLogger();

    @Autowired
    private UserDataService userDataService;


    @RequestMapping(value = "/user", method = RequestMethod.GET)
    public String user(@AuthenticationPrincipal OAuth2User principal) {
        System.out.println("Auth Called: " + principal);
        UserData userData = null;

        if(userDataService.findByGithubUserId(principal.getAttribute("id")) != null) {
            userData = userDataService.findByGithubUserId(principal.getAttribute("id"));
            userData.setLastVisitedOn(new Date());
        }else {
            userData = new UserData();
            userData.setCreatedAt(new Date());
            userData.setGithubUserId(principal.getAttribute("id"));
            userData.setName(principal.getAttribute("name"));
            userData.setUserName(principal.getAttribute("login"));
            userData.setEmail(principal.getAttribute("email"));
            userData.setAccountType(principal.getAttribute("type"));
            userData.setAvatarUrl(principal.getAttribute("avatar_url"));
            userData.setCompany(principal.getAttribute("company"));
            userData.setLocation(principal.getAttribute("location"));
            userData.setBio(principal.getAttribute("bio"));
            userData.setTwitterUsername(principal.getAttribute("twitter_username"));
            userData.setPublicRepos(principal.getAttribute("public_repos"));
            userData.setFollowers(principal.getAttribute("followers"));
            userData.setPrivateOwnedRepos(principal.getAttribute("owned_private_repos"));
            userData.setCollaborators(principal.getAttribute("collaborators"));
            userData.setVisible('Y');
            userData.setLastVisitedOn(new Date());
        }
        userData = userDataService.update(userData);

        String message = "";
        if(userData != null) {
            message = "Data added in the object";
        }else {
            message = "Data not added in the object";
        }
        logger.debug(message);
        return "" + userData.getId();
    }

//    @RequestMapping(value = "/user", method = RequestMethod.GET)
//    public Map<String, Object> user(@AuthenticationPrincipal OAuth2User principal) {
//
//        UserData userData = null;
//
//
//        if(userDataService.findByGithubUserId(principal.getAttribute("id")) != null) {
//            userData = userDataService.findByGithubUserId(principal.getAttribute("id"));
//            userData.setLastVisitedOn(new Date());
//        }else {
//            userData = new UserData();
//            userData.setCreatedAt(new Date());
//            userData.setGithubUserId(principal.getAttribute("id"));
//            userData.setName(principal.getAttribute("name"));
//            userData.setUserName(principal.getAttribute("login"));
//            userData.setEmail(principal.getAttribute("email"));
//            userData.setAccountType(principal.getAttribute("type"));
//            userData.setAvatarUrl(principal.getAttribute("avatar_url"));
//            userData.setCompany(principal.getAttribute("company"));
//            userData.setLocation(principal.getAttribute("location"));
//            userData.setBio(principal.getAttribute("bio"));
//            userData.setTwitterUsername(principal.getAttribute("twitter_username"));
//            userData.setPublicRepos(principal.getAttribute("public_repos"));
//            userData.setFollowers(principal.getAttribute("followers"));
//            userData.setPrivateOwnedRepos(principal.getAttribute("owned_private_repos"));
//            userData.setCollaborators(principal.getAttribute("collaborators"));
//            userData.setVisible('Y');
//            userData.setLastVisitedOn(new Date());
//        }
//        userData = userDataService.update(userData);
//
//        String message = "";
//        if(userData != null) {
//            message = "Data added in the object";
//        }else {
//            message = "Data not added in the object";
//        }
//        logger.debug(message);
//
//
//        return response;
//    }


}