package rooflineviewpart.views;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChart.Series;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.ToolBar;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import org.jfree.chart.fx.ChartViewer;

import rooflineviewpart.heatmap.RooflineHeatmap;
import rooflineviewpart.views.Roofline.JSONRoofline;
import rooflineviewpart.views.Roofline.JSONRooflineMetric;

import com.google.gson.Gson;

import edu.uoregon.tau.perfdmf.Trial;
import edu.uoregon.tau.perfexplorer.glue.TrialResult;

public class RooflineChart extends Application {

	public static class ChartLine {
		private static final String SPEC = "Spec ";

		private static String specName(String name) {
			return SPEC + name;
		}
		public Double endX;
		public Double endY;
		public final Double gFlops;
		public String intersectName = "";
		/**
		 * The name of the value shown by this line/series
		 */
		public final String name;
		/**
		 * The actual value associated with this metric in this roofline model
		 */
		public final Double opInt;

		public Double startX;

		public Double startY;

		public ChartLine(List<Object> tuple, boolean spec) {
			if (tuple.size() >= 2) {
				if (tuple.get(0) instanceof String
						&& tuple.get(1) instanceof Double) {

					String tmpName = (String) tuple.get(0);
					if (spec) {
						tmpName = specName(tmpName);
					}

					this.name = tmpName;

					this.opInt = (Double) tuple.get(1);

					if (tuple.size() == 3) {
						if (tuple.get(3) instanceof Double) {
							this.gFlops = (Double) tuple.get(3);
							return;
						}
					}
					this.gFlops = null;

					return;
				}
			}
			this.name = null;
			this.opInt = null;
			this.gFlops = null;
			System.out.println("Warning: Invalid ChartLine tuple");

		}

		public ChartLine(String name, Double val) {
			this.name = name;
			this.opInt = val;
			this.gFlops = null;
		}

		public ChartLine(String name, Double optInt, Double gFlops, boolean spec) {
			this.name = name;
			if (spec) {
				name = specName(name);
			}
			this.opInt = optInt;
			this.gFlops = gFlops;
		}
	}
	/** a node which displays a value on hover, but is otherwise empty */
	class HoveredThresholdNode extends StackPane {
		HoveredThresholdNode(String name, double xCoord, double yCoord) {
			setPrefSize(15, 15);

			final Label label = createDataThresholdLabel(name, xCoord, yCoord);

			setOnMouseEntered(new EventHandler<MouseEvent>() {
				@Override
				public void handle(MouseEvent mouseEvent) {
					getChildren().setAll(label);
					setCursor(Cursor.NONE);
					toFront();
				}
			});
			setOnMouseExited(new EventHandler<MouseEvent>() {
				@Override
				public void handle(MouseEvent mouseEvent) {
					getChildren().clear();
					setCursor(Cursor.CROSSHAIR);
				}
			});
		}

		private Label createDataThresholdLabel(String name, double xCoord,
				double yCoord) {
			NumberFormat formatter = new DecimalFormat("#0.00");
			final Label label = new Label("Flops/Byte: "
					+ formatter.format(xCoord) + " \nGFlops/Sec: "
					+ formatter.format(yCoord));
			label.setTranslateY(-35); // Move label 25 pixels up
			// label.getStyleClass().addAll("default-color0",
			// "chart-line-symbol", "chart-series-line");
			label.setStyle("-fx-font-size: 20; -fx-font-weight: bold;");
			//
			// if (priorValue == 0) {
			// label.setTextFill(Color.DARKGRAY);
			// } else if (value > priorValue) {
			// label.setTextFill(Color.FORESTGREEN);
			// } else {
			// label.setTextFill(Color.FIREBRICK);
			// }

			label.setMinSize(Label.USE_PREF_SIZE, Label.USE_PREF_SIZE);
			return label;
		}
	}
	
	public static final String BANDWIDTH = "  Bandwidth:";

	public static final String DOT_JSON = ".json";

	public static final String EMPTY = "";

	public static final String GFLOPS = "  GFLOP/s:";
	// private static final String GBYTE="gbytes";
	// private static final String GFLOP="gflops";
	private static final String HOSTNAME = "HOSTNAME";
	public static final String ROOFLINE = "roofline";
	public static final String SPEC = "  Spec:";
	private static boolean standalone = false;
	public static void main(String[] args) {
		standalone = true;
		launch(args);

	}

