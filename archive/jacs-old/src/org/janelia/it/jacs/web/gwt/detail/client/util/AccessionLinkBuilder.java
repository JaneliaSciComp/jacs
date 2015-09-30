
package org.janelia.it.jacs.web.gwt.detail.client.util;

import com.google.gwt.user.client.ui.Widget;

/**
 * Factory for accesion link
 */
public interface AccessionLinkBuilder {
    Widget createAccessionLink(String displayName, String accession);
}
