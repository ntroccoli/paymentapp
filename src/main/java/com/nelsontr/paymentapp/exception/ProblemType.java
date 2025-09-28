package com.nelsontr.paymentapp.exception;

import java.net.URI;

/**
 * Stable URN identifiers for RFC7807 Problem Details types used by the API.
 * These are absolute URIs (URNs) and safe to publish in responses and OpenAPI examples.
 */
public final class ProblemType {
    private ProblemType() {}

    public static final URI VALIDATION_ERROR = URI.create("urn:paymentapp:problem:validation-error");
    public static final URI RESOURCE_NOT_FOUND = URI.create("urn:paymentapp:problem:resource-not-found");
    public static final URI CONSTRAINT_VIOLATION = URI.create("urn:paymentapp:problem:constraint-violation");
    public static final URI MALFORMED_JSON = URI.create("urn:paymentapp:problem:malformed-json");
    public static final URI INTERNAL_ERROR = URI.create("urn:paymentapp:problem:internal-error");
    public static final URI RESOURCE_CONFLICT = URI.create("urn:paymentapp:problem:resource-conflict");
}
