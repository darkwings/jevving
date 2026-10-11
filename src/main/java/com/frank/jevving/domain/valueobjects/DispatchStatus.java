package com.frank.jevving.domain.valueobjects;

public enum DispatchStatus {

    /** The assessor was confident enough: the ticket is assigned to a support team. */
    ASSIGNED,

    /** The assessor was not confident enough: a human has to pick the support team. */
    NEEDS_REVIEW;
}
