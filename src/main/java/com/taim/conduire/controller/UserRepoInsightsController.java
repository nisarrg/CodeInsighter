package com.taim.conduire.controller;

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

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

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

    @PostMapping("/{repo_id}/insights/chat")
    public String process(@PathVariable("repo_id") Integer repoId, @ModelAttribute("formData") FormData formData, Model model) {
        // Handle form submission and set the result in the model
        String data_string = "Data:" + formData.getInputText() + "\n. Consider yourself as: " + formData.getSelectedOption();
        String input_string = data_string + "Give me insights from the given data in 3-4 Sentences. Write in Technical English";
        String result = chatGPTService.chat(input_string);
        model.addAttribute("result", result);
        return "user/insights";
    }

    @RequestMapping(value = "/{repo_id}/selection/{selection}/{role}", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public ResponseEntity<String> getInsights(@PathVariable("repo_id") Integer repoID,
                                              @PathVariable("selection") String insightSelection,
                                              @PathVariable("role") String role) throws IOException {

        System.out.println("repoID: " + repoID + "insightSelection: " + insightSelection + "role: " + role);
        RepoData repoData = repoDataService.getOne(repoID);
        System.out.println("repoData: " + repoData.getName());
        int punchCardStats[] = llmService.getRepositoryPunchCardtest(repoData.getName());
        insightsService.getRepositoryReviewComments(repoData);

        String data_string = "This Data is for everyday daily commit in a week starting from Sunday to Saturday:" + punchCardStats.toString() + "\n. Consider yourself as: " + role;
        String input_string = data_string + "Give me insights from the given data in 3-4 Sentences. Write in Technical English";
        String result = chatGPTService.chat(input_string);
        System.out.println("LLM Inisghts: " + result);

        return ResponseEntity.ok(result);
    }
}
