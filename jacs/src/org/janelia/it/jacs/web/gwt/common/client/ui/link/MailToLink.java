
package org.janelia.it.jacs.web.gwt.common.client.ui.link;

/**
 * @author Michael Press
 */
public class MailToLink extends ExternalLink {
    public MailToLink(String linkText, String emailAddress) {
        super(linkText, "mailto:" + emailAddress);
    }
}
