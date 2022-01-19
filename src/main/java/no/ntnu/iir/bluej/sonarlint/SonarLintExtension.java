package no.ntnu.iir.bluej.sonarlint;

import bluej.extensions2.BlueJ;
import bluej.extensions2.Extension;
import no.ntnu.iir.bluej.extensions.linting.core.handlers.FilesChangeHandler;
import no.ntnu.iir.bluej.extensions.linting.core.handlers.PackageEventHandler;
import no.ntnu.iir.bluej.extensions.linting.core.ui.AuditWindow;
import no.ntnu.iir.bluej.extensions.linting.core.violations.RuleDefinition;
import no.ntnu.iir.bluej.extensions.linting.core.violations.ViolationManager;
import no.ntnu.iir.bluej.sonarlint.checker.CheckerListener;
import no.ntnu.iir.bluej.sonarlint.checker.CheckerService;
import no.ntnu.iir.bluej.sonarlint.util.SonarLintIconMapper;

public class SonarLintExtension extends Extension {
  
  @Override
  public void startup(BlueJ blueJ) {
    RuleDefinition.setIconMapper(new SonarLintIconMapper());
    ViolationManager violationManager = new ViolationManager();
    CheckerService checkerService = new CheckerService(violationManager);
    CheckerListener checkerListener = new CheckerListener(violationManager, checkerService);
    checkerService.setListener(checkerListener);
    AuditWindow.setTitlePrefix(this.getName());
    
    PackageEventHandler packageEventHandler = new PackageEventHandler(
        violationManager, 
        checkerService
    );

    blueJ.addPackageListener(packageEventHandler);
    blueJ.addClassListener(new FilesChangeHandler(violationManager, checkerService));
    blueJ.setPreferenceGenerator(new SonarLintProperties(blueJ, checkerService, violationManager));
  }

  @Override
  public boolean isCompatible() {
    return true;
  }

  @Override
  public String getVersion() {
    return this.getClass().getPackage().getImplementationVersion();
  }

  @Override
  public String getName() {
    return this.getClass().getPackage().getImplementationTitle();
  }

  @Override
  public String getDescription() {
    return String.join(
      "", // delimiter
      "SonarLint for BlueJ."
    );
  }
  
}
