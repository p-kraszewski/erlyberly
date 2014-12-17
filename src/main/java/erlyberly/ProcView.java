package erlyberly;

import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ResourceBundle;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.binding.BooleanBinding;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.PieChart.Data;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import javafx.util.Callback;
import de.jensd.fx.fontawesome.AwesomeIcon;
import de.jensd.fx.fontawesome.Icon;

/**
 * Handles UI related tasks and delegates processing to {@link ProcController}. 
 */
public class ProcView implements Initializable {

	private final ProcController procController;

	private final DateFormat timeFormat;
	
	@FXML
	private TableView<ProcInfo> processView;
	@FXML
	private Label procCountLabel;
	@FXML
	private Button refreshButton;
	@FXML
	private Button pollButton;
	@FXML
	private Button heapPieButton;
	@FXML
	private Button stackPieButton;
	@FXML
	private Button totalHeapPieButton;
	
	public ProcView() {
		procController = new ProcController();
	
		timeFormat = new SimpleDateFormat("h:mm:ssaa");
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public void initialize(URL url, ResourceBundle r) {
		final BooleanBinding notConnected = ErlyBerly.nodeAPI().connectedProperty().not();
		
		procController.getProcs().addListener(this::onProcessCountChange);
		
		heapPieButton.setGraphic(Icon.create().icon(AwesomeIcon.PIE_CHART));
		heapPieButton.getStyleClass().add("erlyberly-icon-button");
		heapPieButton.setStyle("-fx-background-color: transparent;");
		heapPieButton.setText("");
		heapPieButton.disableProperty().bind(notConnected);
		
		stackPieButton.setGraphic(Icon.create().icon(AwesomeIcon.PIE_CHART));
		stackPieButton.setStyle("-fx-background-color: transparent;");
		stackPieButton.setText("");
		stackPieButton.disableProperty().bind(notConnected);
		
		totalHeapPieButton.setGraphic(Icon.create().icon(AwesomeIcon.PIE_CHART));
		totalHeapPieButton.setStyle("-fx-background-color: transparent;");
		totalHeapPieButton.setText("");
		totalHeapPieButton.disableProperty().bind(notConnected);
		
		//ErlyBerly.nodeAPI().connectedProperty().addListener((Observable o) -> { System.out.println("now connected? " + ErlyBerly.nodeAPI().connectedProperty().get()); } );
		refreshButton.setGraphic(Icon.create().icon(AwesomeIcon.ROTATE_LEFT));
		refreshButton.setGraphicTextGap(8d);
		refreshButton.disableProperty().bind(procController.pollingProperty().or(notConnected));
		
		pollButton.setGraphic(Icon.create().icon(AwesomeIcon.REFRESH));
		pollButton.setGraphicTextGap(9d);
		pollButton.disableProperty().bind(notConnected);

		procController.pollingProperty().addListener(this::onPollingChange);
		onPollingChange(null);
		
		TableColumn<ProcInfo, String> pidColumn = (TableColumn<ProcInfo, String>) processView.getColumns().get(0);
		TableColumn<ProcInfo, String> procColumn = (TableColumn<ProcInfo, String>) processView.getColumns().get(1);
		TableColumn<ProcInfo, Long> reducColumn = (TableColumn<ProcInfo, Long>) processView.getColumns().get(2);
		TableColumn<ProcInfo, Long> mQueueLenColumn = (TableColumn<ProcInfo, Long>) processView.getColumns().get(3);
		TableColumn<ProcInfo, Long> heapSizeColumn = (TableColumn<ProcInfo, Long>) processView.getColumns().get(4);
		TableColumn<ProcInfo, Long> stackSizeColumn = (TableColumn<ProcInfo, Long>) processView.getColumns().get(5);
		TableColumn<ProcInfo, Long> totalHeapSizeColumn = (TableColumn<ProcInfo, Long>) processView.getColumns().get(6);
		
		pidColumn.setCellValueFactory(new PropertyValueFactory<ProcInfo, String>("pid"));
		pidColumn.setId("pid");

		procColumn.setCellValueFactory(new PropertyValueFactory<ProcInfo, String>("processName"));
		procColumn.setId("proc");
		
		reducColumn.setCellValueFactory(new PropertyValueFactory<ProcInfo, Long>("reductions"));
		reducColumn.setId("reduc");
		
		mQueueLenColumn.setCellValueFactory(new PropertyValueFactory<ProcInfo, Long>("msgQueueLen"));
		mQueueLenColumn.setId("mqueue");
		
		heapSizeColumn.setCellValueFactory(new PropertyValueFactory<ProcInfo, Long>("heapSize"));
		heapSizeColumn.setId("heapsize");
		
		stackSizeColumn.setCellValueFactory(new PropertyValueFactory<ProcInfo, Long>("stackSize"));
		stackSizeColumn.setId("stacksize");
		
		totalHeapSizeColumn.setCellValueFactory(new PropertyValueFactory<ProcInfo, Long>("totalHeapSize"));
		totalHeapSizeColumn.setId("totalheapsize");
		
		processView.setItems(procController.getProcs());
		processView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		
		initialiseProcessSorting();
	}
	
	@FXML
	private void onHeapPie() {
		ObservableList<PieChart.Data> data = buildData(chartableProcs(), (p) -> {return p.getHeapSize(); });
		
		showPieChart("Process Heap", data);
	}

	@FXML
	private void onStackPie() {
		ObservableList<PieChart.Data> data = buildData(chartableProcs(), (p) -> {return p.getStackSize(); });
			
		showPieChart("Process Stack", data);
	}

	@FXML
	private void onTotalHeapPie() {
		ObservableList<PieChart.Data> data = buildData(chartableProcs(), (p) -> {return p.getTotalHeapSize(); });
		
		showPieChart("Total Heap", data);
	}
	
	private ObservableList<PieChart.Data> buildData(ObservableList<ProcInfo> procs, Callback<ProcInfo, Long> extractor) {
		
		long total = 0;

		for (ProcInfo proc : procs) {
			total += extractor.call(proc);
		}
		
		// threshold is 1%, this is a limit on how many segments are added to the pie chart
		// too many seems to crash the process
		long threshold = total / 200;

		long other = 0;
		
		ObservableList<PieChart.Data> data = FXCollections.observableArrayList();

		for (ProcInfo proc : procs) {
			long value = extractor.call(proc);
			
			if(value >= threshold)
				data.add(new Data(procDescription(proc), proc.getTotalHeapSize()));
			else
				other += value;
		}
		
		if(other > 0)
			data.add(new Data("Other", other));
		
		return data;
	}

	private ObservableList<ProcInfo> chartableProcs() {
		ObservableList<ProcInfo> procs = processView.getSelectionModel().getSelectedItems();
		
		if(procs.isEmpty()) {
			procs = procController.getProcs();
		}
		return procs;
	}

	private void showPieChart(String title, ObservableList<PieChart.Data> data) {
        PieChart pieChart;
        
		pieChart = new PieChart(data);
        pieChart.setTitle(title);
        
		Stage pieStage = new Stage();
		Scene scene = new Scene(pieChart);
    
		CloseWindowOnEscape.apply(scene, pieStage);
		
		pieStage.setScene(scene);
        pieStage.setWidth(800);
        pieStage.setHeight(600);

        pieStage.show();
	}

	private String procDescription(ProcInfo proc) {
		String pid = proc.getProcessName();
		if(pid == null || "".equals(pid)) {
			pid = proc.getPid();
		}
		return pid;
	}

	private void onPollingChange(Observable o) {
		if(procController.pollingProperty().get())
			pollButton.setText("Stop Polling");
		else
			pollButton.setText("Start Polling");
	}
	
	@FXML
	private void onRefresh() {
		procController.refreshOnce();
	}

	@FXML
	private void onTogglePolling() {
		procController.togglePolling();
	}
	
	private void onProcessCountChange(Observable o) {
		procCountLabel.setText(procController.getProcs().size() + " processes at " + timeFormat.format(new Date()).toLowerCase());
	}
	
	private void initialiseProcessSorting() {
		InvalidationListener invalidationListener = new ProcSortUpdater();
		
		for (TableColumn<ProcInfo, ?> col : processView.getColumns()) {
			col.sortTypeProperty().addListener(invalidationListener);
		}
		
		processView.getSortOrder().addListener(invalidationListener);
	}
	
	private final class ProcSortUpdater implements InvalidationListener {
		@Override
		public void invalidated(Observable ob) {
			ProcSort procSort = null;
			
			if(!processView.getSortOrder().isEmpty()) {
				TableColumn<ProcInfo, ?> tableColumn = processView.getSortOrder().get(0);
				
				procSort = new ProcSort(tableColumn.getId(), tableColumn.getSortType());
			}
			procController.procSortProperty().set(procSort);
		}
	}
}
