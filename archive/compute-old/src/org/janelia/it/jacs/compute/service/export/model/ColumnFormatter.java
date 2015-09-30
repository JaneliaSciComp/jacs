
package org.janelia.it.jacs.compute.service.export.model;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Jul 29, 2008
 * Time: 4:00:10 PM
 */
public abstract class ColumnFormatter {

    protected static void add(List<String> list, String item) {
        list.add(item);
    }

}
