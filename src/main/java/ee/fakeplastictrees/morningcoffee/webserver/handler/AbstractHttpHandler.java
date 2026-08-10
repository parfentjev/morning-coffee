package ee.fakeplastictrees.morningcoffee.webserver.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/// Handles requests matching one HTTP method and request-path pattern.
abstract class AbstractHttpHandler implements HttpHandler {
  private static final Logger logger = LogManager.getLogger();

  protected static final String CONTENT_TYPE_TEXT_PLAIN = "text/plain; charset=utf-8";
  protected static final String CONTENT_TYPE_TEXT_HTML = "text/html; charset=utf-8";

  @Override
  public final void handle(HttpExchange exchange) {
    logger.debug("{} {}", exchange.getRequestMethod(), exchange.getRequestURI());

    var matchingMethod = exchange.getRequestMethod().equals(requestMethod());
    var matchingPath = exchange.getRequestURI().toString().matches(requestPath());

    Response response;
    if (matchingMethod && matchingPath) {
      try {
        response = response(exchange);
      } catch (Exception e) {
        logger.error("http handler error", e);
        response = Response.internalError();
      }
    } else if (!matchingPath) {
      response = Response.notFound();
    } else {
      response = Response.badRequest();
    }

    write(exchange, response.statusCode(), response.body(), response.contentType());
  }

  /// Returns the accepted HTTP method.
  ///
  /// @return accepted HTTP method
  protected abstract String requestMethod();

  /// Returns the regular expression matched against the request URI.
  ///
  /// @return accepted request-path pattern
  protected abstract String requestPath();

  /// Builds a response for a matching request.
  ///
  /// @param exchange current HTTP exchange
  /// @return response to send
  /// @throws Exception if the response cannot be built
  protected abstract Response response(HttpExchange exchange) throws Exception;

  private void write(HttpExchange exchange, int statusCode, byte[] body, String contentType) {
    try (exchange) {
      var length = body.length == 0 ? -1 : body.length;
      exchange.getResponseHeaders().set("Content-Type", contentType);
      exchange.sendResponseHeaders(statusCode, length);
      if (length > 0) {
        try (var outputStream = exchange.getResponseBody()) {
          outputStream.write(body);
        }
      }
    } catch (IOException e) {
      // IOException is somewhat normal, I don't need error-level logs for it.
      logger.debug("http exchange i/o error", e);
    } catch (Exception e) {
      logger.error("http exchange error", e);
    }
  }

  /// Represents an HTTP response produced by a handler.
  protected static class Response {
    private final String contentType;
    private final byte[] body;
    private final int statusCode;

    private Response(String contentType, byte[] body, int statusCode) {
      this.contentType = contentType;
      this.body = body;
      this.statusCode = statusCode;
    }

    /// Creates a response from a UTF-8 string body.
    ///
    /// @param contentType response media type
    /// @param body response body
    /// @param statusCode HTTP status code
    /// @return response
    protected static Response of(String contentType, String body, int statusCode) {
      return new Response(contentType, body.getBytes(StandardCharsets.UTF_8), statusCode);
    }

    /// Creates a response from a byte body.
    ///
    /// @param contentType response media type
    /// @param body response body
    /// @param statusCode HTTP status code
    /// @return response
    protected static Response of(String contentType, byte[] body, int statusCode) {
      return new Response(contentType, body, statusCode);
    }

    /// Creates a 400 Bad Request response.
    ///
    /// @return bad-request response
    protected static Response badRequest() {
      var body = "Bad request.".getBytes(StandardCharsets.UTF_8);

      return new Response(CONTENT_TYPE_TEXT_PLAIN, body, 400);
    }

    /// Creates a 404 Not Found response.
    ///
    /// @return not-found response
    protected static Response notFound() {
      var body = "This page does not exist.".getBytes(StandardCharsets.UTF_8);

      return new Response(CONTENT_TYPE_TEXT_PLAIN, body, 404);
    }

    /// Creates a 500 Internal Server Error response.
    ///
    /// @return internal-error response
    protected static Response internalError() {
      var body = "Internal server error.".getBytes(StandardCharsets.UTF_8);

      return new Response(CONTENT_TYPE_TEXT_PLAIN, body, 500);
    }

    /// Returns response media type.
    ///
    /// @return response media type
    protected String contentType() {
      return contentType;
    }

    /// Returns response body.
    ///
    /// @return response body
    protected byte[] body() {
      return body;
    }

    /// Returns HTTP status code.
    ///
    /// @return HTTP status code
    protected int statusCode() {
      return statusCode;
    }
  }
}
