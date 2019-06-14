package rooflineviewpart.heatmap;
import java.awt.Color;
import java.awt.Paint;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.SymbolAxis;
import org.jfree.chart.fx.ChartViewer;
import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.PaintScale;
import org.jfree.chart.renderer.xy.XYBlockRenderer;
import org.jfree.chart.title.PaintScaleLegend;
import org.jfree.data.DomainOrder;
import org.jfree.data.general.DatasetChangeListener;
import org.jfree.data.general.DatasetGroup;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYZDataset;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RectangleEdge;
import org.jfree.ui.RectangleInsets;
import org.jfree.ui.RefineryUtilities;

public class RooflineHeatmap extends ApplicationFrame {
	
	
	
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1288399536692670085L;

	private static class HeatTip implements XYToolTipGenerator{
		
		HeatMapData md=null;
		
		public HeatTip(HeatMapData md){
			this.md=md;
		}

		@Override
		public String generateToolTip(XYDataset data, int arg1, int arg2) {
			
			//System.out.println("tooltip: "+arg2);
			
			if(!(data instanceof XYZDataset))
				return null;
			
			XYZDataset zdata=(XYZDataset)data;
			
			String tip = "X: "+(int)md.xaxis[(int)zdata.getXValue(arg1, arg2)]+" \nY: "+(int)md.yaxis[(int)zdata.getYValue(arg1, arg2)]+" \nZ: "+zdata.getZValue(arg1, arg2);
			
			return tip;
		}
		
	}
	
	private static class HeatPaintScale implements PaintScale{
		
		Color lowerC;
		Color upperC;
		double lower;
		double upper;
		int alpha = 255;
		
		public HeatPaintScale(double lower,double upper,  Color upperC, Color lowerC){
		this.lower=lower;
		this.upper=upper;
		this.lowerC=lowerC;
		this.upperC=upperC;
		}

		@Override
		public double getLowerBound() {
			return lower;
		}

		@Override
		public Paint getPaint(double arg0) {
			double val=arg0;
			if(val>upper){
				val=upper;
			}
			if(val<lower){
				val=lower;
			}
			
			double scale = (val-lower)/(upper-lower);
			int r = (int)(lowerC.getRed()*scale+upperC.getRed()*(1-scale));
			int g = (int)(lowerC.getGreen()*scale+upperC.getGreen()*(1-scale));
			int b = (int)(lowerC.getBlue()*scale+upperC.getBlue()*(1-scale));
			
			return(new Color(r,g,b,alpha));
			
		}

		@Override
		public double getUpperBound() {
			return upper;
		}
		
	}
	
	
	
    /**
     * Constructs the demo application.
     *
     * @param title the frame title.
     */
    public RooflineHeatmap(String  title) {
        super(title);
//        JPanel chartPanel = createDemoPanel();
//        chartPanel.setPreferredSize(new java.awt.Dimension (500, 270));
//        setContentPane(chartPanel);
    }
    
    /**
     * Creates a sample chart.
     *
     * @param dataset the dataset.
     *
     * @return A sample chart.
     */
    private static JFreeChart createChart(HeatMapData md) {
    	if(!md.initialized)
    		return null;
    	
    	XYZDataset dataset = createDataset(md);
        SymbolAxis xAxis = new SymbolAxis("Active Working Set Size",getAxisSymbols(md.xaxis));
        xAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        xAxis.setLowerMargin(0.0);
        xAxis.setUpperMargin(0.0);
        SymbolAxis yAxis = new SymbolAxis("CPU Memory Reuse Times",getAxisSymbols(md.yaxis));
         yAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        yAxis.setLowerMargin(0.0);
        yAxis.setUpperMargin(0.0);
         XYBlockRenderer renderer = new XYBlockRenderer();
        //PaintScale scale = new GrayPaintScale(-2.0, 1.0);
         HeatPaintScale scale = new HeatPaintScale(md.minZ, md.maxZ,Color.GREEN,Color.RED);
 
        renderer.setPaintScale(scale);
        renderer.setBaseToolTipGenerator(new HeatTip(md));
        
        XYPlot plot = new XYPlot(dataset, xAxis, yAxis, renderer);
        plot.setBackgroundPaint(Color.lightGray);
        plot.setDomainGridlinesVisible(false);
        plot.setRangeGridlinePaint(Color.white);
        
        JFreeChart chart = new JFreeChart(md.title, plot);
       chart.removeLegend();
        chart.setBackgroundPaint(Color.white);
        
        
        NumberAxis zAxis= new NumberAxis("Effective Bandwidth");
        zAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        zAxis.setRange(md.minZ, md.maxZ);
        
        PaintScaleLegend psl = new PaintScaleLegend(scale, zAxis);
        psl.setAxisOffset(5.0);
        psl.setPosition(RectangleEdge.RIGHT);
        psl.setMargin(new RectangleInsets(5, 5, 5, 5));
        chart.addSubtitle(psl);
        
        
        return chart;
    }
    
