package com.creanga.jsch;

public class ProcessReturnStatusException extends SshClientException {

    private static final long serialVersionUID = -4265371032071687057L;

    int processReturnStatus;

    public ProcessReturnStatusException(int processReturnStatus) {
        this.processReturnStatus = processReturnStatus;
    }

    public int getProcessReturnStatus() {
        return processReturnStatus;
    }
}
