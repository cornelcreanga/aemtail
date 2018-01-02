package com.creanga.tail;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.creanga.jsch.JschClient;
import com.creanga.jsch.SshCommand;

import java.io.*;

public class JTail {

    public static class AemTailParams{
        @Parameter(names = {"-l", "--label"}, description = "Label")
        private String label;

        @Parameter(names = {"-h", "--host"}, description = "host", required = true)
        private String host;
        @Parameter(names = {"-p", "--password"}, description = "password", required = true)
        private String password;
        @Parameter(names = {"-u", "--user"}, description = "user", required = true)
        private String user;

        @Parameter(names = {"-f", "--folder"}, description = "folder location")
        private String location;


        @Parameter(names = "--help", help = true)
        public boolean help = false;

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getUser() {
            return user;
        }

        public void setUser(String user) {
            this.user = user;
        }

        public String getLocation() {
            return location;
        }

        public void setLocation(String location) {
            this.location = location;
        }
    }


    public static void main(String[] args) throws InterruptedException, IOException {

        LogMessageSshOutputProcessor logMessageSshOutputProcessor = new LogMessageSshOutputProcessor(new LogLineProcessor());
        Runtime.getRuntime().addShutdownHook(new Thread(logMessageSshOutputProcessor::end));

        AemTailParams params = new AemTailParams();
        JCommander jCommander = JCommander.newBuilder()
                .addObject(params)
                .programName("jtail")
                .acceptUnknownOptions(true)
                .build();
        try {
            jCommander.parse(args);
        }catch (ParameterException e){
            System.out.println(e.getMessage());
            System.exit(1);
        }
        if (params.help) {
            jCommander.usage();
            System.exit(0);
        }
        JschClient client = new JschClient(params.getHost(),params.getUser(),params.getPassword());//String host, String username,String password
        SshCommand command = client.createCommand("tail -f /mnt/crx/author/crx-quickstart/logs/error.log");
        client.sshExec(command, System.in, logMessageSshOutputProcessor, System.err);

        while(true);

    }


}
