# NewBusiness — Project Context & Handoff (single-file, token-efficient)

> Purpose: paste/attach this file to give any model full context with minimal tokens.
> Last updated: 2026-06-02.

## 1. What this is
Spring Boot backend for Insurance company **New Business (NB)** insurance journey. Receives a partner
application, runs ~11 downstream API stages in sequence, submits to PAS (policy admin system),
returns an application number. Indian insurance domain (IRDAI / Aadhaar / PAN / CIBIL / premium).

## 2. Tech / build / repo
- Java 21, Spring Boot 3.5, Maven, Lombok, Spring Retry, Postgres + Flyway, OpenAPI/Swagger.
- Build: `JAVA_HOME=C:\Java\JDK21`; `C:\maven\apache-maven-3.9.16\bin\mvn.cmd -DskipTests test-compile`.
- Repo root: `C:\NB-PROJECT-CODEBASE\nb-jdk-21-changes` (Maven module under `newbusiness/`).
- Git remote `origin` = github.com/devalarad2017/NB_SAMPLE_CODE (write access here only; devaltechie removed).
- Latest work: commit `e948d92`, branch `refactor/apicalltemplate-and-resume-rename` (pushed).
- Main/default branch: `main`.

## 3. Architecture / flow (the mental model)
Controller `/api/v1/newbusiness` (POST)
→ `NewBusinessService.receiveAndAcknowledge` [@Transactional, HTTP thread]: store raw_request,
  build JourneyContext, init journey_execution(IN_PROGRESS), return 202 + correlationId.
→ `self.processJourney` [@Async journeyTaskExecutor]:
   - `JourneyOrchestrator.execute()` runs stages in order: CIBIL → UCS → PAN → BI → (EKYC if flag)
     → EDC → DCS → AUW → MRS → FES → PROPOSAL. (PennyDrop commented out.)
   - `PasApiClient.submitAndGetApplicationNumber()` — two-step initial+final push (separate because
     it returns the applicationNumber).
   - mark COMPLETED / on exception mark FAILED (no rethrow in @Async).
- Each stage: skip-if-already-SUCCESS → build request from raw params → `client.call()` → store typed
  result on JourneyContext → log.
- Each ApiClient: `@Retryable(3 attempts, backoff)` + `@Recover` → throws JourneyStageException.

### Two distinct concepts (do NOT conflate — named apart in code):
- **retry** = automatic, single API call, `@Retryable` 3 attempts w/ backoff (transient blips).
- **resume** = manual (UI) re-run of a FAILED journey; skips already-SUCCESS APIs and continues from
  the failed stage. Methods: `resumeJourney`, `resumeByApplicationNumber`. HTTP: `…/resume`.

### Persistence (clean event-log design):
- `journey_execution`: ONE mutable row per journey (status, failed stage/api, application_number).
- `journey_stage_log`: IMMUTABLE append, one row per API attempt. "Succeeded?" = a SUCCESS row exists.
- **Resume reads these back**: `JourneyResultRestorer.restorePriorResults()` reloads last SUCCESS
  response payloads → deserializes into JourneyContext so skipped stages still feed downstream (EDC/PAS).
  `resumeJourney` rebuilds context from `raw_request`. ⇒ payloads needed at rest must stay reversible.

