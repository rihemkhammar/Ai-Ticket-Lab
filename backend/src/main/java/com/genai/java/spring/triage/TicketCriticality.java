package com.genai.java.spring.triage;

/**
 * Criticality level assigned to a ticket by the Triage classification node.
 * Ordering matters: used by OrderQueueNode to sort the dispatch queue
 * (CRITICAL first, then HIGH, MEDIUM, LOW).
 */
public enum TicketCriticality {
    CRITICAL,
    HIGH,
    MEDIUM,
    LOW
}