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

package org.janelia.it.jacs.model.tasks.blast;

import org.janelia.it.jacs.model.vo.ParameterException;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Nov 17, 2009
 * Time: 2:12:23 PM
 */
public class TeragridSimpleBlastTask extends BlastTask {
    transient public static final String TG_SIMPLE_BLAST_TASK_NAME = "TeragridSimpleBlastTask";

    transient public static final String PARAM_teragrid_grant_number = "Teragrid Grant Number";
    transient public static final String PARAM_mpi_blast_program = "MPI Blast Program";
    transient public static final String PARAM_tg_db_name = "Teragrid Blast DB Name";
    transient public static final String PARAM_tg_db_size = "Teragrid Blast DB Size";
    transient public static final String PARAM_path_to_sqlite_map_db = "Sqlite Map DB Path";
    transient public static final String PARAM_mpi_blast_parameters = "MPI Blast Parameters";

    public TeragridSimpleBlastTask() {
        super();
        this.taskName = TG_SIMPLE_BLAST_TASK_NAME;
    }

    public String getDisplayName() {
        return "TeragridSimpleBlastTask";
    }

    public String generateCommandStringNotIncludingIOParams() throws ParameterException {
        throw new ParameterException("Not supported");
    }

}
