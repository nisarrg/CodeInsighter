package com.taim.conduire.constants;

public interface InsightsPrompts {

    String COMMON_CODE_MISTAKES = "\n\nThe above are open PR review comments by the reviewer\n." +
            "Can you give me some insights of Common code mistakes based upon these comments.\n" +
            "Please consider yourself as a Business Analyst and write in Technical English.\n" +
            "And please frame it as if you are writing this response in <p></p> tag of html so to make sure its properly formatted " +
            "using html and shown to user. Make sure you break it into most important points and limit it to only 5 points " +
            "and highlight your reasoning. And Format the response in HTML tags and use Bootstrap classes for better readability";

    String CODE_QUALITY_ENHANCEMENTS = "The provided string is a map with \n" +
            "developers as key and value with list of 2 strings where\n" +
            "First string is the Title of the PR, and second string is the PR Code.\n" +
            "Based on different criteria: Readability, Performance, Correctness, Scalability\n" +
            "Can give a some Code improvements suggestions/comments and\n" +
            "A score for each criteria from 0 to 5 as I want to show it in a visual graph format\n" +
            "please mention for all 4 criteria (Readability, Performance, Correctness, Scalability) even if you don't find them you can score them as 0 if not found.\n" +
            "and make your response in JSON Array format\n" +
            "Generate a JSON array with the following pattern:\n" +
            "[\n" +
            "    {\n" +
            "        \"developer\": \"<developer name>\",\n" +
            "        \"pr_title\": \"<pr title>\",\n" +
            "        \"code_improvements\": [<suggestion1>, <suggestion2>, <suggestion3>],\n" +
            "        \"score\": [<score1>, <score2>, <score3>, <score4>],\n" +
            "        \"criteria\": [\"<criterion1>\", \"<criterion2>\", \"<criterion3>\", \"<criterion4>\"]\n" +
            "    },\n" +
            "]\n" +
            "Keep the score and criteria in the same order so later on it can be fetched.\n\n";

    String BUG_DETECTION_IN_APPLICATION_FLOW = "The provided string is a map with \n" +
            "developers as key and value with list of 2 strings where\n" +
            "First string is the Title of the PR, and second string is the PR Code.\n" +
            "I want you to conduct bug detection to find unexpected bugs being introduced by pushed code in the application flows.\n" +
            "and I want you to display actionable recommendations for resolving these bugs.\n" +
            "Also, I want you to display alerts if this PR is introducing any bug in the application's major flows." +
            "and make your response in JSON Array format\n" +
            "Generate a JSON Array with the following pattern:\n" +
            "[\n" +
            "  {\n" +
            "    \"developer\": \"<developer_name>\",\n" +
            "    \"pr_title\": \"<title_string>\",\n" +
            "    \"bugs\": [\n" +
            "      {\n" +
            "        \"file_location\": \"<file_name_with_extension>\",\n" +
            "        \"code_in_file\": \"<code_string>\",\n" +
            "        \"issue\":  \"<issue_string>\",\n" +
            "        \"recommendation\": [\"<recommendation1>\", \"<recommendation2>\", \"<recommendation3>\", \"<recommendation4>\"]\n"
            +
            "      }\n" +
            "    ],\n" +
            "    \"alerts\": [\"<alert1>\", \"<alert2>\", \"<alert3>\", \"<alert4>\"],\n" +
            "    \"general_recommendation\": [\"<general_recommendation1>\", \"<general_recommendation2>\", \"<general_recommendation3>\", \"<general_recommendation4>\"]\n"
            +
            "  }\n" +
            "]";

    String CUSTOM_CODE_LINTING = "The provided string is a map with \n" +
            "developers as key and value with list of 2 strings where\n" +
            "First string is the Title of the PR, and second string is the PR Code.\n" +
            "Linting Check Criteria: Syntax Errors, Code Standards Adherence, Code Smells, Security Checks.\n" +
            "I want you to conduct linting check based on the above mentioned criteria to find out whether the Linting rules are followed by pushed code.\n"+
            "and I want you to display actionable recommendations for improving the Linting Standards.\n" +
            "and make your response in JSON Array format.\n" +
            "Note: If a PR code follows linting standards then don't include in the below JSON.\n" +
            "Generate a JSON Array with the following pattern:\n" +
            "[\n" +
            "  {\n" +
            "    \"developer\": \"<developer_name>\",\n" +
            "    \"pr_title\": \"<title_string>\",\n" +
            "	 \"lintings\":[\n" +
            "       {\n" +
            "	   		\"file_location\": \"<file_name_with_extension>\",\n" +
            "	       	\"code_in_file\": \"<code_string>\",\n" +
            "	       	\"issue\":  \"<criteria which is violated>\",\n" +
            "	       	\"linting_comments\": [\"<comment1>\", \"<comment2>\", \"<comment3>\"]\n" +
            "	    }\n" +
            "     ],\n" +
            "  }\n" +
            "]";

