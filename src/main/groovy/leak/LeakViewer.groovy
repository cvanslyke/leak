package leak

import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.GridLayout
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.text.DateFormat
import java.text.SimpleDateFormat

import javax.swing.JButton
import javax.swing.JFileChooser
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JMenu
import javax.swing.JMenuBar
import javax.swing.JMenuItem
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.JPopupMenu
import javax.swing.JTextField
import javax.swing.SwingUtilities
import javax.swing.UIManager
import javax.swing.filechooser.FileNameExtensionFilter

public class LeakViewer extends JFrame implements ActionListener {

    JFileChooser projectChooser
    JFileChooser readingChooser
    JFileChooser saveChooser

    //MenuItems.
    JMenuItem newItem
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

        // New
        newItem = new JMenuItem("New Project...")
        newItem.addActionListener(this)
        fileMenu.add(newItem)
        
        // Open
        openItem = new JMenuItem("Open Project...")
        openItem.addActionListener(this)
        fileMenu.add(openItem)

        // Save
        saveItem = new JMenuItem("Save")
        saveItem.addActionListener(this)
        fileMenu.add(saveItem)
        
        // Save As
        saveAsItem = new JMenuItem("Save As...")
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
        saveItem.setEnabled(currentProject != null && currentFile != null)
        saveAsItem.setEnabled(currentProject != null)
        addItem.setEnabled(currentProject != null)
        removeMenu.setEnabled(currentProject != null)

        // Set remove submenu.
        removeMenu.removeAll()
        if (currentProject != null) {
            List<Reading> readings = currentProject.getReadings()
            for (Reading reading : readings) {
                String description = reading.getDescription()
                if (description != null && !description.equals("0") 
                    && !description.equals(Project.ZERO_LOSS_DESCRIPTION) 
                    && !description.equals(Project.NORMAL_EVAPORATION_DESCRIPTION)) {
                    JMenuItem item = new JMenuItem(description)
                    item.addActionListener(this)
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
        } else if (source == newItem) { 
            String description = JOptionPane.showInputDialog(
                this, 
                "Enter Project Name",
                "New Project",
                JOptionPane.PLAIN_MESSAGE)
            if (description != null) {
                currentProject = new Project(description)
                
                LeakChart chart = new LeakChart(currentProject)
                this.setContentPane(chart)
                this.pack()
            }
        } else if (source == openItem) {
            int returnVal = projectChooser.showOpenDialog(this)
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File file = projectChooser.getSelectedFile()
                currentProject = new Project(file)
                currentFile = file

                showChart()
            }
        } else if (source == saveItem) {
            if (currentFile != null) {
                currentProject.write(currentFile)
            }
        } else if (source == saveAsItem) {
            int returnVal = saveChooser.showSaveDialog(this)
            if (returnVal == saveChooser.APPROVE_OPTION) {
                File file = saveChooser.getSelectedFile()
                currentProject.write(file)
                currentFile = file
            }
        } else if (source == addItem) {
            ReadingInputPanel panel = new ReadingInputPanel()
            JOptionPane.showConfirmDialog(
                this,
                panel,
                "Add Reading to Project",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE)
            if (panel.isOk() && currentProject != null) {
                currentProject.addReading(
                    panel.getFile(), panel.getDescription(), panel.getDate())
                showChart()
            }

        } else {
            if (source instanceof JMenuItem) {
                JMenuItem item = (JMenuItem) source
                JPopupMenu popup = item.getParent()
                JMenu parentMenu = popup.getInvoker()
                if (parentMenu == removeMenu) {
                    currentProject.removeReading((String) item.getText())
                    showChart()
                }
            }
        }

        enableDisable()
    }
    
    private showChart() {
        LeakChart chart = new LeakChart(currentProject)
        this.setContentPane(chart)
        this.pack()
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
    
    public class ReadingInputPanel extends JPanel implements ActionListener {
        
        private JTextField descriptionField = new JTextField(20)
        private JTextField dateTimeField = new JTextField(20)
        private JFileChooser csvChooser = new JFileChooser()
        private JTextField fileField = new JTextField(15)
        
        DateFormat df = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss")
        
        public ReadingInputPanel() {
            super()
            this.setLayout(new GridLayout(3,2))
            this.add(new JLabel("Description:"))
            this.add(descriptionField)
            this.add(new JLabel("Date:"))
            this.add(dateTimeField)
            this.add(new JLabel("File:"))
            
            JPanel filePanel = new JPanel()
            filePanel.setLayout(new BorderLayout())
            fileField.setEditable(false)
            filePanel.add(fileField, BorderLayout.WEST)
            JButton fileButton = new JButton("...")
            fileButton.addActionListener(this)
            filePanel.add(fileButton, BorderLayout.EAST)
            this.add(filePanel)
            
            csvChooser.setDialogTitle("Choose Reading")
            FileNameExtensionFilter csvFilter = new FileNameExtensionFilter(
                "CSV Readings", "csv");
            csvChooser.setFileFilter(csvFilter)
            
            // Set Date/Time
            Date date = new Date()
            dateTimeField.setText(df.format(date))
        }
        
        public String getDescription() {
            return descriptionField.getText()
        }
        
        public Date getDate() {
            return df.parse(dateTimeField.getText())
        }
        
        public File getFile() {
            return csvChooser.getSelectedFile()
        }
        
        public boolean isOk() {
            return getFile() != null && getDate() != null && getDescription() != null
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            int returnVal = csvChooser.showOpenDialog(this)
            if (returnVal == csvChooser.APPROVE_OPTION) {
                fileField.setText(csvChooser.getSelectedFile().getName())
            }
        }
    }
}