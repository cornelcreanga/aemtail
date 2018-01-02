package com.creanga.tail;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.creanga.jsch.JschClient;
import com.creanga.jsch.SshCommand;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public class JTail {

    public static class AemTailParams{
        @Parameter(names = {"-l", "--label"}, description = "Label")
        private String label;

        @Parameter(names = {"-h", "--host"}, description = "host", required = true)
        private String host;
        @Parameter(names = {"-p", "--password"}, description = "password")
        private String password;
        @Parameter(names = {"-k", "--key"}, description = "private key path")
        private String key;

        @Parameter(names = {"-u", "--user"}, description = "user")
        private String user;

        @Parameter(names = {"-e", "--excludeFile"}, description = "exclude file - errors produced by threads matching patterns declared into this file are ignored")
        private String exclude;


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

        public String getExclude() {
            return exclude;
        }

        public void setExclude(String exclude) {
            this.exclude = exclude;
        }
    }


    public static void main(String[] args) throws InterruptedException, IOException, TimeoutException, ExecutionException {
        //todo -add connected message
        AemTailParams params = new AemTailParams();
        String privateKeyPath = null;
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

        if (params.getUser()==null){
            String user = System.getProperty("user.name");
            System.out.printf("username is not specified, will use %s\n",user);
            params.setUser(user);
        }

        if (params.getPassword()==null){
            privateKeyPath = System.getProperty("user.home")+"/.ssh/id_rsa";
            File file = new File(privateKeyPath);
            if (!file.exists()){
                System.out.printf("password is not specified and the can't find the private key %s\n",privateKeyPath);
                System.exit(1);
            }
            System.out.printf("password is not specified, will use the private key from %s\n",privateKeyPath);
        }

        if (params.help) {
            jCommander.usage();
            System.exit(0);
        }
        List<String> toExclude = Collections.emptyList();
        if (params.getExclude()!=null){
            toExclude = Files.readAllLines(Paths.get(params.getExclude()), Charset.forName("UTF-8"));
        }else{
            File f = new File(System.getProperty("user.home"),"jtail.exclude");
            if (f.exists()) {
                System.out.printf("using %s for exclusion patterns\n",f.toPath());
                toExclude = Files.readAllLines(f.toPath(), Charset.forName("UTF-8"));
            }
        }
        LogMessageSshOutputProcessor logMessageSshOutputProcessor = new LogMessageSshOutputProcessor(new LogLineProcessor(toExclude,System.out));
        Runtime.getRuntime().addShutdownHook(new Thread(logMessageSshOutputProcessor::end));


        JschClient client = new JschClient(params.getHost(),params.getUser());//String host, String username,String password
        if (params.getPassword()!=null){
            client.setPassword(params.getPassword());
        }else{
            Path path = Paths.get(privateKeyPath);
            client.setClientPrivateKey(Files.readAllBytes(path));
        }

        SshCommand command = client.createCommand("tail -f /mnt/crx/author/crx-quickstart/logs/error.log");
        command.setConnectionRetryInterval(3);
        client.sshExec(command, System.in, logMessageSshOutputProcessor, System.err);
        Optional<Exception> exception = command.await();
        exception.ifPresent(e -> System.out.println(e.getMessage()));


    }


}