    String TEST_CASE_MINIMIZATION = "The provided string is a map with \n" +
            "developers as key and value with list of 2 strings where\n" +
            "First string is the Title of the PR, and second string is the PR Code.\n" +
            "Based on the data provided, mention the number of PRs raised.\n" +
            "And based on all the PRs can you give me an insight about which test cases are trivial" +
            " or which of the test cases can be avoided?\n" +
            " If there are no PRs or no test cases that you can get from the data, give the insight as: " +
            " I did not get any feasible PR or test case. Try again with another repository." +
            " Please provide your response using HTML Tags and use Bootstrap 5 classes for better readability.";

    String ADVANCED_CODE_SEARCH = "in the following piece of code and if there is, mention " +
            "the component name, file name, line number and display that code snippet. "
            + "Give the output in html tags & bootstrap components. Give the Component Name as heading (h3), " +
            "file name in italics, line number in normal text and the code snippet in monospace font in white font color and "
            +
            "keep the background color of the code snippet as \"#323a36\". The background color of the remaining components should be \"#c4df9b\" and "
            +
            "the font color should be \"#508500\". Double Check if the Output is in HTML and Strictly in HTML only. Following is the sample output format desired:"
            +
            "<div style=\"background-color: #c4df9b; padding: 10px; border-radius: 8px; font-family: Nunito, sans-serif; font-size: .875rem; font-weight: 400; line-height: 1.5; color: #508500;\">\n"
            +
            "\n" +
            "    <h3 style=\"color: #508500;\">Component Name: {$ComponentName}</h3>\n" +
            "    <p style=\"color: #508500;\"><em>File Name: {$FileName}</em></p>\n" +
            "    <p style=\"color: #508500;\">Line Number: {$LineNumber}</p>\n" +
            "    <pre style=\"background-color: #323a36; padding: 10px; border-radius: 4px; overflow: auto; color: #f2f2f3;\">\n"
            +
            "        {$CodeComponent}\n" +
            "    </pre>\n" +
            "\t\n" +
            "</div>\n";

    String COLLAB_ANALYSIS_FINAL_PART = "Your task is to determine which two contributors, when collaborating together, " +
            "would be the most productive. Productivity is defined as a combination of commit count and code smells rating, "
            +
            "where lower code smells ratings are preferable.\n\n" +
            "Provide the names of the two contributors and a brief explanation of why you consider them to be the most productive collaborators based on the given criteria."
            +
            "Give the output in the following format and always double check that the output is in this format exactyl:"
            +
            "<div style=\"background-color: #dcf4f9; color: #2f7787; padding: 10px;\">\n" +
            "    <h2>Most Productive Collaborators</h2>\n" +
            "    <p>Based on commit count and code smells ratings, the two most productive collaborators for the GitHub repository are:</p>\n"
            +
            "    \n" +
            "    <ol>\n" +
            "        <li>\n" +
            "            <strong>${Contributor-1}</strong>\n" +
            "            <ul>\n" +
            "                <li>Commit Count: ${Contributor1-CommitCount}</li>\n" +
            "                <li>Code Smells Rating:${Contributor1-SmellRating}</li>\n" +
            "            </ul>\n" +
            "        </li>\n" +
            "        <li>\n" +
            "            <strong>${Contributor-2}</strong>\n" +
            "            <ul>\n" +
            "                <li>Commit Count:  ${Contributor2-CommitCount}</li>\n" +
            "                <li>Code Smells Rating: ${Contributor2-SmellRating}</li>\n" +
            "            </ul>\n" +
            "        </li>\n" +
            "    </ol>\n" +
            "\n" +
            "<p>${Explanation}</p>\n" +
            "</div>";

}
