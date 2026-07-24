package ee.fakeplastictrees.morningcoffee.reader;

import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import ee.fakeplastictrees.morningcoffee.Config;
import ee.fakeplastictrees.morningcoffee.Repository;
import ee.fakeplastictrees.morningcoffee.model.FeedEntry;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Reader {
  private final Logger logger = LogManager.getLogger();

  private final Config.Reader config;
  private final ScheduledExecutorService executor;
  private final Repository repository;

  public Reader(Config.Reader config, ScheduledExecutorService executor, Repository repository) {
    this.config = config;
    this.executor = executor;
    this.repository = repository;
  }

  public void run() {
    var interval = config.getPollIntervalSeconds();
    var timeUnit = TimeUnit.SECONDS;

    executor.scheduleWithFixedDelay(this::fetchFeeds, 0, interval, timeUnit);
    logger.info("Scheduled reader to run every {} {}", interval, timeUnit.name());
  }

  private void fetchFeeds() {
    for (var feed : repository.getFeeds()) {
      try {
        var uri = new URI(feed.url());
        var request = HttpRequest.newBuilder().uri(uri).GET().build();
        var client = HttpClient.newHttpClient();
        var response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
        var body = new SyndFeedInput().build(new XmlReader(response.body()));
        saveEntries(body);
      } catch (URISyntaxException e) {
        logger.warn("Feed URL syntax exception", e);
      } catch (IOException | InterruptedException e) {
        logger.warn("IO exception", e);
      } catch (FeedException e) {
        logger.warn("Parse feed error", e);
      }
    }
  }

  private void saveEntries(SyndFeed body) {
    var items = body.getEntries();
    if (items == null || items.isEmpty()) {
      logger.info("Empty feed: {}", body.getUri());
      return;
    }

    for (var item : items) {
      var title = item.getTitle();
      if (title == null || title.isBlank()) {
        return;
      }

      var link = item.getLink();
      if (link == null || link.isBlank()) {
        return;
      }

      var entry = new FeedEntry(title, link);
      repository.saveFeedEntry(entry);
    }
  }
}
