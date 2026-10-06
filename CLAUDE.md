# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

`sonarlint4bluej` is a BlueJ IDE extension (JavaFX + BlueJ Extensions2 API) that analyzes the Java
classes of open BlueJ projects with [SonarLint](https://github.com/SonarSource/sonarlint-core) and its
Java analyzer (sonar-java), shows the issues in an overview window, and lets users disable rules in
BlueJ's Preferences.

Most of the UI and violation tracking (overview window, `ViolationManager`, package/class event
handlers, `Violation`/`RuleDefinition`) lives in **BlueJ-Linting-Core**
(`no.ntnu.iir.bluej.extensions.linting.core.*`, from JitPack, source usually checked out next to this
repo in `../BlueJ-Linting-Core`). The sibling `../BlueJ-Checkstyle-Plugin` is built on the same core.
When something isn't in this repo's `src/`, it's almost certainly in the core library.

`docs/ARCHITECTURE.md` describes the architecture with class and sequence diagrams (Mermaid). Keep it
in sync when changing how the pieces fit together.

## Build, test, debug

```bash
mvn -B clean verify                 # compile, test, build the shaded jar
mvn -B test -Dtest=AppTest          # run a single test class
```

- **Maven must run on a JDK 21.** `maven.compiler.release` is 21, and anything older fails with
  `release version 21 not supported`. On the maintainer's Mac the default `JAVA_HOME` is JDK 17, so
  prefix commands with `JAVA_HOME=$(/usr/libexec/java_home -v 21)`.
- Output: `target/sonarlint4bluej-<version>.jar` is the shaded jar that gets installed in BlueJ
  (about 70 MB, with the whole SonarLint backend). `-original.jar` is the unshaded jar.
- The analyzer, `sonar-java-plugin.jar`, is copied into `target/classes/plugins/` by the dependency
  plugin in the `prepare-package` phase. It is **not** on the test classpath, so the backend can't be
  started from unit tests (`mvn test`).
- There is only a placeholder test (`AppTest`, JUnit 4). Behavior has to be checked in BlueJ.
- **Debug in BlueJ (macOS):** `tools/debugInBlueJ.sh [--suspend] [--no-build] [--port <port>]` builds
  with JDK 21, installs the jar into `/Applications/BlueJ.app/Contents/Java/extensions2/` and starts
  BlueJ on its **bundled** runtime with JDWP on `localhost:5005` (attach IntelliJ's "Remote JVM Debug").
  BlueJ must not be running. Windows: `tools/buildAndInstallLocally.ps1` builds and copies the jar into
  `C:\Program Files\BlueJ\lib\extensions2\`.
- BlueJ's debug log: `~/Library/Preferences/org.bluej/bluej-debuglog.txt` (macOS). It gets
  `System.out`/`System.err` only; `java.util.logging` output does not end up there.
- `tools/updateBlueJdeps.ps1` (Windows) / `tools/updateBlueJdeps.sh <version> [installDir]` (macOS)
  install BlueJ's `bluej.jar` into the file-based Maven repo in `lib/` as `bluej:bluej:<version>`. Bump
  the `bluej:bluej` version in `pom.xml` to match.

### Releases (CI-driven, not by hand)

Releases are cut by `.github/workflows/release.yml` (manual dispatch from `develop`, with the release
and next development versions as inputs). In one job it checks that `main` is contained in `develop`
and that the tag doesn't exist, runs `mvn release:clean release:prepare release:perform` (release
commits and the `v<version>` tag are pushed to `develop`, the tag is built in `target/checkout/`),
fast-forwards `main` to the tag, and creates the GitHub Release with
`target/checkout/target/sonarlint4bluej-<version>.jar`. `release:prepare` can be tried locally with
`-DdryRun=true` (then `mvn release:clean`).

- **Never commit directly to `main`**; it only moves forward to release tags. Work goes to `develop`.
- `develop` has no branch protection (on purpose, like the sibling repos): the workflow pushes to it
  with the default `GITHUB_TOKEN`, which can't be given a ruleset bypass.
- `main` is the default branch, but PRs merge into `develop`, so `Closes #N` in a PR doesn't close the
  issue. Close fixed issues by hand after a release.

## Architecture

The big picture (details and diagrams in `docs/ARCHITECTURE.md`):

- **Two-step startup through a child-first class loader.** BlueJ loads extensions parent-first and
  ships older slf4j, Guava, JGit and commons-codec than the SonarLint backend needs. So
  `SonarLintExtension` (the class BlueJ loads) is a thin entry point that creates a
  `ChildFirstClassLoader` over its own jar and calls `SonarLintRuntime` **by reflection**; only
  `java.*`/`javax.*`/`jdk.*`, `bluej.*` and `javafx.*` are shared with BlueJ. Never reference
  `SonarLintRuntime` (or anything it uses) directly from `SonarLintExtension`, or BlueJ's loader loads
  it. Relocating slf4j with the shade plugin is not an alternative: the backend exports `org/slf4j` to
  analyzer plugins by name, and sonar-java uses it.
- **SonarLint runs in-process, through its JSON-RPC API.** sonarlint-core 10+ has no Java analysis
  API. `checker/SonarLintBackend` starts the backend (`BackendJsonRpcLauncher`) and its client
  (`ClientJsonRpcLauncher`) connected by piped streams, and wraps the few calls the extension needs
  (initialize, list rules, rule description, disabled rules, analyze). `checker/BackendClient`
  answers the backend's callbacks (only standalone mode, so most are no-ops). Each BlueJ package
  directory is a configuration scope (id = absolute path); files are announced with
  `didUpdateFileSystem` right before each analysis.
- **Checks are synchronous.** Linting-Core's `PackageEventHandler` (project opened) and
  `FilesChangeHandler` (class state changed) call `CheckerService.checkFiles`/`checkFile`
  (`ICheckerService`), which waits for `analyzeFilesAndTrack`'s raw issues and passes each to
  `CheckerListener`, which turns them into Linting-Core `Violation`s in the `ViolationManager`.
- **Rules have impacts, the UI shows type/severity.** sonarlint-core 11 rule definitions only carry
  software impacts; `util/RuleAttributes` derives the legacy type (BUG/VULNERABILITY/CODE_SMELL) and
  severity (BLOCKER..INFO) from the most severe impact, as SonarQube does. Issues carry their own.
- **Disabled rules** are stored as comma-separated rule keys (`java:S106,...`) in BlueJ's extension
  property `SonarLint.DisabledRules` (`SonarLintProperties`), and sent to the backend with
  `updateStandaloneRulesConfiguration`, which replaces the whole override map.

## Pitfalls (each of these broke the extension in BlueJ while headless tests passed)

- **Test with BlueJ's runtime and libraries, not just a JDK.** BlueJ's bundled runtime
  (`BlueJ.app/Contents/PlugIns/<arch>/Contents/Home`) has no `release` file, and sonar-java's ECJ
  parser fails on every file without it (0 issues, errors only in the backend log).
  `SonarLintBackend.createJdkHomeIfMissing()` builds a JDK home in the work dir and passes it as
  `sonar.java.jdkHome`. For a realistic headless test, run on that runtime with BlueJ's jars from
  `BlueJ.app/Contents/Java` in a parent class loader in front of the extension jar.
- The backend replaces the `java.util.logging` root handlers on startup; `SonarLintBackend` restores
  BlueJ's. `BackendClient.log` prints to `System.err` so problems reach BlueJ's debug log.
- Convert file URIs with `Path.of(uri)`, not `new File(uri)`: on Windows, files on network shares have
  `file://server/share/...` URIs, which `new File` rejects.
- BlueJ does not always call `Extension.terminate()` on quit; the backend's temp work dir
  (`sonarlint4bluej*`) is also deleted by a JVM shutdown hook.
- The backend's shutdown logs noise (`ClosedSelectorException`, `InterruptedIOException`); harmless.

## Key constraints

- **Java 21 / BlueJ 6** (Extensions API 3.4; `isCompatible()` accepts 3.2+). BlueJ 5.x (Java 17) can't
  load the jar. JavaFX (`javafx.version`) is pinned to what BlueJ 6 bundles (23.0.2; 24+ needs JDK 22+).
  JavaFX and `bluej:bluej` are `provided` and excluded from the shaded jar.
- **sonarlint-core is pinned at 11.10**: 12.0.x is on Maven Central, but the modules it depends on at
  the same version (`sonarlint-commons`, `sonarlint-rpc-protocol`, ...) are not. Before bumping, check
  that `sonarlint-rpc-protocol` exists at the new version. Check the sonar-java version
  (`sonarlint-java-plugin.version`) against the backend too.
- `bluej:bluej` isn't on Maven Central; it's resolved from the file-based repo in `lib/`
  (`local_repository` in the pom).
- BlueJ-Linting-Core comes from JitPack (`com.github.NTNU-IE-IIR:bluej-linting-core`), built from a
  Git tag. A new core version must be tagged on GitHub before the pom can use it; a version only in
  `~/.m2` builds locally but fails in CI.
- The shade plugin's transformers merge `META-INF/services` and Spring's metadata files; the backend
  doesn't start without them.
