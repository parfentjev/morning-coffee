package ee.fakeplastictrees.morningcoffee.webserver;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import ee.fakeplastictrees.morningcoffee.brief.BriefService;
import ee.fakeplastictrees.morningcoffee.common.Config;
import ee.fakeplastictrees.morningcoffee.feed.FeedService;
import ee.fakeplastictrees.morningcoffee.webserver.handler.RequestRouter;
import ee.fakeplastictrees.morningcoffee.webserver.render.TemplateService;
import java.io.Closeable;
import java.io.IOException;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/// Serves feed entries over HTTP.
public class WebServer implements Closeable {
  private static final Logger logger = LogManager.getLogger();

  private final Config.WebServer config;
  private final FeedService feedService;
  private final BriefService briefService;
  private final TemplateService templateService;
  private final HttpHandler staticHandler;
  private final ExecutorService requestExecutor;

  private HttpServer server;

  /// Creates a web server.
  ///
  /// @param config web server configuration
  /// @param feedService service providing feed entries
  /// @param briefService service that supplies briefs for the brief page
  /// @param templateService service that manages dynamic HTML templates
  /// @param staticHandler handler that serves static resources
  public WebServer(
      Config.WebServer config,
      FeedService feedService,
      BriefService briefService,
      TemplateService templateService,
      HttpHandler staticHandler) {
    this.config = config;
    this.feedService = feedService;
    this.briefService = briefService;
    this.templateService = templateService;
    this.staticHandler = staticHandler;
    this.requestExecutor = Executors.newVirtualThreadPerTaskExecutor();
  }

  /// Starts accepting HTTP requests.
  ///
  /// @throws WebServerException if the HTTP server cannot be created
  public void start() throws WebServerException {
    server = createHttpServer(config.serverHostname(), config.serverPort());
    RequestRouter.registerHandlers(
        config, feedService, server, briefService, templateService, staticHandler);
    server.setExecutor(requestExecutor);
    server.start();

    logger.info("ready to handle requests");
  }

  private HttpServer createHttpServer(String hostname, int port) throws WebServerException {
    try {
      return HttpServer.create(new InetSocketAddress(hostname, port), 0);
    } catch (IllegalArgumentException e) {
      var message = "invalid serverPort number: %d".formatted(port);
      throw new WebServerException(message, e);
    } catch (BindException e) {
      var message = "failed to bind to serverPort: %d".formatted(port);
      throw new WebServerException(message, e);
    } catch (IOException e) {
      var message = "i/o error";
      throw new WebServerException(message, e);
    }
  }

  @Override
  public void close() {
    try {
      logger.info("shutting down");
      if (server != null) {
        server.stop(5);
      }

      requestExecutor.shutdownNow();
      if (requestExecutor.awaitTermination(5, TimeUnit.SECONDS) == false) {
        logger.warn("failed to stop requestExecutor in time");
      }
    } catch (InterruptedException e) {
      logger.warn("interrupted while awaiting termination", e);
      Thread.currentThread().interrupt();
    }
  }
}
