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

package org.janelia.it.jacs.compute.engine.def;

import java.io.Serializable;

/**
 * This class represents the different types of processors that can be specified
 * by the processorType/processor attribute of process, sequence, and operation elements
 * in a process definition
 *
 * @author Tareq Nabeel
 */
public enum ProcessorType implements Serializable {
    LOCAL_SLSB, // would use a local ejb interface to the stateless session bean
    POJO,       // package qualified class name
    LOCAL_MDB,  // MDB listening on a queue on a local JMS provider
    REMOTE_SLSB, // would use a remote ejb interface to the stateless session bean
    REMOTE_MDB, // MDB listening on a queue on a remote JMS provider
    WEB_SERVICE  //
}
