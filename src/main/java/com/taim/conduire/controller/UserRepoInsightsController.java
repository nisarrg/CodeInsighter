package com.taim.conduire.controller;

import com.taim.conduire.domain.FormData;
import com.taim.conduire.domain.RepoData;
import com.taim.conduire.domain.UserData;
import com.taim.conduire.service.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

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
    private InsightsService insightsService;

    @GetMapping("/{repo_id}/insights")
    public String view(@PathVariable("repo_id") Integer repoId, Model model) throws IOException {
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
    public ResponseEntity<String> getCommonCodeMistakesInsights(@PathVariable("repo_id") Integer repoID) throws IOException {
        System.out.println("repoID: " + repoID + "insightType CCM ");
        RepoData repoData = repoDataService.getOne(repoID);
        String commonCodeMistakesInsight = insightsService.getCommonCodeMistakesInsights(repoData);
        return ResponseEntity.ok(commonCodeMistakesInsight);
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

    @RequestMapping(value = "/{repo_id}/get-insights/dvc", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<String> getDVCInsights(@PathVariable("repo_id") Integer repoID) {
        try {
            System.out.println("repoID: " + repoID + " insightType DVC");
            RepoData repoData = repoDataService.getOne(repoID);
            String versions = insightsService.processDependencyFile(repoData);

            if (versions == null) {
                String prompt = "There was no version control file at the root level of the repository.";
                //String finalResponse = chatGPTService.chat(prompt);
                return ResponseEntity.ok(prompt);
            } else {
                String finalResponse = chatGPTService.chat(versions);
                return ResponseEntity.ok(finalResponse);
            }
        } catch (IOException e) {
            // Handle IOException, log, or return an appropriate error response
            e.printStackTrace(); // Log the exception or use a logging framework
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal Server Error");
        }
    }


    @RequestMapping(value = "/{repo_id}/get-repo-prs-collab", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<String> getRepoPRsForCollab(@PathVariable("repo_id") Integer repoID) throws IOException, InterruptedException {
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

    @RequestMapping(value = "/{repo_id}/get-insights/ccl", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public ResponseEntity<String> getCustomCodeLintingInsightInsights(@PathVariable("repo_id") Integer repoID) throws IOException {
        System.out.println("repoID: " + repoID + " insightType CCL");
        RepoData repoData = repoDataService.getOne(repoID);
        String customCodeLintingInsightString = insightsService.getCustomCodeLintingInsights(repoData);
        System.out.println("customCodeLintingInsightString: " + customCodeLintingInsightString);
        return ResponseEntity.ok(customCodeLintingInsightString);
    }
}
