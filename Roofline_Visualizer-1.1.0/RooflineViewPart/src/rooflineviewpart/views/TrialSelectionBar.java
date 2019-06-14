package rooflineviewpart.views;

import java.util.ArrayList;
import java.util.List;

import rooflineviewpart.views.RooflineChart.ChartLine;
import edu.uoregon.tau.perfdmf.Trial;
import edu.uoregon.tau.perfdmf.View;
import edu.uoregon.tau.perfexplorer.common.ConfigureFiles;
import edu.uoregon.tau.perfexplorer.glue.TrialMeanResult;
import edu.uoregon.tau.perfexplorer.glue.Utilities;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextField;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class TrialSelectionBar{
	
	private TrialMeanResult trialMeans=null;
	private RooflineChart rlChart=null;
	private ToolBar theBar=null;
	
	public TrialSelectionBar(RooflineChart chart){
		rlChart=chart;
		trialSelectionBarSetup();
	}
	
	public ToolBar getTrialSelectionBar(){
		return theBar;
	}
	
	private static final String DEFAULT="Default";

	private void trialSelectionBarSetup(){
		
		
		ComboBox<String>databaseBox=new ComboBox<String>();

//		  databaseBox.setCellFactory(
//		            new Callback<ListView<String>, ListCell<String>>() {
//		                @Override public ListCell<String> call(ListView<String> param) {
//		                    final ListCell<String> cell = new ListCell<String>() {
//		                        @Override public void updateItem(String item, 
//		                            boolean empty) {
//		                                super.updateItem(item, empty);
//		                                if (item != null) {
//		                                    setText(item.substring(item.lastIndexOf(".cfg.")+5));    
//		                                }
//		                                else {
//		                                    setText(null);
//		                                }
//		                            }
//		                };
//		                return cell;
//		            }
//		        });
		     
		  
		  ObservableList<String> configFiles = FXCollections.observableList(ConfigureFiles.getConfigurationNames());
		  for(int i=0;i<configFiles.size();i++){
			  String file = configFiles.get(i);
			  int lastDot=file.lastIndexOf(".cfg.");
			  if(lastDot<0){
				  configFiles.set(i, DEFAULT);
			  }
			  else
			  {
				  configFiles.set(i, configFiles.get(i).substring(lastDot+5));
			  }
		  }
		  databaseBox.setItems( configFiles);
		  
		  
		  ComboBox<View>viewBox=new ComboBox<View>();
		  ComboBox<Trial>trialBox=new ComboBox<Trial>();
		  Button funcSearchButton=new Button("Select Functions");
		  viewBox.setDisable(true);
			trialBox.setDisable(true);
			funcSearchButton.setDisable(true);
		  //ComboBox<String> functionBox=new ComboBox<String>();
		  databaseBox.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<String>(){

				@Override
				public void changed(ObservableValue observable, String oldValue,
						String newValue) {
					if(newValue!=null)
					{
						Utilities.setSession(newValue);
						viewBox.setDisable(false);
						 ObservableList<View> views =FXCollections.observableList(Utilities.getAllViews());
						 viewBox.getItems().clear();
						 viewBox.setItems(views);
						
					}
					else
					{
						viewBox.setDisable(true);
						trialBox.setDisable(true);
						funcSearchButton.setDisable(true);
					}
				}
				  
			  });
		  
		  
		  viewBox.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<View>(){

				@Override
				public void changed(ObservableValue observable, View oldValue,
						View newValue) {
					if(newValue!=null)
					{
						 ObservableList<Trial> trials =FXCollections.observableList(Utilities.getTrialsForView(newValue));
						 trialBox.getItems().clear();
						 trialBox.setItems(trials);
						 trialBox.setDisable(false);
							
						
					}
					else{
						trialBox.setDisable(true);
						funcSearchButton.setDisable(true);
					}
				}
				  
			  });
		  final ObservableList<String> funcs = FXCollections.observableArrayList();
		  final ListView<String> list = new ListView<String>();
		  final List<String> funcList = new ArrayList<String>();
		 
		  Runnable r = new Runnable() {
            @Override public void run() {
          	  funcs.clear();
					 funcs.addAll(funcList);// =  FXCollections.observableList(funcList);//tm.getDataSource().getMeanData().getFunctionProfiles());
           	 list.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
					 list.setItems(funcs);
           	 funcSearchButton.setText("Select Functions");

					 funcSearchButton.setDisable(false);
            }
        };
		  trialBox.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Trial>(){

				@Override
				public void changed(ObservableValue observable, Trial oldValue,
						Trial newValue) {
					if(newValue!=null)
					{
						
						
						Task funcTask = new Task(){

							@Override
							protected Object call() throws Exception {
								//TrialResult t1 = new TrialResult(newValue);
								trialMeans = new TrialMeanResult(newValue);
								//System.out.println(tm.getEvents());
								funcList.clear();
								funcList.addAll(trialMeans.getEvents());
								//List<String>funcList=new ArrayList<String>(tm.getEvents());

								 Platform.runLater(r);

								 
//								 functionBox.getItems().clear();
//								 functionBox.setItems(funcs);
								 return true;
							}
						

						};
						funcSearchButton.setText("Loading Functions");
						new Thread(funcTask).start();
					
					}
					else{
						funcSearchButton.setDisable(true);
					}
				}
				  
			  });
		  
		  
		 
		  
		 funcSearchButton.setOnAction(new EventHandler<ActionEvent>(){

			
			  public void search(String oldVal, String newVal) {
				    if (oldVal != null && (newVal.length() < oldVal.length())) {
				      list.setItems(funcs);
				    }
				    String value = newVal.toUpperCase();
				    ObservableList<String> subentries = FXCollections.observableArrayList();
				    for (String entryText : list.getItems()) {
				      boolean match = true;
				      //String entryText = (String) entry;
				      if (!entryText.toUpperCase().contains(value)) {
				        match = false;
				      }
				      if (match) {
				        subentries.add(entryText);
				      }
				    }
				    list.setItems(subentries);
				  }
			 
				@Override
				public void handle(ActionEvent event) {
					
					
					
					 final Stage dialogStage = new Stage();
		                dialogStage.initModality(Modality.WINDOW_MODAL);


		                
		                
		                TextField txt = new TextField();
		                txt.setPromptText("Search");
		                txt.textProperty().addListener(new ChangeListener<Object>() {
		                  public void changed(ObservableValue observable, Object oldVal,
		                      Object newVal) {
		                    search((String) oldVal, (String) newVal);
		                  }
		                });
		                

		                Button yesBtn = new Button("Select");
		                yesBtn.setOnAction(new EventHandler<ActionEvent>() {

		                    @Override
		                    public void handle(ActionEvent arg0) {
		                    	
		                    	ObservableList<String>selectedFuncs =list.getSelectionModel().getSelectedItems();
		                    	
		                    	
		                    	if(trialMeans!=null&&selectedFuncs!=null&&selectedFuncs.size()>0)
		                    	{
		                    		String fpmet  = trialMeans.getFPMetric();
		                    		String tmet = trialMeans.getTimeMetric();
		                    		String missmet = trialMeans.getL2MissMetric();
		                    		if(fpmet!=null&&tmet!=null&&missmet!=null)
		                    		{
		                    			
		                    			rlChart.appGflops.clear();
		                    			for(String func:selectedFuncs)
		                    		{
		                    			double fp=trialMeans.getInclusive(0, func, fpmet);
		                    			double time = trialMeans.getInclusive(0, func, tmet);
		                    			double flops=fp/time/1000.0;
		                    			double miss = trialMeans.getInclusive(0, func, missmet);
		                		
		                				double opint=fp/time/miss;
		                    			
		                    			rlChart.appGflops.add(new ChartLine(func,flops,opint,false));
		                    			
		                    		}
		                    			Roofline item = rlChart.rooflineSelect.getSelectionModel().getSelectedItem();
		                    			if(item!=null)
		                    			{
		                    				rlChart.processRoofline(item);
		                    				rlChart.displayRoofline(item);
		                    			}
		                    		
		                    		}
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

		                HBox hBox = new HBox();
		                hBox.setAlignment(Pos.BASELINE_CENTER);
		                hBox.setSpacing(40.0);
		                hBox.getChildren().addAll(yesBtn, noBtn);
		                hBox.setMaxHeight(noBtn.getHeight()+10);

		                VBox vBox = new VBox();
		                vBox.setSpacing(40.0);
		                vBox.getChildren().addAll(txt,list, hBox);

		                dialogStage.setScene(new Scene(vBox));
		                dialogStage.show();
					
				}
			  });
		this.
		
		 theBar= new ToolBar(databaseBox,viewBox,trialBox,funcSearchButton);
	}
	
}
