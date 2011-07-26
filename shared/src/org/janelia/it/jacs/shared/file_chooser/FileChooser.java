package org.janelia.it.jacs.shared.file_chooser;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


/**
 * This class replaces JFileChooser with one that can add shortcut buttons.
 * Buttons are added with the addShortcutButton method.  This class will
 * have the following behavior:
 *
 * FileChooser mode: DIRECTORIES_ONLY:
 *   Shortcut to Directory: Will select that directory on button press.
 *   Shortcut to File: Will throw an exception on add method
 *
 * FileChooser mode: FILES_ONLY:
 *   Shortcut to Directory: Will navigate to that directory on button press.
 *   Shortcut to File: Will select that file on button press
 *
 * FileChooser mode: FILES_AND_DIRECTORIES:
 *   Shortcut to Directory: Will select that directory on button press.
 *   Shortcut to File: Will select that file on button press
 *
 * Additionally, in DIRECTORIES_ONLY mode, the user will not be able to traverse
 * into a directory unless it has sub-directories.  This is not the default
 * behavior of JFileChooser.
 */
public class FileChooser extends JFileChooser {
    private AccessoryPanel accessory = new AccessoryPanel();
    private DirectoryFileFilter dirFileFilter = new DirectoryFileFilter();

    /**
     * Constructs a <code>FileChooser</code> pointing to the user's
     * home directory.
     */
    public FileChooser() {
        super.setAccessory(accessory);
    }

    /**
     * Constructs a <code>FileChooser</code> pointing to the user's
     * home directory.
     */
    public FileChooser(JComponent accessory) {
        super.setAccessory(accessory);
    }

    /**
     * Constructs a <code>FileChooser</code> using the given path.
     * Passing in a <code>null</code>
     * string causes the file chooser to point to the user's home directory.
     *
     */
    public FileChooser(File currentDirectory) {
        super(currentDirectory);
        super.setAccessory(accessory);
    }

    /**
     * Constructs a <code>FileChooser</code> using the given
     * <code>FileSystemView</code>.
     */
    public FileChooser(FileSystemView fsv) {
        super(fsv);
        super.setAccessory(accessory);
    }

    /**
     * Constructs a <code>FileChooser</code> using the given current directory
     * and <code>FileSystemView</code>.
     */
    public FileChooser(File currentDirectory, FileSystemView fsv) {
        super(currentDirectory, fsv);
        super.setAccessory(accessory);
    }

    /**
     * Constructs a <code>FileChooser</code> using the given path.
     * Passing in a <code>null</code>
     * string causes the file chooser to point to the user's home directory.
     *
     * @param currentDirectoryPath  a <code>String</code> giving the path
     *                                to a file or directory
     */
    public FileChooser(String currentDirectoryPath) {
        super(currentDirectoryPath);
        super.setAccessory(accessory);
    }

    /**
     * Constructs a <code>FileChooser</code> using the given current directory
     * path and <code>FileSystemView</code>.
     */
    public FileChooser(String currentDirectoryPath, FileSystemView fsv) {
        super(currentDirectoryPath, fsv);
        super.setAccessory(accessory);
    }

    /**
     * Override to prevent eliminating the accessory
     */
    public void setAccessory(JComponent component) {
    }

    public void setFileSelectionMode(int mode) {
        if ((mode == DIRECTORIES_ONLY) && accessory.containsFileShortcuts()) {
            throw new IllegalArgumentException(
                    "You cannot set DIRECTORIES_ONLY " + 
                    "once you have added a shortcut button to a file.");
        }

        if (mode == DIRECTORIES_ONLY) {
            setApproveButtonText("Select Directory");
        }

        if (mode == FILES_AND_DIRECTORIES) {
            setApproveButtonText("Select");
        }

        if (mode == FILES_ONLY) {
            setApproveButtonText("Select File");
        }

        super.setFileSelectionMode(mode);

        //Must be done after the super call.
        if (mode == DIRECTORIES_ONLY) {
            forceDirectoryLabellingOfComponents();


            //Ensure we are not in a directory with no subDirectories.
            setCurrentDirectory(getCurrentDirectory());
        }
    }

    /**
     * This method will add a shortCut button to the dialog.
     *
     * @parameter buttonName - The string for the button
     * @parameter file - If the file is a directory and
     *  DIRECTORIES_ONLY is set, then the shortcut will select the directory.
     *  If the file is a directory and files are allowed, the shortcut will
     *  navigate the user to the directory.  If a file is passed, the file
     *  will be selected if files are allowed.  Passing a file when DIRECTORIES_ONLY
     *  is set will result in an IllegalStateException.
     *
     */
    public void addShortcutButton(String buttonName, File file) {
        accessory.addShortcut(buttonName, file);
    }

    /**
     * Override to prevent the user from getting into a directory that has
     * no subDirectories if the mode if DIRECTORIES_ONLY
     */
    public void setCurrentDirectory(File dir) {
        if (dir != null) {
            if (getFileSelectionMode() == JFileChooser.DIRECTORIES_ONLY) {
                File[] subFiles = dir.listFiles(dirFileFilter);

                if ((subFiles == null) || (subFiles.length == 0)) {
                    setSelectedFile(dir);

                    return;
                }
            }
        }

        super.setCurrentDirectory(dir);
    }

