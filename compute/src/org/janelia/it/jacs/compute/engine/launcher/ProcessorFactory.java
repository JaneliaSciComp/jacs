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

package org.janelia.it.jacs.compute.engine.launcher;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.engine.def.OperationDef;
import org.janelia.it.jacs.compute.engine.def.ProcessorType;
import org.janelia.it.jacs.compute.engine.def.SeriesDef;
import org.janelia.it.jacs.compute.engine.service.IService;

/**
 * This class returns the processor instance an action in a process definition
 *
 * @author Tareq Nabeel
 */
public class ProcessorFactory {
    private static Logger logger = Logger.getLogger(ProcessorFactory.class);

    /**
     * Creates the ILauncher instance for the processor specified in the process or sequence element
     * in a process definition
     *
     * @param seriesDef The Series definition
     * @return the ILauncher instance
     * @throws LauncherException if it cannot find the launcher
     */
    public static ILauncher createLauncherForSeries(SeriesDef seriesDef) throws LauncherException {
        return createSpecificLauncherForSeries(seriesDef, seriesDef.getProcessorType());
    }

    /**
     * Creates the ILauncher instance for the processor specified in the process or sequence element
     * in a process definition. Type of launcher spefiefied inside of seriesDef is ignored
     *
     * @param seriesDef The Series definition
     * @param pType     Specified type of launcher to create
     * @return the ILauncher instance (SLSB or Pojo)
     * @throws LauncherException if it cannot find the launcher
     */
    public static ILauncher createSpecificLauncherForSeries(SeriesDef seriesDef, ProcessorType pType) throws LauncherException {
        ILauncher launcher;
        switch (pType) {
            case POJO:
                try {
                    launcher = (ILauncher) Class.forName(seriesDef.getProcessorName()).newInstance();
                }
                catch (Exception e) {
                    throw new RuntimeException(e);
                }
                break;
            case LOCAL_SLSB:
                launcher = EJBFactory.getLocalSeriesLauncher(seriesDef.getProcessorName());
                break;
            default:
                throw new LauncherException("unsupported");
        }
        logger.info("Using ILauncher: " + launcher.getClass().getName() + " for processor: " + seriesDef.getProcessorName());
        return launcher;
    }

    /**
     * Creates the IService instance for the processor specified in the operation element in a process definition
     * or the default processor
     *
     * @param operationDef the operation definition
     * @return the IService instance (SLSB or Pojo)
     */
    public static IService createServiceForOperation(OperationDef operationDef) {
        IService service;
        switch (operationDef.getProcessorType()) {
            case POJO:
            case LOCAL_MDB:
                try {
                    service = (IService) Class.forName(operationDef.getProcessorName()).newInstance();
                }
                catch (Exception e) {
                    throw new RuntimeException("Error loading pojo class: " + operationDef.getProcessorName() +
                            ". If SLSB was intended, make sure processor begins with 'local/' in the operation definition and " +
                            "if message queue was intended, make sure processor begins with 'queue/'", e);
                }
                break;
            case LOCAL_SLSB:
                try {
                    service = EJBFactory.getLocalServiceBean(operationDef.getProcessorName());
                }
                catch (RuntimeException e) {
                    logger.error("Make sure processor begins with 'local/' !!!");
                    throw e;
                }
                break;
            default:
                throw new RuntimeException("unsupported");
        }
        logger.info("Using IService: " + service.getClass().getName() + " for processor: " + operationDef.getProcessorName());
        return service;
    }

}
