package com.taim.conduire.constants;

public interface ConstantCodes {

	String GITHUB_API_URL = "https://api.github.com";
	String GITHUB_USERS = "/users";
	String GITHUB_REPOS = "/repos";
	String GITHUB_PULLS = "/pulls";
	String GITHUB_COMMENTS = "/comments";
	String CODETABS_CLOC_API_URL = "https://api.codetabs.com/v1/loc?github=";
	Integer LLM_TOKEN_LIMIT = 3096;
	String COLLAB_ANALYSIS_FILES_PATH = "src/main/resources/files/";

	// Temporary file path for storing content during processing.
	String TEMP_FILE_PATH = "src/main/resources/files/output.txt";

	// Constant representing the prompt for dependency version insights.
	String DEPENDENCY_VERSION_INSIGHT_PROMPT = "\nWith the above data, provide me insights which tell me whether there" +
			" is a compatibility mismatch issue or if is it breaking any changes in the versions if any. Also, list me" +
			" actionable recommendations for compatibility matching for the given versions and any other general " +
			"insights of the artifactIDs versions being used. Give your answers with a heading for section and a " +
			"paragraph/list. The headings are Compatibility Mismatch Issue, Actionable Recommendations and " +
			"General Insights List the dependencies/plugins that are causing issues and state why." +
			" Give me the response in HTML tags/format using Bootstrap 5" +
			" classes so I can directly append your response directly to my website.\n";

	String DEPENDENCY_VERSION_INSIGHT_PR_PROMPT = "Below are the Open PRs that have changes to pom.xml, indicating upgrades/downgrades of project dependencies.";

	// Index of the captured group representing the tag type in the regular expression match.
	int TAG_TYPE_GROUP_INDEX = 1;

	// Index of the captured group representing the artifactId in the regular expression match.
	int ARTIFACT_ID_GROUP_INDEX = 2;

	// Index of the captured group representing the version in the regular expression match.
	int VERSION_GROUP_INDEX = 3;

}
