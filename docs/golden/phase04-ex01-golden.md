# Phase 04 Exercise 01 — Golden Solution (SEALED)

**Status:** SEALED. Open only after attempt + write-up + self-score.

---

## 1. Scenario 1: `AuditLoggingAdvisor` (Pre/Post-Call Context & Audit Trail)

### TigerStyle Fix
```java
public static class AuditLoggingAdvisor implements CallAdvisor {
    private final List<AdvisorAuditRecord> auditRecords = new CopyOnWriteArrayList<>();

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        String correlationId = UUID.randomUUID().toString();
        ChatClientRequest mutated = request.mutate()
                .context("correlationId", correlationId)
                .build();

        long start = System.currentTimeMillis();
        ChatClientResponse response = chain.nextCall(mutated);
        long latency = Math.max(0, System.currentTimeMillis() - start);

        String promptContent = request.prompt().getInstructions().stream()
                .map(Message::getText)
                .filter(Objects::nonNull)
                .collect(Collectors.joining(" "));

        String responseContent = "";
        if (response.chatResponse() != null && response.chatResponse().getResult() != null && response.chatResponse().getResult().getOutput() != null) {
            responseContent = response.chatResponse().getResult().getOutput().getText();
        }

        auditRecords.add(new AdvisorAuditRecord(getName(), promptContent, responseContent, correlationId, latency, System.currentTimeMillis()));
        return response;
    }

    public List<AdvisorAuditRecord> getAuditRecords() {
        return Collections.unmodifiableList(auditRecords);
    }

    @Override
    public int getOrder() {
        return 200;
    }

    @Override
    public String getName() {
        return "AuditLoggingAdvisor";
    }
}
```

---

## 2. Scenario 2: `KeywordGuardrailAdvisor` (Pre-Call Prompt Security)

### TigerStyle Fix
```java
public static class KeywordGuardrailAdvisor implements CallAdvisor {
    private final List<String> blockedKeywords;

    public KeywordGuardrailAdvisor(List<String> blockedKeywords) {
        this.blockedKeywords = blockedKeywords != null ? List.copyOf(blockedKeywords) : List.of();
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        for (Message msg : request.prompt().getInstructions()) {
            String text = msg.getText();
            if (text != null) {
                for (String blocked : blockedKeywords) {
                    if (text.toUpperCase().contains(blocked.toUpperCase())) {
                        throw new PromptSecurityException("Blocked keyword detected: " + blocked);
                    }
                }
            }
        }
        return chain.nextCall(request);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public String getName() {
        return "KeywordGuardrailAdvisor";
    }
}
```

---

## 3. Scenario 3: `SystemPromptEnforcingAdvisor` (Pre-Call Policy Injection)

### TigerStyle Fix
```java
public static class SystemPromptEnforcingAdvisor implements CallAdvisor {
    private final String enforcedPolicy;

    public SystemPromptEnforcingAdvisor(String enforcedPolicy) {
        this.enforcedPolicy = Objects.requireNonNull(enforcedPolicy, "enforcedPolicy must not be null");
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        List<Message> instructions = new ArrayList<>(request.prompt().getInstructions());
        boolean alreadyPresent = instructions.stream()
                .anyMatch(m -> m.getText() != null && m.getText().contains(enforcedPolicy));

        if (!alreadyPresent) {
            instructions.add(0, new SystemMessage(enforcedPolicy));
        }

        Prompt updatedPrompt = new Prompt(instructions, request.prompt().getOptions());
        ChatClientRequest mutated = request.mutate().prompt(updatedPrompt).build();
        return chain.nextCall(mutated);
    }

    @Override
    public int getOrder() {
        return 50;
    }

    @Override
    public String getName() {
        return "SystemPromptEnforcingAdvisor";
    }
}
```

---

## 4. Scenario 4: `TokenBudgetAdvisor` (Post-Call Usage Accumulator)

