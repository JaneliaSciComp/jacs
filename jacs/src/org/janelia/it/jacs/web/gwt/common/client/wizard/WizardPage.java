
package org.janelia.it.jacs.web.gwt.common.client.wizard;

import com.google.gwt.user.client.ui.Widget;
import org.gwtwidgets.client.wrap.Callback;
import org.gwtwidgets.client.wrap.EffectOption;
import org.janelia.it.jacs.web.gwt.common.client.effect.SafeEffect;
import org.janelia.it.jacs.web.gwt.common.client.panel.TitledBox;
import org.janelia.it.jacs.web.gwt.common.client.panel.TitledPanel;
import org.janelia.it.jacs.web.gwt.common.client.ui.LoadingLabel;

/**
 * Abstract base class for concrete pages in a Wizard.  This base class handles creating the back/next page buttons
 * (though the action that occurs when clicked is delegated to the WizardController).  Concrete subclasses must
 * implement getMainPanel() to create the actual page contents, and can enable/disable buttons dynamically based on
 * user input using the enable/disableBack/NextButton() methods.
 *
 * @author Michael Press
 */
abstract public class WizardPage {

    private boolean _showButtons = true;
    private WizardController _controller;

    abstract public String getPageToken(); // used for history

    abstract public Widget getMainPanel();

    abstract public String getPageTitle();

    protected WizardPage(WizardController controller) {
        this(true, controller);
    }

    protected WizardPage(boolean showButtons, WizardController controller) {
        _showButtons = showButtons;
        _controller = controller;
    }

    //TODO: just return no buttons instead
    public boolean showButtons() {
        return _showButtons;
    }

    public WizardController getController() {
        return _controller;
    }

    public WizardButtonManager getButtonManager() {
        return getController().getButtonManager();
    }

    public boolean checkPageToken(String pageName) {
        return getPageToken().startsWith(pageName);
    }

    /**
     * Subclasses are notified when page will be displayed, and informed of the page that the wizard is coming from
     * (or null if the wizard is starting on the invoked page)
     *
     * @param priorPageNumber page coming from
     */
    protected void preProcess(Integer priorPageNumber) {
    }

    /**
     * Certain operations in IE require the main panel to already be added;
     * This is the wizard notification for the current page that the main
     * panel has just been added
     */
    protected void mainPanelCreated() {
    }

    /**
     * Subclasses are notified when user is leaving page, and the destination page.
     *
     * @param nextPageNumber page going to
     */
    protected void postProcess(Integer nextPageNumber) {
    }

    /**
     * Subclasses can set button state as appropriate for the current user selections (default is both buttons enabled)
     */
    protected void setupButtons() {
        getButtonManager().resetButtonClickListeners();
        getButtonManager().setBackButtonEnabled(true);
        getButtonManager().setNextButtonEnabled(true);
        getButtonManager().setBackButtonText("Back");
        getButtonManager().setNextButtonText("Next");
    }

    /**
     * Fades out an old panel and fades in a new one
     *
     * @param newPanel panel to fade out
     * @param oldPanel panel to fade in
     */
    protected void fadeIn(final TitledPanel newPanel, final TitledPanel oldPanel) {
        fadeIn(newPanel, oldPanel, null);
    }

    /**
     * Fades out an old panel and fades in a new one.  Unshows loading msg (if supplied) when load is complete;  caller
     * must already have set the label to visible
     *
     * @param newPanel     panel to fade out
     * @param oldPanel     panel to fade in
     * @param loadingLabel label to show while fading
     */
    protected void fadeIn(final TitledPanel newPanel, final TitledPanel oldPanel, final LoadingLabel loadingLabel) {
        // Last step - realize and fade in the new panel
        final Callback opacityFinished = new Callback() {
            public void execute() {
                newPanel.setVisible(true);
                if (!newPanel.isRealized())
                    newPanel.realize();
                if (loadingLabel != null)
                    loadingLabel.setVisible(false);
                SafeEffect.fade(newPanel, new EffectOption[]{
                        new EffectOption("to", "1.0")
                        , new EffectOption("duration", "0.2")
                });
            }
        };

        // After fadeout of old panel is complete, set the opacity on the new panel to 1% so it's basically not visible
        Callback fadeOutFinished = new Callback() {
            public void execute() {
                oldPanel.setVisible(false);
                SafeEffect.opacity(newPanel, new EffectOption[]{
                        new EffectOption("to", "0.01")
                        , new EffectOption("duration", "0")
                        , new EffectOption("afterFinish", opacityFinished)
                });
            }
        };

        // Fade out the old panel
        SafeEffect.fade(oldPanel, new EffectOption[]{
                new EffectOption("to", "0.01")
                , new EffectOption("duration", "0.1")
                , new EffectOption("afterFinish", fadeOutFinished)
        });
    }

    /**
     * Fades out an old panel and fades in a new one
     *
     * @param newPanel panel to fade out
     * @param oldPanel panel to fade in
     */
    protected void fadeIn(final TitledBox newPanel, final TitledBox oldPanel) {
        fadeIn(newPanel, oldPanel, null);
    }

    /**
     * Fades out an old panel and fades in a new one.  Unshows loading msg (if supplied) when load is complete;  caller
     * must already have set the label to visible
     *
     * @param newPanel     panel to fade out
     * @param oldPanel     panel to fade in
     * @param loadingLabel label to show while fading
     */
    protected void fadeIn(final TitledBox newPanel, final TitledBox oldPanel, final LoadingLabel loadingLabel) {
        // Last step - realize and fade in the new panel
        final Callback opacityFinished = new Callback() {
            public void execute() {
                newPanel.setVisible(true);
// Not relevant to titled box
//                if (!newPanel.isRealized())
//                    newPanel.realize();
                if (loadingLabel != null)
                    loadingLabel.setVisible(false);
                SafeEffect.fade(newPanel, new EffectOption[]{
                        new EffectOption("to", "1.0")
                        , new EffectOption("duration", "0.2")
                });
            }
        };

        // After fadeout of old panel is complete, set the opacity on the new panel to 1% so it's basically not visible
        Callback fadeOutFinished = new Callback() {
            public void execute() {
                oldPanel.setVisible(false);
                SafeEffect.opacity(newPanel, new EffectOption[]{
                        new EffectOption("to", "0.01")
                        , new EffectOption("duration", "0")
                        , new EffectOption("afterFinish", opacityFinished)
                });
            }
        };

        // Fade out the old panel
        SafeEffect.fade(oldPanel, new EffectOption[]{
                new EffectOption("to", "0.01")
                , new EffectOption("duration", "0.1")
                , new EffectOption("afterFinish", fadeOutFinished)
        });
    }

    /**
     * This method is intended to permit veto of any forward wizard transition, for cases of incomplete data or
     * other problem.  Also, method gives the page the ability to communicate to the user what info is still
     * required.
     *
     * @return boolean if the page transition forward should be allowed.
     */
    protected boolean isProgressionValid() {
        return true;
    }
}
