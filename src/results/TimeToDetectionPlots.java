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
import objects.Scenario;
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

import utilities.Constants;

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
	SlidingCategoryDataset bestMoreThan90;
	
	ChartPanel bestMoreThan90Plot;
	
	SlidingCategoryDataset perScenarioTTD;
	SlidingCategoryDataset scenariosDetected;
	
	protected JScrollBar newMoreThan90ScrollBar;
	protected JScrollBar bestMoreThan90ScrollBar;
	protected JScrollBar perScenarioTTDScrollBar;
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
		bestMoreThan90 = new SlidingCategoryDataset(new DefaultCategoryDataset(), 0, 20);
		perScenarioTTD = new SlidingCategoryDataset(new DefaultCategoryDataset(), 0, 20);
		scenariosDetected = new SlidingCategoryDataset(new DefaultCategoryDataset(), 0, 20);
			
		this.setTitle("Time to detection plots - Run " + run);

		this.setSize(800, 900);
		this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		initializeUI();
		
		this.setVisible(true);
	}
	
	private void initializeUI() {
		this.getContentPane().removeAll();
		
		JPanel topHalf = new JPanel();
		JPanel bottomHalf = new JPanel();

		JPanel twentyFivePercent = new JPanel();
		JPanel fiftyPercent = new JPanel();
		JPanel seventyFivePercent = new JPanel();
		JPanel allPanel = new JPanel();

		// ChartPanel chartPanel25 = new ChartPanel(createChart(lessThan25, "Detected in 25% or less scenarios"));
		ChartPanel chartPanel25 = new ChartPanel(createChart(newMoreThan90, "Iteration", "Time to detection", "New configuration TTD (detecting scenarios only)"));
	//	ChartPanel chartPanel25 = new ChartPanel(createChart(new SlidingCategoryDataset(newMoreThan90, 0, 100), "New configuration TTD when detected in 90% or more scenarios"));
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
	
		double percenttriggeringScenarios = triggeringScenarios * 100;
		bestMoreThan90Plot = new ChartPanel(createChart(bestMoreThan90, "Iteration", "Time to detection", "Best configuration TTD when detected in "+Constants.decimalFormat.format(percenttriggeringScenarios)+"% or more scenarios"));
	//	ChartPanel chartPanel50 = new ChartPanel(createChart(new SlidingCategoryDataset(bestMoreThan90, 0, 100), "Best configuration TTD when detected in 90% or more scenarios"));
	//	ChartPanel chartPanel50 = new ChartPanel(createChart(lessThan50, "Detected in 25-50% of scenarios"));
		bestMoreThan90Plot.setPreferredSize(new Dimension(400, 400));
		fiftyPercent.add(bestMoreThan90Plot, BorderLayout.NORTH);

		bestMoreThan90ScrollBar = new JScrollBar(0, 0, 20, 0, 20);
		bestMoreThan90ScrollBar.getModel().addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				try {
					bestMoreThan90.setFirstCategoryIndex(bestMoreThan90ScrollBar.getValue());		
				} catch (Exception ex) {
					// Do nothing?
				}
			}        	
        });
		bestMoreThan90ScrollBar.setPreferredSize(new Dimension(366, 20));
		fiftyPercent.add(bestMoreThan90ScrollBar, BorderLayout.CENTER);
	
		
		ChartPanel chartPanel75 = new ChartPanel(createChart(perScenarioTTD, "Scenario",  "Time to detection", "TTD for each scenario"));
	//	ChartPanel chartPanel75 = new ChartPanel(createChart(new SlidingCategoryDataset(perScenarioTTD, 0, 100), "TTD for each scenario"));
	//	ChartPanel chartPanel75 = new ChartPanel(createChart(lessThan75, "Detected in 50-75% of scenarios"));
		chartPanel75.setPreferredSize(new Dimension(400, 400));
		chartPanel75.getChart().getLegend().setVisible(false);
		seventyFivePercent.add(chartPanel75, BorderLayout.NORTH);
		
		perScenarioTTDScrollBar =  new JScrollBar(0, 0, 20, 0, 20);
		perScenarioTTDScrollBar.getModel().addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				try {
					perScenarioTTD.setFirstCategoryIndex(perScenarioTTDScrollBar.getValue());		
				} catch (Exception ex) {
					// Do nothing?
				}
			}        	
        });
		perScenarioTTDScrollBar.setPreferredSize(new Dimension(366, 20));
		seventyFivePercent.add(perScenarioTTDScrollBar, BorderLayout.CENTER);
               
		
		ChartPanel chartPanelall = new ChartPanel(createChart(scenariosDetected, "Iteration", "% Scenarios detected", "Percent of scenarios detected"));