### TigerStyle Fix
```java
public static class TokenBudgetAdvisor implements CallAdvisor {
    private final AtomicInteger cumulativePromptTokens = new AtomicInteger(0);
    private final AtomicInteger cumulativeCompletionTokens = new AtomicInteger(0);
    private final AtomicInteger cumulativeTotalTokens = new AtomicInteger(0);
    private final AtomicInteger callCount = new AtomicInteger(0);

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        ChatClientResponse response = chain.nextCall(request);
        callCount.incrementAndGet();

        if (response.chatResponse() != null && response.chatResponse().getMetadata() != null) {
            Usage usage = response.chatResponse().getMetadata().getUsage();
            if (usage != null) {
                int pt = usage.getPromptTokens() != null ? usage.getPromptTokens() : 0;
                int ct = usage.getCompletionTokens() != null ? usage.getCompletionTokens() : 0;
                int tt = usage.getTotalTokens() != null ? usage.getTotalTokens() : (pt + ct);

                cumulativePromptTokens.addAndGet(pt);
                cumulativeCompletionTokens.addAndGet(ct);
                cumulativeTotalTokens.addAndGet(tt);
            }
        }
        return response;
    }

    public TokenUsageSummary getSummary() {
        return new TokenUsageSummary(
                cumulativePromptTokens.get(),
                cumulativeCompletionTokens.get(),
                cumulativeTotalTokens.get(),
                callCount.get()
        );
    }

    public void reset() {
        cumulativePromptTokens.set(0);
        cumulativeCompletionTokens.set(0);
        cumulativeTotalTokens.set(0);
        callCount.set(0);
    }

    @Override
    public int getOrder() {
        return 150;
    }

    @Override
    public String getName() {
        return "TokenBudgetAdvisor";
    }
}
```

---

## 5. Scenario 5: `CostCircuitBreakerAdvisor` (Post-Call Dollar Cap)

### TigerStyle Fix
```java
public static class CostCircuitBreakerAdvisor implements CallAdvisor {
    private final CostBudgetConfig config;
    private double cumulativeCostUsd = 0.0;

    public CostCircuitBreakerAdvisor(CostBudgetConfig config) {
        this.config = Objects.requireNonNull(config, "config must not be null");
    }

    @Override
    public synchronized ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        ChatClientResponse response = chain.nextCall(request);

        if (response.chatResponse() != null && response.chatResponse().getMetadata() != null) {
            Usage usage = response.chatResponse().getMetadata().getUsage();
            if (usage != null) {
                int pt = usage.getPromptTokens() != null ? usage.getPromptTokens() : 0;
                int ct = usage.getCompletionTokens() != null ? usage.getCompletionTokens() : 0;

                double callCost = (pt * config.priceInPerMtok() + ct * config.priceOutPerMtok()) / 1_000_000.0;
                cumulativeCostUsd += callCost;

                if (cumulativeCostUsd > config.maxCostUsd()) {
                    throw new BudgetExceededException(String.format(
                            "Budget limit exceeded: cumulative cost $%.6f exceeds maximum cap $%.6f",
                            cumulativeCostUsd, config.maxCostUsd()
                    ));
                }
            }
        }
        return response;
    }

    public synchronized double getCumulativeCostUsd() {
        return cumulativeCostUsd;
    }

    @Override
    public int getOrder() {
        return 160;
    }

    @Override
    public String getName() {
        return "CostCircuitBreakerAdvisor";
    }
}
```

---

## 6. Scenario 6: `LatencyGuardrailAdvisor` (Execution Wall-Clock Timeout)

### TigerStyle Fix
```java
public static class LatencyGuardrailAdvisor implements CallAdvisor {
    private final long maxLatencyMs;

    public LatencyGuardrailAdvisor(long maxLatencyMs) {
        if (maxLatencyMs <= 0) throw new IllegalArgumentException("maxLatencyMs must be > 0");
        this.maxLatencyMs = maxLatencyMs;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        long start = System.currentTimeMillis();
        ChatClientResponse response = chain.nextCall(request);
        long elapsed = System.currentTimeMillis() - start;

        if (elapsed > maxLatencyMs) {
            throw new LatencyTimeoutException(String.format(
                    "Call latency %dms exceeded limit %dms", elapsed, maxLatencyMs
            ));
        }
        return response;
    }

    @Override
    public int getOrder() {
        return 170;
    }

    @Override
    public String getName() {
        return "LatencyGuardrailAdvisor";
    }
}
```

