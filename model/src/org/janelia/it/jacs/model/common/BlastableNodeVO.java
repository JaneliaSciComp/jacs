/*
 * Copyright (c) 2010-2011, J. Craig Venter Institute, Inc.
 *
 * This file is part of JCVI VICS.
 *
 * JCVI VICS is free software; you can redistribute it and/or modify it
 * under the terms and conditions of the Artistic License 2.0.  For
 * details, see the full text of the license in the file LICENSE.txt.  No
 * other rights are granted.  Any and all third party software rights to
 * remain with the original developer.
 *
 * JCVI VICS is distributed in the hope that it will be useful in
 * bioinformatics applications, but it is provided "AS IS" and WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTIES including but not limited to
 * implied warranties of merchantability or fitness for any particular
 * purpose.  For details, see the full text of the license in the file
 * LICENSE.txt.
 *
 * You should have received a copy of the Artistic License 2.0 along with
 * JCVI VICS.  If not, the license can be obtained from
 * "http://www.perlfoundation.org/artistic_license_2_0."
 */

package org.janelia.it.jacs.model.common;

import com.google.gwt.user.client.rpc.IsSerializable;
import org.janelia.it.jacs.model.user_data.DataSource;

import java.io.Serializable;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Nov 9, 2006
 * Time: 2:38:06 PM
 */
public class BlastableNodeVO implements IsSerializable, Serializable {

    private String nodeName = "";
    private String databaseObjectId = "";
    private String description = "";
    private String dataType = "";
    private String sequenceType = "";
    private String owner = "";
    private String length = "";
    private String visibility = "";
    private DataSource dataSource = null;
    private Boolean isAssembledData;
    private String sequenceCount = "0";
    private Integer order = 0;

    public BlastableNodeVO() {
    }

    public BlastableNodeVO(String databaseObjectId,
                           String nodeName,
                           String description,
                           String dataType,
                           String sequenceType,
                           String owner,
                           String length,
                           String sequenceCount,
                           String visibility,
                           DataSource dataSource,
                           Boolean isAssembledData,
                           Integer order) {
        this.databaseObjectId = databaseObjectId;
        this.nodeName = nodeName;
        this.description = description;
        this.dataType = dataType;
        this.sequenceType = sequenceType;
        this.owner = owner;
        this.length = length;
        setSequenceCount(sequenceCount);
        this.visibility = visibility;
        this.dataSource = dataSource;
        this.isAssembledData = isAssembledData;
        this.order = order;
    }

    public String getDatabaseObjectId() {
        return databaseObjectId;
    }

    public void setDatabaseObjectId(String databaseObjectId) {
        this.databaseObjectId = databaseObjectId;
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDataType() {
        return dataType;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
    }

    public String getSequenceType() {
        return sequenceType;
    }

    public void setSequenceType(String sequenceType) {
        this.sequenceType = sequenceType;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getLength() {
        return length;
    }

    public void setLength(String length) {
        this.length = length;
    }

    public String getVisibility() {
        return visibility;
    }

    public void setVisibility(String visibility) {
        this.visibility = visibility;
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Boolean isAssembledData() {
        return isAssembledData;
    }

    public void setIsAssembledData(Boolean assembledData) {
        isAssembledData = assembledData;
    }

    public String getSequenceCount() {
        return sequenceCount;
    }

    public void setSequenceCount(String sequenceCount) {
        this.sequenceCount = sequenceCount == null ? "0" : sequenceCount;
    }

    public void setSequenceCount(Integer sequenceCount) {
        this.sequenceCount = sequenceCount == null ? "0" : sequenceCount.toString();
    }

    public String toString() {
        return "DataNodeVO{" +
                "databaseObjectId='" + databaseObjectId + '\'' +
                ", nodeName='" + nodeName + '\'' +
                ", description='" + description + '\'' +
                ", dataType='" + dataType + '\'' +
                ", dataSource='" + dataSource.getSourceName() + '\'' +
                ", sequenceType='" + sequenceType + '\'' +
                ", owner='" + owner + '\'' +
                ", length='" + length + '\'' +
                ", sequenceCount='" + sequenceCount + '\'' +
                ", visibility='" + visibility + '\'' +
                ", isAssembledData='" + isAssembledData.toString() + '\'' +
                '}';
    }

    public Integer getOrder() {
        return order;
    }

    public void setOrder(Integer order) {
        this.order = order;
    }
}
