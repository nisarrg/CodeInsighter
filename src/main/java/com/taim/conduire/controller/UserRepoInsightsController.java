package com.taim.conduire.controller;

import com.google.gson.Gson;
import com.taim.conduire.domain.FormData;
import com.taim.conduire.domain.RepoData;
import com.taim.conduire.domain.UserData;
import com.taim.conduire.service.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// TODO --> Designite detected this class not being used --> unutilized abstraction.
@Controller
@RequestMapping("/users/repo")
@Validated
public class UserRepoInsightsController {
    private static final Logger logger = LogManager.getLogger();

    @Autowired
    private ChatGPTService chatGPTService;

    @Autowired
    private UserDataService userDataService;

    @Autowired
    private RepoDataService repoDataService;

    @Autowired
    private LLMService llmService;

    @Autowired
    private InsightsService insightsService;

    @GetMapping("/{repo_id}/insights")
    public String view(@PathVariable("repo_id") Integer repoId, Model model) {
        RepoData repoData = repoDataService.getOne(repoId);
        System.out.println("repoData: " + repoData);
        UserData userData = userDataService.getOne(repoData.getUserId());
        FormData formData = new FormData();
        model.addAttribute("userData", userData);
        model.addAttribute("repoData", repoData);
        model.addAttribute("formData", formData);
        return "user/insights";
    }

    @RequestMapping(value = "/{repo_id}/get-insights/ccm", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public ResponseEntity<String> getCommonCodeMistakesInsights(@PathVariable("repo_id") Integer repoID)
            throws IOException {

        System.out.println("repoID: " + repoID + "insightType CCM ");
        RepoData repoData = repoDataService.getOne(repoID);
        Map<String, List<String>> reviewerComments = insightsService.getRepositoryReviewComments(repoData);
        String commonCodeMistakesPrompt = "These are open PR review comments by the reviewer:"
                + reviewerComments.toString() + "\n." +
                "Can you give me some insights of Common code mistakes based upon these comments.\n" +
                "Please consider yourself as a Business Analyst and write in Technical English.\n" +
                "And please frame it as if you are writing this response in <p></p> tag of html so to make sure its properly formatted " +
                "using html and shown to user. Make sure you break it into most important points and limit it to only 5 points " +
                "and highlight your reasoning. And Format the response in HTML tags and use Bootstrap classes for better readability";
        String commonCodeMistakesInsight = chatGPTService.chat(commonCodeMistakesPrompt);
        Gson gson = new Gson();
        String jsonInsight = gson.toJson(commonCodeMistakesInsight);
        String finalResult = "{\"insights\":" + jsonInsight + "}";
        System.out.println("finalResult: " + finalResult);
        return ResponseEntity.ok(finalResult);
    }
    @RequestMapping(value = "/{repo_id}/get-insights/cqe", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public ResponseEntity<String> getCodeQualityEnhancementsInsights(@PathVariable("repo_id") Integer repoID) throws IOException {

        System.out.println("repoID: " + repoID + " insightType CQE");
        RepoData repoData = repoDataService.getOne(repoID);
        String codeQualityEnhancementInsightString = insightsService.getCodeQualityEnhancementsInsights(repoData);
        System.out.println("codeQualityEnhancementInsightString: " + codeQualityEnhancementInsightString);
        return ResponseEntity.ok(codeQualityEnhancementInsightString);
    }

    @RequestMapping(value = "/{repo_id}/get-repo-prs-collab", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<String> getRepoPRsForCollab(@PathVariable("repo_id") Integer repoID)
            throws IOException, InterruptedException {
        RepoData repoData = repoDataService.getOne(repoID);
        System.out.println("repoData: " + repoData);
        return ResponseEntity.ok(insightsService.getRepositoryPRsCollab(repoData));
    }

    @RequestMapping(value = "/{repo_id}/get-insights/bdaf", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public ResponseEntity<String> getBugDetectionInApplicationFlowInsights(@PathVariable("repo_id") Integer repoID) throws IOException {

        System.out.println("repoID: " + repoID + " insightType BDAF");
        RepoData repoData = repoDataService.getOne(repoID);
        String bugDetectionInApplicationFlowInsightString = insightsService.getBugDetectionInApplicationFlowInsights(repoData);
        System.out.println("bugDetectionInApplicationFlowInsightString: " + bugDetectionInApplicationFlowInsightString);
        return ResponseEntity.ok(bugDetectionInApplicationFlowInsightString);
    }
}