    /**
     * Override update UI, so that directory labelling of components
     * can take place, no matter what.
     */
    public void updateUI() {
        if (super.getFileSelectionMode() == DIRECTORIES_ONLY) {
            forceDirectoryLabellingOfComponents();
        } else {
            super.updateUI();
        }
    }

    /**
     * Removes the button with buttonName if it exists
     */
    public void removeShortcutButton(String buttonName) {
        accessory.removeShortcut(buttonName);
    }

    /**
     * Causes label strings to be changed in the UI manager, and a forced
     * update of the UI so that they take effect on the GUI.  Afterwards,
     * restores previous settings so that non-directory file choosers have
     * file labelling.
     */
    private void forceDirectoryLabellingOfComponents() {
        // Save the old state of the label strings.
        String originalFileNameLabelText = UIManager.getString(
                                                   "FileChooser.fileNameLabelText");
        String originalFilesOfTypeLabelText = UIManager.getString(
                                                      "FileChooser.filesOfTypeLabelText");


        // Change label strings to special value for directory browsing.
        UIManager.put("FileChooser.fileNameLabelText", "Directory:");
        UIManager.put("FileChooser.filesOfTypeLabelText", "Directory Filter:");


        // Must update the UI to cause the changes to the strings to take effect.
        super.updateUI();


        // Once that has happened, however, the directory showing in the top
        // combo box (selectd dir) is in a bizarre state (A: drive!).
        // So it must be reset.
        this.setSelectedFile(this.getCurrentDirectory());


        // Cleanup the UIManager to allow the next filechooser to go on as normal.
        UIManager.put("FileChooser.fileNameLabelText", 
                      originalFileNameLabelText);
        UIManager.put("FileChooser.filesOfTypeLabelText", 
                      originalFilesOfTypeLabelText);
    } // End method

    class AccessoryPanel extends JPanel {
        JPanel mainPanel = new JPanel();
        Map btnNameToButton = new HashMap();
        Map btnNameToFile = new HashMap();

        private AccessoryPanel() {
            this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

            JPanel leftSpacer = new JPanel();
            leftSpacer.setLayout(new BoxLayout(leftSpacer, BoxLayout.X_AXIS));
            leftSpacer.add(Box.createHorizontalStrut(10));
            this.add(leftSpacer);

            mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
            this.add(mainPanel);

            JPanel rightSpacer = new JPanel();
            rightSpacer.setLayout(new BoxLayout(rightSpacer, BoxLayout.X_AXIS));
            rightSpacer.add(Box.createHorizontalStrut(10));
            this.add(rightSpacer);
            addDesktop();
        }

        void addShortcut(String buttonName, File file) {
            if (file.isFile() && 
                    (getFileSelectionMode() == JFileChooser.DIRECTORIES_ONLY)) {
                throw new IllegalStateException(
                        "A file shortcut was passed and the " + 
                        " chooser dialog selection mode is set to DIRECTORIES_ONLY");
            }

            JButton btn = new JButton(buttonName);
            btnNameToButton.put(buttonName, btn);
            mainPanel.add(Box.createVerticalStrut(5));
            mainPanel.add(btn);
            btnNameToFile.put(buttonName, file);

            final File finalFile = file;
            btn.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    switch (getFileSelectionMode()) {
                    case JFileChooser.DIRECTORIES_ONLY:

                        if (finalFile.isDirectory()) {
                            setCurrentDirectory(finalFile);
                        }

                        break;

                    case JFileChooser.FILES_ONLY:

                        if (finalFile.isDirectory()) {
                            setCurrentDirectory(finalFile);
                        } else {
                            setSelectedFile(finalFile);
                        }

                        break;

                    case JFileChooser.FILES_AND_DIRECTORIES:
                        setSelectedFile(finalFile);

                        break;
                    }
                }
            });
        }

        boolean containsFileShortcuts() {
            for (Iterator it = btnNameToFile.entrySet().iterator();
                 it.hasNext();) {
                if (((File) ((Map.Entry) it.next()).getValue()).isFile()) {
                    return true;
                }
            }

            return false;
        }

        boolean containsDirectoryShortcuts() {
            for (Iterator it = btnNameToFile.entrySet().iterator();
                 it.hasNext();) {
                if (((File) ((Map.Entry) it.next()).getValue()).isDirectory()) {
                    return true;
                }
            }

            return false;
        }

        void removeShortcut(String buttonName) {
            if (btnNameToButton.containsKey(buttonName)) {
                mainPanel.remove((Component) btnNameToButton.get(buttonName));
                btnNameToButton.remove(buttonName);
                btnNameToFile.remove(buttonName);
            }
        }

        private void addDesktop() {
            String os = System.getProperty("os.name");

            if (os == null) {
                return;
            }

            if (os.toLowerCase().indexOf("windows") == -1) {
                return;
            }

            String home = System.getProperty("user.home");

            if (home == null) {
                return;
            }

            File desktop = new File(home + "\\Desktop\\.");
            addShortcut("Desktop", desktop);
        }
    }

    class DirectoryFileFilter implements java.io.FileFilter {
        public boolean accept(File f) {
            return f.isDirectory();
        }
    }
}