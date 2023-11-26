package com.taim.conduire.service;

import java.io.IOException;
import java.util.List;

public interface LLMService {

    int[] getRepositoryPunchCardtest(String name) throws IOException;

    int[] computeWeeklyCommits(List<List<Integer>> repoPunchCard);

}
