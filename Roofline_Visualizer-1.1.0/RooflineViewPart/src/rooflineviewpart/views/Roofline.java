package rooflineviewpart.views;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rooflineviewpart.heatmap.RooflineHeatmap.HeatMapData;
import rooflineviewpart.views.RooflineChart.ChartLine;

public class Roofline {
	List<ChartLine> bandwidth = new ArrayList<ChartLine>();

	List<ChartLine> gflops = new ArrayList<ChartLine>();

	double maxX = 100;

	double maxY = 1000;

	Map<String, Object> metadata = new HashMap<String, Object>();

	List<ChartLine> specBandwidth = new ArrayList<ChartLine>();

	List<ChartLine> specGflops = new ArrayList<ChartLine>();

	private String System;
	
	HeatMapData heatmap=null;

	public Roofline() {

	}

	public List<ChartLine> getBandwidth() {
		return bandwidth;
	}

	public List<ChartLine> getGflops() {
		return gflops;
	}
	public List<ChartLine> getSpecBandwidth() {
		return specBandwidth;
	}
	public List<ChartLine> getSpecGflops() {
		return specGflops;
	}
	public String getSystem() {
		return System;
	}
	public void setBandwidth(List<ChartLine> bandwidth) {
		this.bandwidth = bandwidth;
	}
	public void setGflops(List<ChartLine> gflops) {
		this.gflops = gflops;
	}
	public void setSpecBandwidth(List<ChartLine> specBandwidth) {
		this.specBandwidth = specBandwidth;
	}

	public void setSpecGflops(List<ChartLine> specGflops) {
		this.specGflops = specGflops;
	}

	public void setSystem(String system) {
		System = system;
	}

	public String toString() {
		return System;
	}
	
	public HeatMapData getHeatMapData(){
		return heatmap;
	}
	
	public class JSONRoofline {

		JSONRooflineType empirical;

		JSONRooflineType spec;

		private String system = "Roofline";
		
		HeatMapData heatmap;
		
		public String getSystem() {
			return system;
		}
		public void setSystem(String system) {
			this.system = system;
		}
	}

	public class JSONRooflineMetric {
		List<List<Object>> data;
		Object metadata;
	}

	/**
	 * A complete package of roofline data including the gflops and gbytes
	 * metrics and possibly metadata
	 * 
	 * @author wspear
	 *
	 */
	public class JSONRooflineType {
		JSONRooflineMetric gbytes;
		JSONRooflineMetric gflops;
		Map<String, Object> metadata;
	}
}