## 4. DONE (committed e948d92, compiles, pushed)
- **ApiCallTemplate** (`integration.client`, @Component): shared execute-around wrapper owning timing +
  req/resp JSON logging (isolated so a log-serialize error can't fail/retry the call) + journey_stage_log
  tracking + ApiCallException wrapping. Signature: `<Resp> Resp execute(ApiName, Object request,
  JourneyContext, Supplier<Resp> httpCall)`. Supplier = lazy, runs inside try; verb-agnostic +
  type-transparent (bean/array/Map all work).
- 13 standard ApiClients refactored to delegate to it; **@Retryable/@Recover kept on each concrete
  client unchanged** (zero retry-semantics risk). Excluded by design: `PasApiClient` (2-step + NGIN auth)
  and `NGINTokenAPIClient` (swallows errors, returns null, no context).
- **ApiName enum** expanded: `apiName()` (=enum name) + `stageName()`; single source of truth for clients.
  Enum strings kept BYTE-IDENTICAL to old constants (persisted + matched by skip-check/restorer).
- **retry→resume** rename across service+controller methods, HTTP paths (/retry→/resume; endpoints had no
  consumers yet), response text, Swagger; comments/docs swept. `@Retryable`/api.retry.*/RETRYING left "retry".
- **JourneyStateRehydrator → JourneyResultRestorer**, `rehydrate()` → `restorePriorResults()`.
- Net ~258 fewer lines. Decisions made: keep 13 thin classes (they safely carry per-API retry/recover);
  keep explicit `setX(params.get("obj1.stringvalN"))` mapping style (greppable, compile-safe — do NOT
  replace with reflection/generic mapper).

## 5. PENDING — prioritized backlog
### BLOCKER/HIGH
- **B1 PII in plaintext** (HIGHEST): clients log full req/resp JSON; raw_request + journey_stage_log store
  full payloads (PAN, Aadhaar, DOB, mobile, bank) as TEXT. Fix = **MASK for logs (irreversible OK)** but
  **ENCRYPT at-rest** for raw_request + journey_stage_log.response_payload (must stay REVERSIBLE or resume
  breaks, since they're read back). IRDAI/Aadhaar compliance.
- B2 Fail-closed on resume read: `getSucceededApiNames` returns empty set on DB failure → re-runs all
  non-idempotent APIs. Abort/mark FAILED on read failure instead.
- B3 Token failure must stop PAS: `GenerateNginTokenOSB` returns null → PAS called w/o Authorization. Throw.
- B4 No inbound validation: InboundRequest has no @Valid/constraints → NPE/500; add validation + 400 mapping.
- B5 Test/debug endpoints in prod: `/api/v1/bi/execute` + `executeBi` (hardcoded Set("A","B","C")). Remove/guard.
- B6 No overall journey timeout/SLA: ~11 sequential calls × (5s+30s) ⇒ ~5-6 min worst case holding a thread.
- B7 No auth on partner endpoints (no Spring Security). Confirm gateway or add auth.

### MEDIUM
- M1 After-commit async: processJourney fired inside @Transactional → use AFTER_COMMIT.
- M2 Resume guards: only allow resume when FAILED; guard concurrent resume (status CAS / @Version).
- M3 Retry retries non-transient errors (4xx/validation retried 3×). Carry HTTP status; retry only transient.
- M4 NPE-prone success logs: some stages deref nested getters unguarded → a null turns SUCCESS into failure.
- M5 journey_stage_log unbounded growth: retention/archival.
- M6 Pool sizing: Hikari max 20 vs journey executor max 50 → connection starvation.
- M7 No business-response validation (HTTP 200 ≠ business success). [needs BA input; HOLD for PAS]
- M8 Default secrets in application.properties (balicsiapp). Require env injection, fail fast.
- M9 Hardcoded serverEnv:uat2 in token body. Externalize.

### LOW / MAINTAINABILITY
- L1 God classes: JourneyOrchestrator ~2150 lines, PasApiClient.buildPasRequest ~1500. Extract per-stage builders.
- L1a JourneyOrchestrator deep review (recorded, do later, tests first): dead unused `taskExecutor` field +
  misleading "parallel scoring" docs (all sequential); dead `indices1` var; wrong "CIBIL" comment on UCS stage;
  redundant ternary; 12× duplicated skip-check → one guard (adopt ApiName here); static utils → shared class;
  annualPremium formula dup’d; null-safe complete-logs; magic literals ("01","-999",product IDs); biggest:
  **obj1.stringvalN param keys are bare strings 100s of times → typo=silent null = top debug hazard → add a
  documented constants/legend file (the "readable property names" goal).**
- L2 Test coverage: only contextLoads. Add inbound→202, skip+restore, resume-only-FAILED, mapping, pool.
- L3 Enums: ApiName now used by clients; orchestrator skip-checks + restorer keys still string literals (finish).
- L4 Duplication: buildFullName (orchestrator + receipting); empty ReceiptingServiceImpl; dup receiptingApiClient1.
- L5 MDC not cleared on HTTP thread (leak-prone). L6 Swagger in prod. L7 Actuator details w/o security.
- L8 Two HTTP-client beans: all clients on RestClient; PasApiClient + DefaultPartnerNotifier still RestTemplate.
  Migrate (PAS carefully — duplicate-submit risk) or document; retire RestTemplate.
- L9 = DONE (ApiCallTemplate, see §4). L10 = retry→resume + restorer DONE; remaining T1: ApiName in orchestrator/
  restorer + a package-info mental-model doc; T2 = JourneyStage abstraction (tests first); T3 = one @Component/stage.

## 6. Hard constraints / working rules
- No breaking changes; production-grade; low runtime risk; prefer static/explicit/greppable code.
- Approval-gated: deep-dive item-by-item, get OK before changing; compile after each batch.
- KEEP: 13 thin ApiClient classes; explicit param→field setter mapping (no reflection/generic mapper).
- PAS = live submit; never auto-retry (duplicate-policy risk); resume only on FAILED.

## 7. Standing review mandate (always)
Continuously, gradually deep-review the WHOLE project through 3 lenses, record findings:
1) Architecture (resilience, idempotency, failure modes, security, observability, deploy/OCP).
2) Java (readability, maintainability, debuggability, null-safety, concurrency, tests, Java21/Boot3.5 idiom).
3) Indian insurance business (NB journey correctness, IRDAI/Aadhaar PII, no duplicate policy, premium accuracy).
Why: production insurance system — a miss in any lens = outage / compliance breach / financial harm.

## 8. How to resume (any model)
Read this file. Then say which item to start (recommended next: **B1 PII** — mask logs, encrypt at-rest,
keep reversible so resume works). Honor §6 constraints and §7 mandate.
