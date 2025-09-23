package com.tem.be.api.model;

/**
 * A marker interface for all carrier-specific invoice entities.
 * This allows service and controller methods to return a common type
 * without using a generic wildcard, satisfying static analysis tools.
 */
public interface Invoiceable {
}
