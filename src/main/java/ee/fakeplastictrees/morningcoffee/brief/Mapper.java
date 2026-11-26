package ee.fakeplastictrees.morningcoffee.brief;

class Mapper {
  /// Maps a stored Brief into a DTO.
  ///
  /// @param brief brief to expose through the service
  /// @return the corresponding brief DTO
  static BriefDto briefToBriefDto(Brief brief) {
    return new BriefDto(brief.publishedAt(), brief.body());
  }
}
