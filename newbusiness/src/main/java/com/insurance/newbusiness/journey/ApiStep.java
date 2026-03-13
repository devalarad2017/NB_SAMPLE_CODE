package com.insurance.newbusiness.journey;

import java.util.Map;

/**
 * ApiStep = one API call within a StageDefinition.
 *
 * FLAT API (no nested JSON in request):
 *   ApiStep.of("ELIGIBILITY_API")
 *   ApiExecutor posts the resolved Map<String,Object> directly. No class needed.
 *
 * NESTED API (request has nested objects):
 *   ApiStep.of("MEDICAL_API")
 *          .withRequestBuilder(params -> {
 *              MedicalApiRequest req = new MedicalApiRequest();
 *              req.getApplicant().setFirstName((String) params.get("firstName"));
 *              return req;
 *          })
 *   ApiExecutor calls builder to get POJO and posts that instead of the Map.
 *   Create a POJO only for APIs where request JSON has nested objects.
 *
 * API OUTPUT -> NEXT API INPUT:
 *   In v4, inter-stage data is passed via typed fields on JourneyContext.
 *   After a successful API call, set the result on the context directly:
 *     context.setMedicalResult(response);
 *   Downstream stages read from context.getMedicalResult().getXxx() in
 *   JourneyOrchestrator when building the next request — no PostProcessor needed
 *   for the standard pattern. Use withPostProcessor for custom side-effects only.
 *
 * PARALLEL:
 *   ApiStep.parallel("EDC_API") — grouped with other parallel() steps in same stage.
 *   All parallel steps must complete before sequential steps of that stage start.
 *
 * RETRY RESUME:
 *   JourneyOrchestrator queries journey_stage_log for api_names with status=SUCCESS.
 *   Steps in that set are skipped. Resume is at the exact failed API.
 */
public class ApiStep {

    private final String apiName;
    private final boolean parallel;
    private final RequestBuilder requestBuilder;
    private final PostProcessor  postProcessor;

    private ApiStep(String apiName, boolean parallel,
                    RequestBuilder requestBuilder, PostProcessor postProcessor) {
        this.apiName         = apiName;
        this.parallel        = parallel;
        this.requestBuilder  = requestBuilder;
        this.postProcessor   = postProcessor;
    }

    public static ApiStep of(String apiName) {
        return new ApiStep(apiName, false, null, null);
    }

    public static ApiStep parallel(String apiName) {
        return new ApiStep(apiName, true, null, null);
    }

    public ApiStep withRequestBuilder(RequestBuilder rb) {
        return new ApiStep(this.apiName, this.parallel, rb, this.postProcessor);
    }

    public ApiStep withPostProcessor(PostProcessor pp) {
        return new ApiStep(this.apiName, this.parallel, this.requestBuilder, pp);
    }

    public String getApiName()               { return apiName; }
    public boolean isParallel()              { return parallel; }
    public boolean hasRequestBuilder()       { return requestBuilder != null; }
    public boolean hasPostProcessor()        { return postProcessor != null; }
    public RequestBuilder getRequestBuilder(){ return requestBuilder; }
    public PostProcessor getPostProcessor()  { return postProcessor; }

    /** Builds request body for nested JSON APIs. params = resolved Map from mapping layer. */
    @FunctionalInterface
    public interface RequestBuilder {
        Object build(Map<String, Object> params);
    }

    /** Called after a successful API call for custom side-effects (e.g. metrics, notifications). */
    @FunctionalInterface
    public interface PostProcessor {
        void process(JourneyContext context, Map<String, Object> response);
    }
}
