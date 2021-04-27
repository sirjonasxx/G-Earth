package gearth.services.internal_extensions.packetinfoexplorer;

import gearth.Main;
import gearth.extensions.ExtensionForm;
import gearth.extensions.ExtensionInfo;
import gearth.services.packet_info.PacketInfo;
import gearth.services.packet_info.PacketInfoManager;
import gearth.protocol.HMessage;
import gearth.ui.GEarthController;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

@ExtensionInfo(
        Title = "Packet Info",
        Description = "Packet info explorer",
        Version = "0.1",
        Author = "sirjonasxx"
)
public class PacketInfoExplorer extends ExtensionForm {
    public TextField txt_filterHeaderId;
    public TextField txt_filterNameHash;
    public GridPane source_grid;
    public CheckBox chk_toClient;
    public CheckBox chk_toServer;

    private Map<String, CheckBox> chk_sources = new HashMap<>();

    private List<PacketInfo> packetInfoList = new ArrayList<>();

    private TableView<PacketInfo> tableView;
    public GridPane grid;

    @Override
    public ExtensionForm launchForm(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(PacketInfoExplorer.class.getResource("PacketInfoExplorer.fxml"));
        Parent root = loader.load();

        primaryStage.setTitle("Packet info explorer");
        primaryStage.setScene(new Scene(root));
        primaryStage.setMinWidth(430);
        primaryStage.setMinHeight(260);
        primaryStage.getScene().getStylesheets().add(GEarthController.class.getResource("/gearth/ui/bootstrap3.css").toExternalForm());
        primaryStage.getIcons().add(new Image(Main.class.getResourceAsStream("G-EarthLogoSmaller.png")));

        return loader.getController();
    }

    public void initialize() {
        Platform.runLater( () -> grid.requestFocus() );

        tableView = new TableView<>();
        tableView.setTableMenuButtonVisible(true);
        tableView.setStyle("-fx-focus-color: white;");

        tableView.focusedProperty().addListener(observable -> {
            if (tableView.isFocused()) {
                grid.requestFocus();
            }
        });

        TableColumn<PacketInfo, Integer> headerIdColumn = new TableColumn<>("Header ID");
        headerIdColumn.setCellValueFactory(new PropertyValueFactory<>("headerId"));

        TableColumn<PacketInfo, HMessage.Direction> directionColumn = new TableColumn<>("Direction");
        directionColumn.setCellValueFactory(new PropertyValueFactory<>("destination"));
        directionColumn.setPrefWidth(96);

        TableColumn<PacketInfo, String> packetNameColumn = new TableColumn<>("Name");
        packetNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        packetNameColumn.setPrefWidth(220);

        TableColumn<PacketInfo, String> packetHashColumn = new TableColumn<>("Hash");
        packetHashColumn.setVisible(false);
        packetHashColumn.setCellValueFactory(new PropertyValueFactory<>("hash"));
        packetHashColumn.setPrefWidth(220);

        TableColumn<PacketInfo, String> structureColumn = new TableColumn<>("Structure");
        structureColumn.setCellValueFactory(new PropertyValueFactory<>("structure"));
        structureColumn.setPrefWidth(115);

        TableColumn<PacketInfo, String> sourceColumn = new TableColumn<>("Source");
        sourceColumn.setCellValueFactory(new PropertyValueFactory<>("source"));

        tableView.getColumns().addAll(Arrays.asList(headerIdColumn, directionColumn, packetNameColumn,
                packetHashColumn, structureColumn, sourceColumn));

        grid.add(tableView, 0, 1);

        InvalidationListener filterValues = observable -> updateTableValues();
        txt_filterHeaderId.textProperty().addListener(filterValues);
        txt_filterNameHash.textProperty().addListener(filterValues);
        chk_toClient.selectedProperty().addListener(filterValues);
        chk_toClient.selectedProperty().addListener(filterValues);
    }

    @Override
    protected void initExtension() {
        onConnect((host, port, hotelversion, clientIdentifier, clientType, packetInfoManager) -> {
            setPacketInfoManager(packetInfoManager);
        });
    }

    @Override
    protected void onEndConnection() {
        setPacketInfoManager(PacketInfoManager.EMPTY);
    }

    private void setPacketInfoManager(PacketInfoManager packetInfoManager) {
        packetInfoList = packetInfoManager.getPacketInfoList();
        packetInfoList.sort(Comparator.comparingInt(PacketInfo::getHeaderId));

        Platform.runLater(() -> {
            source_grid.getChildren().clear();
            chk_sources.clear();
            for (PacketInfo packetInfo : packetInfoList) {
                if (!chk_sources.containsKey(packetInfo.getSource())) {
                    CheckBox checkBox = new CheckBox(packetInfo.getSource());
                    checkBox.setSelected(true);
                    checkBox.selectedProperty().addListener(observable -> updateTableValues());
                    source_grid.add(checkBox, 0, chk_sources.size());
                    chk_sources.put(packetInfo.getSource(), checkBox);
                }
            }

            primaryStage.setTitle("Packet info explorer | " + packetInfoList.size() + " packets");

            updateTableValues();
        });

    }

    private void updateTableValues() {
        tableView.getItems().clear();

        IntegerProperty doHeaderIdFilter = new SimpleIntegerProperty(-1);
        if (!txt_filterHeaderId.getText().equals("")) {
            try {
                doHeaderIdFilter.setValue(Integer.parseInt(txt_filterHeaderId.getText()));
            }
            catch (Exception ignore) {}
        }


        List<PacketInfo> allPacketInfos = packetInfoList.stream()
                .filter(packetInfo -> {
                    if (doHeaderIdFilter.get() != -1 && packetInfo.getHeaderId() != doHeaderIdFilter.get()) return false;
                    String filterNameHashLower = txt_filterNameHash.getText().toLowerCase();
                    if (!filterNameHashLower.equals("")
                            && (packetInfo.getName() == null || !packetInfo.getName().toLowerCase().contains(filterNameHashLower))
                            && (packetInfo.getHash() == null || !packetInfo.getHash().toLowerCase().contains(filterNameHashLower))) {
                        return false;
                    }
                    if ((!chk_toClient.isSelected() && packetInfo.getDestination() == HMessage.Direction.TOCLIENT)
                            || (!chk_toServer.isSelected() && packetInfo.getDestination() == HMessage.Direction.TOSERVER)) {
                        return false;
                    }
                    if (!chk_sources.get(packetInfo.getSource()).isSelected()) return false;
                    return true;
                }).collect(Collectors.toList());


        tableView.getItems().addAll(allPacketInfos);
    }

    @Override
    protected boolean canLeave() {
        return false;
    }

    @Override
    protected boolean canDelete() {
        return false;
    }
}
