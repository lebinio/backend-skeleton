package com.lebinh.skeleton.web.rest.errors;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNSUPPORTED_MEDIA_TYPE;
import javax.servlet.http.HttpServletRequest;
import org.hibernate.exception.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Order(Ordered.HIGHEST_PRECEDENCE)
@ControllerAdvice
public class SkeletonExceptionHandler {

  @SuppressWarnings("unused")
  private static final Logger log = LoggerFactory.getLogger(SkeletonExceptionHandler.class);

  /**
   * Handle MissingServletRequestParameterException. Triggered when a 'required' request parameter
   * is missing.
   *
   * @param ex MissingServletRequestParameterException
   * @param headers HttpHeaders
   * @param status HttpStatus
   * @param request WebRequest
   * @return the ApiError object
   */
  @ExceptionHandler(MissingServletRequestParameterException.class)
  protected ResponseEntity<Object> handleMissingServletRequestParameter(
      MissingServletRequestParameterException ex, HttpServletRequest req) {
    String error = ex.getParameterName() + " parameter is missing";
    return buildResponseEntity(new ApiError(BAD_REQUEST, req.getRequestURI(), error, ex));
  }

  /**
   * Handle HttpMediaTypeNotSupportedException. This one triggers when JSON is invalid as well.
   *
   * @param ex HttpMediaTypeNotSupportedException
   * @param headers HttpHeaders
   * @param status HttpStatus
   * @param request WebRequest
   * @return the ApiError object
   */
  @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
  protected ResponseEntity<Object> handleHttpMediaTypeNotSupported(
      HttpMediaTypeNotSupportedException ex, HttpServletRequest req) {
    StringBuilder builder = new StringBuilder();
    builder.append(ex.getContentType());
    builder.append(" media type is not supported. Supported media types are ");
    ex.getSupportedMediaTypes().forEach(t -> builder.append(t).append(", "));
    return buildResponseEntity(
        new ApiError(
            UNSUPPORTED_MEDIA_TYPE,
            builder.substring(0, builder.length() - 2),
            req.getRequestURI(),
            ex));
  }

  /**
   * Handle MethodArgumentNotValidException. Triggered when an object fails @Valid validation.
   *
   * @param ex the MethodArgumentNotValidException that is thrown when @Valid validation fails
   * @param headers HttpHeaders
   * @param status HttpStatus
   * @param request WebRequest
   * @return the ApiError object
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  protected ResponseEntity<Object> handleMethodArgumentNotValid(
      MethodArgumentNotValidException ex, HttpServletRequest req) {
    ApiError apiError = new ApiError(BAD_REQUEST);
    apiError.setMessage("Validation error");
    apiError.setPath(req.getRequestURI());
    apiError.addValidationErrors(ex.getBindingResult().getFieldErrors());
    apiError.addValidationError(ex.getBindingResult().getGlobalErrors());
    return buildResponseEntity(apiError);
  }

  /**
   * Handle HttpMessageNotReadableException. Happens when request JSON is malformed.
   *
   * @param ex HttpMessageNotReadableException
   * @param headers HttpHeaders
   * @param status HttpStatus
   * @param request WebRequest
   * @return the ApiError object
   */
  @ExceptionHandler(HttpMessageNotReadableException.class)
  protected ResponseEntity<Object> handleHttpMessageNotReadable(
      HttpMessageNotReadableException ex, HttpServletRequest req) {
    String error = "Malformed JSON request";
    return buildResponseEntity(new ApiError(BAD_REQUEST, error, req.getRequestURI(), ex));
  }

  /**
   * Handle HttpMessageNotWritableException.
   *
   * @param ex HttpMessageNotWritableException
   * @param headers HttpHeaders
   * @param status HttpStatus
   * @param request WebRequest
   * @return the ApiError object
   */
  @ExceptionHandler(HttpMessageNotWritableException.class)
  protected ResponseEntity<Object> handleHttpMessageNotWritable(
      HttpMessageNotWritableException ex, HttpServletRequest req) {
    String error = "Error writing JSON output";
    return buildResponseEntity(new ApiError(INTERNAL_SERVER_ERROR, error, req.getRequestURI(), ex));
  }

  /**
   * Handles javax.validation.ConstraintViolationException. Thrown when @Validated fails.
   *
   * @param ex the ConstraintViolationException
   * @return the ApiError object
   */
  @ExceptionHandler(javax.validation.ConstraintViolationException.class)
  protected ResponseEntity<Object> handleConstraintViolation(
      javax.validation.ConstraintViolationException ex) {
    ApiError apiError = new ApiError(BAD_REQUEST);
    apiError.setMessage("Validation error");
    apiError.addValidationErrors(ex.getConstraintViolations());
    return buildResponseEntity(apiError);
  }

  /**
   * Handles EntityNotFoundException. Created to encapsulate errors with more detail than
   * javax.persistence.EntityNotFoundException.
   *
   * @param ex the EntityNotFoundException
   * @return the ApiError object
   */
  @ExceptionHandler(EntityNotFoundException.class)
  protected ResponseEntity<Object> handleEntityNotFound(EntityNotFoundException ex) {
    ApiError apiError = new ApiError(NOT_FOUND);
    apiError.setMessage(ex.getMessage());
    return buildResponseEntity(apiError);
  }

  /** Handle javax.persistence.EntityNotFoundException */
  @ExceptionHandler(javax.persistence.EntityNotFoundException.class)
  protected ResponseEntity<Object> handleEntityNotFound(
      javax.persistence.EntityNotFoundException ex, HttpServletRequest req) {
    return buildResponseEntity(new ApiError(NOT_FOUND, req.getRequestURI(), ex));
  }

  /**
   * Handle DataIntegrityViolationException, inspects the cause for different DB causes.
   *
   * @param ex the DataIntegrityViolationException
   * @return the ApiError object
   */
  @ExceptionHandler(DataIntegrityViolationException.class)
  protected ResponseEntity<Object> handleDataIntegrityViolation(
      DataIntegrityViolationException ex, HttpServletRequest req) {
    if (ex.getCause() instanceof ConstraintViolationException) {
      return buildResponseEntity(new ApiError(CONFLICT, "Database error", ex.getCause()));
    }
    return buildResponseEntity(new ApiError(INTERNAL_SERVER_ERROR, req.getRequestURI(), ex));
  }

  /**
   * Handle Exception, handle generic Exception.class
   *
   * @param ex the Exception
   * @return the ApiError object
   */
  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  protected ResponseEntity<Object> handleMethodArgumentTypeMismatch(
      MethodArgumentTypeMismatchException ex, WebRequest request) {
    ApiError apiError = new ApiError(BAD_REQUEST);
    apiError.setMessage(
        String.format(
            "The parameter '%s' of value '%s' could not be converted to type '%s'",
            ex.getName(), ex.getValue(), ex.getRequiredType().getSimpleName()));
    apiError.setMessage(ex.getMessage());
    return buildResponseEntity(apiError);
  }

  /**
   * build error {@link ResponseEntity}
   *
   * @param apiError
   * @return error JSON
   */
  private ResponseEntity<Object> buildResponseEntity(ApiError apiError) {
    return new ResponseEntity<>(apiError, apiError.getStatus());
  }
}
