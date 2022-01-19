package no.ntnu.iir.bluej.sonarlint;

import bluej.extensions2.BlueJ;
import bluej.extensions2.PreferenceGenerator;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javafx.animation.PauseTransition;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.util.Duration;
import no.ntnu.iir.bluej.extensions.linting.core.handlers.PackageEventHandler;
import no.ntnu.iir.bluej.extensions.linting.core.violations.ViolationManager;
import no.ntnu.iir.bluej.sonarlint.checker.CheckerService;
import no.ntnu.iir.bluej.sonarlint.util.SonarLintIconMapper;
import no.ntnu.iir.bluej.sonarlint.util.StringUtils;
import org.sonarsource.sonarlint.core.client.api.common.RuleKey;

/**
 * Manages properties for SonarLint.
 */
public class SonarLintProperties implements PreferenceGenerator {
  private BlueJ blueJ;
  private CheckerService checkerService;
  private ViolationManager violationManager;
  private List<RuleKey> disabledRules;
  private HashMap<String, SonarLintRuleDetails> ruleDetailsMap;
  private VBox pane;
  private TextField tableFilterField;
  private TableView<Entry<String, SonarLintRuleDetails>> tableView;
  private WebView ruleWebView;
  private SonarLintIconMapper iconMapper;

  // Configuration keys
  private static final String SONARLINT_DISABLED_RULES = "SonarLint.DisabledRules";

  /**
   * Instantiates a new SonarLint Properties manager.
   * 
   * @param blueJ the BlueJ instance to load configuration values from
   * @param checkerService the CheckerService to configure
   * @param violationManager the ViolationManager associated with the BlueJ instance
   */
  public SonarLintProperties(
      BlueJ blueJ,
      CheckerService checkerService,
      ViolationManager violationManager
  ) {
    this.blueJ = blueJ;
    this.checkerService = checkerService;
    this.violationManager = violationManager;
    this.disabledRules = new ArrayList<>();
    this.ruleDetailsMap = new HashMap<>();
    this.iconMapper = new SonarLintIconMapper();

    this.initPane();
    this.loadValues();
  }

  /**
   * Inits all necessary UI elements for the BlueJ Preferences tab.
   * The UI elements are placed in a wrapping pane.
   */
  private void initPane() {
    this.pane = new VBox();
    this.pane.setSpacing(10);

    HBox filterHBox = new HBox();
    filterHBox.setAlignment(Pos.BASELINE_RIGHT);
    this.tableFilterField = new TextField();
    PauseTransition pause = new PauseTransition(Duration.seconds(1));
    this.tableFilterField.textProperty().addListener((obs, oldValue, newValue) -> {
      pause.setOnFinished(event -> this.filterDisplayedRules(newValue));
      pause.playFromStart();
    });
    filterHBox.getChildren().addAll(
        new Label("Filter rules: "),
        this.tableFilterField
    );

    this.ruleWebView = new WebView();
    this.ruleWebView.setPrefWidth(this.pane.getWidth());
    this.ruleWebView.getEngine().setUserStyleSheetLocation(
        "data:,body { font: 12px Segoe UI, Arial; }"
    );

    this.tableView = new TableView<>();


    TableColumn<Entry<String, SonarLintRuleDetails>, HBox> ruleIconColumn = 
        new TableColumn<>("");
    ruleIconColumn.setSortable(false);
    ruleIconColumn.setCellValueFactory(data -> {
      URL typeIconUrl = this.iconMapper.getIcon(data.getValue().getValue().getType());
      URL severityIconUrl = this.iconMapper.getIcon(data.getValue().getValue().getSeverity());

      Image typeIcon = new Image(typeIconUrl.toString());
      ImageView typeIconView = new ImageView(typeIcon);
      Tooltip.install(
          typeIconView,
          new Tooltip(StringUtils.constantToReadable(data.getValue().getValue().getType()))
      );

      Image severityIcon = new Image(severityIconUrl.toString());
      ImageView severityIconView = new ImageView(severityIcon);
      Tooltip.install(
          severityIconView,
          new Tooltip(StringUtils.constantToReadable(data.getValue().getValue().getSeverity()))
      );

      HBox imageHBox = new HBox(typeIconView, severityIconView);
      imageHBox.setAlignment(Pos.BASELINE_CENTER);
      imageHBox.setSpacing(4);

      return new SimpleObjectProperty<HBox>(imageHBox);
    });

    TableColumn<Entry<String, SonarLintRuleDetails>, String> ruleKeyColumn = 
        new TableColumn<>("Rule key");
    ruleKeyColumn.setCellValueFactory(data -> {
      String ruleId = data.getValue().getValue().getKey().split(":")[1];
      return new SimpleStringProperty(ruleId);
    });

    TableColumn<Entry<String, SonarLintRuleDetails>, ComboBox<String>> ruleEnabledColumn = 
        new TableColumn<>("Enabled");
    ruleEnabledColumn.setSortable(false);
    ruleEnabledColumn.setCellValueFactory(data -> {
      ComboBox<String> enabledComboBox = new ComboBox<>();
      enabledComboBox.getItems().setAll("Yes", "No");
      String selected = (data.getValue().getValue().isEnabled()) ? "Yes" : "No";
      enabledComboBox.getSelectionModel().select(selected);
      enabledComboBox.valueProperty().addListener((obs, oldValue, newValue) -> {
        if (newValue.equals("No")) {
          this.disabledRules.add(RuleKey.parse(data.getValue().getValue().getKey()));
          data.getValue().getValue().setEnabled(false);
        } else {
          this.disabledRules.remove(RuleKey.parse(data.getValue().getValue().getKey()));
          data.getValue().getValue().setEnabled(true);
        }
      });

      return new SimpleObjectProperty<ComboBox<String>>(enabledComboBox);
    });

    TableColumn<Entry<String, SonarLintRuleDetails>, String> ruleNameColumn = 
        new TableColumn<>("Rule name");
    ruleNameColumn.setCellValueFactory(
        data -> new SimpleStringProperty(data.getValue().getValue().getName())
    );

    
    this.tableView.getItems().setAll(this.ruleDetailsMap.entrySet());
    this.tableView.getColumns().add(ruleIconColumn);
    this.tableView.getColumns().add(ruleKeyColumn);
    this.tableView.getColumns().add(ruleEnabledColumn);
    this.tableView.getColumns().add(ruleNameColumn);
    this.tableView.getFocusModel().focusedItemProperty().addListener((obs, oldVal, newVal) -> {
      if (newVal != null) {
        this.ruleWebView.getEngine().loadContent(newVal.getValue().getHtmlDescription());
      }
    });

    this.tableView.prefWidthProperty().bind(this.pane.widthProperty());
    this.ruleWebView.prefWidthProperty().bind(this.pane.widthProperty());

    this.pane.getChildren().add(filterHBox);
    this.pane.getChildren().add(this.tableView);
    this.pane.getChildren().add(this.ruleWebView);
    this.pane.setPrefWidth(600);
  }

