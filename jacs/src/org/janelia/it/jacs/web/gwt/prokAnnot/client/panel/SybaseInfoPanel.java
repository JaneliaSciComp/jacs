
package org.janelia.it.jacs.web.gwt.prokAnnot.client.panel;

import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.TextBox;
import org.janelia.it.jacs.model.user_data.prefs.UserPreference;
import org.janelia.it.jacs.web.gwt.common.client.service.prefs.Preferences;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Jul 16, 2009
 * Time: 4:35:38 PM
 */
public class SybaseInfoPanel extends HorizontalPanel {
    private TextBox _usernameTextBox;
    private TextBox _sybasePasswordTextBox;

    public SybaseInfoPanel() {
        super();
        _usernameTextBox = new TextBox();
        _usernameTextBox.setVisibleLength(15);
        _sybasePasswordTextBox = new TextBox();
        _sybasePasswordTextBox.setVisibleLength(15);

        UserPreference namePref = Preferences.getUserPreference("sbLogin", "ProkPipeline");
        if (null != namePref) {
            _usernameTextBox.setText(namePref.getValue());
        }
        UserPreference passPref = Preferences.getUserPreference("sbPass", "ProkPipeline");
        if (null != passPref) {
            _sybasePasswordTextBox.setText(passPref.getValue());
        }

        this.add(HtmlUtils.getHtml("Username :", "nowrapprompt"));
        this.add(_usernameTextBox);
        this.add(HtmlUtils.getHtml("&nbsp;&nbsp;", "nowrapprompt"));
        this.add(HtmlUtils.getHtml("Sybase Password :", "nowrapprompt"));
        this.add(_sybasePasswordTextBox);
    }

    public String getUsername() {
        return _usernameTextBox.getText();
    }

    public String getSybasePassword() {
        return _sybasePasswordTextBox.getText();
    }
}
