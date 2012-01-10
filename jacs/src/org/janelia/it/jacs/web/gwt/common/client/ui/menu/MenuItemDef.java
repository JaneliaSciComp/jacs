
package org.janelia.it.jacs.web.gwt.common.client.ui.menu;

import com.google.gwt.user.client.Command;

/**
 * @author Michael Press
 */
public class MenuItemDef {
    private String _label;
    private Command _command;

    public MenuItemDef(String label, Command command) {
        _command = command;
        _label = label;
    }

    public void setLabel(String label) {
        _label = label;
    }

    public void setCommand(Command command) {
        _command = command;
    }

    public String getLabel() {
        return _label;
    }

    public Command getCommand() {
        return _command;
    }
}
