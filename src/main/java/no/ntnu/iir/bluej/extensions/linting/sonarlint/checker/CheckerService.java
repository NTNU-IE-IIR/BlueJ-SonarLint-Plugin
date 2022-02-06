package no.ntnu.iir.bluej.extensions.linting.sonarlint.checker;

import bluej.extensions2.BPackage;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import no.ntnu.iir.bluej.extensions.linting.core.checker.ICheckerService;
import no.ntnu.iir.bluej.extensions.linting.core.violations.ViolationManager;
import org.sonarsource.sonarlint.core.StandaloneSonarLintEngineImpl;
import org.sonarsource.sonarlint.core.client.api.common.Language;
import org.sonarsource.sonarlint.core.client.api.common.LogOutput;
import org.sonarsource.sonarlint.core.client.api.common.RuleKey;
import org.sonarsource.sonarlint.core.client.api.common.analysis.ClientInputFile;
import org.sonarsource.sonarlint.core.client.api.common.analysis.IssueListener;
import org.sonarsource.sonarlint.core.client.api.standalone.StandaloneAnalysisConfiguration;
import org.sonarsource.sonarlint.core.client.api.standalone.StandaloneGlobalConfiguration;
import org.sonarsource.sonarlint.core.client.api.standalone.StandaloneRuleDetails;
import org.sonarsource.sonarlint.core.client.api.standalone.StandaloneSonarLintEngine;

/**
 * Represents a CheckerService implementation for SonarLint.
 * Responsible for running checks, and using the set configuration.
 */
public class CheckerService implements ICheckerService {
  private ViolationManager violationManager;
  private StandaloneSonarLintEngine engine;
  private StandaloneGlobalConfiguration globalConfig;
  private LogOutput logOutput;
  private boolean enabled;
  private IssueListener issueListener;
  private Collection<RuleKey> disabledRuleKeys;
      
  /**
   * Instantiates a new CheckerService.
   */
  public CheckerService(ViolationManager violationManager) {
    this.violationManager = violationManager;
    this.logOutput = new CheckerLogOutput();

    this.globalConfig = StandaloneGlobalConfiguration.builder()
        .setLogOutput(logOutput)
        .addEnabledLanguage(Language.JAVA)
        .addPlugin(
          this.getClass()
            .getClassLoader()
            .getResource("plugins/sonar-java-plugin.jar")
        )
        .build();

    this.engine = new StandaloneSonarLintEngineImpl(globalConfig);
    this.enabled = true;
    this.disabledRuleKeys = new ArrayList<>();
  }

  /**
   * Runs checks on a list of files.
   */
  public void checkFiles(List<File> filesToCheck, String charset) {
    if (enabled) {
      try {
        this.violationManager.syncBlueClassMap();
        List<ClientInputFile> clientInputFiles = filesToCheck
            .stream()
            .map(ClientInputFileAdapter::new)
            .collect(Collectors.toList());
    
        Path baseDir = this.findBaseDirPath(filesToCheck.get(0));
  
        if (baseDir != null) {
          StandaloneAnalysisConfiguration configuration = StandaloneAnalysisConfiguration.builder()
              .addInputFiles(clientInputFiles)
              .setBaseDir(baseDir)
              .addExcludedRules(disabledRuleKeys)
              .build();
          
          this.engine.analyze(configuration, this.issueListener, this.logOutput, null);
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
        this.violationManager.syncBlueClassMap();
        Path baseDir = this.findBaseDirPath(fileToCheck);
        if (baseDir != null) {
          StandaloneAnalysisConfiguration configuration = StandaloneAnalysisConfiguration.builder()
              .addInputFile(new ClientInputFileAdapter(fileToCheck))
              .setBaseDir(baseDir)
              .build();
      
          this.engine.analyze(configuration, this.issueListener, this.logOutput, null);
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
   * Returns keys to all rules available in the SonarLint Engine.
   * 
   * @return keys to all rules available in the SonarLint Engine
   */
  public List<String> getRuleKeys() {
    return this.engine.getAllRuleDetails()
      .stream()
      .map(StandaloneRuleDetails::getKey)
      .collect(Collectors.toList());
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
   * @param ruleKeys a collection of RuleKeys to mark as disabled/exclude from checking
   */
  public void setDisabledRules(Collection<RuleKey> ruleKeys) {
    this.disabledRuleKeys = ruleKeys;
  }

  /**
   * Returns rule details for the rules in the CheckerService.
   * 
   * @return rule details for the rules in the CheckerService
   */
  public Collection<StandaloneRuleDetails> getRuleDetails() {
    return this.engine.getAllRuleDetails();
  }

  /**
   * Returns the rule details for a given rule key.
   * 
   * @param ruleKey the rule key to find rule details from 
   * 
   * @return the rule details for the given rule key, if any
   */
  public Optional<StandaloneRuleDetails> getRuleDetails(String ruleKey) {
    return this.engine.getRuleDetails(ruleKey);
  }

  /**
   * Sets the Issuelistener for this CheckerService.
   * 
   * @param listener the IssueListener to listen to this service
   */
  public void setListener(IssueListener listener) {
    this.issueListener = listener;
  }
}
