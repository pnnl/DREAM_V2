package utilities;

import java.awt.Color;
import java.awt.Font;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JFrame;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.RectangleInsets;

public class Plot2D extends ChartPanel {

	private static final long serialVersionUID = 3725867696858493678L;

	public Plot2D(String title, String xAxisLabel, String yAxisLabel, 
			XYSeriesCollection dataset, boolean showLegend, boolean showTooltips) {
		super(ChartFactory.createXYBarChart(title, // String title
				xAxisLabel, // String x-axis label
				false, //show x-Axis in time
				yAxisLabel, // String y-axis label
				dataset, // XYDataset dataset
				PlotOrientation.VERTICAL, // PlotOrientation
				showLegend, // Legend
				showTooltips, // tooltips
				false)); // URLs
		formatChartPanel(this);
	}
	
	public static XYSeriesCollection getDatasetInt(String seriesKey, HashMap<Double, Integer> data) {
		XYSeriesCollection collection = new XYSeriesCollection();
		XYSeries series = new XYSeries(seriesKey);
		for(Double x: data.keySet()) {
			series.add(x, data.get(x));
		}
		collection.addSeries(series);
		return collection;
	}
	
	public static XYSeriesCollection getDataset(String seriesKey, Map<Double, Double> data) {
		XYSeriesCollection collection = new XYSeriesCollection();
		XYSeries series = new XYSeries(seriesKey);
		for(Double x: data.keySet()) {
			series.add(x, data.get(x));
		}
		collection.addSeries(series);
		return collection;
	}
	
	public static void addSeries(XYSeriesCollection collection, String seriesKey, Map<Double, Double> data) {
		XYSeries series = new XYSeries(seriesKey);
		for(Double x: data.keySet()) {
			series.add(x, data.get(x));
		}
		collection.addSeries(series);
	}

	public static XYSeriesCollection getDataset(Map<String, List<double[]>> data) {
		XYSeriesCollection collection = new XYSeriesCollection();
		for(String seriesKey: data.keySet()) {
			XYSeries series = new XYSeries(seriesKey);
			for(double[] point: data.get(seriesKey)) {
				series.add(point[0], point[1]);
			}
			collection.addSeries(series);
		}
		return collection;
	}
	
	public void show(String windowTitle) {
		JFrame frame = new JFrame();
		frame.setTitle(windowTitle);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().add(this);
		frame.pack();
		frame.setVisible(true);
	}

	private void formatChartPanel(ChartPanel chartPanel) {
		
		XYPlot plot = chartPanel.getChart().getXYPlot();
		chartPanel.getChart().getTitle().setFont(TITLE_FONT);

		plot.setBackgroundPaint(CHART_BACKGROUND_COLOR);
		plot.setDomainGridlinePaint(GRID_LINE_COLOR);
		plot.setRangeGridlinePaint(GRID_LINE_COLOR);
		plot.setDomainGridlinesVisible(true);
		plot.setRangeGridlinesVisible(true);
		plot.setInsets(new RectangleInsets(2, 12, 2, 12));

		// Default axis
		plot.getRangeAxis().setAutoRange(true);
		plot.getRangeAxis().setLabelFont(LABEL_FONT);
		plot.getRangeAxis().setLabelPaint(Color.BLACK);
		plot.getRangeAxis().setTickLabelFont(TICK_FONT);
		plot.getRangeAxis().setTickLabelPaint(TICK_COLOR);

		// plot.getDomainAxis().setAutoRange(true);
		plot.getDomainAxis().setRange(0, 200);
		plot.getDomainAxis().setLabelFont(LABEL_FONT);
		plot.getDomainAxis().setLabelPaint(Color.BLACK);
		plot.getDomainAxis().setTickLabelFont(TICK_FONT);
		plot.getDomainAxis().setTickLabelPaint(TICK_COLOR);

		plot.setAxisOffset(new RectangleInsets(5, 5, 5, 5));
				
		chartPanel.updateUI();

		chartPanel
				.setBorder(BorderFactory.createCompoundBorder(BorderFactory
						.createEmptyBorder(15, 15, 15, 15), BorderFactory
						.createMatteBorder(1, 1, 1, 1, new java.awt.Color(60, 60, 60))));
	}
	
	public static void main(String[] args) {
//		Map<String, List<double[]>> data = new HashMap<String, List<double[]>>();
//		Random rand = new Random();
//		for(String seriesKey: new String[]{"A", "B", "C"}) {			
//			List<double[]> points = new ArrayList<double[]>();
//			for(int i = 0; i < 20; i++) {
//				double x = rand.nextDouble()*rand.nextInt(100);
//				double y = rand.nextDouble()*rand.nextInt(100);
//				points.add(new double[]{x,y});
//			}
//			data.put(seriesKey, points);
//		}
//		new Plot2D("Test", "X", "Y", Plot2D.getDataset(data), true, true).show("Test");	
		
				
		HashMap<Double, Integer> testMap = new HashMap<Double, Integer>();
		String delims = ",";	
		
		try
		{
			FileReader fr = new FileReader("C:\\Users\\d3x078\\Desktop\\Results\\Meta_Results_130417_232629.txt");
			BufferedReader br = new BufferedReader(fr);
			String line = br.readLine();	
			while(line !=null)
			{
				String[] tokens = line.split(delims);
				testMap.put(Double.parseDouble(tokens[0]), Integer.parseInt(tokens[1]));
				line = br.readLine();
			}
			br.close();
		}
		catch (Exception e)
		{		
		}		
		
		new Plot2D("Algorithm, 1000 iterations", "E[TFD]", "Occurences", Plot2D.getDatasetInt("Series1",testMap), true, true).show("Histogram");	
	}
	
	
	protected static final Font LABEL_FONT = new Font(null, Font.PLAIN, 12);
	protected static final Color BACKGROUND_COLOR = new java.awt.Color(248, 248, 248);
	protected static final Color CHART_BACKGROUND_COLOR = Color.WHITE;
	protected static final Color GRID_LINE_COLOR = new java.awt.Color(80, 80, 80);
	protected static final Color TICK_COLOR = new java.awt.Color(60, 60, 60);
	protected static final Font TITLE_FONT = new Font(null, Font.PLAIN, 11);
	protected static final Font LABEL_FONT_BOLD = new Font(null, Font.BOLD, 12);
	protected static final Font TICK_FONT = new Font(null, Font.PLAIN, 12);
	protected static final Font EQUATION_FONT = new Font(Font.SERIF, Font.PLAIN, 18 );
	protected static final Font TEXT_FONT = new Font(Font.SERIF, Font.PLAIN, 14 );

}
