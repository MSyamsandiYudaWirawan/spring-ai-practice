package phase01;

import java.util.List;
import java.util.Objects;

/**
 * Immutable contracts and data models for Phase 01 Exercise 02 (FinTech Fraud & Risk Operations).
 * DO NOT MODIFY THIS FILE.
 */
public final class FraudModelContracts {

    private FraudModelContracts() {}

    public record Transaction(
            String txId,
            String accountId,
            double amount,
            String merchant,
            String country
    ) {
        public Transaction {
            Objects.requireNonNull(txId, "txId must not be null");
            Objects.requireNonNull(accountId, "accountId must not be null");
            Objects.requireNonNull(merchant, "merchant must not be null");
            Objects.requireNonNull(country, "country must not be null");
        }
    }

    public record DisputeNotice(
            String customerName,
            String disputeId,
            double amount,
            String currency,
            String reason
    ) {
        public DisputeNotice {
            Objects.requireNonNull(customerName, "customerName must not be null");
            Objects.requireNonNull(disputeId, "disputeId must not be null");
            Objects.requireNonNull(currency, "currency must not be null");
            Objects.requireNonNull(reason, "reason must not be null");
        }
    }

    public record AuditResult(
            String responseContent,
            long promptTokens,
            long completionTokens
    ) {
        public AuditResult {
            Objects.requireNonNull(responseContent, "responseContent must not be null");
        }
    }

    public record FewShotExample(String userText, String assistantReply) {}

    public static final String BASE_SYSTEM_PROMPT =
            "You are a FinTech fraud analysis assistant. Analyze all inputs for financial anomalies.";

    public static final String EXPEDITED_SYSTEM_PROMPT =
            "You are an expedited emergency incident response officer. Provide immediate mitigation steps.";

    public static final double BASE_TEMPERATURE = 0.7;
    public static final double BASE_TOP_P = 0.95;

    public static final double OVERRIDE_TEMPERATURE = 0.0;
    public static final int OVERRIDE_MAX_TOKENS = 500;

    public static final String DISPUTE_TEMPLATE =
            "Notice of Dispute [{disputeId}] for customer {customerName}. Transaction amount: {currency} {amount}. Reason cited: {reason}. Draft formal inquiry.";

    public static final String REGIONAL_AUDITOR_SYSTEM_TEMPLATE =
            "You are a regulatory compliance auditor for {jurisdiction} operating under {standard} standards. Verify all statutory limits.";

    public static final List<FewShotExample> CLASSIFICATION_FEW_SHOTS = List.of(
            new FewShotExample("My card was charged twice for lunch at Subway", "DUPLICATE_CHARGE"),
            new FewShotExample("Cancel my annual premium subscription immediately", "SUBSCRIPTION_CANCELLATION"),
            new FewShotExample("I see a $900 transfer to an unknown overseas IBAN", "UNAUTHORIZED_TRANSACTION")
    );
}
