package leak

import java.awt.Color
import java.awt.Image
import java.awt.geom.Arc2D.Double;

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
    
    // 0.34 is about 4".
    private static Range DEFAULT_RANGE = new Range(-0.34, 0.05)
    
    private JFreeChart chart
    private Project currentProject
    
    public LeakChart(Project project) {
                
        currentProject = project
        XYDataset dataset = createDataset(project)
        chart = ChartFactory.createXYLineChart(
            project.title,
            "Hours", "Estimated Water Loss (ft.)",
            dataset, 
            PlotOrientation.VERTICAL,
            true, true, false)
        
        Range timeRange = new Range(0.0, 24.0)
        chart.getXYPlot().getDomainAxis().setRange(timeRange)
        chart.getXYPlot().getDomainAxis().setDefaultAutoRange(timeRange)
        
        Range depthRange = DEFAULT_RANGE
        chart.getXYPlot().getRangeAxis().setRange(depthRange)
        chart.getXYPlot().getRangeAxis().setDefaultAutoRange(depthRange)
        
        chart.setBackgroundImage(new ImageIcon("/logo.png").getImage())
        
        ChartPanel panel =  new ChartPanel(chart)
        this.add(panel)
        
        XYItemRenderer renderer = chart.getXYPlot().getRenderer()
        renderer.setSeriesPaint(0, Color.GREEN)
        renderer.setSeriesPaint(1, Color.GREEN)
    }
    
    private XYDataset createDataset(Project project) {
        
        XYSeriesCollection dataset = new XYSeriesCollection() 
        project.readings.each { reading ->
            if (reading.visible) {
                XYSeries series = new XYSeries(reading.description)
                series.add(0.0, 0.0)
                
                // add last point.
                double point = (reading.changeRate * 86400)
                series.add((double) 24.0, point)
                dataset.addSeries(series)
            }
        }
        
        return dataset
    }
    
    public void zoom(ZoomLevelEnum zoomLevel) {
        
        Range depthRange = DEFAULT_RANGE
        
        if (zoomLevel == ZoomLevelEnum.FIT) {
            double lowestPoint = -0.34
            double highestPoint = 0.05
            for (Reading reading : currentProject.getReadings()) {
                if (reading.visible) {
                    double point = (reading.changeRate * 86400)
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
    }
}