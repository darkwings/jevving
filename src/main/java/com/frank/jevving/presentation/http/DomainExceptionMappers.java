package com.frank.jevving.presentation.http;

import com.frank.jevving.domain.exceptions.InvalidAssessmentException;
import com.frank.jevving.domain.exceptions.SupportTeamNotFoundException;
import com.frank.jevving.domain.exceptions.TicketNotFoundException;
import com.frank.jevving.domain.exceptions.TicketNotInReviewException;
import com.frank.jevving.presentation.http.dto.ErrorResponse;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

/**
 * Translates domain exceptions into HTTP responses.
 */
public class DomainExceptionMappers {

    @ServerExceptionMapper
    public RestResponse<ErrorResponse> ticketNotFound(TicketNotFoundException e) {
        return RestResponse.status(RestResponse.Status.NOT_FOUND, new ErrorResponse(e.getMessage()));
    }

    @ServerExceptionMapper
    public RestResponse<ErrorResponse> ticketNotInReview(TicketNotInReviewException e) {
        return RestResponse.status(RestResponse.Status.CONFLICT, new ErrorResponse(e.getMessage()));
    }

    @ServerExceptionMapper
    public RestResponse<ErrorResponse> supportTeamNotFound(SupportTeamNotFoundException e) {
        return RestResponse.ResponseBuilder.<ErrorResponse>create(422)
                .entity(new ErrorResponse(e.getMessage()))
                .build();
    }

    @ServerExceptionMapper
    public RestResponse<ErrorResponse> invalidAssessment(InvalidAssessmentException e) {
        return RestResponse.status(RestResponse.Status.BAD_GATEWAY, new ErrorResponse(e.getMessage()));
    }
}
