package org.janelia.it.FlyWorkstation.gui.framework.console;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import org.janelia.it.FlyWorkstation.gui.util.Icons;
import org.janelia.it.FlyWorkstation.gui.util.WrapLayout;
import org.janelia.it.FlyWorkstation.shared.util.Utils;

/**
 * A lazy-loading image with a title on top and optional annotation tags underneath.
 */
public class AnnotatedImageButton extends JToggleButton {
	
    private final List<String> tags = new ArrayList<String>();
    private final JPanel tagPanel;
    private final JLabel imageLabel;
    private final String title;
    private final String imageFilename;
	private BufferedImage maxSizeImage;
	private double scale;
	
    public AnnotatedImageButton(String title, String imageFilename, final int index) {
    	
    	this.title = title;
        this.imageFilename = imageFilename;
        
        GridBagConstraints c = new GridBagConstraints();
        JPanel imagePanel = new JPanel(new GridBagLayout());
        imagePanel.setOpaque(false);
        add(imagePanel);

        JTextPane imageCaption = new JTextPane();
        imageCaption.setFocusable(false);
        imageCaption.setText(title);
        imageCaption.setFont(new Font("Sans Serif", Font.PLAIN, 14));
        imageCaption.setAlignmentX(Component.CENTER_ALIGNMENT);
        imageCaption.setEditable(false);
        imageCaption.setOpaque(false);
        StyledDocument doc = imageCaption.getStyledDocument();
        SimpleAttributeSet center = new SimpleAttributeSet();
        StyleConstants.setAlignment(center, StyleConstants.ALIGN_CENTER);
        doc.setParagraphAttributes(0, doc.getLength(), center, false);
        
        c.gridx = 0;
        c.gridy = 0;
        c.insets = new Insets(0,0,5,0);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.PAGE_START;
        c.weighty = 0;
        imagePanel.add(imageCaption,c);

        imageLabel = new JLabel();
        imageLabel.setSize(300, 300);
        imageLabel.setIcon(Icons.loadingIcon);
        imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        imageLabel.setVerticalAlignment(SwingConstants.TOP);
        
        c.gridx = 0;
        c.gridy = 1;
        c.insets = new Insets(0,0,5,0);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.PAGE_START;
        c.weighty = 0;
        imagePanel.add(imageLabel,c);

        tagPanel = new JPanel(new WrapLayout());
        tagPanel.setOpaque(false);
        refreshTags();

        c.gridx = 0;
        c.gridy = 2;
        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.PAGE_START;
        c.weighty = 1;
        imagePanel.add(tagPanel, c);

    }

    public void loadImage(double scale) {
    	
    	try {
        	maxSizeImage = Utils.readImage(imageFilename);
        	rescaleImage(scale);
        	
    	}
    	catch (IOException e) {
    		
	    	imageLabel.setIcon(null);
	    	imageLabel.setForeground(Color.red);
        	imageLabel.setIcon(Icons.missingIcon);
        	imageLabel.setVerticalTextPosition(JLabel.BOTTOM);
        	imageLabel.setHorizontalTextPosition(JLabel.CENTER);
        	
    		if (e instanceof FileNotFoundException) {
    	    	imageLabel.setText("File not found");
    		}
    		else {
    			e.printStackTrace();
    	    	imageLabel.setText("Image could not be loaded");
    		}
    	}
    }
    
    public void rescaleImage(double scale) {
    	if (maxSizeImage == null) return;
    	imageLabel.setIcon(new ImageIcon(Utils.getScaledImageIcon(maxSizeImage, scale)));
    	setScale(scale);
    }
    
    public Icon getImage() {
    	return imageLabel.getIcon();
    }
    
	public String getTitle() {
		return title;
	}

	public String getImageFilename() {
		return imageFilename;
	}

	private synchronized void setScale(double scale) {
		this.scale = scale;
	}

	public synchronized double getScale() {
		return scale;
	}

	public List<String> getTags() {
        return tags;
    }

    public void refreshTags() {

        tagPanel.removeAll();

        Border paddingBorder = BorderFactory.createEmptyBorder(5,5,5,5);
        Border lineBorder = BorderFactory.createLineBorder(Color.black, 1);
        Border border = BorderFactory.createCompoundBorder(lineBorder, paddingBorder);
        
        for(final String tag : tags) {
            JLabel tagLabel = new JLabel(tag);
            tagLabel.setBorder(border);
            tagLabel.setFont(new Font("Sans Serif", Font.BOLD, 12));
            tagLabel.setOpaque(true);
            tagLabel.setBackground(Color.white);
            tagLabel.setForeground(Color.black);
            tagPanel.add(tagLabel);

            tagLabel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount()==2) {
                        AnnotatedImageButton.this.addOrRemoveTag(tag);
                        SwingUtilities.updateComponentTreeUI(AnnotatedImageButton.this);
                    }
                }
            });

        }
    }

    /**
     * Add or remove the given tag from the currently selected image button.
     * @param tag
     * @return true if tag was added
     */
    public boolean addOrRemoveTag(String tag) {

        if (tags.contains(tag)) {
            tags.remove(tag);
            refreshTags();
            return false;
        }
        else {
            tags.add(tag);
            refreshTags();
            return true;
        }

    }

	    
}