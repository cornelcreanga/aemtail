package com.creanga.jsch;

import com.jcraft.jsch.Session;

import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class SshCommand {

    private enum State {
        CREATED, RUNNING,ABORTED,DONE
    }

    private String command;
    private Future<Optional<Exception>> future;

    private int connectionRetries = 3;
    private int connectionRetryInterval = 10;

    //timeout for channel initiation
    private int channelTimeout = 30;
    private long timeout=-1;

    private State state = State.CREATED;

    SshCommand(String command) {
        this.command = command;
    }

    SshCommand(String command,long timeout) {
        this.command = command;
        this.timeout = timeout;
    }

    public int getChannelTimeout() {
        return channelTimeout;
    }

    public void setChannelTimeout(int channelTimeout) {
        this.channelTimeout = channelTimeout;
    }

    public long getTimeout() {
        return timeout;
    }

    public void setFuture(Future future) {
        this.future = future;
    }

    public State getState() {
        return state;
    }

    public boolean isAborted(){
        return state==State.ABORTED;
    }

    public void abort() throws ExecutionException, InterruptedException {
        future.cancel(true);
    }

    public Optional<Exception> await() throws ExecutionException, InterruptedException, TimeoutException {
        if (timeout>-1)
            return future.get(timeout, TimeUnit.MILLISECONDS);
        return future.get();
    }


    public String get() {
        return command;
    }

    public void set(String command) {
        this.command = command;
    }

    public int getConnectionRetries() {
        return connectionRetries;
    }

    public void setConnectionRetries(int connectionRetries) {
        this.connectionRetries = connectionRetries;
    }

    public int getConnectionRetryInterval() {
        return connectionRetryInterval;
    }

    public void setConnectionRetryInterval(int connectionRetryInterval) {
        this.connectionRetryInterval = connectionRetryInterval;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }
}

