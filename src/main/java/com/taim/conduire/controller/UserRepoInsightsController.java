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
import java.util.HashMap;
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

    @RequestMapping(value = "{repo_id}/get-insights/{repo_id}/ccm", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public ResponseEntity<Map<String, String>> getInsights(@PathVariable("repo_id") Integer repoID) throws IOException {

        System.out.println("repoID: " + repoID + "insightType CCM ");
        RepoData repoData = repoDataService.getOne(repoID);
        Map<String, List<String>> reviewerComments = insightsService.getRepositoryReviewComments(repoData);
        Map<String, String> roleInisghts = new HashMap<>();

        String businessAnalystPrompt = "These are open PR review comments by the reviewer:" + reviewerComments.toString() + "\n." +
                "Can you give me some insights of Common code mistakes based upon these comments.\n" +
                "Please consider yourself as a Business Analyst and write in Technical English.\n" +
                "And please frame it as if you are writing this response in <p></p> tag of html so to make sure its properly formatted " +
                "using html and shown to user. Make sure you break it into most important points and limit it to only 5 points " +
                "and highlight your reasoning." ;
        String businessAnalystInsight = chatGPTService.chat(businessAnalystPrompt);
        roleInisghts.put("businessAnalyst", businessAnalystInsight);

        String seniorManagerPrompt = "These are open PR review comments by the reviewer:" + reviewerComments.toString() + "\n." +
                "Can you give me some insights of Common code mistakes based upon these comments.\n" +
                "Please consider yourself as a Senior Manager and write in Technical English.\n" +
                "And please frame it as if you are writing this response in <p></p> tag of html so to make sure its properly formatted " +
                "using html and shown to user. Make sure you break it into most important points and limit it to only 5 points " +
                "and highlight your reasoning." ;
        String seniorManagerInsight = chatGPTService.chat(seniorManagerPrompt);
        roleInisghts.put("seniorManager", seniorManagerInsight);

        String ctoPrompt = "These are open PR review comments by the reviewer:" + reviewerComments.toString() + "\n." +
                "Can you give me some insights of Common code mistakes based upon these comments.\n" +
                "Please consider yourself as a company's CTO and write in Technical English.\n" +
                "And please frame it as if you are writing this response in <p></p> tag of html so to make sure its properly formatted " +
                "using html and shown to user. Make sure you break it into most important points and limit it to only 5 points " +
                "and highlight your reasoning." ;
        String ctoInsight = chatGPTService.chat(ctoPrompt);
        roleInisghts.put("cto", ctoInsight);
        System.out.println("roleInisghts: " + roleInisghts.size());

        return ResponseEntity.ok(roleInisghts);
    }
}
