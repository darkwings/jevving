package com.frank.jevving.domain.exceptions;

public class SupportTeamNotFoundException extends RuntimeException {

    public SupportTeamNotFoundException(String id) {
        super("Support team not found: " + id);
    }
}
