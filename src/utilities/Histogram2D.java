package utilities;

import java.awt.Color;
import java.awt.Font;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JFrame;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.ClusteredXYBarRenderer;
import org.jfree.chart.renderer.xy.StandardXYBarPainter;
import org.jfree.data.statistics.HistogramDataset;
import org.jfree.ui.RectangleInsets;

/**
 * Depreciated code
 * @author port091
 */

public class Histogram2D extends ChartPanel {

	private static final long serialVersionUID = 8966381864360082983L;
	
	public Histogram2D(String title, String xAxisLabel, String yAxisLabel, 
			HistogramDataset dataset, boolean showLegend, boolean showTooltips) {
		super(ChartFactory.createHistogram(title, // String title
				xAxisLabel, // String x-axis label
				yAxisLabel, // String y-axis label
				dataset, // XYDataset dataset
				PlotOrientation.VERTICAL, // PlotOrientation
				showLegend, // Legend
				showTooltips, // tooltips
				false)); // URLs
		formatChartPanel(this);
	}
	
	public static HistogramDataset getDataset(String seriesKey, Set<Double> data, int bins) {
		HistogramDataset dataset = new HistogramDataset();
		double[] array = new double[data.size()];
		for(int i = 0; i < data.size(); i++) 
			array[i] = (Double) data.toArray()[i];
		dataset.addSeries(seriesKey, array, bins);		
		return dataset;
	}
	
	
	public static HistogramDataset getDataset(String seriesKey, List<Double> data, int bins) {
		HistogramDataset dataset = new HistogramDataset();
		double[] array = new double[data.size()];
		for(int i = 0; i < data.size(); i++) 
			array[i] = data.get(i);
		dataset.addSeries(seriesKey, array, bins);		
		return dataset;
	}
	
	
	public static HistogramDataset getDataset(String seriesKey, double[] data, int bins) {
		HistogramDataset dataset = new HistogramDataset();
		dataset.addSeries(seriesKey, data, bins);		
		return dataset;
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

		plot.setAxisOffset(new RectangleInsets(5,5,5,5));
		chartPanel.getChart().getTitle().setFont(new Font(null, Font.PLAIN, 16));
		ClusteredXYBarRenderer xyRenderer = new ClusteredXYBarRenderer(0, true);
		xyRenderer.setShadowVisible(false);
		xyRenderer.setBarPainter(new StandardXYBarPainter());
		chartPanel.getChart().getXYPlot().setRenderer(xyRenderer);
		
		// Default axis
		plot.getRangeAxis().setAutoRange(true);
		plot.getRangeAxis().setLabelFont(LABEL_FONT);
		plot.getRangeAxis().setLabelPaint(Color.BLACK);
		plot.getRangeAxis().setTickLabelFont(TICK_FONT);
		plot.getRangeAxis().setTickLabelPaint(TICK_COLOR);

		plot.getDomainAxis().setAutoRange(true);
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
		Map<String, List<double[]>> data = new HashMap<String, List<double[]>>();
		Random rand = new Random();			
		double[] points = new double[200];
		for(int i = 0; i < 200; i++) {
			points[i] = rand.nextDouble()*rand.nextInt(100);
		}
		new Histogram2D("Test", "X", "Y", Histogram2D.getDataset("A", points, 10), true, true).show("Test");		
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
