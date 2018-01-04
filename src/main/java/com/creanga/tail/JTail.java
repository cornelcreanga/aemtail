package com.creanga.tail;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.creanga.jsch.JschClient;
import com.creanga.jsch.SshCommand;

import java.io.File;
import java.io.IOException;
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

    public static class AemTailParams {

        @Parameter(names = {"-i", "--inventory"}, description = "Inventory")
        private boolean inventory;


        @Parameter(names = {"-l", "--label"}, description = "Label")
        private String label;

        @Parameter(names = {"-h", "--host"}, description = "host")
        private String host;
        @Parameter(names = {"-p", "--password"}, description = "password")
        private String password;
        @Parameter(names = {"-k", "--key"}, description = "private key path")
        private String key;

        @Parameter(names = {"-u", "--user"}, description = "user")
        private String user;

        @Parameter(names = {"-e", "--excludeFile"}, description = "exclude file - errors produced by threads matching patterns declared into this file are ignored")
        private String exclude;


        @Parameter(names = {"-f", "--file"}, description = "file location")
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

        public boolean isInventory() {
            return inventory;
        }

        public void setInventory(boolean inventory) {
            this.inventory = inventory;
        }
    }


    public static void main(String[] args) throws InterruptedException, IOException, TimeoutException, ExecutionException {
        AemTailParams params = new AemTailParams();
        String privateKeyPath = null;
        JCommander jCommander = JCommander.newBuilder()
                .addObject(params)
                .programName("jtail")
                .acceptUnknownOptions(true)
                .build();
        try {
            jCommander.parse(args);
        } catch (ParameterException e) {
            System.out.println(e.getMessage());
            System.exit(1);
        }

        File labelFile = new File(System.getProperty("user.home") + "/jtail.labels");

        if (params.isInventory()) {
            List<String> lines = Files.readAllLines(labelFile.toPath(), Charset.forName("UTF-8"));
            for (String line : lines) {
                System.out.println(line);
            }
            System.exit(0);
        }

        if (params.getLabel() != null) {

            if (!labelFile.exists()) {
                if (!labelFile.createNewFile()) {
                    System.err.printf("Can't create %s\n", labelFile);
                    System.exit(1);
                }
            }
            //label,host,user,password,location
            List<String> lines = Files.readAllLines(labelFile.toPath(), Charset.forName("UTF-8"));
            boolean found = false;

            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                String[] items = line.split(",");
                if (items[0].equals(params.getLabel())) {
                    if (params.getHost() != null) {
                        items[1] = params.getHost();
                    } else {
                        params.setHost(toNull(items[1]));
                    }
                    if (params.getUser() != null) {
                        items[2] = params.getUser();
                    } else {
                        params.setUser(toNull(items[2]));
                    }
                    if (params.getPassword() != null) {
                        items[3] = params.getPassword();
                    } else {
                        params.setPassword(toNull(items[3]));
                    }
                    if (params.getLocation() != null) {
                        items[4] = params.getLocation();
                    } else {
                        params.setLocation(toNull(items[4]));
                    }
                    found = true;
                    lines.set(i, items[0] + "," + nvl(items[1]) + "," + nvl(items[2]) + "," + nvl(items[3]) + "," + nvl(items[4]));
                    break;
                }

            }
            if (!found)
                lines.add(params.getLabel() + "," + nvl(params.getHost()) + "," + nvl(params.getUser()) + "," + nvl(params.getPassword()) + "," + nvl(params.getLocation()));
            Files.write(labelFile.toPath(), lines, Charset.forName("UTF-8"));
        }

        if (params.getUser() == null) {
            String user = System.getProperty("user.name");
            System.out.printf("username is not specified, will use %s\n", user);
            params.setUser(user);
        }

        if (params.getPassword() == null) {
            privateKeyPath = System.getProperty("user.home") + "/.ssh/id_rsa";
            File file = new File(privateKeyPath);
            if (!file.exists()) {
                System.out.printf("password is not specified and the can't find the private key %s\n", privateKeyPath);
                System.exit(1);
            }
            System.out.printf("password is not specified, will use the private key from %s\n", privateKeyPath);
        }

        if (params.help) {
            jCommander.usage();
            System.exit(0);
        }
        List<String> toExclude = Collections.emptyList();
        if (params.getExclude() != null) {
            toExclude = Files.readAllLines(Paths.get(params.getExclude()), Charset.forName("UTF-8"));
        } else {
            File f = new File(System.getProperty("user.home"), "jtail.exclude");
            if (f.exists()) {
                System.out.printf("using %s for exclusion patterns\n", f.toPath());
                toExclude = Files.readAllLines(f.toPath(), Charset.forName("UTF-8"));
            }
        }
        LogMessageSshOutputProcessor logMessageSshOutputProcessor = new LogMessageSshOutputProcessor(new SimpleLineProcessor(toExclude, System.out));
        Runtime.getRuntime().addShutdownHook(new Thread(logMessageSshOutputProcessor::end));


        JschClient client = new JschClient(params.getHost(), params.getUser());//String host, String username,String password
        if (params.getPassword() != null) {
            client.setPassword(params.getPassword());
        } else {
            Path path = Paths.get(privateKeyPath);
            client.setClientPrivateKey(Files.readAllBytes(path));
        }

        SshCommand command = client.createCommand("tail -f " + params.getLocation());
        command.setConnectionRetryInterval(3);
        client.sshExec(command, System.in, logMessageSshOutputProcessor, System.err);
        //client.sshExec(command, System.in, System.out, System.err);
        Optional<Exception> exception = command.await();
        exception.ifPresent(e -> System.out.println(e.getMessage()));


    }

    static String nvl(String s) {
        return s == null ? "" : s;
    }

    static String toNull(String s) {
        if (s.trim().length() == 0)
            return null;
        return s;
    }
}