    public static class HeatMapData{
    	String title="Roofline Heatmap";
		int xdim;
		int ydim;
		double[] xaxis;
		double[] yaxis;
		double[][] data;
		double maxZ;
		double minZ;
		boolean initialized=false;
		public HeatMapData(int x_size, int y_size){
			
			xdim=x_size;
			ydim=y_size;
			xaxis=new double[x_size];
			yaxis=new double[y_size];
			data=new double[x_size][y_size];
		}
		
		public HeatMapData(){
			
		}
		public HeatMapData(boolean demo){
			xdim=6;
			ydim=4;
			xaxis=new double[]{1024, 16384, 262144, 4194304, 67108864, 1073741824};
			yaxis=new double[]{1, 10, 50, 100};
			data=new double[][]{
					
					
					
					
					{0.27,    3.07 ,   5.43 ,   6.17 ,   6.23 ,   6.37},
					{0.26 ,   3.08 ,   5.45,    6.19 ,   6.26 ,   6.41},
					{0.23,    2.50 ,   4.45,    5.00 ,   5.03 ,   5.04},
					{0.15,    2.07,    6.76,    8.27,    8.46,    8.55}
					
			};
			minZ=.15;
			maxZ=8.55;
			initialized=true;
		}
		public void initializeHeatMap(String title){
			if(title==null){
				title="Roofline";
			}
			this.title=title+" Heatmap";
			minZ=data[0][0];
			maxZ=data[0][0];
			for(int i=0;i<data.length;i++){
				for(int j=0;j<data[0].length;j++){
					if(data[i][j]<minZ){
						minZ=data[i][j];
					}
					if(data[i][j]>maxZ){
						maxZ=data[i][j];
					}
				}
			}
			initialized=true;
		}
	}
    
    public static String[] getAxisSymbols(double[] values){
    	String[] symbols = new String[values.length];
    	
    	for(int i=0;i<values.length;i++){
    		symbols[i]=Integer.toString((int)values[i]);
    	}
    	
    	return symbols;
    }
    
    /**
     * Creates a sample dataset.
     */
   private static XYZDataset createDataset(HeatMapData md) {
         return new XYZDataset() {
            public int getSeriesCount() {
                return 1;
             }
            public int getItemCount(int series) {
                return md.xdim*md.ydim;
            }
             public Number getX(int series, int item) {
                return new Double(getXValue(series, item));
            }
             public double getXValue(int series, int item) {
            	 
            	 int xdex=item %md.xdim;
            	 
            	 //System.out.println("x. Item: "+item+" Val: "+xdex);
            	 
                return xdex;
            }
           public Number getY(int series, int item) {
                return new Double (getYValue(series, item));
             }
            public double getYValue(int series, int item) {
            	
            	int ydex=item/md.xdim;
            	
            	//System.out.println("y. Item: "+item+" Val: "+ydex);
            	
                return ydex;
            }
            public Number  getZ(int series, int item) {
                return new Double (getZValue(series, item));
            }
            public double getZValue(int series, int item) {
            	int xdex=item %md.xdim;
            	int ydex=item/md.xdim;
                 
                return md.data[ydex][xdex];
            }
             public void addChangeListener(DatasetChangeListener listener) {}
             public void removeChangeListener(DatasetChangeListener listener) {}
            public DatasetGroup getGroup() {
                 return null;
             }
            public void setGroup(DatasetGroup group) {
                 // ignore
 }
           public Comparable getSeriesKey(int series) {
                 return "Map Data";
            }
             public int indexOf(Comparable  seriesKey) {
                 return 0;
            }
            public DomainOrder getDomainOrder() {
                return DomainOrder.ASCENDING;
             }
         };
    }
   
   
     /**
      * Creates a panel full of heatmap
     *
      * @return A panel.
      */
     public static ChartViewer createHeatmapPanel(HeatMapData data) {
    	 
         return new ChartViewer(createChart(data));
     }
     
     /**
      * Starting point for the demonstration application.
     *
      * @param args ignored.
      */
     public static void main(String [] args) {
         RooflineHeatmap demo = new RooflineHeatmap("Roofline Heatmap");
        demo.pack();
        RefineryUtilities.centerFrameOnScreen(demo);
         demo.setVisible(true);
     }

 }

