package com.frank.jevving.presentation.http.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "Error", description = "Why the request could not be fulfilled")
public record ErrorResponse(String message) {
}
