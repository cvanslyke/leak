package leak

import java.awt.Container
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.event.ActionEvent
import java.awt.event.ActionListener

import javax.swing.JFileChooser
import javax.swing.JFrame
import javax.swing.JMenu
import javax.swing.JMenuBar
import javax.swing.JMenuItem
import javax.swing.SwingUtilities
import javax.swing.UIManager
import javax.swing.filechooser.FileNameExtensionFilter

public class LeakViewer extends JFrame implements ActionListener {

    JFileChooser projectChooser
    JFileChooser readingChooser
    JFileChooser saveChooser

    //MenuItems.
    JMenuItem openItem
    JMenuItem saveAsItem
    JMenuItem saveItem
    JMenuItem exitItem
    JMenuItem addItem
    JMenu removeMenu

    Project currentProject
    File currentFile

    public LeakViewer() {
        super("Aaron's Leak Detection")
        setLayout(new FlowLayout())
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE)
        setJMenuBar(createMenuBar())

        projectChooser = new JFileChooser()
        projectChooser.setDialogTitle("Choose Project")
        FileNameExtensionFilter xmlFilter = new FileNameExtensionFilter(
                "XML Projets", "xml");
        projectChooser.setFileFilter(xmlFilter)

        readingChooser = new JFileChooser()
        readingChooser.setDialogTitle("Choose .csv file to add to Project")
        FileNameExtensionFilter csvFilter = new FileNameExtensionFilter(
                "CSV files", "csv");
        readingChooser.setFileFilter(csvFilter)
        
        saveChooser = new JFileChooser()
        saveChooser.setDialogTitle("Specify file to Save")
        saveChooser.setFileFilter(xmlFilter)
    }

    public JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar()

        JMenu fileMenu = new JMenu("File")
        menuBar.add(fileMenu)

        // Open
        openItem = new JMenuItem("Open")
        openItem.addActionListener(this)
        fileMenu.add(openItem)

        // Save
        saveItem = new JMenuItem("Save")
        saveItem.addActionListener(this)
        fileMenu.add(saveItem)
        
        // Save As
        saveAsItem = new JMenuItem("Save As")
        saveAsItem.addActionListener(this)
        fileMenu.add(saveAsItem)

        // Exit
        exitItem = new JMenuItem("Exit")
        exitItem.addActionListener(this)
        fileMenu.add(exitItem)

        JMenu editMenu = new JMenu("Edit")
        menuBar.add(editMenu)

        // Add Reading
        addItem = new JMenuItem("Add Reading")
        addItem.addActionListener(this)
        editMenu.add(addItem)

        // Remove Reading
        removeMenu = new JMenu("Remove Reading")
        editMenu.add(removeMenu)

        enableDisable()

        return menuBar
    }

    public void enableDisable() {
        saveItem.setEnabled(currentProject != null)
        saveAsItem.setEnabled(currentProject != null)
        addItem.setEnabled(currentProject != null)
        removeMenu.setEnabled(currentProject != null)

        // Set remove submenu.
        if (currentProject != null) {
            List<Reading> readings = currentProject.getReadings()
            readings.each {
                String description = it.getDescription()
                if (description != null && !description.equals("0")) {
                    JMenuItem item = new JMenuItem(description)
                    removeMenu.add(item)
                }
            }
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Object source = e.getSource()
        if (source == exitItem) {
            System.exit(0)
        } else if (source == openItem) {
            int returnVal = projectChooser.showOpenDialog(this)
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File file = projectChooser.getSelectedFile()
                currentProject = new Project(file)
                currentFile = file

                LeakChart chart = new LeakChart(currentProject)
                this.setContentPane(chart)
                this.pack()
            }
        } else if (source == saveItem) {
            currentProject.write(currentFile)
        } else if (source == saveAsItem) {
            int returnVal = saveChooser.showSaveDialog(this)
            if (returnVal == saveChooser.APPROVE_OPTION) {
                File file = saveChooser.getSelectedFile()
                currentProject.write(file)
                currentFile = file
            }
        } else if (source == addItem) {
            int returnVal = readingChooser.showOpenDialog(this)
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File file = readingChooser.getSelectedFile()
                
                // TODO: get description, date.
                //currentProject.addReading(file)
            }
        } else {
            if (source instanceof JMenuItem) {
                JMenuItem item = (JMenuItem) source
                if (item.getParent() == removeMenu) {
                    currentProject.removeReading(item.getName())
                }
            }
        }

        enableDisable()
    }

    private static void createAndShowGUI() {
        //Create and set up the window.
        JFrame frame = new LeakViewer()

        //Display the window.
        frame.setPreferredSize(new Dimension(750, 500))
        frame.pack();
        frame.setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                UIManager.put("swing.boldMetal", Boolean.FALSE);
                createAndShowGUI();
            }
        });
    }
}