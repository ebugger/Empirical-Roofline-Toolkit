package rooflineviewpart.views;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

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
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextField;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import edu.uoregon.tau.common.MetaDataMap;
import edu.uoregon.tau.common.MetaDataMap.MetaDataKey;
import edu.uoregon.tau.common.MetaDataMap.MetaDataValue;
import edu.uoregon.tau.perfdmf.Trial;
import edu.uoregon.tau.perfdmf.View;
import edu.uoregon.tau.perfexplorer.common.ConfigureFiles;
import edu.uoregon.tau.perfexplorer.glue.TrialMeanResult;
import edu.uoregon.tau.perfexplorer.glue.Utilities;

public class EasyTrialSelectionBar{

	private TrialMeanResult trialMeans=null;
	private RooflineChart rlChart=null;
	private ToolBar theBar=null;

	public EasyTrialSelectionBar(RooflineChart chart){
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

		ComboBox<View>applicationBox=new ComboBox<View>();

		//ComboBox<View>viewBox=new ComboBox<View>();
		//ComboBox<Trial>trialBox=new ComboBox<Trial>();
		//Button funcSearchButton=new Button("Select Functions");
		Button trialSearchButton=new Button("Select Trial");
		//viewBox.setDisable(true);
		//trialBox.setDisable(true);
		applicationBox.setDisable(true);
		//funcSearchButton.setDisable(true);
		trialSearchButton.setDisable(true);
		//ComboBox<String> functionBox=new ComboBox<String>();
		databaseBox.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<String>(){

			@Override
			public void changed(ObservableValue observable, String oldValue,
					String newValue) {
				if(newValue!=null)
				{
					int res = -2;
					if(newValue.equals(DEFAULT)){
						res=Utilities.setSession("");
					}
					else
					{
						res=Utilities.setSession(newValue);
					}
					System.out.println("session result = "+res);
					//viewBox.setDisable(false);
					applicationBox.setDisable(false);
					List<View>views=Utilities.getAllViews();
					List<View>applications=new ArrayList<View>();
					for(View v:views){
						if(v.toString().startsWith("Application-")){
							applications.add(v);
						}
					}
					ObservableList<View> olApplications =FXCollections.observableList(applications);
					applicationBox.getItems().clear();
					applicationBox.setItems(olApplications);

				}
				else
				{
					applicationBox.setDisable(true);
					//trialBox.setDisable(true);
					trialSearchButton.setDisable(true);
					//funcSearchButton.setDisable(true);
				}
			}

		});

		final ObservableList<Trial> trialOList = FXCollections.observableArrayList();
		final ListView<Trial> trialListView = new ListView<Trial>();
		final List<Trial> trialList = new ArrayList<Trial>();
		
		Runnable trialSetupRunner = new Runnable() {
			@Override public void run() {
				trialOList.clear();
				trialOList.addAll(trialList);// =  FXCollections.observableList(funcList);//tm.getDataSource().getMeanData().getFunctionProfiles());
				trialListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
				trialListView.setItems(trialOList);
				trialSearchButton.setText("Select Trials");

				trialSearchButton.setDisable(false);
			}
		};
		
		

