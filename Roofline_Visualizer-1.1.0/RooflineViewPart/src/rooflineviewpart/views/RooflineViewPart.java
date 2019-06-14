package rooflineviewpart.views;

import javafx.scene.Scene;

import org.eclipse.fx.ui.workbench3.FXViewPart;

public class RooflineViewPart extends FXViewPart {
	
	RooflineChart rc =null;
	
	@Override
	protected Scene createFxScene() {
		rc = new RooflineChart();
		return rc.createRooflineScene();
	}
	

	public RooflineChart getRooflineChart(){
		return rc;
	}
	

	@Override
	protected void setFxFocus() {
		
	}
}