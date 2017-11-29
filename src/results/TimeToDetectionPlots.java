package results;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import objects.ExtendedConfiguration;
import objects.ScenarioSet;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.Range;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.category.SlidingCategoryDataset;
import org.jfree.ui.RectangleInsets;

/**
 * Plots displayed for the user if they enable "show plots" on the last page. Show how configurations are performing over time.
 * @author port091
 */

public class TimeToDetectionPlots extends JFrame {

	private static final long serialVersionUID = 1L;

	protected final static Color CHART_BACKGROUND_COLOR = new java.awt.Color(250, 250, 250);
	protected final static Color GRID_LINE_COLOR = new java.awt.Color(80, 80, 80);
	protected final static Color TICk_COLOR = new java.awt.Color(60, 60, 60);
	protected final static Font TITLE_FONT = new Font(null, Font.PLAIN, 16);
	protected final static Font LABEL_FONT = new Font(null, Font.PLAIN, 14);
	protected final static Font TICK_FONT = new Font(null, Font.PLAIN, 10);
	
	
	SlidingCategoryDataset newMoreThan90;	
	SlidingCategoryDataset scenariosDetected;
	
	protected JScrollBar newMoreThan90ScrollBar;
	protected JScrollBar scenariosDetectedScrollBar;
	
	double triggeringScenarios = 0;
	
	public static void main(String[] args) {
		new TimeToDetectionPlots(100, 100);
	}
	
	protected int iterations;
	protected double maxYear = 0.0;
	
	
	public TimeToDetectionPlots(int iterations, double maxyear){
		this(iterations, maxyear, 1);
	}
	
	public TimeToDetectionPlots(int iterations, double maxYear, int run) {
		
		this.maxYear = maxYear;
		this.iterations = iterations;
		newMoreThan90 = new SlidingCategoryDataset(new DefaultCategoryDataset(), 0, 20);
		scenariosDetected = new SlidingCategoryDataset(new DefaultCategoryDataset(), 0, 20);
			
		this.setTitle("Time to detection plots - Run " + run);

		this.setSize(800, 480);
		this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		initializeUI();
		
		this.setVisible(true);
	}
	
