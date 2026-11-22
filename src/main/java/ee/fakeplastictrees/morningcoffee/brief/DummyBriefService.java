package ee.fakeplastictrees.morningcoffee.brief;

import java.time.OffsetDateTime;
import java.util.List;

class DummyBriefService implements BriefService {
  private static final String LATEST =
"""
## Sub-Heading 1

Some text. And then...

- Woah
- Is this
- A list??

You bet your ass it _is_!

## Sub-Heading 2

Mamma mia.
""";

  @Override
  public void generate(String idempotencyKey, String sourceMaterial) {}

  @Override
  public List<BriefDto> latest(int n) {
    var dto = new BriefDto(OffsetDateTime.now(), LATEST);

    return List.of(dto);
  }
}
