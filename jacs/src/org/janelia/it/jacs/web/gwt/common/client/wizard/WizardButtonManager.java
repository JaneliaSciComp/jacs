
package org.janelia.it.jacs.web.gwt.common.client.wizard;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Widget;
import org.janelia.it.jacs.web.gwt.common.client.ui.ButtonSet;
import org.janelia.it.jacs.web.gwt.common.client.ui.RoundedButton;

/**
 * Delegate for the WizardController, to manage the buttons on a WizardPage.  Allows the WizardPage to have access
 * to the buttons to set the text and enable/disable the buttons based on user input.  The WizardButtonManager
 * handles receiving button click events, but delegates the actual action to perform to the WizardController.
 *
 * @author Michael Press
 */
public class WizardButtonManager {
    private WizardController _controller;
    private RoundedButton _backButton;
    private RoundedButton _nextButton;
    private ButtonSet _buttonSet;
    private String _backButtonText = "Back"; //default
    private String _nextButtonText = "Next"; //default

    public WizardButtonManager(WizardController controller) {
        _controller = controller;
        init();
    }

    public void init() {
        _backButton = new RoundedButton(_backButtonText, new BackButtonClickListener());
        _nextButton = new RoundedButton(_nextButtonText, new NextButtonClickListener());
        _buttonSet = new ButtonSet(new RoundedButton[]{_backButton, _nextButton});
    }

    public ButtonSet getButtonSet() {
        return _buttonSet;
    }

    public void setBackButtonText(String backButtonText) {
        _backButton.setText(backButtonText);
    }

    public void setNextButtonText(String nextButtonText) {
        _nextButton.setText(nextButtonText);
    }

    public void setBackButtonEnabled(boolean enabled) {
        _backButton.setEnabled(enabled);
    }

    public void setNextButtonEnabled(boolean enabled) {
        _nextButton.setEnabled(enabled);
    }

    public void resetButtonClickListeners() {
        _backButton.setClickListener(new BackButtonClickListener());
        _nextButton.setClickListener(new NextButtonClickListener());
    }

    /** Back button destination page can be overriden */
    //public void setBackDestinationPage(final int destPage)
    //{
    //    _backButton.addClickListener(new ClickListener() {
    //        public void onClick(Widget widget)
    //        {
    //            _controller.gotoPage(destPage);
    //        }
    //    });
    //}

    /**
     * Delegate to the WizardController when the user clicks back
     */
    public class BackButtonClickListener implements ClickListener {
        public void onClick(Widget widget) {
            _controller.back();
        }
    }

    /**
     * Delegate to the WizardController when the user clicks next
     */
    public class NextButtonClickListener implements ClickListener {
        public void onClick(Widget widget) {
            _controller.next();
        }
    }

    public RoundedButton getBackButton() {
        return _backButton;
    }

    public RoundedButton getNextButton() {
        return _nextButton;
    }
}
