/*****************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/
package jakarta.ai.agent.example.frauddetection;

import jakarta.ai.agent.Agent;
import jakarta.ai.agent.Trigger;
import jakarta.ai.agent.Decision;
import jakarta.ai.agent.Action;
import jakarta.ai.agent.Outcome;
import jakarta.ai.agent.LargeLanguageModel;
import jakarta.ai.agent.Result;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.validation.Valid;

/**
 * Example agent for bank fraud detection.
 * Demonstrates use of Jakarta Agentic AI annotations and LLM facade.
 */
@Agent(name = "FraudDetection", description = "Detects bank fraud transactions.")
public class FraudDetectionAgent {
    @Inject
    private LargeLanguageModel model;
    @Inject
    @SuppressWarnings("unused")
    private EntityManager entityManager;

    @Trigger
    private void handleTransaction(@Valid BankTransaction transaction) {
        // Workflow trigger logic
    }

    @Decision
    private Result checkFraud(BankTransaction transaction) {
        String output = model.query(
            "Is this a fraudulent transaction? If so, how serious is it?", transaction);
        boolean fraud = isFraud(output);
        Fraud details = fraud ? getFraudDetails(output) : null;
        return new Result(fraud, details);
    }

    @Action
    private void handleFraud(Fraud fraud, BankTransaction transaction) {
        if (fraud.isSerious()) {
            alertBankSecurity(fraud);
        }
        Customer customer = getCustomer(transaction);
        alertCustomer(fraud, transaction, customer);
    }

    @Outcome
    private void markTransaction(BankTransaction transaction) {
        // Mark transaction suspect, probably in the database.
    }

    private boolean isFraud(String output) {
        // Simple custom text parsing logic
        return output != null && output.contains("fraud");
    }

    private Fraud getFraudDetails(String output) {
        // Parse details from output or query database
        return new Fraud();
    }

    private Customer getCustomer(BankTransaction transaction) {
        // Lookup customer from transaction
        return new Customer();
    }

    private void alertBankSecurity(Fraud fraud) {
        // Notify bank security
    }

    private void alertCustomer(Fraud fraud, BankTransaction transaction, Customer customer) {
        // Notify customer
    }
}
