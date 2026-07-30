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

class FeedParser {
  public List<FeedEntry> parseResponse(byte[] response) throws FeedParserException {
    try (var reader = new XmlReader(new ByteArrayInputStream(response))) {
      var feed = new SyndFeedInput().build(reader);
      if (feed.getEntries() == null || feed.getEntries().isEmpty()) {
        return List.of();
      }

      return mapEntries(feed.getEntries());
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

  private List<FeedEntry> mapEntries(List<SyndEntry> syndEntries) throws IllegalArgumentException {
    var output = new ArrayList<FeedEntry>();
    for (var syndEntry : syndEntries) {
      output.add(FeedEntryMapper.syndEntryToFeedEntry(syndEntry));
    }

    return output;
  }
}