//		ChartPanel chartPanelall = new ChartPanel(createChart(new SlidingCategoryDataset(scenariosDetected, 0, 100), "Percent of scenarios detected"));
	//	ChartPanel chartPanelall = new ChartPanel(createChart(moreThan75, "Detected in 75% or more scenarios"));
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
               
		twentyFivePercent.setPreferredSize(new Dimension(400, 460));
		fiftyPercent.setPreferredSize(new Dimension(400, 460));
		seventyFivePercent.setPreferredSize(new Dimension(400, 440));
		allPanel.setPreferredSize(new Dimension(400, 440));

		twentyFivePercent.setBackground(CHART_BACKGROUND_COLOR);
		fiftyPercent.setBackground(CHART_BACKGROUND_COLOR);
		seventyFivePercent.setBackground(CHART_BACKGROUND_COLOR);
		allPanel.setBackground(CHART_BACKGROUND_COLOR);

		topHalf.setLayout(new BorderLayout());
		bottomHalf.setLayout(new BorderLayout());
		this.setLayout(new BorderLayout());

		topHalf.add(twentyFivePercent, BorderLayout.WEST);
		topHalf.add(fiftyPercent, BorderLayout.EAST);

		bottomHalf.add(seventyFivePercent, BorderLayout.WEST);
		bottomHalf.add(allPanel, BorderLayout.EAST);

		this.add(topHalf, BorderLayout.NORTH);
		this.add(bottomHalf, BorderLayout.SOUTH);
		
		topHalf.setBackground(CHART_BACKGROUND_COLOR);
		bottomHalf.setBackground(CHART_BACKGROUND_COLOR);
		this.setBackground(CHART_BACKGROUND_COLOR);
		this.getContentPane().setBackground(CHART_BACKGROUND_COLOR);
		this.getContentPane().repaint();
		this.getContentPane().validate();
	}

	public void addData(Results.Type type, int iteration, ExtendedConfiguration configuration, ScenarioSet set) {
		

		boolean addTick = false;
		
		// These will contain just the detecting scenarios: configuration.getTimesToDetection()	
		for(Scenario scenario: configuration.getTimesToDetection().keySet()) {
			double unweightedTTD = configuration.getTimesToDetection().get(scenario);
			if(type == Results.Type.New) {
				((DefaultCategoryDataset)perScenarioTTD.getUnderlyingDataset()).addValue(unweightedTTD, scenario.getScenario(), String.valueOf(iteration));
				addTick = true;
			}
		}

		if(addTick) {
			perScenarioTTDScrollBar.setMaximum(perScenarioTTDScrollBar.getMaximum()+1);
			if(perScenarioTTDScrollBar.getMaximum() > 40)
				perScenarioTTDScrollBar.setValue(perScenarioTTDScrollBar.getValue() + 1);
		}
		
		float totalTTDTriggerOnly = configuration.getNormalizedAverageTimeToDetection(set.getScenarioWeights());
		float detected = configuration.getNormalizedPercentScenariosDetected(set.getScenarioWeights(), set.getTotalScenarioWeight());
		
		if(type == Results.Type.New) {
			((DefaultCategoryDataset)newMoreThan90.getUnderlyingDataset()).addValue(totalTTDTriggerOnly, type.toString(), String.valueOf(iteration));
			newMoreThan90ScrollBar.setMaximum(newMoreThan90ScrollBar.getMaximum()+1); // increment this every time we add a value?
			if(newMoreThan90ScrollBar.getMaximum() > 40)
				newMoreThan90ScrollBar.setValue(newMoreThan90ScrollBar.getValue() + 1);
		} else if(type == Results.Type.Best && detected >= this.triggeringScenarios) {
			if(detected != this.triggeringScenarios) {
				this.triggeringScenarios = detected;
				((DefaultCategoryDataset)bestMoreThan90.getUnderlyingDataset()).clear();
				bestMoreThan90Plot.getChart().setTitle("Best configuration TTD when detected in "+Constants.decimalFormat.format(triggeringScenarios*100)+"% or more scenarios");						
				bestMoreThan90ScrollBar.setMaximum(20);
			}
			((DefaultCategoryDataset)bestMoreThan90.getUnderlyingDataset()).addValue(totalTTDTriggerOnly, type.toString(), String.valueOf(iteration));			
			bestMoreThan90ScrollBar.setMaximum(bestMoreThan90ScrollBar.getMaximum()+1); // increment this every time we add a value?
			if(bestMoreThan90ScrollBar.getMaximum() > 40)
				bestMoreThan90ScrollBar.setValue(bestMoreThan90ScrollBar.getValue() + 1);
		
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

		// set the range axis to display integers only...
		//final NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
		//rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
		
		// disable bar outlines...
		/*
		final BarRenderer renderer = (BarRenderer) plot.getRenderer();

		// set up gradient paints for series...
		final GradientPaint gp0 = new GradientPaint(
				0.0f, 0.0f, Color.blue, 
				0.0f, 0.0f, Color.gray
				);
		final GradientPaint gp1 = new GradientPaint(
				0.0f, 0.0f, Color.green, 
				0.0f, 0.0f, Color.gray
				);
		final GradientPaint gp2 = new GradientPaint(
				0.0f, 0.0f, Color.red, 
				0.0f, 0.0f, Color.gray
				);
		renderer.setSeriesPaint(0, gp0);
		renderer.setSeriesPaint(1, gp1);
		renderer.setSeriesPaint(2, gp2);
*/
		final CategoryAxis domainAxis = plot.getDomainAxis();
		domainAxis.setCategoryLabelPositions(
				CategoryLabelPositions.createUpRotationLabelPositions(Math.PI / 3.0));
	
		return chart;

	}


}
