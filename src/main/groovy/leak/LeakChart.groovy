package leak

import java.awt.Color
import java.awt.Image

import javax.swing.ImageIcon
import javax.swing.JPanel

import org.jfree.chart.ChartFactory
import org.jfree.chart.ChartPanel
import org.jfree.chart.JFreeChart
import org.jfree.chart.plot.PlotOrientation
import org.jfree.chart.renderer.xy.XYItemRenderer
import org.jfree.data.Range
import org.jfree.data.xy.XYDataset
import org.jfree.data.xy.XYSeries
import org.jfree.data.xy.XYSeriesCollection

public class LeakChart extends JPanel {
    
    private Image img
    
    public enum ZoomLevelEnum {
        DEFAULT,
        FIT
    }
    
    private static Range DEFAULT_Y_RANGE = new Range(-1.0, 0.05)
    private static Range DEFAULT_X_RANGE = new Range(0.0, 24.0)
    
    private JFreeChart chart
    private Project currentProject
    
    private static Color ZERO_LOSS_COLOR = Color.GREEN
    private static Color NORMAL_EVAP_COLOR = new Color(51, 255, 51)
    private static Color HIGH_EVAP_COLOR = new Color(102, 255, 102)
    
    public LeakChart(Project project) {
                
        currentProject = project
        XYDataset dataset = createDataset(project)
        chart = ChartFactory.createXYLineChart(
            project.title,
            "Hours", "Estimated Water Loss (inches)",
            dataset, 
            PlotOrientation.VERTICAL,
            true, true, false)
        
        Range timeRange = DEFAULT_X_RANGE
        chart.getXYPlot().getDomainAxis().setRange(timeRange)
        chart.getXYPlot().getDomainAxis().setDefaultAutoRange(timeRange)
        
        Range depthRange = DEFAULT_Y_RANGE
        chart.getXYPlot().getRangeAxis().setRange(depthRange)
        chart.getXYPlot().getRangeAxis().setDefaultAutoRange(depthRange)
        
        chart.setBackgroundImage(new ImageIcon("/logo.png").getImage())
        
        ChartPanel panel =  new ChartPanel(chart)
        this.add(panel)
        
        // Set colors.
        XYItemRenderer renderer = chart.getXYPlot().getRenderer()
        int index = 0
        for (Reading reading : project.getReadings()) {

            if (reading.isVisible()) {
                Color color = null;
                if (reading.description.equals(Project.ZERO_LOSS_DESCRIPTION)) {
                    color = ZERO_LOSS_COLOR
                } else if (reading.description.equals(Project.NORMAL_EVAPORATION_DESCRIPTION)) {
                    color = NORMAL_EVAP_COLOR
                } else if (reading.description.equals(Project.HIGH_EVAPORATION_DESCRIPTION)) {
                    color = HIGH_EVAP_COLOR
                } else {
                    if (reading.color != null) {
                        color = reading.color
                    } else {
                        Random rand = new Random()
                        int r = rand.nextInt(255)
                        int g = 0
                        int b = rand.nextInt(255)
                        color = new Color(r, g, b)
                        reading.color = color
                    }
                }

                renderer.setSeriesPaint(index, color)
                index++
            }
        }
    }
    
    private XYDataset createDataset(Project project) {
        
        XYSeriesCollection dataset = new XYSeriesCollection() 
        project.readings.each { reading ->
            if (reading.visible) {
                XYSeries series = new XYSeries(reading.description)
                series.add(0.0, 0.0)
                
                // add last point.
                // changeRate (ft/s) * 12 to get inches * 86400 (seconds in a day)
                double point = (reading.changeRate * 12 * 86400)
                series.add((double) 24.0, point)
                dataset.addSeries(series)
            }
        }
        
        return dataset
    }
    
    public void zoom(ZoomLevelEnum zoomLevel) {
        
        Range depthRange = DEFAULT_Y_RANGE
        Range timeRange = DEFAULT_X_RANGE
        
        if (zoomLevel == ZoomLevelEnum.FIT) {
            double lowestPoint = -1.0
            double highestPoint = 0.05
            for (Reading reading : currentProject.getReadings()) {
                if (reading.visible) {
                    double point = (reading.changeRate * 12 * 86400)
                    if (point < lowestPoint) {
                        lowestPoint = point
                    } else if (point > highestPoint) {
                        highestPoint = point
                    }
                }
            }
            depthRange = new Range(lowestPoint, highestPoint)
        }
        
        chart.getXYPlot().getRangeAxis().setRange(depthRange)
        chart.getXYPlot().getRangeAxis().setDefaultAutoRange(depthRange)
        
        chart.getXYPlot().getDomainAxis().setRange(timeRange)
        chart.getXYPlot().getDomainAxis().setDefaultAutoRange(timeRange)
    }
}