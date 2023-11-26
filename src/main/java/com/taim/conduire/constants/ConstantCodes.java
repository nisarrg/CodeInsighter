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

	// Index of the captured group representing the tag type in the regular expression match.
	int TAG_TYPE_GROUP_INDEX = 1;

	// Index of the captured group representing the artifactId in the regular expression match.
	int ARTIFACT_ID_GROUP_INDEX = 2;

	// Index of the captured group representing the version in the regular expression match.
	int VERSION_GROUP_INDEX = 3;

}
