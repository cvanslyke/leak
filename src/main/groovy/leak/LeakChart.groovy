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
    
    public LeakChart(Project project) {
                
        XYDataset dataset = createDataset(project)
        JFreeChart lineChart = ChartFactory.createXYLineChart(
            project.title,
            "Hours", "Estimated Water Loss (ft.)",
            dataset, 
            PlotOrientation.VERTICAL,
            true, true, false)
        
        Range timeRange = new Range(0.0, 24.0)
        lineChart.getXYPlot().getDomainAxis().setRange(timeRange)
        lineChart.getXYPlot().getDomainAxis().setDefaultAutoRange(timeRange)
        
        // 0.34 is about 4". 
        Range depthRange = new Range(-0.34, 0.05) 
        lineChart.getXYPlot().getRangeAxis().setRange(depthRange)
        lineChart.getXYPlot().getRangeAxis().setDefaultAutoRange(depthRange)
        
        lineChart.setBackgroundImage(new ImageIcon("/logo.png").getImage())
        
        ChartPanel panel =  new ChartPanel(lineChart)
        this.add(panel)
        
        XYItemRenderer renderer = lineChart.getXYPlot().getRenderer()
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
                double point = /*10.0 + */(reading.changeRate * 86400)
                series.add((double) 24.0, point)
                dataset.addSeries(series)
            }
        }
        
        return dataset
    }
}