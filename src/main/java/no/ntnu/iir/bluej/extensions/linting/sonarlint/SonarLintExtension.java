package no.ntnu.iir.bluej.extensions.linting.sonarlint;

import bluej.extensions2.BlueJ;
import bluej.extensions2.Extension;
import java.net.URL;
import no.ntnu.iir.bluej.extensions.linting.core.handlers.FilesChangeHandler;
import no.ntnu.iir.bluej.extensions.linting.core.handlers.PackageEventHandler;
import no.ntnu.iir.bluej.extensions.linting.core.ui.AuditWindow;
import no.ntnu.iir.bluej.extensions.linting.core.violations.RuleDefinition;
import no.ntnu.iir.bluej.extensions.linting.core.violations.ViolationManager;
import no.ntnu.iir.bluej.extensions.linting.sonarlint.checker.CheckerListener;
import no.ntnu.iir.bluej.extensions.linting.sonarlint.checker.CheckerService;
import no.ntnu.iir.bluej.extensions.linting.sonarlint.util.SonarLintIconMapper;

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
    blueJ.setMenuGenerator(new SonarLintMenuBuilder(packageEventHandler));
  }

  @Override
  public boolean isCompatible() {
    int versionMajor = Extension.getExtensionsAPIVersionMajor();
    int versionMinor = Extension.getExtensionsAPIVersionMinor();
    return (versionMajor == 3 && versionMinor == 2);
  }

  @Override
  public String getVersion() {
    return this.getClass().getPackage().getImplementationVersion();
  }

  @Override
  public URL getURL() {
    try {
      return new URL("https://github.com/NTNU-IE-IIR/BlueJ-SonarLint-Plugin/");
    } catch (Exception e) {
      return null;
    }
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
