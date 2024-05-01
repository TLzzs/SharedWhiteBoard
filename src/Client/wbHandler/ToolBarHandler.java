package Client.wbHandler;

import Client.WhiteBoardGUI;
import DrawingObject.drawingPanelElements.DeleteAll;
import DrawingObject.drawingPanelElements.SavedCanvas;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.NumberFormatter;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;


public class ToolBarHandler {
    private String currentTool = "FreeLine"; // Default tool
    private JToggleButton textButton;
    private JPanel subToolBarShape, subToolBarText;
    private Graphics2D g2d;
    private WhiteBoardGUI whiteBoardGUI;

    private JToggleButton buttonShape, buttonText;
    private BufferedImage canvas;

    public ToolBarHandler(Graphics2D g2d, WhiteBoardGUI whiteBoardGUI, BufferedImage canvas) {
        this.g2d = g2d;
        this.whiteBoardGUI = whiteBoardGUI;
        this.canvas = canvas;
    }

    public JPanel setupToolBar() {
        JPanel toolBar = new JPanel();
        toolBar.setLayout(new GridLayout(8, 1));
        toolBar.setBackground(Color.LIGHT_GRAY);

        // Define tool icons
        String[] iconPaths = {
                "src/icons/freeLine.png", "src/icons/line.png", "src/icons/circle.png",
                "src/icons/rectangle.png", "src/icons/oval.png", "src/icons/eraser.png",
                "src/icons/text.png", "src/icons/bin.png"
        };
        String[] toolNames = {
                "FreeLine", "Line", "Circle", "Rectangle", "Oval", "Eraser", "Text", "Bin"
        };

        ButtonGroup toolsGroup = new ButtonGroup();
        JToggleButton[] buttons = new JToggleButton[toolNames.length];

        for (int i = 0; i < toolNames.length; i++) {
            buttons[i] = createToggleButton(iconPaths[i], toolNames[i]);
            toolsGroup.add(buttons[i]);
            toolBar.add(buttons[i]);
        }
        return toolBar;

    }

    public String getCurrentTool() {
        return currentTool;
    }

    public JToggleButton getTextButton() {
        return textButton;
    }

    public void setG2d(Graphics2D g2d) {
        this.g2d = g2d;
    }

    private JToggleButton createToggleButton(String iconPath, String actionCommand) {
        Icon icon = new ImageIcon(iconPath);
        JToggleButton button = new JToggleButton(icon);
        button.addActionListener(e -> {
            currentTool = actionCommand;
            if ("Text".equals(actionCommand)) {
                textButton = button;
            }
            if ("Bin".equals(currentTool)) {
                whiteBoardGUI.deleteAll();
                whiteBoardGUI.sendUpdateToServer(new DeleteAll());
            }
            whiteBoardGUI.updatesubToolBarVisibility();
        } );
        return button;
    }


    public JPanel setupsubToolBarShape() {
        subToolBarShape = new JPanel();
        subToolBarShape.setLayout(new FlowLayout(FlowLayout.LEFT));

        buttonShape = setUpFileButton(subToolBarShape);

        // Stroke size selection with icon
        Icon strokeIcon = new ImageIcon("src/icons/stroke.png");
        JLabel strokeLabel = new JLabel(strokeIcon);

        // Setup JSpinner for stroke size
        SpinnerModel model = new SpinnerNumberModel(2, 1, 10, 1); // initial value, min, max, step
        JSpinner strokeSizeSpinner = new JSpinner(model);
        strokeSizeSpinner.setPreferredSize(new Dimension(50, 20)); // Adjust size as needed

        JSpinner.NumberEditor editor = (JSpinner.NumberEditor)strokeSizeSpinner.getEditor();

        NumberFormatter formatter = new NumberFormatter();
        formatter.setValueClass(Integer.class);
        formatter.setMinimum(1);
        formatter.setMaximum(20);
        formatter.setAllowsInvalid(false); // this is the key part
        formatter.setCommitsOnValidEdit(true);

        editor.getTextField().setFormatterFactory(new DefaultFormatterFactory(formatter));

        strokeSizeSpinner.setEditor(editor);
        Icon colorIcon = new ImageIcon("src/icons/color.png");
        JLabel colorLabel = new JLabel(colorIcon);
        JButton colorButton = new JButton("Choose");
        JColorChooser colorChooser = new JColorChooser();
        colorButton.addActionListener(e -> {
            Color newColor = JColorChooser.showDialog(null, "Choose a color", g2d.getColor());
            if (newColor != null) {
                System.out.println("color is" + newColor);
                g2d.setPaint(newColor);
            }
        });
        subToolBarShape.add(Box.createHorizontalStrut(270));
        subToolBarShape.add(strokeLabel);
        subToolBarShape.add(strokeSizeSpinner);
        subToolBarShape.add(Box.createHorizontalStrut(20)); // Adds spacing
        subToolBarShape.add(colorLabel);
        subToolBarShape.add(colorButton);

        subToolBarShape.setVisible(false);  // Initially visible


        // Listener to update the stroke based on spinner value change
        strokeSizeSpinner.addChangeListener(e -> {
            int strokeSize = (int) ((JSpinner) e.getSource()).getValue();
            g2d.setStroke(new BasicStroke(strokeSize));
        });
        return subToolBarShape;
    }

