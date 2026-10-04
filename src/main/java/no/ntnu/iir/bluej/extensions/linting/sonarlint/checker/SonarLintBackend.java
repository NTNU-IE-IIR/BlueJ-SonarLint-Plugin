package no.ntnu.iir.bluej.extensions.linting.sonarlint.checker;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.sonarsource.sonarlint.core.rpc.client.ClientJsonRpcLauncher;
import org.sonarsource.sonarlint.core.rpc.impl.BackendJsonRpcLauncher;
import org.sonarsource.sonarlint.core.rpc.protocol.SonarLintRpcServer;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.analysis.AnalyzeFilesAndTrackParams;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.config.binding.BindingConfigurationDto;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.config.scope.ConfigurationScopeDto;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.config.scope.DidAddConfigurationScopesParams;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.file.DidUpdateFileSystemParams;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.initialize.ClientConstantInfoDto;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.initialize.HttpConfigurationDto;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.initialize.InitializeParams;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.initialize.TelemetryClientConstantAttributesDto;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.log.LogLevel;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.rules.GetStandaloneRuleDescriptionParams;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.rules.GetStandaloneRuleDescriptionResponse;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.rules.RuleDefinitionDto;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.rules.StandaloneRuleConfigDto;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.rules.UpdateStandaloneRulesConfigurationParams;
import org.sonarsource.sonarlint.core.rpc.protocol.client.analysis.RawIssueDto;
import org.sonarsource.sonarlint.core.rpc.protocol.common.ClientFileDto;
import org.sonarsource.sonarlint.core.rpc.protocol.common.Language;

/**
 * Represents the SonarLint backend, running in-process.
 * The backend is driven through its JSON-RPC API over a pair of piped streams, the same API
 * the SonarLint IDE plugins use. Each BlueJ package directory is registered as a standalone
 * (unbound) configuration scope, identified by the absolute path of the directory.
 */
public class SonarLintBackend {
  private static final Logger LOGGER = Logger.getLogger(SonarLintBackend.class.getName());
  private static final String PLUGIN_RESOURCE = "plugins/sonar-java-plugin.jar";
  private static final long STARTUP_TIMEOUT_SECONDS = 120;
  private static final long REQUEST_TIMEOUT_SECONDS = 120;

  private final BackendClient client;
  private final ClientJsonRpcLauncher clientLauncher;
  private final SonarLintRpcServer server;
  private final Path workDir;
  private final Set<String> configScopeIds;

  /**
   * Starts and initializes a new SonarLint backend.
   *
   * @param productVersion the version of this extension, reported to the backend
   * @throws IOException if the working directory or the pipes could not be created
   */
  public SonarLintBackend(String productVersion) throws IOException {
    this.workDir = Files.createTempDirectory("sonarlint4bluej");
    this.configScopeIds = ConcurrentHashMap.newKeySet();
    this.client = new BackendClient();

    Path pluginPath = this.extractPlugin();

    PipedInputStream clientToServerIn = new PipedInputStream();
    PipedOutputStream clientToServerOut = new PipedOutputStream(clientToServerIn);
    PipedInputStream serverToClientIn = new PipedInputStream();
    PipedOutputStream serverToClientOut = new PipedOutputStream(serverToClientIn);

    // BlueJ loads extensions in their own class loader. The backend (Spring, lsp4j, Gson) uses the
    // context class loader, and its worker threads inherit it, so it must be ours while starting up.
    this.clientLauncher = withExtensionClassLoader(() -> {
      new BackendJsonRpcLauncher(clientToServerIn, serverToClientOut);
      return new ClientJsonRpcLauncher(serverToClientIn, clientToServerOut, this.client);
    });
    this.server = this.clientLauncher.getServerProxy();

    String version = productVersion != null ? productVersion : "dev";
    InitializeParams params = new InitializeParams(
        new ClientConstantInfoDto("BlueJ", "sonarlint4bluej " + version),
        new TelemetryClientConstantAttributesDto(
            "bluej", "SonarLint for BlueJ", version, "BlueJ", Map.of()
        ),
        HttpConfigurationDto.defaultConfig(),
        null,
        Set.of(),
        this.workDir.resolve("storage"),
        this.workDir.resolve("work"),
        Set.of(pluginPath),
        Map.of(),
        Set.of(Language.JAVA),
        Set.of(),
        Set.of(),
        List.of(),
        List.of(),
        this.workDir.resolve("home").toString(),
        Map.of(),
        false,
        null,
        false,
        null,
        LogLevel.INFO
    );
    await(this.server.initialize(params), STARTUP_TIMEOUT_SECONDS);
  }

  /**
   * Returns all rules available for analysis, keyed by rule key (e.g. "java:S100").
   *
   * @return all rules available for analysis
   */
  public Map<String, RuleDefinitionDto> listRules() {
    return await(
        this.server.getRulesService().listAllStandaloneRulesDefinitions(),
        STARTUP_TIMEOUT_SECONDS
    ).getRulesByKey();
  }

  /**
   * Returns the definition and description of a single rule.
   *
   * @param ruleKey the key of the rule
   * @return the definition and description of the rule
   */
  public GetStandaloneRuleDescriptionResponse getRuleDescription(String ruleKey) {
    return await(
        this.server.getRulesService()
            .getStandaloneRuleDetails(new GetStandaloneRuleDescriptionParams(ruleKey)),
        REQUEST_TIMEOUT_SECONDS
    );
  }

