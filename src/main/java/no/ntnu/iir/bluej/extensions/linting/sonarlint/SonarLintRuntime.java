package no.ntnu.iir.bluej.extensions.linting.sonarlint;

import bluej.extensions2.BlueJ;
import no.ntnu.iir.bluej.extensions.linting.core.handlers.FilesChangeHandler;
import no.ntnu.iir.bluej.extensions.linting.core.handlers.PackageEventHandler;
import no.ntnu.iir.bluej.extensions.linting.core.ui.AuditWindow;
import no.ntnu.iir.bluej.extensions.linting.core.violations.RuleDefinition;
import no.ntnu.iir.bluej.extensions.linting.core.violations.ViolationManager;
import no.ntnu.iir.bluej.extensions.linting.sonarlint.checker.CheckerListener;
import no.ntnu.iir.bluej.extensions.linting.sonarlint.checker.CheckerService;
import no.ntnu.iir.bluej.extensions.linting.sonarlint.util.SonarLintIconMapper;

/**
 * Represents the running SonarLint extension.
 * Loaded by {@link SonarLintExtension} through a {@link ChildFirstClassLoader}, so this class
 * and everything it uses is isolated from the libraries bundled with BlueJ.
 */
public class SonarLintRuntime {
  private CheckerService checkerService;

  /**
   * Starts SonarLint and registers the extension's listeners, preferences and menu with BlueJ.
   *
   * @param blueJ the BlueJ instance
   * @param name the name of the extension
   * @param version the version of the extension
   */
  public void startup(BlueJ blueJ, String name, String version) {
    RuleDefinition.setIconMapper(new SonarLintIconMapper());
    ViolationManager violationManager = new ViolationManager();
    try {
      this.checkerService = new CheckerService(violationManager, version);
    } catch (Exception e) {
      System.err.println("SonarLintExtension: unable to start SonarLint");
      e.printStackTrace();
      return;
    }
    CheckerListener checkerListener = new CheckerListener(violationManager, this.checkerService);
    this.checkerService.setListener(checkerListener);
    AuditWindow.setTitlePrefix(name);

    PackageEventHandler packageEventHandler = new PackageEventHandler(
        violationManager,
        this.checkerService
    );

    blueJ.addPackageListener(packageEventHandler);
    blueJ.addClassListener(new FilesChangeHandler(violationManager, this.checkerService));
    blueJ.setPreferenceGenerator(new SonarLintProperties(blueJ, this.checkerService, violationManager));
    blueJ.setMenuGenerator(new SonarLintMenuBuilder(packageEventHandler));
  }

  /**
   * Stops SonarLint.
   */
  public void terminate() {
    if (this.checkerService != null) {
      this.checkerService.shutdown();
    }
  }
}
