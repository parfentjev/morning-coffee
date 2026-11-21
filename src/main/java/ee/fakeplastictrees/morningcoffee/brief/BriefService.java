package ee.fakeplastictrees.morningcoffee.brief;

import java.util.Optional;

public interface BriefService {
  void generate(String idempotencyKey, String sourceMaterial);

  Optional<BriefDto> latest();
}
