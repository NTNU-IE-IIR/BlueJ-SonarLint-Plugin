package no.ntnu.iir.bluej.extensions.linting.sonarlint.checker;

import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import org.sonarsource.sonarlint.core.rpc.client.ConfigScopeNotFoundException;
import org.sonarsource.sonarlint.core.rpc.client.SonarLintCancelChecker;
import org.sonarsource.sonarlint.core.rpc.client.SonarLintRpcClientDelegate;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.config.binding.BindingSuggestionDto;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.tracking.TaintVulnerabilityDto;
import org.sonarsource.sonarlint.core.rpc.protocol.client.binding.AssistBindingParams;
import org.sonarsource.sonarlint.core.rpc.protocol.client.binding.AssistBindingResponse;
import org.sonarsource.sonarlint.core.rpc.protocol.client.binding.NoBindingSuggestionFoundParams;
import org.sonarsource.sonarlint.core.rpc.protocol.client.connection.AssistCreatingConnectionParams;
import org.sonarsource.sonarlint.core.rpc.protocol.client.connection.AssistCreatingConnectionResponse;
import org.sonarsource.sonarlint.core.rpc.protocol.client.connection.ConnectionSuggestionDto;
import org.sonarsource.sonarlint.core.rpc.protocol.client.hotspot.HotspotDetailsDto;
import org.sonarsource.sonarlint.core.rpc.protocol.client.http.GetProxyPasswordAuthenticationResponse;
import org.sonarsource.sonarlint.core.rpc.protocol.client.http.ProxyDto;
import org.sonarsource.sonarlint.core.rpc.protocol.client.http.X509CertificateDto;
import org.sonarsource.sonarlint.core.rpc.protocol.client.issue.IssueDetailsDto;
import org.sonarsource.sonarlint.core.rpc.protocol.client.log.LogParams;
import org.sonarsource.sonarlint.core.rpc.protocol.client.message.MessageType;
import org.sonarsource.sonarlint.core.rpc.protocol.client.message.ShowSoonUnsupportedMessageParams;
import org.sonarsource.sonarlint.core.rpc.protocol.client.progress.ReportProgressParams;
import org.sonarsource.sonarlint.core.rpc.protocol.client.progress.StartProgressParams;
import org.sonarsource.sonarlint.core.rpc.protocol.client.smartnotification.ShowSmartNotificationParams;
import org.sonarsource.sonarlint.core.rpc.protocol.client.telemetry.TelemetryClientLiveAttributesResponse;
import org.sonarsource.sonarlint.core.rpc.protocol.common.ClientFileDto;
import org.sonarsource.sonarlint.core.rpc.protocol.common.Either;
import org.sonarsource.sonarlint.core.rpc.protocol.common.TokenDto;
import org.sonarsource.sonarlint.core.rpc.protocol.common.UsernamePasswordDto;

/**
 * Represents the client side of the SonarLint backend connection, answering the backend's requests.
 * Only standalone analysis is supported: there are no connections to SonarQube Server or Cloud,
 * so the binding, connection, hotspot and telemetry callbacks are no-ops.
 * Files are announced to the backend as they are analyzed (see {@link SonarLintBackend}),
 * so no files are listed up front.
 */
public class BackendClient implements SonarLintRpcClientDelegate {
  private static final Logger LOGGER = Logger.getLogger(BackendClient.class.getName());

  private final Map<String, Path> baseDirsByConfigScopeId = new ConcurrentHashMap<>();

  /**
   * Registers the base directory of a configuration scope.
   *
   * @param configScopeId the id of the configuration scope
   * @param baseDir the base directory of the configuration scope
   */
  public void addBaseDir(String configScopeId, Path baseDir) {
    this.baseDirsByConfigScopeId.put(configScopeId, baseDir);
  }

  @Override
  public Path getBaseDir(String configScopeId) throws ConfigScopeNotFoundException {
    Path baseDir = this.baseDirsByConfigScopeId.get(configScopeId);
    if (baseDir == null) {
      throw new ConfigScopeNotFoundException();
    }
    return baseDir;
  }

  @Override
  public List<ClientFileDto> listFiles(String configScopeId) {
    return List.of();
  }

