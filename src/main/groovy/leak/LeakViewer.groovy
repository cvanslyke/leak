package leak

import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.text.DateFormat
import java.text.DecimalFormat
import java.text.SimpleDateFormat

import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JFileChooser
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JMenu
import javax.swing.JMenuBar
import javax.swing.JMenuItem
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.JPopupMenu
import javax.swing.JScrollPane
import javax.swing.JTextArea
import javax.swing.JTextField
import javax.swing.SwingUtilities
import javax.swing.UIManager
import javax.swing.filechooser.FileNameExtensionFilter

import leak.LeakChart.ZoomLevelEnum

public class LeakViewer extends JFrame implements ActionListener {
    
    JFileChooser projectChooser
    JFileChooser saveChooser

    //MenuItems.
    JMenuItem newItem
    JMenuItem openItem
    JMenuItem saveAsItem
    JMenuItem saveItem
    JMenuItem exitItem
    JMenuItem addItem
    JMenuItem defaultZoom
    JMenuItem fitZoom
    JMenu readingsMenu
    JMenu removeMenu
    JMenu viewMenu
    JMenu zoomMenu

    Project currentProject
    File currentFile
    LeakChart currentChart

    public LeakViewer() {
        super("Aaron's Leak Detection")
        setLayout(new FlowLayout())
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE)
        setJMenuBar(createMenuBar())
        
        String homePath = new JFileChooser().getFileSystemView().getDefaultDirectory().getAbsolutePath()
        
        // Create directory if not exists.
        File leakPath = new File(homePath + "/LeakDetection")
        if (!leakPath.exists()) {
            leakPath.mkdir()
        }
        
        File projectPath = new File(leakPath.getAbsolutePath() + "/projects")
        if (!projectPath.exists()) {
            projectPath.mkdir()
        }
        
        File readingsPath = new File(leakPath.getAbsolutePath() + "/readings")
        if (!readingsPath.exists()) {
            readingsPath.mkdir()
        }

        projectChooser = new JFileChooser(projectPath)
        projectChooser.setDialogTitle("Choose Project")
        FileNameExtensionFilter xmlFilter = new FileNameExtensionFilter(
                "XML Projets", "xml");
        projectChooser.setFileFilter(xmlFilter)
        
        saveChooser = new JFileChooser(projectPath)
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

        readingsMenu = new JMenu("Readings")
        menuBar.add(readingsMenu)

        // Add Reading
        addItem = new JMenuItem("Add")
        addItem.addActionListener(this)
        readingsMenu.add(addItem)

        // Remove Reading
        removeMenu = new JMenu("Remove")
        readingsMenu.add(removeMenu)
        
        // View Reading
        viewMenu = new JMenu("View")
        readingsMenu.add(viewMenu)
        
        // Zoom
        zoomMenu = new JMenu("Zoom")
        defaultZoom = new JMenuItem("Default")
        defaultZoom.addActionListener(this)
        fitZoom = new JMenuItem("Zoom to Fit")
        fitZoom.addActionListener(this)
        zoomMenu.add(defaultZoom)
        zoomMenu.add(fitZoom)
        menuBar.add(zoomMenu)

        enableDisable()

