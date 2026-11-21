package ee.fakeplastictrees.morningcoffee.brief;

import java.util.Optional;

interface BriefRepository {
  void save(Brief brief);

  Optional<Brief> latest();
}
