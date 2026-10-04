package phase08.model;

/**
 * Finite State Machine stages for autonomous Saga agent loop.
 */
public enum SagaState {
    IDLE,
    DECIDE,
    APPLY,
    MEASURE,
    JUDGE,
    COMPENSATE,
    FINISH
}