---

## 7. Scenario 7: `PiiMaskingAdvisor` (Post-Call Response Redaction)

### TigerStyle Fix
```java
public static class PiiMaskingAdvisor implements CallAdvisor {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");
    private static final Pattern SSN_PATTERN = Pattern.compile("\\b\\d{3}-\\d{2}-\\d{4}\\b");

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        ChatClientResponse response = chain.nextCall(request);

        if (response.chatResponse() != null && response.chatResponse().getResult() != null && response.chatResponse().getResult().getOutput() != null) {
            String originalText = response.chatResponse().getResult().getOutput().getText();
            if (originalText != null) {
                String masked = EMAIL_PATTERN.matcher(originalText).replaceAll("[REDACTED_EMAIL]");
                masked = SSN_PATTERN.matcher(masked).replaceAll("[REDACTED_SSN]");

                if (!masked.equals(originalText)) {
                    Generation newGen = new Generation(new AssistantMessage(masked));
                    ChatResponse newChatResponse = new ChatResponse(List.of(newGen), response.chatResponse().getMetadata());
                    return response.mutate().chatResponse(newChatResponse).build();
                }
            }
        }
        return response;
    }

    @Override
    public int getOrder() {
        return 180;
    }

    @Override
    public String getName() {
        return "PiiMaskingAdvisor";
    }
}
```

---

## 8. Scenario 8: `OrderTrackingAdvisor` (Advisor Precedence & Order)

### TigerStyle Fix
```java
public static class OrderTrackingAdvisor implements CallAdvisor {
    private final String name;
    private final int order;
    private final List<ExecutionOrderTrace> traces;

    public OrderTrackingAdvisor(String name, int order, List<ExecutionOrderTrace> traces) {
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.order = order;
        this.traces = Objects.requireNonNull(traces, "traces must not be null");
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        traces.add(new ExecutionOrderTrace(name, "PRE_CALL", System.currentTimeMillis()));
        ChatClientResponse response = chain.nextCall(request);
        traces.add(new ExecutionOrderTrace(name, "POST_CALL", System.currentTimeMillis()));
        return response;
    }

    @Override
    public int getOrder() {
        return order;
    }

    @Override
    public String getName() {
        return name;
    }
}
```

---

## 9. Scenario 9: `ChatClient_AdvisorRegistration`

### TigerStyle Fix
```java
public ChatClient createGuardedChatClient(ChatModel model, CallAdvisor... advisors) {
    return ChatClient.builder(model)
            .defaultAdvisors(advisors)
            .build();
}
```

---

## 10. Scenario 10: `ComprehensiveSagaGuardrailSuite`

### TigerStyle Fix
```java
public ChatClient createProductionDiagnosticStack(
        ChatModel model,
        List<String> blockedKeywords,
        String policy,
        CostBudgetConfig budgetConfig,
        TokenBudgetAdvisor tokenAdvisor,
        AuditLoggingAdvisor auditAdvisor
) {
    KeywordGuardrailAdvisor keywordAdvisor = new KeywordGuardrailAdvisor(blockedKeywords);
    SystemPromptEnforcingAdvisor policyAdvisor = new SystemPromptEnforcingAdvisor(policy);
    CostCircuitBreakerAdvisor costAdvisor = new CostCircuitBreakerAdvisor(budgetConfig);
    PiiMaskingAdvisor piiAdvisor = new PiiMaskingAdvisor();

    return ChatClient.builder(model)
            .defaultAdvisors(
                    keywordAdvisor,
                    policyAdvisor,
                    tokenAdvisor,
                    costAdvisor,
                    piiAdvisor,
                    auditAdvisor
            )
            .build();
}
```
