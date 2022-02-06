package no.ntnu.iir.bluej.extensions.linting.sonarlint.checker;

import bluej.extensions2.BClass;
import bluej.extensions2.editor.TextLocation;
import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import no.ntnu.iir.bluej.extensions.linting.core.violations.RuleDefinition;
import no.ntnu.iir.bluej.extensions.linting.core.violations.Violation;
import no.ntnu.iir.bluej.extensions.linting.core.violations.ViolationManager;
import no.ntnu.iir.bluej.extensions.linting.sonarlint.util.StringUtils;
import org.sonarsource.sonarlint.core.client.api.common.analysis.ClientInputFile;
import org.sonarsource.sonarlint.core.client.api.common.analysis.Issue;
import org.sonarsource.sonarlint.core.client.api.common.analysis.IssueListener;
import org.sonarsource.sonarlint.core.client.api.standalone.StandaloneRuleDetails;

/**
 * Represents a CheckerListener. 
 * Is a Listener of the CheckerService and handles adding violations to the ViolationManager
 * when they occur. Also handles necessary formatting/preprocessing if necessary.
 */
public class CheckerListener implements IssueListener {
  private ViolationManager violationManager;
  private CheckerService checkerService;

  public CheckerListener(ViolationManager violationManager, CheckerService checkerService) {
    this.violationManager = violationManager;
    this.checkerService = checkerService;
  }

  /**
   * Handles issues from the CheckerService.
   */
  @Override
  public void handle(Issue issue) {
    ClientInputFile inputFile = issue.getInputFile();

    if (inputFile != null) {
      URI fileUri = inputFile.uri();
      File file = new File(fileUri);
      String fileName = file.getPath();
      BClass sourceBClass = this.violationManager.getBlueClass(file.getPath());

      Optional<StandaloneRuleDetails> ruleDetails = this.checkerService.getRuleDetails(
          issue.getRuleKey()
      );
  
      RuleDefinition ruleDefinition = null;
  
      if (ruleDetails.isPresent()) {
        ruleDefinition = new RuleDefinition(
          issue.getRuleName(), 
          issue.getRuleKey(), 
          StringUtils.formatHtmlDescription(ruleDetails.get()), 
          issue.getSeverity(), 
          issue.getType()
        );
      }
  
      int startLine = 0;
      int startLineOffset = 0;
  
      // turns out some of the issues don't have startLine and startLineOffset defined
      // fall back to 1 for these, instantiating TextLocation with 0-values will cause problems
      try {
        startLine = issue.getStartLine();
        startLineOffset = issue.getStartLineOffset();
      } catch (Exception e) {
        startLine = 1;
        startLineOffset = 1;
      }
  
      Violation violation = new Violation(
          issue.getMessage(),
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
