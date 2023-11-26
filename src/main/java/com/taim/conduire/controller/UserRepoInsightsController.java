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

    /**
     * Handles the GET request to retrieve Dependency Version Control (DVC) insights for a repository.
     *
     * @param repoID The ID of the repository.
     * @return ResponseEntity with insights on DVC or an error response.
     */
    @RequestMapping(value = "/{repo_id}/get-insights/dvc", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<String> getDVCInsights(@PathVariable("repo_id") Integer repoID) {
        try {
            // Log the start of processing the DVC insights for the specified repository.
            System.out.println("Processing DVC insights for repoID: " + repoID);

            // Retrieve repository data based on the provided repository ID.
            RepoData repoData = repoDataService.getOne(repoID);

            // Process the dependency file to get versions.
            String versions = insightsService.processDependencyFile(repoData);

            if (versions == null) {
                // Log that there was no version control file at the root level of the repository.
                System.out.println("No version control file found at the root level of the repository.");

                // Provide a prompt indicating the absence of a version control file.
                String prompt = "This isn't a Java Maven repository. Kindly retry with one!";

                // Return a response entity with the prompt.
                return ResponseEntity.ok(prompt);
            } else {
                // Log the start of processing chatGPT with the retrieved versions.
                System.out.println("Processing chatGPT with versions: " + versions);

                // Use chatGPT to generate insights based on the retrieved versions.
                String finalResponse = chatGPTService.chat(versions);

                // Log the generated final response.
                System.out.println("Final response: " + finalResponse);

                // Return a response entity with the final generated response.
                return ResponseEntity.ok(finalResponse);
            }
        } catch (IOException e) {
            // Handle IOException, log, or return an appropriate error response.
            e.printStackTrace(); // Log the exception or use a logging framework

            // Log an internal server error message.
            System.err.println("Internal Server Error");

            // Return an internal server error response.
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
