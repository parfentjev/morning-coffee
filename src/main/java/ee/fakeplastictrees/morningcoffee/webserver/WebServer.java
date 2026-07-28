package ee.fakeplastictrees.morningcoffee.webserver;

import com.sun.net.httpserver.HttpServer;
import ee.fakeplastictrees.morningcoffee.Config;
import ee.fakeplastictrees.morningcoffee.repository.Repository;
import ee.fakeplastictrees.morningcoffee.webserver.handler.HandlerManager;
import ee.fakeplastictrees.morningcoffee.webserver.render.TemplateException;
import ee.fakeplastictrees.morningcoffee.webserver.render.TemplateService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WebServer {
  private final Logger logger = LogManager.getLogger();

  private final Config.WebServer config;
  private final Repository repository;
  private final ExecutorService requestExecutor;

  private HttpServer server;

  public WebServer(Config.WebServer config, Repository repository) {
    this.config = config;
    this.repository = repository;
    this.requestExecutor = Executors.newVirtualThreadPerTaskExecutor();
  }

  public void start() throws WebServerException, TemplateException {
    Runtime.getRuntime().addShutdownHook(new Thread(this::stop));

    var templateService = TemplateService.init();

    server = createHttpServer(config.port());
    HandlerManager.registerHandlers(config, server, templateService, repository);
    server.setExecutor(requestExecutor);
    server.start();

    logger.info("ready to handle requests");
  }

  private HttpServer createHttpServer(int port) throws WebServerException {
    try {
      return HttpServer.create(new InetSocketAddress(port), 0);
    } catch (IllegalArgumentException e) {
      var message = "invalid port number: %d".formatted(port);
      throw new WebServerException(message, e);
    } catch (BindException e) {
      var message = "failed to bind to port: %d".formatted(port);
      throw new WebServerException(message, e);
    } catch (IOException e) {
      var message = "i/o error";
      throw new WebServerException(message, e);
    }
  }

  private void stop() {
    logger.info("shutting down");
    if (server != null) {
      server.stop(5);
    }

    requestExecutor.shutdownNow();
  }
}
