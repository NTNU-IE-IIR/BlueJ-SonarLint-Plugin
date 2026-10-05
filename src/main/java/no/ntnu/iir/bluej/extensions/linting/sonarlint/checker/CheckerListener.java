package no.ntnu.iir.bluej.extensions.linting.sonarlint.checker;

import bluej.extensions2.BClass;
import bluej.extensions2.editor.TextLocation;
import java.io.File;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import no.ntnu.iir.bluej.extensions.linting.core.violations.RuleDefinition;
import no.ntnu.iir.bluej.extensions.linting.core.violations.Violation;
import no.ntnu.iir.bluej.extensions.linting.core.violations.ViolationManager;
import no.ntnu.iir.bluej.extensions.linting.sonarlint.util.RuleAttributes;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.rules.RuleDefinitionDto;
import org.sonarsource.sonarlint.core.rpc.protocol.client.analysis.RawIssueDto;
import org.sonarsource.sonarlint.core.rpc.protocol.common.RuleType;
import org.sonarsource.sonarlint.core.rpc.protocol.common.TextRangeDto;

/**
 * Represents a CheckerListener.
 * Is a Listener of the CheckerService and handles adding violations to the ViolationManager
 * when they occur. Also handles necessary formatting/preprocessing if necessary.
 */
public class CheckerListener {
  private ViolationManager violationManager;
  private CheckerService checkerService;

  public CheckerListener(ViolationManager violationManager, CheckerService checkerService) {
    this.violationManager = violationManager;
    this.checkerService = checkerService;
  }

  /**
   * Handles issues from the CheckerService.
   */
  public void handle(RawIssueDto issue) {
    URI fileUri = issue.getFileUri();

    // Security hotspots are not issues in the code, but code for a human to review
    if (fileUri != null && issue.getType() != RuleType.SECURITY_HOTSPOT) {
      // Path.of, unlike new File(URI), accepts the URIs of files on network shares on Windows
      // (e.g. file://server/share/X.java for \\server\share\X.java)
      File file = Path.of(fileUri).toFile();
      String fileName = file.getPath();
      BClass sourceBClass = this.violationManager.getBlueClass(file.getPath());

      Optional<RuleDefinitionDto> ruleDetails = this.checkerService.getRuleDefinition(
          issue.getRuleKey()
      );

      RuleDefinition ruleDefinition = null;

      if (ruleDetails.isPresent()) {
        ruleDefinition = new RuleDefinition(
          ruleDetails.get().getName(),
          issue.getRuleKey(),
          this.checkerService.getHtmlDescription(issue.getRuleKey()),
          issue.getSeverity() != null
              ? issue.getSeverity().name()
              : RuleAttributes.severityOf(ruleDetails.get().getSoftwareImpacts()),
          issue.getType() != null
              ? issue.getType().name()
              : RuleAttributes.typeOf(ruleDetails.get().getSoftwareImpacts())
        );
      }

      // some of the issues are on the file as a whole, and have no text range.
      // fall back to 1 for these, instantiating TextLocation with 0-values will cause problems
      int startLine = 1;
      int startLineOffset = 1;
      TextRangeDto textRange = issue.getTextRange();
      if (textRange != null) {
        startLine = textRange.getStartLine();
        startLineOffset = textRange.getStartLineOffset();
      }

      Violation violation = new Violation(
          issue.getPrimaryMessage(),
          sourceBClass,
          new TextLocation(startLine, startLineOffset),
          ruleDefinition
      );

      List<Violation> violations = violationManager.getViolations(fileName);
      if (violations != null) {
        violations.add(violation);
        violationManager.setViolations(fileName, violations);
      } else {
        ArrayList<Violation> violationList = new ArrayList<>();
        violationList.add(violation);
        violationManager.addViolations(fileName, violationList);
      }
    }
  }
}