  @Override
  public void log(LogParams params) {
    String message = params.getMessage();
    if (params.getStackTrace() != null) {
      message += System.lineSeparator() + params.getStackTrace();
    }
    switch (params.getLevel()) {
      case ERROR -> LOGGER.severe(message);
      case WARN -> LOGGER.warning(message);
      default -> {
        // do not log anything for other levels
      }
    }
  }

  @Override
  public void showMessage(MessageType type, String text) {
    LOGGER.info(text);
  }

  @Override
  public String getClientLiveDescription() {
    return "BlueJ";
  }

  @Override
  public TelemetryClientLiveAttributesResponse getTelemetryLiveAttributes() {
    return new TelemetryClientLiveAttributesResponse(Map.of());
  }

  @Override
  public void didChangeAnalysisReadiness(Set<String> configScopeIds, boolean areReadyForAnalysis) {
    // analysis requests are queued by the backend until the scope is ready
  }

  @Override
  public void startProgress(StartProgressParams params) {
    // progress is not shown
  }

  @Override
  public void reportProgress(ReportProgressParams params) {
    // progress is not shown
  }

  @Override
  public void openUrlInBrowser(URL url) {
    // only used in connected mode
  }

  @Override
  public void showSoonUnsupportedMessage(ShowSoonUnsupportedMessageParams params) {
    // only used in connected mode
  }

  @Override
  public void showSmartNotification(ShowSmartNotificationParams params) {
    // only used in connected mode
  }

  @Override
  public void showHotspot(String configScopeId, HotspotDetailsDto hotspotDetails) {
    // only used in connected mode
  }

  @Override
  public void showIssue(String configScopeId, IssueDetailsDto issueDetails) {
    // only used in connected mode
  }

  @Override
  public void suggestBinding(Map<String, List<BindingSuggestionDto>> suggestionsByConfigScope) {
    // only used in connected mode
  }

  @Override
  public void suggestConnection(
      Map<String, List<ConnectionSuggestionDto>> suggestionsByConfigScope
  ) {
    // only used in connected mode
  }

  @Override
  public void noBindingSuggestionFound(NoBindingSuggestionFoundParams params) {
    // only used in connected mode
  }

  @Override
  public AssistCreatingConnectionResponse assistCreatingConnection(
      AssistCreatingConnectionParams params,
      SonarLintCancelChecker cancelChecker
  ) {
    throw new CancellationException("Connected mode is not supported");
  }

  @Override
  public AssistBindingResponse assistBinding(
      AssistBindingParams params,
      SonarLintCancelChecker cancelChecker
  ) {
    throw new CancellationException("Connected mode is not supported");
  }

  @Override
  public void didSynchronizeConfigurationScopes(Set<String> configScopeIds) {
    // only used in connected mode
  }

  @Override
  public Either<TokenDto, UsernamePasswordDto> getCredentials(String connectionId) {
    return null;
  }

  @Override
  public List<ProxyDto> selectProxies(URI uri) {
    return List.of(ProxyDto.NO_PROXY);
  }

  @Override
  public GetProxyPasswordAuthenticationResponse getProxyPasswordAuthentication(
      String host, int port, String protocol, String prompt, String scheme, URL targetHost
  ) {
    return new GetProxyPasswordAuthenticationResponse(null, null);
  }

  @Override
  public boolean checkServerTrusted(List<X509CertificateDto> chain, String authType) {
    return false;
  }

  @Override
  public String matchSonarProjectBranch(
      String configScopeId,
      String mainBranchName,
      Set<String> allBranchesNames,
      SonarLintCancelChecker cancelChecker
  ) {
    return mainBranchName;
  }

  @Override
  public void didChangeMatchedSonarProjectBranch(String configScopeId, String newBranchName) {
    // only used in connected mode
  }

  @Override
  public void didChangeTaintVulnerabilities(
      String configScopeId,
      Set<UUID> closedTaintVulnerabilityIds,
      List<TaintVulnerabilityDto> addedTaintVulnerabilities,
      List<TaintVulnerabilityDto> updatedTaintVulnerabilities
  ) {
    // only used in connected mode
  }
}