  /**
   * Filters the displayed rules using case insensitive matching on the rules searchable string.
   * 
   * @param filterString the String to use for matching
   */
  private void filterDisplayedRules(String filterString) {
    Pattern regexPattern = Pattern.compile(Pattern.quote(filterString), Pattern.CASE_INSENSITIVE);
    Set<Entry<String, SonarLintRuleDetails>> filteredRules = new HashSet<>();
    this.ruleDetailsMap.entrySet().forEach(entry -> {
      String fieldValue = entry.getValue().getSearchableString();
      if (regexPattern.matcher(fieldValue).find()) {
        filteredRules.add(entry);
      }
    });
    this.tableView.getItems().setAll(filteredRules);
  }

  /**
   * Returns the Window that should be rendered to the BlueJ preferences.
   * 
   * @return the Window that should be rendered to the BlueJ preferences
   */
  @Override
  public Pane getWindow() {
    return this.pane;
  }

  /**
   * Loads configuration values from the BlueJ Extension Properties.
   * Also handles the necessary processing for these values.
   */
  @Override
  public void loadValues() {
    String[] disabledRulesList = this.blueJ.getExtensionPropertyString(
        SONARLINT_DISABLED_RULES, 
        ""
    ).split(",");

    for (String ruleKeyString : disabledRulesList) {
      // guard condition to ignore empty string(s)
      if (!ruleKeyString.equals("")) { 
        RuleKey ruleKey = RuleKey.parse(ruleKeyString);
        this.disabledRules.add(ruleKey);
      }
    }

    this.checkerService.getRuleDetails().forEach(ruleDetails -> {
      this.ruleDetailsMap.put(ruleDetails.getKey(), new SonarLintRuleDetails(ruleDetails, true));
      this.disabledRules.forEach(ruleKey -> {
        SonarLintRuleDetails details = this.ruleDetailsMap.get(ruleKey.toString());
        if (details != null) {
          details.setEnabled(false);
        }
      });
    });

    this.checkerService.setDisabledRules(this.disabledRules);
    this.tableView.getItems().setAll(this.ruleDetailsMap.entrySet());

    this.tableFilterField.clear();
    violationManager.clearViolations();
    PackageEventHandler.checkAllPackagesOpen(violationManager, checkerService);
  }

  /**
   * Saves configuration values to the BlueJ Extension Properties.
   */
  @Override
  public void saveValues() {
    List<String> ruleKeyList = this.disabledRules
        .stream()
        .map(RuleKey::toString)
        .collect(Collectors.toList());
    
    String disabledRulesString = String.join(",", ruleKeyList);
    
    this.blueJ.setExtensionPropertyString(
        SONARLINT_DISABLED_RULES,
        disabledRulesString
    );
  }
}
