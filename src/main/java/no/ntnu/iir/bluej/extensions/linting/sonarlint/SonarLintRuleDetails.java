package no.ntnu.iir.bluej.extensions.linting.sonarlint;

import no.ntnu.iir.bluej.extensions.linting.sonarlint.checker.CheckerService;
import no.ntnu.iir.bluej.extensions.linting.sonarlint.util.RuleAttributes;
import no.ntnu.iir.bluej.extensions.linting.sonarlint.util.StringUtils;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.rules.RuleDefinitionDto;

/**
 * Represents an adapter for SonarLint rule details.
 */
public class SonarLintRuleDetails {
  private RuleDefinitionDto definition;
  private CheckerService checkerService;
  private boolean enabled;

  /**
   * Instantiates new rule details.
   *
   * @param definition the SonarLint rule definition
   * @param checkerService the CheckerService to fetch the rule description from
   * @param enabled whether the rule is enabled
   */
  public SonarLintRuleDetails(
      RuleDefinitionDto definition,
      CheckerService checkerService,
      boolean enabled
  ) {
    this.definition = definition;
    this.checkerService = checkerService;
    this.enabled = enabled;
  }

  public String getKey() {
    return this.definition.getKey();
  }

  public String getName() {
    return this.definition.getName();
  }

  public String getType() {
    return RuleAttributes.typeOf(this.definition.getSoftwareImpacts());
  }

  public String getSeverity() {
    return RuleAttributes.severityOf(this.definition.getSoftwareImpacts());
  }

  public boolean isEnabled() {
    return enabled;
  }

  public String getHtmlDescription() {
    return this.checkerService.getHtmlDescription(this.getKey());
  }

  /**
   * Builds a String that is designed to be used for Regex matching across all fields of the type.
   *
   * @return a String containing all fields joined together represented as a String
   */
  public String getSearchableString() {
    String[] formatElements = {
      StringUtils.constantToReadable(this.getType()),
      StringUtils.constantToReadable(this.getSeverity()),
      this.getKey(),
      this.isEnabled() ? "Yes" : "No",
      this.getName()
    };

    return String.join(" ", formatElements);
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }
}
