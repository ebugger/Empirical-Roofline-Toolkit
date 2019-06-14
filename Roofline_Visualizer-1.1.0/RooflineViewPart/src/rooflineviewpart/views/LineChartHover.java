package rooflineviewpart.views;

import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.scene.Cursor;
import javafx.scene.chart.Axis;
import javafx.scene.chart.LineChart;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class LineChartHover extends Application{

	
	final LineChart<Number,Number> linechart;
	
	LineChartHover(Axis<Number> xAxis, Axis<Number> yAxis){
		linechart=new LineChart<Number,Number>(xAxis,yAxis);
	}
	
	@Override
	public void start(Stage primaryStage) throws Exception {
		// TODO Auto-generated method stub
		
	}
	
	
	
	
	
	  /** a node which displays a value on hover, but is otherwise empty */
	  class HoveredThresholdNode extends StackPane {
	    HoveredThresholdNode(int priorValue, int value) {
	      setPrefSize(15, 15);
	 
	      final Label label = createDataThresholdLabel(priorValue, value);
	 
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
	 
	    private Label createDataThresholdLabel(int priorValue, int value) {
	      final Label label = new Label(value + "");
	      label.setTranslateY(-25); //Move label 25 pixels up
	      label.getStyleClass().addAll("default-color0", "chart-line-symbol", "chart-series-line");
	      label.setStyle("-fx-font-size: 20; -fx-font-weight: bold;");
	 
	      if (priorValue == 0) {
	        label.setTextFill(Color.DARKGRAY);
	      } else if (value > priorValue) {
	        label.setTextFill(Color.FORESTGREEN);
	      } else {
	        label.setTextFill(Color.FIREBRICK);
	      }
	 
	      label.setMinSize(Label.USE_PREF_SIZE, Label.USE_PREF_SIZE);
	      return label;
	    }
	  }





	public void setTitle(String string) {
		linechart.setTitle(string);
		
	}
	 

}
