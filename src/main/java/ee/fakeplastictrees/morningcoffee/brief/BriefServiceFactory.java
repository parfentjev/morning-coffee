package ee.fakeplastictrees.morningcoffee.brief;

public class BriefServiceFactory {
  public static BriefService get(Provider provider) {
    return switch (provider) {
      case DUMMY -> new DummyBriefService();
    };
  }

  public enum Provider {
    DUMMY
  }
}
