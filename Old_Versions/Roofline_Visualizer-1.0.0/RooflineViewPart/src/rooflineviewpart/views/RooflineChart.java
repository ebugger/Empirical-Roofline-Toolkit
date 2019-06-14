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
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ietf.jgss.Oid;

import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChart.Series;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToolBar;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import com.google.gson.Gson;

import edu.uoregon.tau.perfdmf.Trial;
import edu.uoregon.tau.perfexplorer.glue.TrialMeanResult;

public class RooflineChart extends Application {
	
	  FileChooser chooser = new FileChooser();
	  FileChooser.ExtensionFilter rooflineFilter = new FileChooser.ExtensionFilter("Roofline files (*.roofline)", "*.roofline");
	  FileChooser.ExtensionFilter jsonFilter = new FileChooser.ExtensionFilter("JSON files (*.json)", "*.json");
	public RooflineChart(){
		
		chooser.getExtensionFilters().add(jsonFilter);
		chooser.getExtensionFilters().add(rooflineFilter);
		
	}
	
	public static class ChartLine {
		/**
		 * The name of the value shown by this line/series
		 */
		  public final String name; 
		  
		  /**
		   * The actual value associated with this metric in this roofline model
		   */
		  public final Double value;
		  public Double startX;
		  public Double startY;
		  public Double endX;
		  public Double endY;
		  public String intersectName="";
		  public ChartLine(String name, Double val) { 
		    this.name = name; 
		    this.value = val; 
		  } 
		  
		  public ChartLine(List<Object> tuple){
			  if(tuple.size()==2)
			  {
				  if(tuple.get(0) instanceof String && tuple.get(1) instanceof Double){
					  this.name=(String) tuple.get(0);
					  this.value=(Double) tuple.get(1);
					  return;
				  }
			  }
			  this.name=null;
			  this.value=null;
			  System.out.println("Warning: Invalid ChartLine tuple");
			  
		  }
		} 
	
	public class Roofline{
		public String getSystem() {
			return System;
		}
		public void setSystem(String system) {
			System = system;
		}
		public List<ChartLine> getBandwidth() {
			return bandwidth;
		}
		public void setBandwidth(List<ChartLine> bandwidth) {
			this.bandwidth = bandwidth;
		}
		public List<ChartLine> getGflops() {
			return gflops;
		}
		public void setGflops(List<ChartLine> gflops) {
			this.gflops = gflops;
		}
		public List<ChartLine> getSpecBandwidth() {
			return specBandwidth;
		}
		public void setSpecBandwidth(List<ChartLine> specBandwidth) {
			this.specBandwidth = specBandwidth;
		}
		public List<ChartLine> getSpecGflops() {
			return specGflops;
		}
		public void setSpecGflops(List<ChartLine> specGflops) {
			this.specGflops = specGflops;
		}
		String System;
		double maxX=100;
		double maxY=1000;
		List<ChartLine> bandwidth=new ArrayList<ChartLine>();
		List<ChartLine> gflops=new ArrayList<ChartLine>();
		List<ChartLine> specBandwidth=new ArrayList<ChartLine>();
		List<ChartLine> specGflops=new ArrayList<ChartLine>();
		
		Map<String,String> metadata = new HashMap<String,String>();
		
		public Roofline(){
			
		}
		
		public String toString(){
			return System;
		}
	}
	
	/**
	 * A complete package of roofline data including the gflops and gbytes metrics and possibly metadata
	 * @author wspear
	 *
	 */
	public class TerryRooflineType{
		TerryRooflineMetric gflops;
		TerryRooflineMetric gbytes;
		Map<String,Object> metadata;
	}
	
	public class TerryRooflineMetric{
		List<List<Object>> data;
		Object metadata;
	}
//	public class TeriRooflineType{
//		List<TeriRooflineMetric> empirical;
//		
////		TeriRooflineMetric gbytes;
////		Object gflops;
//	}
	public class TerryRoofline{
		
