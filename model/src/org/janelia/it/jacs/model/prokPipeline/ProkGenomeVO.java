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

package org.janelia.it.jacs.model.prokPipeline;

import com.google.gwt.user.client.rpc.IsSerializable;
import org.janelia.it.jacs.model.tasks.Event;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Jul 17, 2009
 * Time: 9:27:32 AM
 */
public class ProkGenomeVO implements IsSerializable, Serializable {
    private List<Event> _events = new ArrayList<Event>();
    private String _localGenomeDirName;
    private String _targetOutputDirectory;

    public ProkGenomeVO() {
    }

    public ProkGenomeVO(String _localGenomeDirName) {
        this._localGenomeDirName = _localGenomeDirName;
    }

    public List<Event> getEvents() {
        return _events;
    }

    public void setEvents(List<Event> events) {
        this._events = events;
    }

    public String getLocalGenomeDirName() {
        return _localGenomeDirName;
    }

    public String getTargetOutputDirectory() {
        return _targetOutputDirectory;
    }

    public void setLocalGenomeDirName(String localGenomeDirName) {
        _localGenomeDirName = localGenomeDirName;
    }

    public void setTargetOutputDirectory(String directoryPath) {
        _targetOutputDirectory = directoryPath;
    }
}