	LineChart<Number, Number> ac = null;

	List<ChartLine> appGflops = new ArrayList<ChartLine>();

	FileChooser chooser = new FileChooser();

	Map<String, Set<String>> filterMap = new HashMap<String, Set<String>>();

	/**
	 * The selected metadata key/value set from the filter
	 */
	Map<String, String> filterSelectMap = new HashMap<String, String>();

	FileChooser.ExtensionFilter jsonFilter = new FileChooser.ExtensionFilter(
			"JSON files (*.json)", "*.json");

	FileChooser.ExtensionFilter rooflineFilter = new FileChooser.ExtensionFilter(
			"Roofline files (*.roofline)", "*.roofline");

	List<Roofline> allRooflines = new ArrayList<Roofline>();
	ObservableList<Roofline> selectedRooflines = FXCollections.observableArrayList();

	ComboBox<Roofline> rooflineSelect;

	private List<String> selectedFuncs = null;

	ToolBar toolBar = null;

	private List<TrialResult> trialMeans = new ArrayList<TrialResult>();

	public RooflineChart() {

		chooser.getExtensionFilters().add(jsonFilter);
		chooser.getExtensionFilters().add(rooflineFilter);

	}

	/**
	 * Put the starting and ending coordinates for an application line into val
	 * based on the y value maxY. App values run vertically, intercepting the x
	 * axis at their value.
	 * 
	 * @param val
	 * @param maxY
	 */
	void calcAppCoordinates(ChartLine val) {
		val.startX = val.opInt;
		val.startY = 0.01;

		val.endX = val.opInt;
		val.endY = val.gFlops;

		//System.out.println(val.name + " From: " + val.startX + "," + val.startY+ " to: " + val.endX + ", " + val.endY);
	}
	/**
	 * Put the starting and ending coordinates for a bandwidth line into val
	 * based on the y value maxY. Bandwidth starts near 0 on the x axis and
	 * treats the roofline data value as the slope
	 * 
	 * @param val
	 * @param maxY
	 */
	void calcBandwidthCoordinates(ChartLine val, double maxY) {
		val.startX = 0.01;
		val.startY = val.startX * val.opInt;

		val.endX = maxY / val.opInt;
		val.endY = maxY;
	}
	/**
	 * Put the starting and ending coordinates for a gflops line into val, where
	 * maxX is the end of the line and slope is the slope. Gflops lines run
	 * horizontally from the right to their intercept with a bandwidth line on
	 * the left.
	 * 
	 * @param val
	 * @param slope
	 * @param maxX
	 */
	void calcGFLOPSCoordinates(ChartLine val, Double slope, Double maxX) {

		Double minX = horizontalIntercept(slope, val.opInt);

		val.startX = minX;
		val.startY = val.opInt;

		val.endX = maxX;
		val.endY = val.opInt;
	}
	Tab heatMapTab = new Tab();
	TabPane tabPane = new TabPane();
	/**
	 * Creates the Roofline scene
	 * 
	 * @return
	 */
	public Scene createRooflineScene() {

		rooflineSelect = new ComboBox<Roofline>();
		rooflineSelect.getSelectionModel().selectedItemProperty()
				.addListener(new ChangeListener<Roofline>() {

					@Override
					public void changed(ObservableValue observable,
							Roofline oldValue, Roofline newValue) {
						if (newValue != null) {
							displayRoofline(newValue);
						}
					}

				});

		Button loadButton = new Button("Load");
		loadButton.setOnAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent event) {

				chooser.setTitle("Load Roofline");
				File file = chooser.showOpenDialog(null);
				if (file != null) {
					chooser.setInitialDirectory(file.getParentFile());
					readRooflines(file);
					createFilterMap();
					selectedRooflines.clear();
					selectedRooflines.addAll(allRooflines);
					rooflineSelect.setItems(selectedRooflines);

					Roofline selected = rooflineSelect.getItems().get(0);

					rooflineSelect.setValue(selected);
				}

			}
		});

		Button loadRemoteButton = new Button("Load Remote");
		String baseurl = "http://nic.uoregon.edu/~wspear/roofline";
		// TextField remoteBase = new TextField(baseurl);
		// remoteBase.setText(value);
		loadRemoteButton.setOnAction(new EventHandler<ActionEvent>() {

			

			@Override
			public void handle(ActionEvent event) {

				readRemoteRooflines(baseurl);// remoteBase.getText());
				createFilterMap();
				selectedRooflines.clear();
				selectedRooflines.addAll(allRooflines);
				rooflineSelect.setItems(selectedRooflines);
				Roofline selected = rooflineSelect.getItems().get(0);

				rooflineSelect.setValue(selected);

			}
		});

		Button loadFilterButton = new Button("Filter...");
		loadFilterButton.setOnAction(new EventHandler<ActionEvent>() {


			@Override
			public void handle(ActionEvent event) {
				final Stage dialogStage = new Stage();
				dialogStage.setTitle("Roofline Filter");
				final String noFilter = "<no filter>";
				dialogStage.initModality(Modality.WINDOW_MODAL);

				GridPane grid = new GridPane();
				grid.setPadding(new Insets(5, 5, 5, 5));
				grid.setHgap(5);
				grid.setVgap(5);

				Map<String, String> tmpFilterSelectMap = new HashMap<String, String>(
						filterSelectMap);
				List<ComboBox<String>> boxList = new ArrayList<ComboBox<String>>();
				int row = 0;
				for (Entry<String, Set<String>> e : filterMap.entrySet()) {
					if (e.getValue().size() <= 1) {
						continue;
					}
					Label l = new Label(e.getKey());
					ObservableList<String> mapKeys = FXCollections
							.observableArrayList();
					mapKeys.add(noFilter);
					mapKeys.addAll(e.getValue());
					ComboBox<String> c = new ComboBox<String>(mapKeys);

					String tmpValue = tmpFilterSelectMap.get(e.getKey());
					if (tmpValue != null && mapKeys.contains(tmpValue)) {
						c.setValue(tmpValue);
					} else {
						c.setValue(noFilter);
					}
					c.getSelectionModel().selectedItemProperty()
							.addListener(new ChangeListener<String>() {

								@Override
								public void changed(ObservableValue observable,
										String oldValue, String newValue) {
									if (newValue != null) {
										if (newValue.equals(noFilter)) {
											tmpFilterSelectMap.remove(e
													.getKey());
										} else {
											tmpFilterSelectMap.put(e.getKey(),
													newValue);
										}
									}
								}

							});
					c.setMaxWidth(Double.MAX_VALUE);
					GridPane.setHgrow(c, Priority.ALWAYS);
					GridPane.setFillWidth(c, true);
					grid.add(l, 0, row);
					grid.add(c, 1, row);
					boxList.add(c);

					row++;
				}

				Button yesBtn = new Button("Filter");
				yesBtn.setOnAction(new EventHandler<ActionEvent>() {

					@Override
					public void handle(ActionEvent arg0) {

						filterSelectMap.clear();
						filterSelectMap.putAll(tmpFilterSelectMap);
						Roofline current = rooflineSelect.getValue();
						selectedRooflines.clear();
						//As soon as we find a non-matching metadata we continue out of the outer loop. Only if the inner loop finishes we add the roofline.
						rooflineloop:
						for(Roofline r:allRooflines){
							for(Entry<String,String> e:filterSelectMap.entrySet()){
								
								if(!r.metadata.containsKey(e.getKey())||!r.metadata.get(e.getKey()).equals(e.getValue())){
									continue rooflineloop;
								}
							}
							selectedRooflines.add(r);
						}
						
						if(selectedRooflines.contains(current)){
							rooflineSelect.setValue(current);
						}
						else if(selectedRooflines.size()>0)
						{
							rooflineSelect.setValue(selectedRooflines.get(0));
						}
						dialogStage.close();

					}
				});
				Button noBtn = new Button("Cancel");

				noBtn.setOnAction(new EventHandler<ActionEvent>() {

					@Override
					public void handle(ActionEvent arg0) {
						dialogStage.close();

					}
				});

				Button clrBtn = new Button("Clear");

				clrBtn.setOnAction(new EventHandler<ActionEvent>() {

					@Override
					public void handle(ActionEvent arg0) {
						tmpFilterSelectMap.clear();
						for (ComboBox<String> c : boxList) {
							c.setValue(noFilter);
						}

					}
				});

				// grid.add(keyList, 0, 0, 3, 1);
				
				HBox hBox = new HBox();
				hBox.setAlignment(Pos.BASELINE_CENTER);
				hBox.setSpacing(40.0);
				hBox.getChildren().addAll(yesBtn,clrBtn, noBtn);
				hBox.setMaxHeight(noBtn.getHeight()+10);
				
//				grid.add(yesBtn, 0, row);
//				grid.add(clrBtn, 1, row);
//				grid.add(noBtn, 2, row);
				grid.add(hBox, 0, row);
				ScrollPane sp = new ScrollPane();
				sp.setContent(grid);
				dialogStage.setScene(new Scene(sp));
				dialogStage.show();

			}
		});

		toolBar = new ToolBar(rooflineSelect, loadButton, loadRemoteButton,
				loadFilterButton);// TODO: Make remote support
									// internal,loadRemoteButton,remoteBase);//,

		ToolBar tb = null;
		if (standalone) {
			TrialSelectionBar tsb = new TrialSelectionBar(this);
			tb = tsb.getTrialSelectionBar();
		} else {
			EasyTrialSelectionBar etsb = new EasyTrialSelectionBar(this);
			tb = etsb.getTrialSelectionBar();
		}

		final ToolBar bottomToolBar = tb;

		final BorderPane bpane = new BorderPane();
		// stage.setTitle("Roofline Chart Sample");
		final LogarithmicAxis xAxis = new LogarithmicAxis(0.01, 100);
		xAxis.setLabel("Operational Intensity (Flops/Byte)");

		final LogarithmicAxis yAxis = new LogarithmicAxis(1.0, 1000);
		yAxis.setLabel("Attainable GFlops/sec");
		ac = new LineChart<Number, Number>(xAxis, yAxis);

		toolBar.setPrefWidth(800);
		ac.setPrefSize(800, 600);

		
		Tab acTab = new Tab();
		acTab.setClosable(false);
		acTab.setText("Roofline Chart");
		acTab.setContent(ac);

		heatMapTab.setClosable(false);
		heatMapTab.setText("Heat Map");
		//heatMapTab.getContent().setVisible(false);
		//heatMapTab.setContent(heatPanel);
		
		
		tabPane.getTabs().add(acTab);
		
		 
		 
		 
		//tabPane.getTabs().add(heatMapTab);
		// Group root = new Group();
		// root.getChildren().add(toolBar);
		// root.getChildren().add(ac);
		bpane.setTop(toolBar);
		bpane.setCenter(tabPane);
		
		//bpane.setBottom(bottomToolBar);
		Scene scene = new Scene(bpane, 800, 600);
