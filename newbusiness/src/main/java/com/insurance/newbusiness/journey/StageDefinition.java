package com.insurance.newbusiness.journey;

import java.util.List;

/**
 * Pure data class — a stage name and its ordered list of ApiSteps.
 * No logic here. All execution is in JourneyOrchestrator.
 */
public class StageDefinition {
    private final String stageName;
    private final List<ApiStep> steps;

    public StageDefinition(String stageName, List<ApiStep> steps) {
        this.stageName = stageName;
        this.steps     = steps;
    }

    public String getStageName()    { return stageName; }
    public List<ApiStep> getSteps() { return steps; }
}
