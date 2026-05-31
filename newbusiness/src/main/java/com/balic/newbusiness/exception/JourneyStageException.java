package com.balic.newbusiness.exception;

public class JourneyStageException extends NewBusinessException {
    private final String stageName;
    public JourneyStageException(String stageName, String message) {
        super("JOURNEY_STAGE_ERROR", "Stage [" + stageName + "] failed: " + message);
        this.stageName = stageName;
    }
    public String getStageName() { return stageName; }
}
