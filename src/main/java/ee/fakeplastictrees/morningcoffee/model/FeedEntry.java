package ee.fakeplastictrees.morningcoffee.model;

import java.time.Instant;
import java.util.UUID;

public class FeedEntry {
  private UUID id;
  private String extrnalId;
  private Instant publishedAt;
  private UUID feedId;
  private String title;
  private String link;

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public String getExtrnalId() {
    return extrnalId;
  }

  public void setExtrnalId(String extrnalId) {
    this.extrnalId = extrnalId;
  }

  public Instant getPublishedAt() {
    return publishedAt;
  }

  public void setPublishedAt(Instant publishedAt) {
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
