package com.creanga.jsch;


import com.jcraft.jsch.*;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Optional;
import java.util.concurrent.*;

public class JschClient {


    private static final Logger logger = LogManager.getLogger(JschClient.class);
    //host data
    private String host = null;
    private byte[] hostPublicKey = null;
    private int port = 22;

    //authentication
    private String username;
    private String password = null;
    private byte[] clientPrivateKey = null;
    private byte[] passphrase = null;

    //timeout for session initiation
    private int sessionTimeout = 30;
    //connection alive config
    private int aliveInterval = 60;
    private int aliveRetries = 3;


    CancellingExecutor executor;
    CopyOnWriteArrayList<SshCommand> runningCommands = new CopyOnWriteArrayList<>();

    public JschClient(String host, String username, String password) {
        this.host = host;
        this.username = username;
        this.password = password;
        executor = new CancellingExecutor(64, 64,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(),
                r -> {
                    Thread t = Executors.defaultThreadFactory().newThread(r);
                    t.setDaemon(true);
                    return t;
                });

    }

    public JschClient(String host, String username) {
        this(host, username, null);
    }

    public String getHost() {
        return host;
    }

    public byte[] getHostPublicKey() {
        return hostPublicKey;
    }

    public JschClient setHostPublicKey(byte[] hostPublicKey) {
        this.hostPublicKey = hostPublicKey;
        return this;
    }

    public int getPort() {
        return port;
    }

