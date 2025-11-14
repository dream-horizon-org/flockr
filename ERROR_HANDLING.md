# Generic Error Handling for REST APIs

This document describes the generic error handling system implemented in the Flockr application that can be reused across all API flows.

## Overview

The error handling system provides:
- ✅ **Centralized Exception Handling**: Automatic conversion of exceptions to REST responses
- ✅ **Consistent Error Format**: Uniform error structure across all APIs
- ✅ **Reactive Support**: Built-in support for RxJava Single operations
- ✅ **Custom Error Mapping**: Flexible error mapping capabilities
- ✅ **Logging**: Automatic error logging with context
- ✅ **HTTP Status Code Mapping**: Smart mapping of exceptions to appropriate HTTP status codes

## Components

### 1. GenericExceptionMapper (Provider)

**Location**: `com.ascend.flockr.provider.GenericExceptionMapper`

This is a JAX-RS `@Provider` that automatically catches all uncaught exceptions in REST endpoints and converts them to appropriate error responses.

**Features**:
- Handles `RestException` from Dream11 library
- Handles `WebApplicationException` from JAX-RS
- Handles common exceptions like `IllegalArgumentException`, `NullPointerException`
- Provides meaningful error messages for different HTTP status codes
- Returns errors in `ResponseEntity.Failure` format

**Auto-Registration**: This provider is automatically registered by JAX-RS and requires no manual registration.

### 2. ErrorHandler Utility

**Location**: `com.ascend.flockr.util.ErrorHandler`

A utility class that provides helper methods for handling errors in REST controller flows.

#### Main Methods

##### `handleAsync(Single<T> single, String operationName)`

Wraps a reactive Single operation with error handling and maps to `ResponseEntity.Success`.

**Usage in Controllers**:
```java
@POST
@Path("/v1/audiences")
public CompletionStage<ResponseEntity.Success<Long>> createAudience(CreateAudienceRequest requestBody) {
    return ErrorHandler.handleAsync(
        audienceService.createAudience(requestBody),
        "createAudience");
}
```

**What it does**:
1. Executes the service method (reactive Single)
2. Logs any errors that occur with context
3. Maps exceptions to appropriate `RestException` with HTTP status codes
4. Returns `ResponseEntity.Success<T>` on success

##### `handleAsyncWithCustomError(Single<T> single, String operationName, Function<Throwable, RestException> errorMapper)`

Same as `handleAsync` but allows custom error mapping logic.

**Usage**:
```java
return ErrorHandler.handleAsyncWithCustomError(
    audienceService.getAudienceDetails(audienceId),
    "getAudienceDetails",
    throwable -> {
        if (throwable instanceof NoSuchElementException) {
            return ErrorHandler.notFoundException("Audience", audienceId);
        }
        return ErrorHandler.badRequestException(throwable.getMessage());
    });
```

##### Helper Methods for Common Scenarios

```java
// Not found (404)
ErrorHandler.notFoundException("Audience", audienceId)

// Bad request (400)
ErrorHandler.badRequestException("Invalid input provided")

// Conflict (409)
ErrorHandler.conflictException("Audience with this name already exists")

// Service unavailable (503)
ErrorHandler.serviceUnavailableException("Flink Service")
```

### 3. ErrorEnum

**Location**: `com.ascend.flockr.exception.ErrorEnum`

An enum that defines common error codes and messages for the application.

**Available Error Codes**:
- `REST_HEALTH_CHECK_FAILED` (500): Health check failures
- `AUDIENCE_NOT_FOUND` (404): Audience not found
- `RULE_NOT_FOUND` (404): Rule not found
- `INVALID_REQUEST_BODY` (400): Invalid request body parameters
- `DATABASE_ERROR` (500): Database operation failures
- `INTERNAL_ERROR` (500): Unexpected internal errors

**Usage with ExceptionUtil**:
```java
throw ExceptionUtil.getException(ErrorEnum.AUDIENCE_NOT_FOUND);
```

## Error Response Format

All errors return a consistent `ResponseEntity.Failure` structure:

```json
{
  "error": {
    "code": "INVALID_ARGUMENT",
    "message": "Invalid argument in createAudience: name cannot be empty",
    "cause": "name cannot be empty"
  }
}
```

## How to Use in Controllers

### Basic Usage (Recommended)

Replace this pattern:
```java
return audienceService
    .createAudience(requestBody)
    .map(ResponseEntity.Success::new)
    .toCompletionStage();
```

With this:
```java
return ErrorHandler.handleAsync(
    audienceService.createAudience(requestBody),
    "createAudience");
```

### Custom Error Handling

If you need custom error logic:
```java
return ErrorHandler.handleAsyncWithCustomError(
    service.getResource(id),
    "getResource",
    throwable -> {
        if (throwable instanceof NoSuchElementException) {
            return ErrorHandler.notFoundException("Resource", id);
        } else if (throwable instanceof IllegalStateException) {
            return ErrorHandler.conflictException("Resource is locked");
        }
        // Fall back to default mapping
        return new RestException(
            "RESOURCE_ERROR",
            "Failed to get resource: " + throwable.getMessage(),
            HttpStatus.SC_INTERNAL_SERVER_ERROR,
            throwable);
    });
```

## Exception to HTTP Status Code Mapping

The `ErrorHandler` automatically maps exceptions to appropriate HTTP status codes:

| Exception Type | HTTP Status | Error Code |
|----------------|-------------|------------|
| `RestException` | (from exception) | (from exception) |
| `IllegalArgumentException` | 400 | INVALID_ARGUMENT |
| `NullPointerException` | 400 | MISSING_REQUIRED_FIELD |
| `SQLException` | 500 | DATABASE_ERROR |
| `IOException` | 500 | DATABASE_ERROR |
| Other exceptions | 500 | INTERNAL_ERROR |