//		final  StackPane header = (StackPane) tabPane.lookup(".tab-header-area");
//		 header.setPrefHeight(0);
//
//			tabPane.getTabs().addListener((ListChangeListener) change -> {
//				 
//				  
//				  if(header != null) {
//				    if(tabPane.getTabs().size() <= 1) header.setPrefHeight(0);
//				    else header.setPrefHeight(-1);
//				  }
//				});
		return scene;
	}

	Series<Number, Number> createSeries(ChartLine cl) {
		XYChart.Series<Number, Number> series = new XYChart.Series<Number, Number>();
		series.setName(cl.name);
		XYChart.Data<Number, Number> data;

		data = new XYChart.Data<Number, Number>(cl.startX, cl.startY);
		data.setNode(new HoveredThresholdNode(cl.name, cl.startX, cl.startY));
		series.getData().add(data);

		data = new XYChart.Data<Number, Number>(cl.endX, cl.endY);
		data.setNode(new HoveredThresholdNode(cl.name, cl.endX, cl.endY));
		series.getData().add(data);

		// System.out.println(cl.name+" is ("+cl.startX+","+cl.startY+") to ("+cl.endX+","+cl.endY+")");

		return series;
	}

	private void addToFilterMap(String key, String value) {
		
		if(value.length()>128){
			System.out.println(value+" too big for metadata selection");
			return;
		}
		
		if (filterMap.containsKey(key)) {
			filterMap.get(key).add(value);
			//System.out.print(value + ",");
		} else {
			Set<String> s = new HashSet<String>();
			s.add(value);
			filterMap.put(key, s);
			//System.out.print("\n" + key + ":" + value + ",");
		}
	}

	private void createFilterMap() {
		filterMap.clear();
		for (Roofline r : allRooflines) {
			
			for (Entry e : r.metadata.entrySet()) {
				// System.out.println(e.getValue().getClass());
				
				addToFilterMap(e.getKey().toString(), e.getValue()
						.toString());
			}
		}

	}
	
	public void displayFunctions() {

		String fpmet = "PAPI_DP_OPS";// trialMeans.get(0).getFPMetric();
		String tmet = trialMeans.get(0).getTimeMetric();
		String missmet = "PAPI_L3_TCM";// trialMeans.get(0).getL2MissMetric();
		if (selectedFuncs == null) {
			selectedFuncs = new ArrayList<String>();
			selectedFuncs.add(".TAU application");
		}
		if (fpmet != null && tmet != null) {

			appGflops.clear();

		} else {
			return;
		}

		for (String functest : selectedFuncs) {
			String func = ".TAU application";
			Set<String> events = trialMeans.get(0).getEvents();
			String noParams = functest;
			int firstparen = functest.indexOf('(');
			if (firstparen > 0) {
				noParams = functest.substring(0, firstparen);
			}
			// String type=noParams.substring(0,noParams.indexOf(' '));
			// String
			// noParamsNoType=noParams.substring(noParams.indexOf(' ')+1);
			for (String s : events) {

				// if(!s.contains("=>")&&s.contains(noParamsNoType)){
				// System.out.println(s);
				// }

				if (s.startsWith(noParams)) {
					func = s;
					break;
				}
			}

			TrialResult tm = trialMeans.get(0);
			double fp = tm.getInclusive(0, func, fpmet);
			double time = tm.getInclusive(0, func, tmet);
			double miss = tm.getInclusive(0, func, missmet);
			double flops = fp / time;
			double gflops = flops / 1000;
			double opint = fp / (miss * 64);

			appGflops.add(new ChartLine(func, opint, gflops, false));

		}

		Roofline item = rooflineSelect.getSelectionModel().getSelectedItem();
		if (item != null) {
			processRoofline(item);
			displayRoofline(item);
		}
	}

	void displayRoofline(Roofline r) {

		LogarithmicAxis yAxis = (LogarithmicAxis) ac.getYAxis();
		yAxis.setUpperBound(r.maxY * 2);

		LogarithmicAxis xAxis = (LogarithmicAxis) ac.getXAxis();
		xAxis.setUpperBound(r.maxX / 2);

		ac.setTitle("Roofline: " + r.getSystem());
		ac.getData().removeAll(ac.getData());
		for (ChartLine c : r.specGflops) {
			ac.getData().add(createSeries(c));
		}

		for (ChartLine c : r.specBandwidth) {
			ac.getData().add(createSeries(c));
		}

		for (ChartLine c : r.gflops) {
			ac.getData().add(createSeries(c));
		}

		for (ChartLine c : r.bandwidth) {
			ac.getData().add(createSeries(c));
		}

		// TODO: Eventually this should be more strongly associated with the
		// current roofline
		for (ChartLine c : appGflops) {
			ac.getData().add(createSeries(c));
		}
		
		if(r.heatmap!=null){
			ChartViewer heatPanel = RooflineHeatmap.createHeatmapPanel(r.heatmap);
			heatPanel.setVisible(true);
			heatMapTab.setContent(heatPanel);
			tabPane.getTabs().add(heatMapTab);
		}else{
			this.tabPane.getTabs().remove(this.heatMapTab);
		}

	}

	/**
	 * Calculates the x coordinate of the intercept point of a line through the
	 * origin of slope m and a horizontal line with y=y2
	 * 
	 * @param m
	 * @param y3
	 * @return
	 */
	Double horizontalIntercept(Double m, double y3) {

		// First line: (x1,y1) and (x2,y2)
		// Second line: (x3,y3) and (x4,y4)
		double x1 = 0;
		double y1 = 0;

		double x2 = 100;
		double y2 = x2 * m;

		double x3 = 0;

		double x4 = 100;
		double y4 = y3;

		double d = (x1 - x2) * (y3 - y4) - (y1 - y2) * (x3 - x4);
		double xi = 0;
		// Y value is not used.
		// double yi=0;
		if (d != 0) { // lines do intersect
			// coordinates of intersection point
			xi = ((x3 - x4) * (x1 * y2 - y1 * x2) - (x1 - x2)
					* (x3 * y4 - y3 * x4))
					/ d;
			// yi = ((y3-y4)*(x1*y2-y1*x2)-(y1-y2)*(x3*y4-y3*x4))/d;
		}
		return xi;
	}

	/**
	 * Parse the values for a single line in a roofline model based on the input
	 * string. Set indicates that this is a 'Spec' value if it is equal to 3.
	 * 
	 * @param in
	 * @param set
	 * @return
	 */
	ChartLine parseCounter(String in, int set) {
		String[] both = in.split(" \\(");
		Double d = Double.parseDouble(both[0].trim());
		String s = both[1].trim();
		s = s.substring(0, s.length() - 1);
		if (set == 3) {
			s = "Spec: " + s;
		}
		ChartLine tup = new ChartLine(s, d);

		return tup;
	}

	void processRoofline(Roofline r) {
		Double maxYSpec = 0.0;
		Double maxY = 0.0;
		Double maxX = 50.0;

		Double maxB = 0.0;
		Double maxBSpec = 0.0;
		for (ChartLine c : r.specGflops) {
			maxYSpec = Math.max(maxYSpec, c.opInt);
		}
		for (ChartLine c : r.gflops) {
			maxY = Math.max(maxY, c.opInt);
		}

		for (ChartLine c : r.specBandwidth) {
			maxBSpec = Math.max(maxBSpec, c.opInt);
		}
		for (ChartLine c : r.bandwidth) {
			maxB = Math.max(maxB, c.opInt);
		}

		for (ChartLine c : r.specGflops) {
			calcGFLOPSCoordinates(c, maxBSpec, maxX);
		}
		for (ChartLine c : r.gflops) {
			calcGFLOPSCoordinates(c, maxB, maxX);
		}

		for (ChartLine c : r.specBandwidth) {
			calcBandwidthCoordinates(c, maxYSpec);
		}
		for (ChartLine c : r.bandwidth) {
			calcBandwidthCoordinates(c, maxY);
		}

		for (ChartLine c : appGflops) {
			//System.out.println("appGflops");
			calcAppCoordinates(c);
		}
		r.maxY = Math.max(maxYSpec, maxY);
		r.maxX = maxX;
	}
	void processTerryRoofline(JSONRoofline r, Roofline rl) {
		if(r==null){
			System.out.println("Bad Roofline");
			rl=null;
			return;
		}
		if(r.empirical==null){
			System.out.println("No Emperical data");
			rl=null;
			return;
		}
		
		r.setSystem(rl.getSystem());
		
		JSONRooflineMetric trm = r.empirical.gbytes;// .get(GBYTE);
		Map<String, Object> md = r.empirical.metadata;// .get(METADATA);
		if (md != null) {
			//System.out.println(md.get(HOSTNAME).getClass());
			/*
			 * String hostname = md.get(HOSTNAME).toString();
			 * if(hostname!=null&&hostname.length()>0){ rl.setSystem(hostname);
			 * }
			 */
			rl.metadata.putAll(md);
		}

		md = (Map<String, Object>) r.empirical.gbytes.metadata;
		if (md != null) {
			//System.out.println(md.get(HOSTNAME).getClass());
			/*
			 * String hostname = md.get(HOSTNAME).toString();
			 * if(hostname!=null&&hostname.length()>0){ rl.setSystem(hostname);
			 * }
			 */
			rl.metadata.putAll(md);
		}
		md = (Map<String, Object>) r.empirical.gflops.metadata;
		if (md != null) {
			//System.out.println(md.get(HOSTNAME).getClass());
			/*
			 * String hostname = md.get(HOSTNAME).toString();
			 * if(hostname!=null&&hostname.length()>0){ rl.setSystem(hostname);
			 * }
			 */
			rl.metadata.putAll(md);
		}
		rl.metadata.put("System", rl.getSystem());

		for (List<Object> tuple : trm.data) {
			rl.bandwidth.add(new ChartLine(tuple, false));
		}

		trm = r.empirical.gflops;// .get(GFLOP);

		for (List<Object> tuple : trm.data) {
			rl.gflops.add(new ChartLine(tuple, false));
		}

		if (r.spec != null) {
			trm = r.spec.gbytes;
			for (List<Object> tuple : trm.data) {
				rl.specBandwidth.add(new ChartLine(tuple, true));
			}
			trm = r.spec.gflops;
			for (List<Object> tuple : trm.data) {
				rl.specGflops.add(new ChartLine(tuple, true));
			}
		}
		
		if(r.heatmap!=null){
			r.heatmap.initializeHeatMap(r.getSystem());
			rl.heatmap=r.heatmap;
		}

		processRoofline(rl);
	}

	/**
	 * Generate a complete roofline model based on the rooflines at the remote
	 * location
	 * 
	 * @return
	 */
	void readRemoteRooflines(String url) {

		List<JSONRoofline> rls = RooflineDataManager.getRemoteRooflines(url);
		if (rls == null || rls.size() == 0) {
			return;
		}
		allRooflines.clear();//removeAll(rooflines);
		for (JSONRoofline trl : rls) {
			if (trl != null) {
				Roofline rl = new Roofline();
				rl.setSystem(trl.getSystem());
				processTerryRoofline(trl, rl);
				if(rl!=null)
				{
					allRooflines.add(rl);
				}
			}
		}

	}

	/**
	 * Generate a complete roofline model based on the selected roofline dataset
	 * 
	 * @return
	 */
	void readRooflines(File f) {

		int currentSet = 0;
		// File f = new File("/home/wspear/Desktop/test.roofline");
		if (f.canRead()) {
			allRooflines.clear();//.removeAll(allRooflines);
			// System.out.println("ok");
			try {
				FileInputStream fis = new FileInputStream(f);
				BufferedReader br = new BufferedReader(new InputStreamReader(
						fis));
				String fileName = f.getName();
				if (fileName.endsWith(DOT_JSON)) {

					Gson gson = new Gson();
					if (fileName.toLowerCase().startsWith(ROOFLINE)) {
						JSONRoofline trl = gson.fromJson(br,
								JSONRoofline.class);

						Roofline rl = new Roofline();

						/**
						 * Getting the chart name from the filename is a backup
						 * option. The hostname metadata field will override
						 * this if it is available.
						 */
						int firstdot = fileName.indexOf('.');
						int lastdot = fileName.lastIndexOf('.');

						if (firstdot == lastdot) {
							rl.setSystem(fileName.substring(0, firstdot));
						} else {
							rl.setSystem(fileName.substring(firstdot + 1,
									lastdot));
						}
						processTerryRoofline(trl, rl);
						if(rl!=null)
						{
							allRooflines.add(rl);
						}

						br.close();
					}

					else {
						Roofline[] RLArray = gson
								.fromJson(br, Roofline[].class);
						for (int i = 0; i < RLArray.length; i++) {
							processRoofline(RLArray[i]);
							allRooflines.add(RLArray[i]);
						}
						br.close();
					}
					return;
				}
				Roofline r = new Roofline();
				String line = null;
				boolean first = true;
				while ((line = br.readLine()) != null) {
					// System.out.println(line);
					if (line.trim().equals(EMPTY)) {
						currentSet = 0;
						continue;
					}

					if (line.charAt(0) != ' ') {
						if (!first) {
							processRoofline(r);
							allRooflines.add(r);
							r = new Roofline();
						}

						r.setSystem(line.substring(0, line.length() - 1));

						first = false;
					} else if (line.equals(BANDWIDTH)) {
						currentSet = 1;
					} else if (line.equals(GFLOPS)) {
						currentSet = 2;
					} else if (line.equals(SPEC)) {
						currentSet = 3;
					} else {
						ChartLine tup = parseCounter(line, currentSet);
						if (currentSet == 1) {
							r.bandwidth.add(tup);
						} else if (currentSet == 2) {
							r.gflops.add(tup);
						} else if (currentSet == 3) {
							if (tup.name.contains("GFLOP")) {
								r.specGflops.add(tup);
							} else {
								r.specBandwidth.add(tup);
							}
						}
					}
				}
				processRoofline(r);
				allRooflines.add(r);
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return;

		}
	}

	public void setFunctions(List<String> selectedFuncs) {
		if (selectedFuncs != null && selectedFuncs.size() > 0) {
			this.selectedFuncs = selectedFuncs;
			displayFunctions();
		}
	}

	public void setTrials(ObservableList<Trial> trials) {

		if (trials != null && trials.size() > 0) {
			trialMeans.clear();
			trialMeans = new ArrayList<TrialResult>();
			for (Trial t : trials) {
				trialMeans.add(new TrialResult(t));
			}
			displayFunctions();
		}

	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		primaryStage.setTitle("Roofline Visualizer");
		RooflineChart rc = new RooflineChart();
		Scene scene = rc.createRooflineScene();
		primaryStage.setScene(scene);
		primaryStage.show();
	}

}