	private void initializeUI() {
		this.getContentPane().removeAll();
		
		JPanel topHalf = new JPanel();

		JPanel twentyFivePercent = new JPanel();
		JPanel allPanel = new JPanel();
		
		// First plot
		ChartPanel chartPanel25 = new ChartPanel(createChart(newMoreThan90, "Iteration", "Time to detection", "New configuration TTD (detecting scenarios only)"));
		chartPanel25.setPreferredSize(new Dimension(400, 400));
		twentyFivePercent.add(chartPanel25, BorderLayout.NORTH);
		
		newMoreThan90ScrollBar = new JScrollBar(0, 0, 20, 0, 20);
		newMoreThan90ScrollBar.getModel().addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				try {
					newMoreThan90.setFirstCategoryIndex(newMoreThan90ScrollBar.getValue());
					newMoreThan90.validateObject();
				} catch (Exception ex) {
					// Do nothing?
				}
			}
        });
		newMoreThan90ScrollBar.setPreferredSize(new Dimension(366, 20));
		twentyFivePercent.add(newMoreThan90ScrollBar, BorderLayout.CENTER);
        
		// Fourth plot
		ChartPanel chartPanelall = new ChartPanel(createChart(scenariosDetected, "Iteration", "% Scenarios detected", "Percent of scenarios detected"));
		chartPanelall.setPreferredSize(new Dimension(400, 400));
		allPanel.add(chartPanelall, BorderLayout.NORTH);


		scenariosDetectedScrollBar =  new JScrollBar(0, 0, 20, 0, 20);
		scenariosDetectedScrollBar.getModel().addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				try {
					scenariosDetected.setFirstCategoryIndex(scenariosDetectedScrollBar.getValue());
				} catch (Exception ex) {
					// Do nothing?
				}
			}
        });
		scenariosDetectedScrollBar.setPreferredSize(new Dimension(366, 20));
        allPanel.add(scenariosDetectedScrollBar, BorderLayout.CENTER);
        
        // Layout plots
		twentyFivePercent.setPreferredSize(new Dimension(400, 460));
		allPanel.setPreferredSize(new Dimension(400, 440));

		twentyFivePercent.setBackground(CHART_BACKGROUND_COLOR);
		allPanel.setBackground(CHART_BACKGROUND_COLOR);

		topHalf.setLayout(new BorderLayout());
		this.setLayout(new BorderLayout());

		topHalf.add(twentyFivePercent, BorderLayout.WEST);
		topHalf.add(allPanel, BorderLayout.EAST);

		this.add(topHalf, BorderLayout.CENTER);
		
		topHalf.setBackground(CHART_BACKGROUND_COLOR);
		this.setBackground(CHART_BACKGROUND_COLOR);
		this.getContentPane().setBackground(CHART_BACKGROUND_COLOR);
		this.getContentPane().repaint();
		this.getContentPane().validate();
	}
	
	public void addData(Results.Type type, int iteration, ExtendedConfiguration configuration, ScenarioSet set) {
				
		float totalTTDTriggerOnly = configuration.getNormalizedAverageTimeToDetection(set.getScenarioWeights());
		float detected = configuration.getNormalizedPercentScenariosDetected(set.getScenarioWeights(), set.getTotalScenarioWeight());
		
		if(type == Results.Type.New) {
			((DefaultCategoryDataset)newMoreThan90.getUnderlyingDataset()).addValue(totalTTDTriggerOnly, type.toString(), String.valueOf(iteration));
			newMoreThan90ScrollBar.setMaximum(newMoreThan90ScrollBar.getMaximum()+1); // increment this every time we add a value?
			if(newMoreThan90ScrollBar.getMaximum() > 40)
				newMoreThan90ScrollBar.setValue(newMoreThan90ScrollBar.getValue() + 1);
		}

		if(type == Results.Type.New) {
			((DefaultCategoryDataset)scenariosDetected.getUnderlyingDataset()).addValue(detected*100, type.toString(), String.valueOf(iteration));
			scenariosDetectedScrollBar.setMaximum(scenariosDetectedScrollBar.getMaximum()+1); // increment this every time we add a value?
			if(scenariosDetectedScrollBar.getMaximum() > 40)
				scenariosDetectedScrollBar.setValue(scenariosDetectedScrollBar.getValue() + 1);
		}
	}
	
	
	private JFreeChart createChart(CategoryDataset dataset, String domainAxisLabel, String rangeAxisLabel, String title) {
		
		// create the chart...
		final JFreeChart chart = ChartFactory.createLineChart(
				title,         // chart title
				domainAxisLabel,               // domain axis label
				rangeAxisLabel,                  // range axis label
				dataset,                  // data
				PlotOrientation.VERTICAL, // orientation
				true,                     // include legend
				true,                     // tooltips?
				false                     // URLs?
				);
		
		
		// set the background color for the chart...
		chart.setBackgroundPaint(CHART_BACKGROUND_COLOR);
		chart.getTitle().setFont(TITLE_FONT);
		
		CategoryPlot plot = chart.getCategoryPlot();
		
		
		plot.setBackgroundPaint(CHART_BACKGROUND_COLOR);
		plot.setDomainGridlinePaint(GRID_LINE_COLOR);
		plot.setRangeGridlinePaint(GRID_LINE_COLOR);
		plot.setDomainGridlinesVisible(true);
		plot.setRangeGridlinesVisible(true);
		plot.setInsets(new RectangleInsets(2, 12, 2, 12));
		
		plot.getRangeAxis().setLabelFont(LABEL_FONT);
		plot.getRangeAxis().setLabelPaint(Color.BLACK);
		plot.getRangeAxis().setTickLabelFont(TICK_FONT);
		plot.getRangeAxis().setTickLabelPaint(TICk_COLOR);
		plot.getRangeAxis().setRange(new Range(0, title.contains("Percent") ? 100 : maxYear));
		plot.getRangeAxis().setAutoRange(false);
		
		plot.getDomainAxis().setLabelFont(LABEL_FONT);
		plot.getDomainAxis().setLabelPaint(Color.BLACK);
		plot.getDomainAxis().setTickLabelFont(TICK_FONT);
		plot.getDomainAxis().setTickLabelPaint(TICk_COLOR);
		
		final CategoryAxis domainAxis = plot.getDomainAxis();
		domainAxis.setCategoryLabelPositions(CategoryLabelPositions.createUpRotationLabelPositions(Math.PI / 3.0));
		
		return chart;
	}
}
