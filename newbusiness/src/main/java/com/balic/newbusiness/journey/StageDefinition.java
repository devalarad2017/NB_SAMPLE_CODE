package com.balic.newbusiness.journey;

import java.util.List;

/**
 * A stage name and its ordered list of ApiSteps.
 * All execution is in JourneyOrchestrator.
 */
public class StageDefinition {
    private final String stageName;
    private final List<ApiStep> steps;

    public StageDefinition(String stageName, List<ApiStep> steps) {
        this.stageName = stageName;
        this.steps     = steps;
    }

    public String getStageName(){ 
    	return stageName; 
    }
    public List<ApiStep> getSteps() {
    	return steps; 
    }
}
