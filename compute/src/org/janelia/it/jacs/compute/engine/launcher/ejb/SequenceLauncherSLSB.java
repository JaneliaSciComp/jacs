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

package org.janelia.it.jacs.compute.engine.launcher.ejb;

import org.jboss.annotation.ejb.PoolClass;
import org.jboss.annotation.ejb.TransactionTimeout;
import org.janelia.it.jacs.compute.engine.launcher.ILauncher;
import org.janelia.it.jacs.compute.engine.launcher.SequenceLauncher;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

/**
 * This Stateless Session bean wraps a SequenceLauncher and is used to demarcate the transaction boundary for all actions
 * within the sequence launched by it's Sequence Launcher.  This SLSB runs with "Mandatory" transaction setting making it
 * run within the transaction context of it's parent (could be ProcessLauncherSLSB or ComputeBeanImpl for example).  It first
 * glance one would wonder how this SLSB can demarcate transactions if it always runs within the context of it parent.
 * On second glance, we could extend this SLSB (as BlastLauncherSLSB does) and have that SLSB run in a separate transaction
 * context. You could need to handle the exceptions appropriately as this SLSB lets the calling EJB handle the transaction.
 * <p/>
 * Rule of thumb:  Always have the initiator of transactions decide what it needs to do with the transaction in case of error.
 * <p/>
 * Short story:  Create a subclass of this launcher if you need your sequence to run in a separate transaction.
 *
 * @author Tareq Nabeel
 */
@Stateless
@TransactionAttribute(value = TransactionAttributeType.MANDATORY)
@TransactionTimeout(432000)
@PoolClass(value = org.jboss.ejb3.StrictMaxPool.class, maxSize = 60, timeout = 10000)
public class SequenceLauncherSLSB extends SeriesLauncherSLSB {

    protected ILauncher getLauncher() {
        return new SequenceLauncher();
    }

}
