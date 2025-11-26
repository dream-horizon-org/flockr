package io.ascend.flockr.admin.provider;

import com.ascend.flockr.constants.Constants;
import com.dream11.rest.exception.RestException;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;

@Slf4j
@Provider
public class CustomConstraintExceptionMapper
    implements ExceptionMapper<ConstraintViolationException> {

  @Override
  public Response toResponse(ConstraintViolationException constraintViolationException) {
    log.error("Constraint violation: ", constraintViolationException);
    String errorMessage =
        constraintViolationException.getConstraintViolations().stream()
            .map(
                violation ->
                    violation.getPropertyPath()
                        + ": "
                        + (violation.getMessage() != null
                            ? violation.getMessage()
                            : violation.getMessageTemplate()))
            .collect(Collectors.joining(Constants.COMMA));

    RestException restException =
        new RestException(
            "INVALID_REQUEST",
            errorMessage,
            HttpStatus.SC_BAD_REQUEST,
            constraintViolationException);
    return Response.status(restException.getHttpStatusCode())
        .entity(restException.toString())
        .build();
  }
}
