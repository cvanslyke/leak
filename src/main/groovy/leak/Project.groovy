package leak

import groovy.xml.XmlUtil

import java.time.LocalDate

import org.apache.commons.math3.stat.regression.SimpleRegression
import org.slf4j.Logger
import org.slf4j.LoggerFactory

public class Project {
    
    // TODO: Add 2nd plot line in here. 
    def example = """
    <project>
      <title/>
      <readings>
        <reading>
          <description>0</description>
          <slope>0.0</slope>
        </reading>    
      </readings>
    </project>
    """
    private static Logger logger = LoggerFactory.getLogger(Project.class)
    
    String title    
    List<Reading> readings = new ArrayList<Reading>()
    
    def project
    def parser = new XmlParser()
    
    Project(String title) {
        project = parser.parseText(example)
        parseXml()
        
        setTitle(title)
    }
    
    Project(File xml) {
        project = parser.parse(xml)
        parseXml()
    }
    
    private void parseXml() {
        if (project.title) {
            this.title = project.title.text()
        }
        
        if (project.readings) {
            project.readings.reading.each {
                Reading reading = new Reading()
                
                def description = it.description?.text()
                if (description && !description.empty) {
                    reading.description = description
                }
                
                def date = it.date?.text()
                if (date && !date.empty) {
                    reading.date = LocalDate.parse(date)
                }
                
                def slope = it.slope?.text()
                if (slope && !slope.empty) {
                    reading.slope = Double.parseDouble(slope)
                }
                readings.add(reading)
            }
        }
    }
    
    void setTitle(String title) {
        if (project.title) {
            project.title[0].value = title
        }
        
        this.title = title
    }
    
    /**
     * Adds CSV reading from a file. 
     * @param File path to file. 
     */
    public void addReading(File csvFile, String description, Date date) {
        InputStream stream = new FileInputStream(csvFile)
        def values = []
        def index = 0
        
        stream.eachLine() { line ->
            index++
            if (index > 2) {
                def tokens = line.tokenize(",")
                values << Double.parseDouble(tokens[1])
            }
        }
            
        // calculate slope.
        SimpleRegression regression = new SimpleRegression()
        values.eachWithIndex { num, idx ->
            regression.addData(idx, num)
        }
        
        Reading reading = new Reading()
        reading.description = description
        reading.date = date
        reading.slope = regression.getSlope()
        
        readings.add(reading)
        
        // Add to project
        parser.createNode(project.readings[0], "reading", 
            [description: reading.description,
             date: reading.date,
             slope: reading.slope])
    }
    
    public List<Reading> getReadings() {
        return readings
    }
    
    public void removeReading(Reading reading) {
        readings.remove(reading)
    }
    
    public void removeReading(String description) {
        readings.each {
            if (it.description.equals(description)) {
                removeReading(it)
            }
        }
    }
    
    public void write(File file) {
        def writer = new FileWriter(file)
        XmlUtil.serialize(project, writer)
    }
    
    public String toString() {
        return "Title: $title $readings"
    }
}

class Reading {

    String description
    Date date
    Double slope
    
    public String toString() {
        return "Reading - $description $date slope: $slope"
    }
    
    public boolean equals(Reading reading) {
        return this.description.equals(reading.description) && 
            this.date.equals(reading.date) && 
            this.slope.equals(reading.slope)
    }
}