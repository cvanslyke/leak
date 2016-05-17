package leak

import groovy.xml.XmlUtil

import java.text.DateFormat
import java.text.SimpleDateFormat

import org.slf4j.Logger
import org.slf4j.LoggerFactory

public class Project {
    
    private static String ZERO_LOSS_DESCRIPTION = "0 Loss"
    private static Double ZERO_LOSS = 0.0
    private static String NORMAL_EVAPORATION_DESCRIPTION = "Normal Evaporation (0.25\" a day)"
    private static Double NORMAL_EVAPORATION = -2.411265432098765E-7
    private static String HIGH_EVAPORATION_DESCRIPTION = "High Evaporation (0.5\" a day)"
    private static Double HIGH_EVAPORATION = NORMAL_EVAPORATION * 2
    
    def example = """
    <project>
      <title/>
      <readings>
        <reading>
          <description>${ZERO_LOSS_DESCRIPTION}</description>
          <changeRate>${ZERO_LOSS}</changeRate>
          <visible>true</visible>
        </reading>
        <reading>
          <description>${NORMAL_EVAPORATION_DESCRIPTION}</description>
          <changeRate>${NORMAL_EVAPORATION}</changeRate>
          <visible>true</visible>
        </reading>
        <reading>
          <description>${HIGH_EVAPORATION_DESCRIPTION}</description>
          <changeRate>${HIGH_EVAPORATION}</changeRate>
          <visible>false</visible>
        </reading>   
      </readings>
    </project>
    """
    private static Logger logger = LoggerFactory.getLogger(Project.class)
    
    String title    
    List<Reading> readings = new ArrayList<Reading>()
    DateFormat df = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss")
    
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
                    reading.date = df.parse(date)
                }
                
                def notes = it.notes?.text()
                if (notes && !notes.empty) {
                    reading.notes = notes
                }
                
                def visible = it.visible.text()
                if (visible && !visible.empty) {
                    reading.visible = visible.toBoolean()
                }
                
                def changeRate = it.changeRate?.text()
                if (changeRate && !changeRate.empty) {
                    reading.changeRate = Double.parseDouble(changeRate)
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
    public void addReading(File csvFile, String description, Date date, String notes) {
        InputStream stream = new FileInputStream(csvFile)
        def timedValues = [:]
        def averagedValues = []
        def changeRates = []
        def index = 0
        
        stream.eachLine() { line ->
            index++
            if (index > 2) {
                def tokens = line.tokenize(",")
                def time = tokens[0]
                def values
                
                if (timedValues.containsKey(time)) {
                    values = timedValues[time]
                } else {
                    values = []
                    timedValues.put(time, values)
                }
                values << Double.parseDouble(tokens[1])
            }
        }
        
        // Average out the values for each time.
        timedValues.keySet().each {
            def values = timedValues[it]
            averagedValues << (values.sum() / values.size())
        }
        
        // Find the changeRate
        averagedValues.eachWithIndex { item, idx ->
            if (idx + 1 < averagedValues.size()) {
                double change = averagedValues[idx + 1] - averagedValues[idx]
                changeRates << change
            }
        }
        
        Reading reading = new Reading()
        reading.description = description
        reading.date = date
        reading.changeRate = (changeRates.sum() / changeRates.size())
        reading.notes = notes
        reading.visible = true
        
        addReading(reading)
    }
    
    public void addReading(Reading reading) {
        readings.add(reading)
        
        // Add to project
        Node rNode = parser.createNode(project.readings[0], "reading", [:])
        rNode.appendNode("description", reading.description)
        if (reading.date != null) {
            rNode.appendNode("date", df.format(reading.date))
        }
        rNode.appendNode("changeRate", reading.changeRate)
        rNode.appendNode("notes", reading.notes)
        rNode.appendNode("visible", reading.visible)
    }
    
    public List<Reading> getReadings() {
        return readings
    }
    
    public Reading getReading(String description) {
        for (Reading reading : readings) {
            if (reading.description.equals(description)) {
                return reading
            }
        }
        
        return null
    }
    
    public void removeReading(Reading reading) {
        if (reading != null) {
            readings.remove(reading)

            // Remove from project.
            def toRemove = project.readings.reading.find {
                it.description.text().equals(reading.description) }
                                                
            def parent = toRemove.parent()
            parent.remove(toRemove)
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
    String notes
    Date date
    boolean visible
    
    // ft/sec
    Double changeRate
    
    public String toString() {
        return "Reading - $description $date changeRate: $changeRate"
    }
    
    public boolean equals(Reading reading) {
        return this.description.equals(reading.description) && 
            this.date.equals(reading.date) && 
            this.changeRate.equals(reading.changeRate)
    }
}