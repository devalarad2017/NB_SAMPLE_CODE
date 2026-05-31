package com.balic.newbusiness.journey;

import java.util.Map;

/**
 * ApiStep = one API call within a StageDefinition.
 *
 * FLAT API (no nested JSON in request):
 *   ApiStep.of("CRIF_API")
 *   ApiExecutor posts the resolved Map<String,Object> directly. No class needed.
 *
 * NESTED API (request has nested objects):
 *   ApiStep.of("UCS_API")
 *          .withRequestBuilder(params -> {
 *              UCSApiRequest req = new UCSApiRequest();
 *              req.getApplicant().setFirstName((String) params.get("firstName"));
 *              return req;
 *          })
 *   ApiExecutor calls builder to get POJO and posts that instead of the Map.
 *   Create a POJO only for APIs where request JSON has nested objects.
 *
 * API OUTPUT -> NEXT API INPUT:
 *   ApiStep.of("CIBIL_API")
 *          .withPostProcessor((context, response) -> {
 *              context.addEnrichedParam("CIBILScore", response.get("score"));
 *          })
 *   ParameterMappingService checks enrichedParams first. CIBIL_API
 *   will find CIBIL there automatically — no extra code in ApiExecutor.
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

    /** Called after successful API call. Use context.addEnrichedParam() to chain to next step. */
    @FunctionalInterface
    public interface PostProcessor {
        void process(JourneyContext context, Map<String, Object> response);
    }
}
