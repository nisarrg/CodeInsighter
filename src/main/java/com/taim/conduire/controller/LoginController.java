package com.taim.conduire.controller;

import com.taim.conduire.domain.UserData;
import com.taim.conduire.service.UserDataService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;


// TODO --> Designite detected this class not being used --> unutilized abstraction.
@RestController
@RequestMapping("/")
@Validated
public class LoginController {
    private static final Logger logger = LogManager.getLogger();

    @Autowired
    private UserDataService userDataService;


    @RequestMapping(value = "/user", method = RequestMethod.GET)
    public String user(@AuthenticationPrincipal OAuth2User principal, @RegisteredOAuth2AuthorizedClient OAuth2AuthorizedClient authorizedClient) {
        String accessToken = authorizedClient.getAccessToken().getTokenValue();
        System.out.println("User Access Token: " + accessToken);
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
        userData.setUserAccessToken(accessToken);
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
}