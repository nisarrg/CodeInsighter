package com.taim.conduire.constants;

public interface ConstantCodes {

	String GITHUB_API_URL = "https://api.github.com";
	String GITHUB_USERS = "/users";
	String GITHUB_REPOS = "/repos";
	String GITHUB_LANG = "/languages";
	String GITHUB_PULLS = "/pulls";
	String GITHUB_COMMENTS = "/comments";
	String CODETABS_CLOC_API_URL = "https://api.codetabs.com/v1/loc?github=";
	public static final Integer LLM_TOKEN_LIMIT = 4096;
	String COLLAB_ANALYSIS_FILES_PATH = "src/main/resources/files/";
	String TMP_PATH = "output.txt";

	String DEPENDENCY_VERSION_INSIGHT_PROMPT = "\nWith the above data, provide me insights which tell me whether there" +
			" is a compatibility mismatch issue or if is it breaking any changes in the versions if any. Also, list me" +
			" actionable recommendations for compatibility matching for the given versions and any other general " +
			"insights of the artifactIDs versions being used. Give me the response in HTML tags/format using Bootstrap 5" +
			" classes so I can directly append your response directly to my website.\n";

}
