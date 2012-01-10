
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
