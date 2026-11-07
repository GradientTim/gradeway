/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.event

/**
 * Represents the base type for events that produce or carry a result.
 *
 * Subclasses define the specific event context while using the provided default
 * result as the initial value for event handling. Event handlers may update the
 * result to communicate an outcome or influence subsequent processing.
 *
 * @param TResult The type of result associated with this event.
 * @param defaultResult The initial result value assigned to the event.
 */
abstract class Event<TResult>(defaultResult: TResult) {
    var result: TResult = defaultResult

    /**
     * Represents the absence of a meaningful event result.
     *
     * Use this result type for events that do not need to produce, carry, or modify
     * an outcome during handling. Implementations are restricted to the predefined
     * result values, providing a type-safe way to model result-less events.
     */
    sealed interface EmptyResult {
        data object None : EmptyResult
    }

    /**
     * Represents the outcome of cancellation-aware event handling.
     *
     * Use this result to indicate whether the associated operation or event flow
     * should continue or be canceled. Implementations are limited to the predefined
     * results, ensuring that cancellation decisions are explicit and type-safe.
     */
    sealed interface CancelledResult {
        data object Allow : CancelledResult
        data object Cancel : CancelledResult
    }
}
