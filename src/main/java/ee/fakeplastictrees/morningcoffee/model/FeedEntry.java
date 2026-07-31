package ee.fakeplastictrees.morningcoffee.model;

import java.time.OffsetDateTime;
import java.util.UUID;

/// Represents a feed entry fetched for persistence.
public class FeedEntry {
  private String externalId;
  private OffsetDateTime publishedAt;
  private UUID feedId;
  private String title;
  private String link;

  public String getExternalId() {
    return externalId;
  }

  public void setExternalId(String externalId) {
    this.externalId = externalId;
  }

  public OffsetDateTime getPublishedAt() {
    return publishedAt;
  }

  public void setPublishedAt(OffsetDateTime publishedAt) {
    this.publishedAt = publishedAt;
  }

  public UUID getFeedId() {
    return feedId;
  }

  public void setFeedId(UUID feedId) {
    this.feedId = feedId;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getLink() {
    return link;
  }

  public void setLink(String link) {
    this.link = link;
  }
}
