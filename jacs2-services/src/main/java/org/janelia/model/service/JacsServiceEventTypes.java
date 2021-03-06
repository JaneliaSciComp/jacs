package org.janelia.model.service;

public enum JacsServiceEventTypes {
    CANCELED,
    COMPLETED,
    SUSPEND,
    TIMEOUT,
    QUEUED,
    CREATE_CHILD_SERVICE,
    CLUSTER_SUBMIT,
    CLUSTER_JOB_ERROR,
    CLUSTER_STOP_JOB,
    FAILED,
    CREATED_RUNNING_SCRIPT,
    SCRIPT_CREATION_ERROR,
    START_PROCESS,
    START_PROCESS_ERROR,
    PROCESSING_ERROR,
    PROCESSING_COMPLETED,
    REMOVE_DATA,
    UPDATE_STATE
}