		public void setSystem(String system){
			this.system=system;
		}
		public String getSystem(){
			return system;
		}
		String system="Roofline";
		TerryRooflineType empirical;
		TerryRooflineType spec;
		//TeriRooflineType empirical;
	}
	
	public static final String EMPTY="";
	public static final String BANDWIDTH="  Bandwidth:";
	public static final String SPEC="  Spec:";
	public static final String GFLOPS="  GFLOP/s:";
	public static final String ROOFLINE="roofline";
	public static final String DOT_JSON=".json";
	
	/**
	 * Generate a complete roofline model based on the selected roofline dataset
	 * @return
	 */
	void readRooflines(File f){
		
		
		
		int currentSet = 0;
		//File f = new File("/home/wspear/Desktop/test.roofline");
		if(f.canRead()){
			rooflines.removeAll(rooflines);
			//System.out.println("ok");
			try {
			FileInputStream fis = new FileInputStream(f);
			BufferedReader br = new BufferedReader(new InputStreamReader(fis));
			String fileName = f.getName();
			if(fileName.endsWith(DOT_JSON))
			{
				
				
				Gson gson = new Gson();
				if(fileName.toLowerCase().startsWith(ROOFLINE)){
				 TerryRoofline trl = gson.fromJson(br, TerryRoofline.class);
				 
				 Roofline rl = new Roofline();
				 
				 /**
				  * Getting the chart name from the filename is a backup option. The hostname metadata field will override this if it is available.
				  */
				int firstdot=fileName.indexOf('.');
				int lastdot=fileName.indexOf('.');
				 
				if(firstdot==lastdot){
					rl.System=fileName.substring(0,firstdot);
				}
				else
				{
					rl.System=fileName.substring(firstdot+1,lastdot);
				}
				 processTerryRoofline(trl,rl);
				 
				 rooflines.add(rl);
				 
//				 for(int i=0;i<RLArray.length;i++){
//					 processRoofline(RLArray[i]);
//					 rooflines.add(RLArray[i]);
//				 }
				 br.close();
				}
				
//				Gson gson = new Gson();
				else{
				 Roofline[] RLArray = gson.fromJson(br, Roofline[].class);
				 for(int i=0;i<RLArray.length;i++){
					 processRoofline(RLArray[i]);
					 rooflines.add(RLArray[i]);
				 }
				 br.close();
				}
				return;
			}
			Roofline r = new Roofline();
			String line = null;
			boolean first = true;
				while ((line = br.readLine()) != null) {
					//System.out.println(line);
					if(line.trim().equals(EMPTY)){
						currentSet=0;
						continue;
					}
					
					if(line.charAt(0)!=' '){
						if(!first){
							processRoofline(r);
							rooflines.add(r);
							r = new Roofline();
						}
						
						r.setSystem(line.substring(0,line.length()-1));
						
						first=false;
					}
					else if(line.equals(BANDWIDTH)){
						currentSet=1;
					}					
					else if(line.equals(GFLOPS)){
						currentSet=2;
					}
					else if(line.equals(SPEC)){
						currentSet=3;
					}else{
						ChartLine tup = parseCounter(line, currentSet);
						if(currentSet==1){
							r.bandwidth.add(tup);
						}
						else if (currentSet==2){
							r.gflops.add(tup);
						}
						else if(currentSet==3){
							if(tup.name.contains("GFLOP")){
								r.specGflops.add(tup);
							}else{
								r.specBandwidth.add(tup);
							}
						}
					}
				}
				processRoofline(r);
				rooflines.add(r);
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		 return;
			
		}
	}
	
	
	/**
	 * Generate a complete roofline model based on the rooflines at the remote location
	 * @return
	 */
	void readRemoteRooflines(String url){
		
		List<TerryRoofline> rls  = RooflineDataManager.getRemoteRooflines(url);
		if(rls==null||rls.size()==0){
			return;
		}
		rooflines.removeAll(rooflines);
		for(TerryRoofline trl:rls){
			if(trl!=null)
			{ 
				Roofline rl = new Roofline();
				rl.System=trl.getSystem();
				processTerryRoofline(trl,rl);
				rooflines.add(rl);
			}
		}
	
	}
	
	
	/**
	 * Parse the values for a single line in a roofline model based on the input string. Set indicates that this is a 'Spec' value if it is equal to 3.
	 * @param in
	 * @param set
	 * @return
	 */
	ChartLine parseCounter(String in, int set){
		String[] both = in.split(" \\(");
		Double d = Double.parseDouble(both[0].trim());
		String s = both[1].trim();
		s=s.substring(0,s.length()-1);
		if(set==3){
			s="Spec: "+s;
		}
		ChartLine tup = new ChartLine(s,d);
		
		return tup;
	}
	
	/**
	 * Put the starting and ending coordinates for a bandwidth line into val based on the y value maxY. Bandwidth starts near 0 on the x axis and treats the roofline data value as the slope
	 * @param val
	 * @param maxY
	 */
	void calcBandwidthCoordinates(ChartLine val, double maxY){
		val.startX=0.01;
		val.startY=val.startX*val.value;  
		
		
		val.endX=maxY/val.value;
		val.endY=maxY;
	}
	
	
	/**
	 * Put the starting and ending coordinates for an application line into val based on the y value maxY. App values run vertically, intercepting the x axis at their value.
	 * @param val
	 * @param maxY
	 */
	void calcAppCoordinates(ChartLine val, double maxY){
		val.startX=val.value;
		val.startY=0.01;  
		
		
		val.endX=val.value;
		val.endY=maxY;
		
		System.out.println(val.name+" From: "+val.startX+","+val.startY+" to: "+val.endX+", "+val.endY);
	}
	
	
	/**
	 * Put the starting and ending coordinates for a gflops line into val, where maxX is the end of the line and slope is the slope. Gflops lines run horizontally from the right to their intercept with a bandwidth line on the left.
	 * @param val
	 * @param slope
	 * @param maxX
	 */
	void calcGFLOPSCoordinates(ChartLine val, Double slope,Double maxX){
		
		Double minX=horizontalIntercept(slope,val.value);
		
		val.startX=minX;
		val.startY=val.value;
		
		val.endX=maxX;
		val.endY=val.value;
	}
	
	void processRoofline(Roofline r){
		Double maxYSpec=0.0;
		Double maxY=0.0;
		Double maxX=50.0;
		
		Double maxB=0.0;
		Double maxBSpec=0.0;
		for(ChartLine c:r.specGflops){
			maxYSpec=Math.max(maxYSpec, c.value);
		}
		for(ChartLine c:r.gflops){
			maxY=Math.max(maxY, c.value);
		}
		
		for(ChartLine c:r.specBandwidth){
			maxBSpec=Math.max(maxBSpec, c.value);
		}
		for(ChartLine c:r.bandwidth){
			maxB=Math.max(maxB, c.value);
		}
		
		
		for(ChartLine c:r.specGflops){
			calcGFLOPSCoordinates(c,maxBSpec,maxX);
		}
		for(ChartLine c:r.gflops){
			calcGFLOPSCoordinates(c,maxB,maxX);
		}
		
		for(ChartLine c:r.specBandwidth){
			calcBandwidthCoordinates(c,maxYSpec);
		}
		for(ChartLine c:r.bandwidth){
			calcBandwidthCoordinates(c,maxY);
		}
		
		for(ChartLine c:appGflops){
			System.out.println("appGflops");
			calcAppCoordinates(c,Math.max(maxYSpec,maxY));
		}
		r.maxY=Math.max(maxYSpec,maxY);
		r.maxX=maxX;
	}
	
	//private static final String GBYTE="gbytes";
	//private static final String GFLOP="gflops";
	private static final String HOSTNAME="HOSTNAME";
	//private static final String METADATA="metadata";
	
	void processTerryRoofline(TerryRoofline r, Roofline rl){
		TerryRooflineMetric trm = r.empirical.gbytes;//.get(GBYTE);
		 Map<?, ?> md = r.empirical.metadata;//.get(METADATA);
		if(md!=null){
			System.out.println(md.get(HOSTNAME).getClass());
			String hostname = md.get(HOSTNAME).toString();
			if(hostname!=null&&hostname.length()>0){
				rl.setSystem(hostname);
			}
		}
		
		
		for(List<Object> tuple:trm.data){
			rl.bandwidth.add(new ChartLine(tuple));
		}
		
		
		trm=r.empirical.gflops;//.get(GFLOP);
		
		

		for(List<Object> tuple:trm.data){
			rl.gflops.add(new ChartLine(tuple));
		}

		processRoofline(rl);
	}
	
	/**
	 * Calculates the x coordinate of the intercept point of a line through the origin of slope m and a horizontal line with y=y2
	 * @param m
	 * @param y3
	 * @return
	 */
	Double horizontalIntercept(Double m, double y3){
		
		 // First line: (x1,y1) and (x2,y2)
		 // Second line: (x3,y3) and (x4,y4)
		double x1 = 0;
		double y1=  0;
		
		double x2=100;
		double y2=x2*m;
		
		double x3=0;
		
		double x4=100;
		double y4=y3;
		
		 double d = (x1-x2)*(y3-y4) - (y1-y2)*(x3-x4);
		 double xi=0;
		 //Y value is not used.
		 //double yi=0;
		 if (d != 0)  { // lines do intersect
		    // coordinates of intersection point
		    xi = ((x3-x4)*(x1*y2-y1*x2)-(x1-x2)*(x3*y4-y3*x4))/d;
		    //yi = ((y3-y4)*(x1*y2-y1*x2)-(y1-y2)*(x3*y4-y3*x4))/d;
		}
		return xi;
	}
	
	Series<Number,Number> createSeries(ChartLine cl){
		XYChart.Series<Number, Number> series= new XYChart.Series<Number, Number>();
        series.setName(cl.name);
        XYChart.Data<Number, Number> data;
        
        data=new XYChart.Data<Number, Number>(cl.startX, cl.startY);
        data.setNode(
    	          new HoveredThresholdNode(
    	        	cl.name,
    	              cl.startX,
    	              cl.startY
    	          ));
        series.getData().add(data);

        
        data=new XYChart.Data<Number, Number>(cl.endX, cl.endY);
  	    data.setNode(
  		          new HoveredThresholdNode(
  		        		  cl.name,
  		        		 cl.endX,
  	    	              cl.endY
  	    	          ));
        series.getData().add(data);
        
        //System.out.println(cl.name+" is ("+cl.startX+","+cl.startY+") to ("+cl.endX+","+cl.endY+")");
        
        return series;
	}
	
//	/** @return plotted y values for monotonically increasing integer x values, starting from x=1 */
//	  public ObservableList<XYChart.Data<Double, Double>> plot(double... y) {
//	    final ObservableList<XYChart.Data<Double, Double>> dataset = FXCollections.observableArrayList();
//	    int i = 0;
//	    while (i < y.length) {
//	      final XYChart.Data<Double, Double> data = new XYChart.Data<>(i + 1.0, y[i]);
//	      data.setNode(
//	          new HoveredThresholdNode(
//	              (i == 0) ? 0 : y[i-1],
//	              y[i]
//	          )
//	      );
//	 
//	      dataset.add(data);
//	      i++;
//	    }
//	 
//	    return dataset;
//	  }
	 
	  /** a node which displays a value on hover, but is otherwise empty */
	  class HoveredThresholdNode extends StackPane {
	    HoveredThresholdNode(String name, double xCoord, double yCoord) {
	      setPrefSize(15, 15);
	 
	      final Label label = createDataThresholdLabel(name, xCoord, yCoord);
	 
	      setOnMouseEntered(new EventHandler<MouseEvent>() {
	        @Override public void handle(MouseEvent mouseEvent) {
	          getChildren().setAll(label);
	          setCursor(Cursor.NONE);
	          toFront();
	        }
	      });
	      setOnMouseExited(new EventHandler<MouseEvent>() {
	        @Override public void handle(MouseEvent mouseEvent) {
	          getChildren().clear();
	          setCursor(Cursor.CROSSHAIR);
	        }
	      });
	    }
	 
	    private Label createDataThresholdLabel(String name, double xCoord, double yCoord) {
	    	NumberFormat formatter = new DecimalFormat("#0.00");   
	      final Label label = new Label("Flops/Byte: "+formatter.format(xCoord) + " \nGFlops/Sec: "+formatter.format(yCoord));
	      label.setTranslateY(-35); //Move label 25 pixels up
//	      label.getStyleClass().addAll("default-color0", "chart-line-symbol", "chart-series-line");
	      label.setStyle("-fx-font-size: 20; -fx-font-weight: bold;");
//	 
//	      if (priorValue == 0) {
//	        label.setTextFill(Color.DARKGRAY);
//	      } else if (value > priorValue) {
//	        label.setTextFill(Color.FORESTGREEN);
//	      } else {
//	        label.setTextFill(Color.FIREBRICK);
//	      }
	 
	      label.setMinSize(Label.USE_PREF_SIZE, Label.USE_PREF_SIZE);
	      return label;
	    }
	  }
	  
//	  private static ObservableList<String> rooflineList(ArrayList<Roofline> list){
//		  ObservableList<String> oaList = FXCollections.observableArrayList();
//		  for(Roofline r:list){
//			  oaList.add(r.getSystem());
//		  }
//		  return oaList;
//	  }
	  
	  List<ChartLine> appGflops=new ArrayList<ChartLine>();
	  ToolBar toolBar=null;
	  ComboBox<Roofline> rooflineSelect;
	  public Scene createRooflineScene(){
		  
		  rooflineSelect = new ComboBox<Roofline>();
		  rooflineSelect.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Roofline>(){

			@Override
			public void changed(ObservableValue observable, Roofline oldValue,
					Roofline newValue) {
				if(newValue!=null)
				{
					displayRoofline(newValue);
				}
			}
			  
		  });
		  
		 
			
			
		  
		  Button loadButton = new Button("Load");
		  loadButton.setOnAction(new EventHandler<ActionEvent>(){

			@Override
			public void handle(ActionEvent event) {
				
				
				File file = chooser.showOpenDialog(null);
				if(file!=null){
					chooser.setInitialDirectory(file.getParentFile());
					readRooflines(file);
					
					rooflineSelect.setItems(rooflines);
					
					Roofline selected=rooflineSelect.getItems().get(0);
					
					rooflineSelect.setValue(selected);
				}
				
			}
		  });
		  
		  
		  Button loadRemoteButton = new Button("Load Remote");
		  String baseurl="http://nic.uoregon.edu/~wspear/roofline";
		  //TextField remoteBase = new TextField(baseurl);
		  //remoteBase.setText(value);
		  loadRemoteButton.setOnAction(new EventHandler<ActionEvent>(){

				@Override
				public void handle(ActionEvent event) {
					
					
					 
					  readRemoteRooflines(baseurl);//remoteBase.getText());
					  rooflineSelect.setItems(rooflines);
					  Roofline selected=rooflineSelect.getItems().get(0);
						
						rooflineSelect.setValue(selected);
					
				}
			  });
		  
		  

		  
		  toolBar = new ToolBar(rooflineSelect,loadButton,loadRemoteButton);//TODO: Make remote support internal,loadRemoteButton,remoteBase);//,
		  
		  
		  
		  
		  ToolBar tb = null;
		  if(standalone){
			  TrialSelectionBar tsb = new TrialSelectionBar(this);
			  tb=tsb.getTrialSelectionBar();
		  }
		  else{
			  EasyTrialSelectionBar etsb = new EasyTrialSelectionBar(this);
			  tb=etsb.getTrialSelectionBar();
		  }
		  
		  
		  final ToolBar bottomToolBar=tb;
		  
			
		  final BorderPane bpane = new BorderPane();
	        //stage.setTitle("Roofline Chart Sample");
	        final LogarithmicAxis xAxis = new LogarithmicAxis(0.01, 100);
	        xAxis.setLabel("Operational Intensity (Flops/Byte)");
	        
	        final LogarithmicAxis yAxis = new LogarithmicAxis(1.0,1000);
	        yAxis.setLabel("Attainable GFlops/sec");
	        ac = 
	            new LineChart<Number,Number>(xAxis,yAxis);
	        
	        
	        toolBar.setPrefWidth(800);
	        ac.setPrefSize(800, 600);
	        
	        //Group root = new Group();
	        //root.getChildren().add(toolBar);
	        //root.getChildren().add(ac);
	        bpane.setTop(toolBar);
	        bpane.setCenter(ac);
	        bpane.setBottom(bottomToolBar);
	        Scene scene  = new Scene(bpane,800,600);
			
	       
	        return scene;
	  }
	  