  /**
   * Replaces the set of rules excluded from analysis. All other rules use their default activation.
   *
   * @param disabledRuleKeys keys of the rules to exclude from analysis
   */
  public void setDisabledRules(Collection<String> disabledRuleKeys) {
    Map<String, StandaloneRuleConfigDto> ruleConfig = disabledRuleKeys.stream()
        .distinct()
        .collect(Collectors.toMap(
          ruleKey -> ruleKey,
          ruleKey -> new StandaloneRuleConfigDto(false, Map.of())
        ));
    this.server.getRulesService()
        .updateStandaloneRulesConfiguration(new UpdateStandaloneRulesConfigurationParams(ruleConfig));
  }

  /**
   * Analyzes files from the current content on disk, and waits for the result.
   *
   * @param baseDir the directory of the BlueJ package the files belong to
   * @param files the files to analyze
   * @return the issues found in the files
   */
  public List<RawIssueDto> analyze(Path baseDir, List<File> files) {
    String configScopeId = this.ensureConfigScope(baseDir);

    // (Re-)announce the files, so files created after the scope was added are known to the backend
    List<ClientFileDto> clientFiles = files.stream()
        .map(file -> toClientFileDto(configScopeId, baseDir, file))
        .toList();
    this.server.getFileService()
        .didUpdateFileSystem(new DidUpdateFileSystemParams(List.of(), clientFiles, List.of()));

    List<URI> fileUris = clientFiles.stream().map(ClientFileDto::getUri).toList();
    AnalyzeFilesAndTrackParams params = new AnalyzeFilesAndTrackParams(
        configScopeId, UUID.randomUUID(), fileUris, Map.of(), false
    );
    return await(
        this.server.getAnalysisService().analyzeFilesAndTrack(params),
        REQUEST_TIMEOUT_SECONDS
    ).getRawIssues();
  }

  /**
   * Shuts down the backend and removes its working directory.
   */
  public void shutdown() {
    try {
      this.server.shutdown().get(10, TimeUnit.SECONDS);
    } catch (Exception e) {
      LOGGER.log(Level.WARNING, "Unable to shut down the SonarLint backend cleanly", e);
    }
    try {
      this.clientLauncher.close();
    } catch (Exception e) {
      LOGGER.log(Level.WARNING, "Unable to close the SonarLint client", e);
    }
    deleteRecursively(this.workDir);
  }

  private String ensureConfigScope(Path baseDir) {
    String configScopeId = baseDir.toAbsolutePath().toString();
    if (this.configScopeIds.add(configScopeId)) {
      this.client.addBaseDir(configScopeId, baseDir);
      ConfigurationScopeDto scope = new ConfigurationScopeDto(
          configScopeId,
          null,
          false,
          baseDir.getFileName().toString(),
          new BindingConfigurationDto(null, null, true)
      );
      this.server.getConfigurationService()
          .didAddConfigurationScopes(new DidAddConfigurationScopesParams(List.of(scope)));
    }
    return configScopeId;
  }

  private static ClientFileDto toClientFileDto(String configScopeId, Path baseDir, File file) {
    Path path = file.toPath().toAbsolutePath();
    return new ClientFileDto(
        path.toUri(),
        baseDir.toAbsolutePath().relativize(path),
        configScopeId,
        false,
        "UTF-8",
        path,
        null,
        Language.JAVA,
        true
    );
  }

  /**
   * Copies the bundled analyzer out of the extension jar, as the backend loads plugins from files.
   */
  private Path extractPlugin() throws IOException {
    Path pluginPath = this.workDir.resolve("plugins").resolve("sonar-java-plugin.jar");
    Files.createDirectories(pluginPath.getParent());
    try (InputStream in = this.getClass().getClassLoader().getResourceAsStream(PLUGIN_RESOURCE)) {
      if (in == null) {
        throw new IOException("Missing bundled analyzer: " + PLUGIN_RESOURCE);
      }
      Files.copy(in, pluginPath, StandardCopyOption.REPLACE_EXISTING);
    }
    return pluginPath;
  }

  private static <T> T withExtensionClassLoader(Supplier<T> action) {
    Thread thread = Thread.currentThread();
    ClassLoader previous = thread.getContextClassLoader();
    thread.setContextClassLoader(SonarLintBackend.class.getClassLoader());
    try {
      return action.get();
    } finally {
      thread.setContextClassLoader(previous);
    }
  }

  private static <T> T await(CompletableFuture<T> future, long timeoutSeconds) {
    try {
      return future.get(timeoutSeconds, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Interrupted while waiting for SonarLint", e);
    } catch (Exception e) {
      throw new IllegalStateException("SonarLint request failed", e);
    }
  }

  private static void deleteRecursively(Path dir) {
    try (var paths = Files.walk(dir)) {
      paths.sorted(Comparator.reverseOrder()).forEach(path -> path.toFile().delete());
    } catch (IOException e) {
      LOGGER.log(Level.FINE, "Unable to delete " + dir, e);
    }
  }
}
