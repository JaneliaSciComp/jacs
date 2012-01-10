
package org.janelia.it.jacs.web.gwt.common.client.ui.link;

import com.google.gwt.user.client.ui.*;

/**
 * @author Michael Press
 */
public class ActionLink extends Composite implements HasActionLink {
    private HistorySafeHyperlink _link;
    private SimplePanel _leftImagePanel;
    private SimplePanel _rightImagePanel;
    private HTML _leftBracketHtml;
    private HTML _rightBracketHtml;
    private Image _image;
    private boolean _showBrackets;
    private ClickListenerCollection _clickListenerCollection = new ClickListenerCollection();

    public enum Side {
        LEFT, RIGHT
    }

    /**
     * Simple action link with brackets and no image: [edit]
     */
    public ActionLink(String linkText) {
        this(linkText, /*clickListener*/ null);
    }

    /**
     * Simple action link with brackets and no image: [edit]
     */
    public ActionLink(String linkText, ClickListener clickListener) {
        this(linkText, /*image*/ null, clickListener);
        setShowBrackets(true);
    }

    /**
     * Action link with image, and NO brackets: (img) link
     */
    public ActionLink(String linkText, Image image, ClickListener clickListener) {
        init();
        setShowBrackets(false);
        setText(linkText);
        setImage(image);
        addClickListener(clickListener);
    }

    private void init() {
        // Create the text link
        _link = new HistorySafeHyperlink();
        _link.setStyleName("actionLink");

        // Create a panel to put everything together
        HorizontalPanel panel = new HorizontalPanel();
        _leftImagePanel = new SimplePanel();
        _rightImagePanel = new SimplePanel();
        _leftBracketHtml = new HTML();
        _rightBracketHtml = new HTML();

        panel.add(_leftBracketHtml);
        panel.add(_leftImagePanel);
        panel.add(_link);
        panel.add(_rightImagePanel);
        panel.add(_rightBracketHtml);

        // Composite is required to notify parent of the core wrapped widget, in this case the whole panel
        initWidget(panel);
    }

    /**
     * Sets the single ClickListener (removes any old ClickListener)
     */
    public void addClickListener(ClickListener clickListener) {
        if (clickListener != null) {
            _clickListenerCollection.add(clickListener);
            _link.addClickListener(clickListener);
            if (_image != null)
                _image.addClickListener(clickListener);
        }
    }

    public void setLinkStyleName(String styleName) {
        _link.setStyleName(styleName);
    }

    public void addLinkStyleName(String styleName) {
        _link.addStyleName(styleName);
        if (showBrackets())
            addBracketStyleName(styleName);
    }

    public void addBracketStyleName(String styleName) {
        _leftBracketHtml.addStyleName(styleName);
        _rightBracketHtml.addStyleName(styleName);
    }

    public void addImageStyleName(String styleName) {
        _image.addStyleName(styleName);
    }

    public void setText(String text) {
        _link.setText(text);
    }

    public String getText() {
        return _link.getText();
    }

    /**
     * Places an image to the left of the text link.  The image is responsible for setting margin styles
     * for the correct offset between it and the text link.
     */
    public void setImage(Image image) {
        setImage(image, Side.LEFT);
    }

    public void setImage(Image image, Side side) {
        _leftImagePanel.clear();
        _rightImagePanel.clear();

        if (image != null) {
            _image = image;
            addImageStyleName("actionLinkImage"); // Give the image the hand cursor and any existing click listeners
            for (ClickListener clickListener : _clickListenerCollection)
                _image.addClickListener(clickListener);

            SimplePanel imagePanel = (side == Side.LEFT) ? _leftImagePanel : _rightImagePanel;
            imagePanel.add(_image);
        }
    }

    protected Image getImage() {
        return _image;

    }

    public String getLinkTargetHistoryToken() {
        return _link.getTargetHistoryToken();
    }

    public void setTargetHistoryToken(String targetHistoryToken) {
        _link.setTargetHistoryToken(targetHistoryToken);
    }

    public String getTargetHistoryToken() {
        return _link.getTargetHistoryToken();
    }

    //TODO: support this?
    public void setPopupText(String popupText) {
    }

    public void setShowBrackets(boolean showBrackets) {
        _showBrackets = showBrackets;
        if (showBrackets()) {
            _leftBracketHtml.setText("[");
            _rightBracketHtml.setText("]");
            _leftBracketHtml.addStyleName("actionLinkBracket");
            _rightBracketHtml.addStyleName("actionLinkBracket");
            _link.removeStyleName("actionLinkTextWrapper");
        }
        else {
            _leftBracketHtml.setText("");
            _rightBracketHtml.setText("");
            _leftBracketHtml.removeStyleName("actionLinkBracket");
            _rightBracketHtml.removeStyleName("actionLinkBracket");
            _link.addStyleName("actionLinkTextWrapper"); // space the text away from images
        }
    }

    public boolean showBrackets() {
        return _showBrackets;
    }
}