## Examples

### Example 1: Simple GET Endpoint
```java
@GET
@Path("/v1/audiences/{audienceId}")
public CompletionStage<ResponseEntity.Success<AudienceDetailsResponse>> getAudienceDetails(
    @PathParam("audienceId") Long audienceId) {
    return ErrorHandler.handleAsync(
        audienceService.getAudienceDetails(audienceId),
        "getAudienceDetails");
}
```

### Example 2: POST Endpoint with Validation
```java
@POST
@Path("/v1/audiences")
public CompletionStage<ResponseEntity.Success<Long>> createAudience(
    CreateAudienceRequest requestBody) {
    return ErrorHandler.handleAsync(
        audienceService.createAudience(requestBody),
        "createAudience");
}
```

### Example 3: Custom Error Handling for Specific Cases
```java
@DELETE
@Path("/v1/audiences/{audienceId}")
public CompletionStage<ResponseEntity.Success<Boolean>> deleteAudience(
    @PathParam("audienceId") Long audienceId) {
    return ErrorHandler.handleAsyncWithCustomError(
        audienceService.deleteAudience(audienceId),
        "deleteAudience",
        throwable -> {
            if (throwable.getMessage().contains("has active rules")) {
                return ErrorHandler.conflictException(
                    "Cannot delete audience with active rules");
            }
            // Use default mapping for other errors
            return new RestException(
                "DELETE_FAILED",
                throwable.getMessage(),
                HttpStatus.SC_INTERNAL_SERVER_ERROR,
                throwable);
        });
}
```

### Example 4: Throwing Specific Errors in Service Layer
```java
public Single<AudienceDetailsResponse> getAudienceDetails(Long audienceId) {
    return audienceRepository.findById(audienceId)
        .switchIfEmpty(Single.error(
            new RestException(
                "AUDIENCE_NOT_FOUND",
                "Audience with ID " + audienceId + " not found",
                HttpStatus.SC_NOT_FOUND)))
        .map(this::mapToResponse);
}
```

Or using ErrorEnum:
```java
public Single<AudienceDetailsResponse> getAudienceDetails(Long audienceId) {
    return audienceRepository.findById(audienceId)
        .switchIfEmpty(Single.error(
            ExceptionUtil.getException(ErrorEnum.AUDIENCE_NOT_FOUND)))
        .map(this::mapToResponse);
}
```

## Benefits

1. **Reduced Boilerplate**: No need to write error handling in every controller method
2. **Consistent Error Responses**: All APIs return errors in the same format
3. **Better Logging**: Automatic logging with operation context
4. **Type Safety**: Compile-time checks for error handling
5. **Maintainability**: Centralized error handling logic
6. **Testability**: Easy to test error scenarios

## Migration Guide

To migrate existing controllers to use the new error handling:

### Step 1: Add Import
```java
import com.ascend.flockr.util.ErrorHandler;
```

### Step 2: Replace the Pattern
**Before**:
```java
public CompletionStage<ResponseEntity.Success<T>> endpoint() {
    return service.operation()
        .map(ResponseEntity.Success::new)
        .toCompletionStage();
}
```

**After**:
```java
public CompletionStage<ResponseEntity.Success<T>> endpoint() {
    return ErrorHandler.handleAsync(
        service.operation(),
        "operationName");
}
```

### Step 3: Remove Manual Error Handling
Remove any manual `.onErrorResumeNext()` or `.onErrorReturn()` calls unless you need custom error logic.

## Testing

### Unit Testing Controllers
```java
@Test
public void testCreateAudience_Success() {
    when(audienceService.createAudience(any()))
        .thenReturn(Single.just(123L));
    
    CompletionStage<ResponseEntity.Success<Long>> result = 
        controller.createAudience(request);
    
    // Assert success response
}

@Test
public void testCreateAudience_ValidationError() {
    when(audienceService.createAudience(any()))
        .thenReturn(Single.error(new IllegalArgumentException("Invalid name")));
    
    CompletionStage<ResponseEntity.Success<Long>> result = 
        controller.createAudience(request);
    
    // Assert error response with 400 status
}
```

## Best Practices

1. **Use Descriptive Operation Names**: The operation name is used in logs, so make it descriptive
2. **Throw Specific Exceptions in Services**: Use `RestException` with appropriate status codes
3. **Use ErrorEnum for Common Errors**: Add new error types to `ErrorEnum` for reusability
4. **Don't Catch Generic Exceptions**: Let the error handler deal with generic exceptions
5. **Use Custom Error Mapping Sparingly**: Only use it when you need specific business logic
6. **Log at Service Layer**: Service layer should log details; controller errors are logged automatically

## Troubleshooting

### Error Not Being Caught
- Make sure the controller method returns `CompletionStage<ResponseEntity.Success<T>>`
- Ensure you're using `ErrorHandler.handleAsync()` to wrap the service call

### Wrong HTTP Status Code
- Check if the exception is being mapped correctly in `ErrorHandler.mapToRestException()`
- Consider using custom error mapping for specific cases

### Error Response Format Doesn't Match
- Ensure `GenericExceptionMapper` is being registered (it should auto-register as a `@Provider`)
- Check if there are other exception mappers that might be intercepting the error

## Future Enhancements

Possible future improvements:
- [ ] Add support for internationalized error messages
- [ ] Add error rate limiting
- [ ] Add error metrics collection
- [ ] Add retry logic for transient errors
- [ ] Add correlation IDs for error tracking

