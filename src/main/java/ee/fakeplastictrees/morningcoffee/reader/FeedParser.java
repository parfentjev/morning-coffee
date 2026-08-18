package ee.fakeplastictrees.morningcoffee.reader;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import ee.fakeplastictrees.morningcoffee.model.FeedEntry;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

class FeedParser {
  private static final Logger logger = LogManager.getLogger();

  public List<FeedEntry> parseResponse(UUID feedId, byte[] response) throws FeedParserException {
    try (var reader = new XmlReader(new ByteArrayInputStream(response))) {
      var feed = new SyndFeedInput().build(reader);
      if (feed.getEntries() == null || feed.getEntries().isEmpty()) {
        return List.of();
      }

      return mapEntries(feedId, feed.getEntries());
    } catch (IOException e) {
      var message = "failed to read response body";
      throw new FeedParserException(message, e);
    } catch (IllegalArgumentException e) {
      var message = "illegal argument in feed body";
      throw new FeedParserException(message, e);
    } catch (FeedException e) {
      var message = "failed to parse feed";
      throw new FeedParserException(message, e);
    }
  }

  private List<FeedEntry> mapEntries(UUID feedId, List<SyndEntry> entries) {
    var output = new ArrayList<FeedEntry>();
    for (var syndEntry : entries) {
      try {
        var mappedEntry = FeedEntryMapper.syndEntryToFeedEntry(feedId, syndEntry);
        output.add(mappedEntry);
      } catch (IllegalArgumentException e) {
        logger.info("failed to map feed entry, skipping", e);
        continue;
      }
    }

    return output;
  }
}
