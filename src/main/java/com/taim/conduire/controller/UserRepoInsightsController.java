package com.taim.conduire.controller;

import com.taim.conduire.domain.FormData;
import com.taim.conduire.domain.RepoData;
import com.taim.conduire.domain.UserData;
import com.taim.conduire.service.ChatGPTService;
import com.taim.conduire.service.InsightsService;
import com.taim.conduire.service.RepoDataService;
import com.taim.conduire.service.UserDataService;
import com.taim.conduire.service.impl.InsightsServiceImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

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

    @Autowired
    private InsightsServiceImpl insightsServiceImpl;

    /**
     * Controller method for viewing insights for a repository by repository ID.
     *
     * @param repoId The unique identifier for the repository.
     * @param model  The Spring MVC model for passing data to the view.
     * @return The view name for displaying repository, user data, and form data for insights.
     * @author Zeel Ravalani
     */
    @GetMapping("/{repo_id}/insights")
    public String view(@PathVariable("repo_id") Integer repoId, Model model) {
        // Retrieving repository data by repository ID
        RepoData repoData = repoDataService.getOne(repoId);
        logger.debug("repoData: " + repoData);

        // Retrieving user data associated with the repository
        UserData userData = userDataService.getOne(repoData.getUserId());

        // Creating a new FormData object
        FormData formData = new FormData();

        // Adding user, repository, and form data to the model
        model.addAttribute("userData", userData);
        model.addAttribute("repoData", repoData);
        model.addAttribute("formData", formData);

        // Returning the view name
        return "user/insights";
    }

    /**
     * Controller method for getting insights on common code mistakes for a repository by repository ID.
     *
     * @author Zeel Ravalani
     * @param repoID The unique identifier for the repository.
     * @return ResponseEntity with a string representation of common code mistakes insights.
     * @throws IOException If an I/O error occurs during the operation.
     */
    @RequestMapping(value = "/{repo_id}/get-insights/ccm", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public ResponseEntity<String> getCommonCodeMistakesInsights(@PathVariable("repo_id") Integer repoID)
            throws IOException {
        logger.debug("repoID: " + repoID + "insightType CCM ");

        // Retrieving repository data by repository ID
        RepoData repoData = repoDataService.getOne(repoID);

        // Retrieving and returning insights on common code mistakes
        String commonCodeMistakesInsight = insightsService.getCommonCodeMistakesInsights(repoData);
        return ResponseEntity.ok(commonCodeMistakesInsight);
    }

    /**
     * Controller method for getting insights on code quality enhancements for a repository by repository ID.
     *
     * @author Zeel Ravalani
     * @param repoID The unique identifier for the repository.
     * @return ResponseEntity with a string representation of code quality enhancements insights.
     * @throws IOException If an I/O error occurs during the operation.
     */
    @RequestMapping(value = "/{repo_id}/get-insights/cqe", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public ResponseEntity<String> getCodeQualityEnhancementsInsights(@PathVariable("repo_id") Integer repoID)
            throws IOException {
        logger.debug("repoID: " + repoID + " insightType CQE");

        // Retrieving repository data by repository ID
        RepoData repoData = repoDataService.getOne(repoID);

        // Retrieving and returning insights on code quality enhancements
        String codeQualityEnhancementInsightString = insightsService.getCodeQualityEnhancementsInsights(repoData);
        logger.debug("codeQualityEnhancementInsightString: " + codeQualityEnhancementInsightString);
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
    public ResponseEntity<String> getDVCInsights(@PathVariable("repo_id") Integer repoID) throws IOException {
        // Log the start of processing the DVC insights for the specified repository.
        logger.debug("Processing DVC insights for repoID: " + repoID);

        // Retrieve repository data based on the provided repository ID.
        RepoData repoData = repoDataService.getOne(repoID);

        // Process the dependency file to get versions.
        // Return a response entity with the final generated response.
        return ResponseEntity.ok(insightsServiceImpl.getDependencyVersionInsights(repoData));
    }

    @RequestMapping(value = "/{repo_id}/get-repo-prs-collab", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<String> getRepoPRsForCollab(@PathVariable("repo_id") Integer repoID)
            throws IOException, InterruptedException {
        RepoData repoData = repoDataService.getOne(repoID);
        logger.debug("repoData: " + repoData);
        return ResponseEntity.ok(insightsService.getRepositoryPRsCollab(repoData));
    }

    /**
     * Controller method for getting insights on bug detection in application flow for a repository by repository ID.
     *
     * @author Zeel Ravalani
     * @param repoID The unique identifier for the repository.
     * @return ResponseEntity with a string representation of bug detection insights in application flow.
     * @throws IOException If an I/O error occurs during the operation.
     */
    @RequestMapping(value = "/{repo_id}/get-insights/bdaf", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public ResponseEntity<String> getBugDetectionInApplicationFlowInsights(@PathVariable("repo_id") Integer repoID)
            throws IOException {
        logger.debug("repoID: " + repoID + " insightType BDAF");

        // Retrieving repository data by repository ID
        RepoData repoData = repoDataService.getOne(repoID);

        // Retrieving and returning insights on bug detection in application flow
        String bugDetectionInApplicationFlowInsightString = insightsService
                .getBugDetectionInApplicationFlowInsights(repoData);
        logger.debug("bugDetectionInApplicationFlowInsightString: " + bugDetectionInApplicationFlowInsightString);
        return ResponseEntity.ok(bugDetectionInApplicationFlowInsightString);
    }


    @RequestMapping(value = "/{repo_id}/advanced-code-search", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<String> AdvancedCodeSearch(@PathVariable("repo_id") Integer repoID,
            @RequestParam("inputData") String input) throws IOException {
        logger.debug("repoID: " + repoID + " insightType ACSI");
        input.replaceAll("%20", " ");
        RepoData repoData = repoDataService.getOne(repoID);
        String getAdvancedCodeSearchInsightString = insightsService.getAdvancedCodeSearchInsight(repoData, input);
        logger.debug("getAdvancedCodeSearchInsightString: " + getAdvancedCodeSearchInsightString);
        return ResponseEntity.ok(getAdvancedCodeSearchInsightString);
    }

    @RequestMapping(value = "/{repo_id}/get-insights/ccl", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public ResponseEntity<String> getCustomCodeLintingInsightInsights(@PathVariable("repo_id") Integer repoID)
            throws IOException {
        logger.debug("repoID: " + repoID + " insightType CCL");
        RepoData repoData = repoDataService.getOne(repoID);
        String customCodeLintingInsightString = insightsService.getCustomCodeLintingInsights(repoData);
        logger.debug("customCodeLintingInsightString: " + customCodeLintingInsightString);
        return ResponseEntity.ok(customCodeLintingInsightString);
    }

    @RequestMapping(value = "/{repo_id}/get-insights/tcm", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<String> getTestCaseMinimizationInsights(@PathVariable("repo_id") Integer repoID) throws IOException {

        logger.debug("repoID: " + repoID + "insightType TCM");
        RepoData repoData = repoDataService.getOne(repoID);
        String getTestCaseMinimizationInsightString = insightsService.getTestCaseMinimizationInsights(repoData);
        logger.debug("codeQualityEnhancementInsightString: " + getTestCaseMinimizationInsightString);
        return ResponseEntity.ok(getTestCaseMinimizationInsightString);
    }

}
