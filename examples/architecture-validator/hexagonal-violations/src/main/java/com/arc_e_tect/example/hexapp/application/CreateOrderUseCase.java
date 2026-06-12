package com.arc_e_tect.example.hexapp.application;

// DELIBERATE VIOLATION §4 — Misplaced UseCase class
// Rule triggered: "Inbound port interfaces must reside in the inbound ports package"
// Any class whose simple name ends with "UseCase" must live in the inbound ports package
// (..port.in..). This class is in the application package instead — it will be found by
// ArchUnit because the rule checks the class's location by name suffix.
// See VIOLATIONS.md §4 for full explanation.

/**
 * Inbound port for creating an order. This class intentionally resides in the wrong package
 * to demonstrate the placement rule violation.
 *
 * <p>Correct location: {@code com.arc_e_tect.example.hexapp.port.in.CreateOrderUseCase}</p>
 * <p>Actual location:  {@code com.arc_e_tect.example.hexapp.application.CreateOrderUseCase} ← VIOLATION</p>
 */
public interface CreateOrderUseCase {

    void execute(String product);
}