	  private List<TrialMeanResult> trialMeans=new ArrayList<TrialMeanResult>();
	  public void setTrials(ObservableList<Trial> trials){
		  
		  if(trials!=null&&trials.size()>0){
			  trialMeans.clear();
			  for(Trial t:trials){
				  trialMeans.add(new TrialMeanResult(t));
			  }
			  displayFunctions();
		  }

	  }
	  
	  private List<String> selectedFuncs=null;
	  public void setFunctions(List<String> selectedFuncs){
		  if(selectedFuncs!=null&&selectedFuncs.size()>0)
		  {
			  this.selectedFuncs=selectedFuncs;
			  displayFunctions();
		  }
	  }
	  
	  
	  public void displayFunctions(){
		  
		  
		  
		  String fpmet  = trialMeans.get(0).getFPMetric();
			String tmet = trialMeans.get(0).getTimeMetric();
			if(selectedFuncs!=null&&fpmet!=null&&tmet!=null)
			{

				appGflops.clear();

				

			}
			else{
				return;
			}
		  
			for(String functest:selectedFuncs)
			{
				String func=".TAU application";
				Set<String> events = trialMeans.get(0).getEvents();
				String noParams=functest.substring(0,functest.indexOf('('));
				for(String s:events){
					if(s.startsWith(noParams)){
						func=s;
						break;
					}
				}
				
				
				double fp=trialMeans.get(0).getInclusive(0, func, fpmet);
				double time = trialMeans.get(0).getInclusive(0, func, tmet);
				double flops=fp/time/1000.0;

				appGflops.add(new ChartLine(func,flops));

			}
		  
		  Roofline item = rooflineSelect.getSelectionModel().getSelectedItem();
			if(item!=null)
			{
				processRoofline(item);
				displayRoofline(item);
			}
	  }
	  
