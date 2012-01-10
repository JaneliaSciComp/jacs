
package org.janelia.it.jacs.shared.processors.recruitment;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Sep 11, 2007
 * Time: 8:53:34 AM
 */
public class SampleData implements Comparable, IsSerializable {
    private Integer orderNumber;
    private String name;
    private String description;
    private String projectAccession;
    private String projectName;

    public SampleData() {
    }

    public SampleData(String name, String description, Integer orderNumber, String projectAccession, String projectName) {
        this.name = name;
        this.description = description;
        this.orderNumber = orderNumber;
        this.projectAccession = projectAccession;
        this.projectName = projectName;
    }

    public int compareTo(Object o) {
        return this.orderNumber.compareTo(((SampleData) o).getOrderNumber());
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Integer getOrderNumber() {
        return orderNumber;
    }

    public String getProjectAccession() {
        return projectAccession;
    }

    public String getProjectName() {
        return projectName;
    }
}
