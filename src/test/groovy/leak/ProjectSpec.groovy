package leak

import java.time.LocalDate

import spock.lang.Specification

class ProjectSpec extends Specification {

    void setup() {
        
    }

    void "Create new Project(String)"() {
        given:
        String name = 'test'
        def project

        when:
        project = new Project(name)

        then:
        project.title == 'test'
        project.readings.size() == 3
    }
    
    void "Create new Project(File)"() {
        given:
        def file = new File("build/resources/test/test.xml")
        def project

        when:
        project = new Project(file)

        then:
        project.title == 'Test'
        project.readings.size() == 2
        project.toString().contains("reading1")
    }
    
    void "Write Project to a File"() {
        given:
        def file = new File("build/resources/test/test.xml")
        def project
        def newFile = new File("build/resources/test/test2.xml")

        when:
        project = new Project(file)
        project.write(newFile)

        then:
        newFile.exists()
    }
    
    void "Add Reading to a Project"() {
        given:
        def file = new File("build/resources/test/test.xml")
        def reading = new File("build/resources/main/drain_4_good.csv")
        def newFile = new File("build/resources/test/test3.xml")
        def project

        when:
        project = new Project(file)
        LocalDate date = LocalDate.now()
       
        project.addReading(reading, "test reading", new Date(), "")
        project.write(newFile)
        println "$project"

        then:
        project.readings.size() == 3
    }
    
    void "Remove Reading from a Project"() {
        given:
        def file = new File("build/resources/test/test.xml")
        def project

        when:
        project = new Project(file)
        project.removeReading(project.getReadings()[1])

        then:
        project.readings.size() == 1
    }
}