    private JToggleButton setUpFileButton(JPanel subToolBar) {
        JToggleButton fileButton = new JToggleButton("File");
        JPopupMenu fileMenu = new JPopupMenu();

        // Add menu items to the popup menu
        JMenuItem saveItem = new JMenuItem("Save");
        saveItem.addActionListener(e -> saveAction());
        fileMenu.add(saveItem);

        JMenuItem saveAsItem = new JMenuItem("Save As");
        saveAsItem.addActionListener(e -> saveAsAction());
        fileMenu.add(saveAsItem);

        JMenuItem newItem = new JMenuItem("New");
        newItem.addActionListener(e -> newAction());
        fileMenu.add(newItem);

        JMenuItem openItem = new JMenuItem("Open");
        openItem.addActionListener(e -> newAction());
        fileMenu.add(openItem);

        fileButton.addActionListener(e -> fileMenu.show(fileButton, 0, fileButton.getHeight()));
        subToolBar.add(fileButton);
        fileButton.setVisible(false);
        return fileButton;
    }

    public JPanel setupSubToolBarText() {
        subToolBarText = new JPanel();
        subToolBarText.setLayout(new FlowLayout(FlowLayout.LEFT));

        buttonText = setUpFileButton(subToolBarText);
        // Font size selection
        JLabel fontSizeLabel = new JLabel(new ImageIcon("src/icons/fontSize.png"));  // Adjust icon path as needed
        SpinnerModel fontSizeModel = new SpinnerNumberModel(12, 8, 48, 1);  // Default 12, min 8, max 48, step 1
        JSpinner fontSizeSpinner = new JSpinner(fontSizeModel);
        fontSizeSpinner.setPreferredSize(new Dimension(50, 20));

        // Font family selection
        JLabel fontLabel = new JLabel(new ImageIcon("src/icons/font.png"));  // Adjust icon path as needed
        String[] availableFonts = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
        JComboBox<String> fontComboBox = new JComboBox<>(availableFonts);
        fontComboBox.setSelectedItem("Arial");
        fontComboBox.setPreferredSize(new Dimension(150, 20));

        fontSizeSpinner.addChangeListener(e -> {
            int newSize = (int) ((JSpinner) e.getSource()).getValue();
            Font currentFont = g2d.getFont();
            g2d.setFont(new Font(currentFont.getFontName(), currentFont.getStyle(), newSize));
        });

        fontComboBox.addActionListener(e -> {
            String fontName = (String) ((JComboBox) e.getSource()).getSelectedItem();
            Font currentFont = g2d.getFont();
            g2d.setFont(new Font(fontName, currentFont.getStyle(), currentFont.getSize()));
        });

        // Font color chooser
        JLabel fontColorLabel = new JLabel(new ImageIcon("src/icons/color.png"));  // Adjust icon path as needed
        JButton fontColorButton = new JButton("Choose Color");
        fontColorButton.addActionListener(e -> {
            Color newFontColor = JColorChooser.showDialog(null, "Choose Font Color", g2d.getColor());
            if (newFontColor != null) {
                g2d.setColor(newFontColor);  // Update the current drawing color to the chosen color
            }
        });

        subToolBarText.add(Box.createHorizontalStrut(250));
        subToolBarText.add(fontSizeLabel);
        subToolBarText.add(fontSizeSpinner);
        subToolBarText.add(Box.createHorizontalStrut(20));
        subToolBarText.add(fontLabel);
        subToolBarText.add(fontComboBox);
        subToolBarText.add(Box.createHorizontalStrut(20));  // Adds spacing
        subToolBarText.add(fontColorLabel);
        subToolBarText.add(fontColorButton);

        subToolBarText.setVisible(false);  // Initially hidden until the Text tool is selected
        return subToolBarText;
    }


    private void saveAction() {
        String canvasName = JOptionPane.showInputDialog("Enter a name for the canvas:");
        if (canvasName != null && !canvasName.trim().isEmpty()) {
            SavedCanvas savedCanvas = new SavedCanvas(canvasName, canvas);
            whiteBoardGUI.sendUpdateToServer(savedCanvas);
        } else {
            JOptionPane.showMessageDialog(null, "Canvas name cannot be empty.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }



    public void setCanvas(BufferedImage canvas) {
        this.canvas = canvas;
    }

    private void saveAsAction() {
        // Logic to save current work with a new filename
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Specify a file to save");

        // Set the default directory to user's home or get from a saved setting
        fileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));

        // Optional: Set a file filter to restrict file types
        fileChooser.setFileFilter(new FileNameExtensionFilter("PNG Images", "png"));

        int userSelection = fileChooser.showSaveDialog(null);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            // Ensure the file has the correct extension
            if (!fileToSave.getAbsolutePath().endsWith(".png")) {
                fileToSave = new File(fileToSave.getAbsolutePath() + ".png");
            }

            try {
                // Assuming `canvas` is a BufferedImage of your whiteboard area
                ImageIO.write(canvas, "PNG", fileToSave);
                JOptionPane.showMessageDialog(null, "Save successful!", "Save Image", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(null, "Error saving image: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        }
    }

    private void newAction() {
        // Options for the JOptionPane
        String[] options = {"Save", "Save As", "No"};

        int choice = JOptionPane.showOptionDialog(null,
                "Do you want to save the current project before starting a new one?",
                "Save Current Work?",
                JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE,
                null, options, options[0]);

        switch (choice) {
            case 0:
                saveAction();
                break;
            case 1:
                saveAsAction();
                break;
            case 2:
                clearEverything();
                break;
            default:
                return;
        }
    }

    private void clearEverything() {
        whiteBoardGUI.deleteAll();
    }
    public void toggleFileButtonVisibility(boolean visible) {
        buttonShape.setVisible(visible);
        buttonText.setVisible(visible);
    }

}