	  ObservableList<Roofline> rooflines = FXCollections.observableArrayList();
	  
	  LineChart<Number,Number> ac = null;
	  
	  void displayRoofline(Roofline r){
		  
		  LogarithmicAxis yAxis=(LogarithmicAxis) ac.getYAxis();
		  yAxis.setUpperBound(r.maxY*2);
		  
		  LogarithmicAxis xAxis=(LogarithmicAxis) ac.getXAxis();
		  xAxis.setUpperBound(r.maxX/2);
		  
		  ac.setTitle("Roofline: "+r.System);
		  ac.getData().removeAll(ac.getData());
		  for(ChartLine c:r.specGflops){
				ac.getData().add(createSeries(c));
			}
			
			for(ChartLine c:r.specBandwidth){
				ac.getData().add(createSeries(c));
			}
			
			for(ChartLine c:r.gflops){
				ac.getData().add(createSeries(c));
			}
			
			for(ChartLine c:r.bandwidth){
				ac.getData().add(createSeries(c));
			}
			
			//TODO: Eventually this should be more strongly associated with the current roofline
			for(ChartLine c:appGflops){
				ac.getData().add(createSeries(c));
			}
			
	  }
	  
	  private static boolean standalone=false;
	  
	public static void main(String[] args) {
		standalone=true;
		launch(args);

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