		applicationBox.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<View>(){

			@Override
			public void changed(ObservableValue observable, View oldValue,
					View newValue) {
				if(newValue!=null)
				{
					Task<Object> trialTask = new Task<Object>(){

						@Override
						protected Object call() throws Exception {
							//TrialResult t1 = new TrialResult(newValue);
							//trialMeans = new TrialMeanResult(newValue);
							//System.out.println(tm.getEvents());
							trialList.clear();
							trialList.addAll(Utilities.getTrialsForView(newValue, true));
							//List<String>funcList=new ArrayList<String>(tm.getEvents());

							Platform.runLater(trialSetupRunner);


							//								 functionBox.getItems().clear();
							//								 functionBox.setItems(funcs);
							return true;
						}


					};
					trialSearchButton.setText("Loading Functions");
					new Thread(trialTask).start();
//
					
					
					
					

				}
				else{
					//applicationBox.setDisable(true);
					trialSearchButton.setDisable(true);
					//funcSearchButton.setDisable(true);
				}
			}

		});



		trialSearchButton.setOnAction(new EventHandler<ActionEvent>(){

			@Override
			public void handle(ActionEvent event) {
				
				
				
				final Stage dialogStage = new Stage();
				dialogStage.initModality(Modality.WINDOW_MODAL);

				
				
				HashMap<MetaDataKey,HashSet<String>> metaHash = new HashMap<MetaDataKey,HashSet<String>>();
				
				for(Trial t:trialOList){
					MetaDataMap mdm = t.getMetaData();
					if(mdm!=null){
						
						for(Entry<MetaDataKey, MetaDataValue> mde:mdm.entrySet()){
							HashSet<String>mdv= metaHash.get(mde.getKey());
							if(mdv==null){
								mdv=new HashSet<String>();
								metaHash.put(mde.getKey(), mdv);
							}
							mdv.add(mde.getValue().toString());
						}
					}
				}
				
				Iterator<Entry<MetaDataKey,HashSet<String>>> emapit = metaHash.entrySet().iterator();
				while(emapit.hasNext()){
					Entry<MetaDataKey,HashSet<String>> emap = emapit.next();
					if(emap.getValue().size()<=1){
						emapit.remove();
					}
				}

				ArrayList<MetaDataKey> sortedList = new ArrayList<MetaDataKey>(metaHash.keySet());
				sortedList.sort(new KeyComparator());
				final ObservableList<MetaDataKey> OKeys=FXCollections.observableList(sortedList);//Sort this
				
			
				final Map<MetaDataKey,String> searchMetaMap = new HashMap<MetaDataKey,String>();
				

				Label txtLabel=new Label();
				txtLabel.setText("Name Search");
				
				TextField txt = new TextField();
				txt.setPromptText("Search");
				txt.textProperty().addListener(new ChangeListener<Object>() {
					public void changed(ObservableValue observable, Object oldVal,
							Object newVal) {
						search((String) oldVal, (String) newVal,trialListView,trialOList,searchMetaMap,false);
					}
				});


				Button yesBtn = new Button("Select");
				yesBtn.setOnAction(new EventHandler<ActionEvent>() {

					@Override
					public void handle(ActionEvent arg0) {

						ObservableList<Trial>selectedTrial =trialListView.getSelectionModel().getSelectedItems();

						if(selectedTrial!=null&&selectedTrial.size()>0){
							rlChart.setTrials(selectedTrial);
							rlChart.displayFunctions();
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

				
				
				
				VBox textBox = new VBox();
				textBox.getChildren().addAll(txtLabel,txt);
				
				HBox hBox = new HBox();
				hBox.setAlignment(Pos.BASELINE_CENTER);
				hBox.setSpacing(40.0);
				hBox.getChildren().addAll(yesBtn, noBtn);
				hBox.setMaxHeight(noBtn.getHeight()+10);

				VBox metaDataBox = new VBox();
				VBox vBox = new VBox();
				
				
				
				
				
				Button addSearch=new Button("Add Search Criteria");
				addSearch.setOnAction(new EventHandler<ActionEvent>() {

					@Override
					public void handle(ActionEvent event) {
						
						HBox metaBox = new HBox();
						ComboBox<MetaDataKey> keyCombo=new ComboBox<MetaDataKey>();
						keyCombo.setItems(OKeys);
						
						ComboBox<String> valueCombo=new ComboBox<String>();
						valueCombo.setDisable(true);
						
						keyCombo.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<MetaDataKey>(){

							@Override
							public void changed(
									ObservableValue<? extends MetaDataKey> observable,
									MetaDataKey oldValue, MetaDataKey newValue) {
									
									if(newValue==null||newValue.toString().length()<1){
										valueCombo.setDisable(true);
										valueCombo.setItems(null);
										return;
									}
									
									if(searchMetaMap.containsKey(newValue)){
										keyCombo.setValue(oldValue);
										return;
									}
									
									List<String> valueList=new ArrayList<String>(metaHash.get(newValue));
									valueList.sort(Collator.getInstance(Locale.US));
									ObservableList<String> metaOValues = FXCollections.observableList(valueList);
									valueCombo.setItems(metaOValues);
									valueCombo.setDisable(false);
							}
							
						});
						
						
						valueCombo.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<String>(){

							@Override
							public void changed(
									ObservableValue<? extends String> observable,
									String oldValue, String newValue) {
								if(newValue==oldValue){
									return;
								}
								
								searchMetaMap.put(keyCombo.getValue(), newValue);
								search(txt.getText(), txt.getText(),trialListView,trialOList,searchMetaMap,false);
								
							}
							
						});
						
						
						Button remove = new Button("-");
						remove.setOnAction(new EventHandler<ActionEvent>(){

							@Override
							public void handle(ActionEvent event) {
								searchMetaMap.remove(keyCombo.getValue());
								metaDataBox.getChildren().remove(metaBox);
								search(txt.getText(), txt.getText(),trialListView,trialOList,searchMetaMap,true);
								
							}
							
						});
						metaBox.getChildren().addAll(keyCombo,valueCombo,remove);
						metaDataBox.getChildren().add(metaBox);
					}
					
				});
				
				
				
				
				//vBox.setSpacing(40.0);
				vBox.getChildren().addAll(textBox,metaDataBox,addSearch,trialListView, hBox);

				dialogStage.setScene(new Scene(vBox));
				dialogStage.show();
				
				
				
				
				
				

			}

		});



		theBar= new ToolBar(databaseBox,applicationBox,trialSearchButton);
	}
	
	
	
	
	static class KeyComparator implements Comparator<MetaDataKey>{

		@Override
		public int compare(MetaDataKey o1, MetaDataKey o2) {
			return o1.toString().compareTo(o2.toString());
		}
		
	}
	
	
	
	/**
	 * 
	 * @param oldVal The previous filter value
	 * @param newVal The current filter value
	 * @param list The list object where the names are displayed
	 * @param names Every name, totally unfiltered
	 */
	public static void search(String oldVal, String newVal, ListView<Trial> list, ObservableList<Trial> names, Map<MetaDataKey,String> metaMap,boolean clear) {
		//If we have an oldval and the newval is shorter we started typing something else, reset the list
		if (clear || (oldVal != null && (newVal.length() < oldVal.length()))) {
			list.setItems(names);
		}
		
		String value = newVal.toUpperCase();
		ObservableList<Trial> subentries = FXCollections.observableArrayList();
		for (Trial entryText : list.getItems()) {
			
			if (entryText.toString().toUpperCase().contains(value)&&metaTest(entryText,metaMap)) {
				
				subentries.add(entryText);
			}

		}
		
		
		
		list.setItems(subentries);
	}
	
	private static boolean metaTest(Trial t,Map<MetaDataKey,String> metaMap){
		if(metaMap!=null&&metaMap.size()>0){
			for(Entry<MetaDataKey,String> e:metaMap.entrySet()){
				MetaDataMap m = t.getMetaData();
				if(m==null){
					return false;
				}
				MetaDataKey k = e.getKey();
				MetaDataValue mdval=m.get(k);
				
				if(mdval==null){
					return false;
				}
				
				String val = mdval.toString();
				if(val==null||!e.getValue().equals(val)){
					return false;
				}
			}
		}
		return true;
	}


}