    public JschClient setPort(int port) {
        this.port = port;
        return this;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public JschClient setPassword(String password) {
        this.password = password;
        return this;
    }

    public byte[] getClientPrivateKey() {
        return clientPrivateKey;
    }

    public JschClient setClientPrivateKey(byte[] clientPrivateKey) {
        this.clientPrivateKey = clientPrivateKey;
        return this;
    }

    public byte[] getPassphrase() {
        return passphrase;
    }

    public JschClient setPassphrase(byte[] passphrase) {
        this.passphrase = passphrase;
        return this;
    }

    public int getSessionTimeout() {
        return sessionTimeout;
    }

    public JschClient setSessionTimeout(int sessionTimeout) {
        this.sessionTimeout = sessionTimeout;
        return this;
    }

    public int getAliveInterval() {
        return aliveInterval;
    }

    public JschClient setAliveInterval(int aliveInterval) {
        this.aliveInterval = aliveInterval;
        return this;
    }

    public int getAliveRetries() {
        return aliveRetries;
    }

    public JschClient setAliveRetries(int aliveRetries) {
        this.aliveRetries = aliveRetries;
        return this;
    }

//    public void sftpGet(String source, String destination) {
//        sftpGet(source,destination,null);
//    }
//
//    public void sftpGet(String source, String destination, SftpProgressMonitor monitor) {
//        sftpOperation(SftpOperation.GET, source, destination,monitor);
//    }
//
//    public void sftpPut(String source, String destination) {
//        sftpPut(source,destination,null);
//    }
//
//    public void sftpPut(String source, String destination, SftpProgressMonitor monitor) {
//        sftpOperation(SftpOperation.PUT, source, destination,monitor);
//    }


//    private void sftpOperation(SftpOperation operation, String source, String destination, SftpProgressMonitor monitor) {
//
//        executor.submit(() -> {
//            try {
//                Session session;
//
//                for (int i = 1; i <= connectionRetries; i++) {
//
//                    try {
//                        session = connectSession();
//                        try {
//                            Channel channel = session.openChannel("sftp");
//                            channel.connect(channelTimeout * 1000);
//                            ChannelSftp sftpChannel = (ChannelSftp) channel;
//                            if (operation.equals(SftpOperation.GET))
//                                sftpChannel.get(source, destination, monitor);
//                            else
//                                sftpChannel.put(source, destination, monitor);
//                        } finally {
//                            if (session != null && session.isConnected()) {
//                                session.disconnect();
//                            }
//                        }
//
//                        break;
//                    } catch (JSchException e) {
//                        if (i > connectionRetries) {
//                            throw new SshClientException("Failed to make an ssh connection: " + e.getMessage());
//                        }
//                        try {
//                            Thread.sleep(connectionRetryInterval);
//                        } catch (InterruptedException ie) {
//                            Thread.currentThread().interrupt();
//                        }
//                    }
//                }
//            } catch (SftpException | SshClientException e) {
//                return Optional.of(e);
//            }
//            return Optional.empty();
//        });
//
//
//    }

    private class SftpProgressMonitorPrint implements SftpProgressMonitor {
        long totalBytes = 0;

        public void init(int op, String src, String dest, long max) {
        }

        public boolean count(long count) {
            totalBytes += count;
            System.out.printf("\rCopying %s             ", Util.readableSizeBytes(totalBytes));
            return true;
        }

        public void end() {
            System.out.printf("\r");
        }
    }

    public SshCommand createCommand(String command) {
        return new SshCommand(command);
    }

    public SshCommand createCommand(String command, int timeout) {
        return new SshCommand(command, timeout);
    }


    public void awaitAllCommands() throws InterruptedException, ExecutionException, TimeoutException {
        for (SshCommand sshCommand : runningCommands) {
            sshCommand.await();
        }
    }

    public void cancelAllRunningCommands() throws InterruptedException, ExecutionException, TimeoutException {
        for (SshCommand sshCommand : runningCommands) {
            sshCommand.abort();
        }
        runningCommands.clear();
    }


    public void sshExec(SshCommand command, InputStream stdin, SshOutputProcessor outProcessor, SshOutputProcessor errProcessor) {
        Future future = executor.submit(new Command(command, stdin, outProcessor, errProcessor));
        command.setFuture(future);
    }

    public void sshExec(SshCommand command, InputStream stdin, SshOutputProcessor outProcessor, OutputStream stderr) {
        SshOutputProcessor errConsole = new SshOutputProcessor() {
            @Override
            public void dataReceived(byte[] data) {
                try {
                    stderr.write(data);
                } catch (IOException ignored) {}

            }
            @Override
            public void end() {

            }
        };
        Future future = executor.submit(new Command(command, stdin, outProcessor, errConsole));
        command.setFuture(future);
    }


    public void sshExec(SshCommand command, InputStream stdin, OutputStream stdout, OutputStream stderr) {
        SshOutputProcessor outConsole = new SshOutputProcessor() {
            @Override
            public void dataReceived(byte[] data) {
                try {
                    stdout.write(data);
                } catch (IOException ignored) {}
            }
            @Override
            public void end() {

            }
        };
        SshOutputProcessor errConsole = new SshOutputProcessor() {
            @Override
            public void dataReceived(byte[] data) {
                try {
                    stderr.write(data);
                } catch (IOException ignored) {}

            }
            @Override
            public void end() {

            }
        };
        sshExec(command, stdin, outConsole, errConsole);
    }

    private Session connectSession() throws JSchException {
        Session session;
        try {
            JSch jSch = new JSch();

            if (hostPublicKey != null)
                jSch.setHostKeyRepository(new SimpleHostKeyRepository(hostPublicKey));
            if (clientPrivateKey != null)
                jSch.addIdentity(username, clientPrivateKey, null, passphrase);

            session = jSch.getSession(username, host, port);
            session.setPassword(password);
            session.setConfig("StrictHostKeyChecking", hostPublicKey == null ? "no" : "yes");
            session.setConfig("PreferredAuthentications", "publickey,password");
            session.setServerAliveInterval(aliveInterval * 1000);
            session.setServerAliveCountMax(aliveRetries);
            session.connect(sessionTimeout * 1000);
        } catch (JSchException e) {
            throw e;
        }

        return session;
    }

    private enum SftpOperation {
        GET, PUT
    }

    public class Command implements CancellableTask<Optional<Exception>> {
        private Session session;
        private InputStream stdin;
        SshOutputProcessor stdout;
        SshOutputProcessor stderr;
        private boolean abort;
        SshCommand sshCommand;

        public Command(SshCommand sshCommand, InputStream stdin, SshOutputProcessor stdout, SshOutputProcessor stderr) {
            this.stdin = stdin;
            this.stdout = stdout;
            this.stderr = stderr;
            this.sshCommand = sshCommand;
        }

        @Override
        public Optional<Exception> call() throws Exception {
            final String threadName = Thread.currentThread().getName();
            Thread.currentThread().setName(threadName+"("+sshCommand.get()+")");
            runningCommands.add(sshCommand);
            for (int i = 1; i <= sshCommand.getConnectionRetries(); i++) {
                if (abort)
                    break;
                try {
                    session = connectSession();
                    logger.info("connected to server, executing " + sshCommand.get());
                    //command.setSession(session);
                    final ChannelExec channel = (ChannelExec) session.openChannel("exec");
                    channel.setPty(true);
                    channel.setCommand(sshCommand.get());
                    channel.setInputStream(stdin);

                    InputStream stdoutChannelStream = channel.getInputStream();
                    InputStream stderrChannelStream = channel.getErrStream();
                    channel.connect(sshCommand.getChannelTimeout() * 1000);

                    Runnable outStreamThread = () -> copy(stdoutChannelStream, stdout, "exception while reading ssh outstream ");
                    Runnable errStreamThread = () -> copy(stderrChannelStream, stderr, "exception while reading ssh errstream ");
                    //the abort command will disconnect the session and stop this threads
                    Future futureOut = executor.submit(outStreamThread);
                    Future futureErr = executor.submit(errStreamThread);
                    try {
                        if (sshCommand.getTimeout() > -1)
                            futureOut.get(sshCommand.getTimeout(), TimeUnit.MILLISECONDS);
                        else
                            futureOut.get();
                    } catch (ExecutionException e) {
                    }
                    try {
                        if (sshCommand.getTimeout() > -1)
                            futureErr.get(sshCommand.getTimeout(), TimeUnit.MILLISECONDS);
                        else
                            futureErr.get();
                    } catch (ExecutionException e) {
                    }
                    int exitStatus = channel.getExitStatus();
                    logger.info("command exit status " + exitStatus);
                    if (exitStatus != 0) {
                        throw new ProcessReturnStatusException(exitStatus);
                    }
                    break;//exit for

                } catch (JSchException e) {
                    if (i > sshCommand.getConnectionRetries()) {
                        throw new SshClientException("failed to make an ssh connection: " + e.getMessage());
                    }
                    try {
                        logger.info("cannot connect, will sleep " + (sshCommand.getConnectionRetryInterval() * 1000));
                        Thread.sleep(sshCommand.getConnectionRetryInterval() * 1000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (IOException | TimeoutException | ProcessReturnStatusException e) {
                    return Optional.of(e);
                } finally {
                    if (session != null && session.isConnected()) {
                        session.disconnect();
                    }
                }
            }
            Thread.currentThread().setName(threadName);
            runningCommands.remove(sshCommand);
            return Optional.empty();
        }

        @Override
        public synchronized void cancel() {
            this.abort = true;
            session.disconnect();
        }

        public RunnableFuture<Optional<Exception>> newTask() {
            return new FutureTask<Optional<Exception>>(this) {
                public boolean cancel(boolean mayInterruptIfRunning) {
                    try {
                        Command.this.cancel();
                    } finally {
                        return super.cancel(mayInterruptIfRunning);
                    }
                }
            };
        }

        private void copy(InputStream in, SshOutputProcessor outputProcessor, String exceptionMessage) {
            try {
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                int nRead;
                byte[] data = new byte[1024];
                while ((nRead = in.read(data, 0, data.length)) != -1) {
                    buffer.write(data, 0, nRead);
                    outputProcessor.dataReceived(buffer.toByteArray());
                    buffer.reset();
                }
                outputProcessor.end();
            } catch (IOException e) {
                logger.warn(exceptionMessage + e.getMessage());
            }
        }
    }

}