        return menuBar
    }

    public void enableDisable() {
        saveItem.setEnabled(currentProject != null && currentFile != null)
        saveAsItem.setEnabled(currentProject != null)
        addItem.setEnabled(currentProject != null)
        removeMenu.setEnabled(currentProject != null)
        viewMenu.setEnabled(currentProject != null)
        zoomMenu.setEnabled(currentProject != null)
        readingsMenu.setEnabled(currentProject != null)

        removeMenu.removeAll()
        viewMenu.removeAll()
        if (currentProject != null) {
            List<Reading> readings = currentProject.getReadings()
            for (Reading reading : readings) {
                String description = reading.getDescription()
                if (description != null && !description.equals("0") 
                    && !description.equals(Project.ZERO_LOSS_DESCRIPTION) 
                    && !description.equals(Project.NORMAL_EVAPORATION_DESCRIPTION)) {
                    JMenuItem viewItem = new JMenuItem(description)
                    viewItem.addActionListener(this)
                    viewMenu.add(viewItem)
                    
                    if (!description.equals(Project.HIGH_EVAPORATION_DESCRIPTION)) {
                        JMenuItem removeItem = new JMenuItem(description)
                        removeItem.addActionListener(this)
                        removeMenu.add(removeItem)
                    }
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
                
                showChart()
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
                if (!file.getName().endsWith(".xml")) {
                    file = new File(file.getParent(), file.getName() + ".xml")
                }
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
                    panel.getFile(), panel.getDescription(), panel.getDate(), panel.getNotes())
                showChart()
            }

        } else if (source == defaultZoom) {
            currentChart.zoom(ZoomLevelEnum.DEFAULT)
        } else if (source == fitZoom) { 
            currentChart.zoom(ZoomLevelEnum.FIT)
        } else {
            if (source instanceof JMenuItem) {
                JMenuItem item = (JMenuItem) source
                JPopupMenu popup = item.getParent()
                JMenu parentMenu = popup.getInvoker()
                if (parentMenu == removeMenu) {
                    Reading reading = currentProject.getReading((String) item.getText())
                    currentProject.removeReading(reading)
                    showChart()
                } else if (parentMenu == viewMenu) {
                    Reading reading = currentProject.getReading((String) item.getText())
                    ReadingViewPanel viewPanel = new ReadingViewPanel(reading)
                    int returnVal = JOptionPane.showConfirmDialog(
                            this,
                            viewPanel,
                            "Leak Reading Properties:",
                            JOptionPane.OK_CANCEL_OPTION,
                            JOptionPane.PLAIN_MESSAGE);
                    if (returnVal == JOptionPane.OK_OPTION) {
                        
                        Reading newReading = new Reading()
                        newReading.description = viewPanel.getDescription()
                        newReading.notes = viewPanel.getNotes()
                        newReading.date = viewPanel.getDate()
                        newReading.visible = viewPanel.getVisible()
                        newReading.changeRate = reading.changeRate
                        
                        currentProject.removeReading(reading)
                        currentProject.addReading(newReading)
                        
                        showChart()
                    }
                }
            }
        }

        enableDisable()
    }
    
    private void showChart() {
        currentChart = new LeakChart(currentProject)
        this.setContentPane(currentChart)
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
        private JTextField fileField = new JTextField(16)
        private JTextArea notesField = new JTextArea(10, 20)
        private JFileChooser csvChooser = null
        
        DateFormat df = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss")
        
        public ReadingInputPanel() {
            super()
            this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS))
            
            JPanel descriptionPanel = new JPanel(new BorderLayout())
            descriptionPanel.add(new JLabel("Description:"), BorderLayout.WEST)
            descriptionPanel.add(descriptionField, BorderLayout.EAST)
            
            JPanel datePanel = new JPanel(new BorderLayout())
            datePanel.add(new JLabel("Date:"), BorderLayout.WEST)
            datePanel.add(dateTimeField, BorderLayout.EAST)
            
            JPanel filePanel = new JPanel(new BorderLayout())
            filePanel.add(new JLabel("File:"), BorderLayout.WEST)
            
            JPanel fPanel = new JPanel(new BorderLayout())
            fileField.setEditable(false)
            fPanel.add(fileField, BorderLayout.WEST)
            JButton fileButton = new JButton("...")
            fileButton.addActionListener(this)
            fPanel.add(fileButton, BorderLayout.EAST)
            filePanel.add(fPanel, BorderLayout.EAST)
            
            JPanel notesPanel = new JPanel(new BorderLayout())
            JScrollPane notesScrollPane = new JScrollPane(notesField)
            notesPanel.add(new JLabel("Notes:"), BorderLayout.WEST)
            notesPanel.add(notesScrollPane, BorderLayout.EAST)
            
            this.add(descriptionPanel)
            this.add(datePanel)
            this.add(filePanel)
            this.add(notesPanel)
            
            String homePath = new JFileChooser().getFileSystemView().getDefaultDirectory().getAbsolutePath()
            File readingsPath = new File(homePath + "/LeakDetection/readings")
            
            csvChooser = new JFileChooser(readingsPath)
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
        
        public String getNotes() {
            return notesField.getText()
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
    
    public class ReadingViewPanel extends JPanel {
        
        private JTextField descriptionField = null
        private JTextField dateTimeField = null
        private JTextField totalLoss = null
        private JCheckBox visibleField = null
        private JTextArea notesField = null
        
        DateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss")
        DecimalFormat decimalFormat = new DecimalFormat("##.###")
        
        public ReadingViewPanel(Reading reading) {
            super()
            this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS))
            
            JPanel descriptionPanel = new JPanel(new BorderLayout())
            descriptionPanel.add(new JLabel("Description:"), BorderLayout.WEST)
            descriptionField = new JTextField(20)
            descriptionPanel.add(descriptionField, BorderLayout.EAST)
            descriptionField.setText(reading.description)
            
            JPanel datePanel = new JPanel(new BorderLayout())
            if (reading.date != null) {
                datePanel.add(new JLabel("Date:"), BorderLayout.WEST)
                dateTimeField = new JTextField(20)
                datePanel.add(dateTimeField, BorderLayout.EAST)
                dateTimeField.setText(dateFormat.format(reading.date))
            }
            
            JPanel isVisblePanel = new JPanel(new BorderLayout())
            isVisblePanel.add(new JLabel("Visible?:"), BorderLayout.WEST)
            visibleField = new JCheckBox("", reading.visible)
            isVisblePanel.add(visibleField, BorderLayout.EAST)
            
            JPanel notesPanel = new JPanel(new BorderLayout())
            notesField = new JTextArea(10, 20)
            JScrollPane notesScrollPane = new JScrollPane(notesField)
            notesPanel.add(new JLabel("Notes:"), BorderLayout.WEST)
            notesPanel.add(notesScrollPane, BorderLayout.EAST)
            notesField.setText(reading.notes)
            
            JPanel totalLossPanel = new JPanel(new BorderLayout())
            totalLossPanel.add(new JLabel("Total Loss Over 24 Hrs (in ft.):"), BorderLayout.WEST)
            totalLoss = new JTextField(20)
            totalLossPanel.add(totalLoss, BorderLayout.EAST)
            totalLoss.setEditable(false)
            totalLoss.setText(decimalFormat.format(reading.changeRate * 86400))
            
            this.add(descriptionPanel)
            if (reading.date != null) {
                this.add(datePanel)
            }
            this.add(isVisblePanel)
            this.add(notesPanel)
            this.add(totalLossPanel)
        }
        
        public String getDescription() {
            return descriptionField.getText()
        }
        
        public Date getDate() {
            if (dateTimeField != null) {
                return dateFormat.parse(dateTimeField.getText())
            } else {
                return null
            }
        }
        
        public String getNotes() {
            return notesField.getText()
        }
        
        public boolean getVisible() {
            return visibleField.isSelected()
        }
        
        public boolean isOk() {
            return getDate() != null && getDescription() != null
        }
    }
}