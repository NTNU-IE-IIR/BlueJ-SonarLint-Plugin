package no.ntnu.iir.bluej.extensions.linting.sonarlint.util;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.rules.ImpactDto;

/**
 * Utility class for deriving the legacy rule type (BUG, VULNERABILITY, CODE_SMELL)
 * and severity (BLOCKER, CRITICAL, MAJOR, MINOR, INFO) of a rule from its software impacts.
 * SonarLint only describes rules by their impacts, while this extension shows the legacy
 * type and severity. The mapping is the one used by SonarQube.
 */
public class RuleAttributes {
  private static final String DEFAULT_TYPE = "CODE_SMELL";
  private static final String DEFAULT_SEVERITY = "MAJOR";

  protected RuleAttributes() {}

  /**
   * Returns the legacy type matching the most severe impact.
   *
   * @param impacts the software impacts of a rule
   * @return the legacy type, e.g. "BUG"
   */
  public static String typeOf(List<ImpactDto> impacts) {
    return mostSevere(impacts)
      .map(impact -> switch (impact.getSoftwareQuality()) {
        case RELIABILITY -> "BUG";
        case SECURITY -> "VULNERABILITY";
        case MAINTAINABILITY -> DEFAULT_TYPE;
      })
      .orElse(DEFAULT_TYPE);
  }

  /**
   * Returns the legacy severity matching the most severe impact.
   *
   * @param impacts the software impacts of a rule
   * @return the legacy severity, e.g. "MAJOR"
   */
  public static String severityOf(List<ImpactDto> impacts) {
    return mostSevere(impacts)
      .map(impact -> switch (impact.getImpactSeverity()) {
        case BLOCKER -> "BLOCKER";
        case HIGH -> "CRITICAL";
        case MEDIUM -> DEFAULT_SEVERITY;
        case LOW -> "MINOR";
        case INFO -> "INFO";
      })
      .orElse(DEFAULT_SEVERITY);
  }

  private static Optional<ImpactDto> mostSevere(List<ImpactDto> impacts) {
    if (impacts == null) {
      return Optional.empty();
    }
    return impacts.stream().max(Comparator.comparing(ImpactDto::getImpactSeverity));
  }
}
