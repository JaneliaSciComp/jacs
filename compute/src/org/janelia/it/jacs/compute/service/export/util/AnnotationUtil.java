
package org.janelia.it.jacs.compute.service.export.util;

import org.janelia.it.jacs.model.genomics.AnnotationDescription;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Jul 9, 2008
 * Time: 3:35:24 PM
 */
public class AnnotationUtil {

    private static final String ROW_SEP = "\n";
    private static final String LIST_SEP = "\t";

    public static List<AnnotationDescription> createAnnotationListFromString(String listString) {
        return createAnnotationListFromString(listString, ROW_SEP, LIST_SEP);
    }

    public static List<AnnotationDescription> createAnnotationListFromString(String listString, String rowSep, String listSep) {
        List<AnnotationDescription> annoList = new ArrayList<AnnotationDescription>();
        if (listString == null)
            return annoList;
        String[] rowArr = listString.split(rowSep);
        for (String r : rowArr) {
            String[] l = r.split(listSep);
            if (l.length >= 2) {
                AnnotationDescription ad = new AnnotationDescription();
                ad.setId(l[0]);
                StringBuffer dBuffer = new StringBuffer();
                for (int i = 1; i < l.length; i++) {
                    if (i == 1)
                        dBuffer.append(l[i]);
                    else
                        dBuffer.append(", ").append(l[i]);
                }
                ad.setDescription(dBuffer.toString());
                annoList.add(ad);
            }
        }
        return annoList;
    }
}


