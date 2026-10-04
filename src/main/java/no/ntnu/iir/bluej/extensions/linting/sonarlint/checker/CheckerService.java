package no.ntnu.iir.bluej.extensions.linting.sonarlint.checker;

import bluej.extensions2.BPackage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import no.ntnu.iir.bluej.extensions.linting.core.checker.ICheckerService;
import no.ntnu.iir.bluej.extensions.linting.core.violations.ViolationManager;
import no.ntnu.iir.bluej.extensions.linting.sonarlint.util.RuleAttributes;
import no.ntnu.iir.bluej.extensions.linting.sonarlint.util.StringUtils;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.rules.GetStandaloneRuleDescriptionResponse;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.rules.RuleDefinitionDto;

/**
 * Represents a CheckerService implementation for SonarLint.
 * Responsible for running checks, and using the set configuration.
 */
public class CheckerService implements ICheckerService {
  private ViolationManager violationManager;
  private SonarLintBackend backend;
  private Map<String, RuleDefinitionDto> rulesByKey;
  private Map<String, String> htmlDescriptionsByKey;
  private boolean enabled;
  private CheckerListener listener;

  /**
   * Instantiates a new CheckerService, starting the SonarLint backend.
   *
   * @param violationManager the ViolationManager to report violations to
   * @param version the version of this extension
   * @throws IOException if the SonarLint backend could not be started
   */
  public CheckerService(ViolationManager violationManager, String version) throws IOException {
    this.violationManager = violationManager;
    this.backend = new SonarLintBackend(version);
    this.rulesByKey = this.backend.listRules();
    this.htmlDescriptionsByKey = new ConcurrentHashMap<>();
    this.enabled = true;
  }

  /**
   * Runs checks on a list of files.
   */
  public void checkFiles(List<File> filesToCheck, String charset) {
    if (enabled) {
      try {
        this.violationManager.syncBlueClassMap();
        Path baseDir = this.findBaseDirPath(filesToCheck.get(0));

        if (baseDir != null) {
          this.backend.analyze(baseDir, filesToCheck).forEach(this.listener::handle);
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  /**
   * Runs checks on a single file.
   */
  public void checkFile(File fileToCheck, String charset) {
    if (enabled) {
      try {
        // When the engine analyzes a file,
        // it does not give us a complete list of violations for the file.
        // Due to this, we have to clear the violations before each check.
        this.violationManager.setViolations(fileToCheck.getPath(), new ArrayList<>());

        this.violationManager.syncBlueClassMap();
        Path baseDir = this.findBaseDirPath(fileToCheck);
        if (baseDir != null) {
          this.backend.analyze(baseDir, List.of(fileToCheck)).forEach(this.listener::handle);
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  /**
   * Finds the base directory from the mapped package.
   *
   * @param file the file to find the base directory from
   * @return the found base directory - or null if none was found
   */
  private Path findBaseDirPath(File file) {
    Path baseDir = null;

    String path = file.toString();
    Iterator<BPackage> packageIterator = this.violationManager.getBluePackages().iterator();

    // find the base directory for the files being checked
    while (baseDir == null && packageIterator.hasNext()) {
      try {
        File currentDir = packageIterator.next().getDir();
        if (path.startsWith(currentDir.toString())) {
          baseDir = currentDir.toPath();
        }
      } catch (Exception e) {
        // should never happen - bluePackages are removed from violationManager as they are closed.
      }
    }

    return baseDir;
  }

  /**
   * Enables the CheckerService.
   */
  @Override
  public void enable() {
    this.enabled = true;
  }

  /**
   * Disables the CheckerService.
   * Should prevent the CheckerService from running further checks.
   */
  @Override
  public void disable() {
    this.enabled = false;
  }

  /**
   * Returns a boolean representing the CheckerServices state.
   *
   * @return a boolean representing the CheckerServices state
   */
  @Override
  public boolean isEnabled() {
    return this.enabled;
  }

  /**
   * Sets the disabled rules for this CheckerService.
   *
   * @param ruleKeys a collection of rule keys (e.g. "java:S100") to exclude from checking
   */
  public void setDisabledRules(Collection<String> ruleKeys) {
    this.backend.setDisabledRules(ruleKeys);
  }

  /**
   * Returns the definitions of all rules in the CheckerService.
   *
   * @return the definitions of all rules in the CheckerService
   */
  public Collection<RuleDefinitionDto> getRuleDefinitions() {
    return this.rulesByKey.values();
  }

  /**
   * Returns the definition of a given rule key.
   *
   * @param ruleKey the rule key to find the definition of
   *
   * @return the definition of the given rule key, if any
   */
  public Optional<RuleDefinitionDto> getRuleDefinition(String ruleKey) {
    return Optional.ofNullable(this.rulesByKey.get(ruleKey));
  }

  /**
   * Returns the formatted HTML description of a rule, including its name, type and severity.
   *
   * @param ruleKey the rule key to get the description of
   * @return the formatted HTML description of the rule
   */
  public String getHtmlDescription(String ruleKey) {
    return this.htmlDescriptionsByKey.computeIfAbsent(ruleKey, key -> {
      GetStandaloneRuleDescriptionResponse response = this.backend.getRuleDescription(key);
      RuleDefinitionDto definition = response.getRuleDefinition();
      return StringUtils.formatHtmlDescription(
        definition.getName(),
        definition.getKey(),
        RuleAttributes.typeOf(definition.getSoftwareImpacts()),
        RuleAttributes.severityOf(definition.getSoftwareImpacts()),
        StringUtils.descriptionToHtml(response.getDescription())
      );
    });
  }

  /**
   * Sets the CheckerListener for this CheckerService.
   *
   * @param listener the CheckerListener to receive issues found by this service
   */
  public void setListener(CheckerListener listener) {
    this.listener = listener;
  }

  /**
   * Stops the SonarLint backend. The CheckerService can not be used afterwards.
   */
  public void shutdown() {
    this.enabled = false;
    this.backend.shutdown();
  }
}
