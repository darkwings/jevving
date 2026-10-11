package com.frank.jevving.domain.exceptions;

/** The assessor returned an answer the domain cannot use, e.g. a support team that does not exist. */
public class InvalidAssessmentException extends RuntimeException {

    public InvalidAssessmentException(String message) {
        super(message);
    }
}
