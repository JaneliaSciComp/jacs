
package org.janelia.it.jacs.web.gwt.download.client.model;

import com.google.gwt.user.client.rpc.IsSerializable;
import org.janelia.it.jacs.model.download.DataFile;

import java.io.Serializable;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: Lfoster
 * Date: Sep 25, 2006
 * Time: 10:56:22 AM
 * <p/>
 * Project, which has publications.  One such project is "PROJECT".
 */
public interface Project extends Serializable, IsSerializable, Comparable {
    public static final String NAME_SORT = "name";
    public static final String PI_SORT = "principal_investigators";
    public static final String ORG_SORT = "organization";
    public static final String HABITAT_SORT = "habitat";//TODO
    public static final String DESCRIPTION_SORT = "description";

    List<Publication> getPublications(); // Return a list of publication objects.

    String getProjectSymbol(); // This is the actual ID - added for reference purposes

    String getProjectName(); // There might be portions of the code which use the name for dentification.

    String getDescription();

    String getPrincipalInvestigators();     // comma-separated list

    String getOrganization();

    String getEmail();

    String getWebsite();

    String getFundedBy();                   // comma-separated list

    String getInstitutionalAffiliation();   // comma-separated list

    List<DataFile> getRolledUpArchivesOfPublications(); // Return list of all-pubs-compressed archives.
}
