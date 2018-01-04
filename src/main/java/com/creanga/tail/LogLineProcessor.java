package com.creanga.tail;

import java.io.PrintStream;
import java.util.Collections;
import java.util.List;

public class LogLineProcessor implements LineProcessor {

    private StringBuilder message = new StringBuilder();
    private PrintStream out;
    private List<String> toExclude = Collections.emptyList();

    public LogLineProcessor(PrintStream out) {
        this.out = out;
    }

    public LogLineProcessor(List<String> toExclude, PrintStream out) {
        this.toExclude = toExclude;
        this.out = out;
    }

    @Override
    public void lineReceived(String line) {
        if (line.contains("*ERROR*") || line.contains("*WARN*") || line.contains("*INFO*") || line.contains("*DEBUG*")) {
            if (message.length() > 0) {
                try {
                    parseAndPrint();
                } catch (Exception e) {
                    out.println(message.toString());
                }

                message.setLength(0);
            }
            message.append(line).append(lineSeparator);
        } else {
            message.append(line).append(lineSeparator);
        }
    }

    @Override
    public void end() {
        try {
            parseAndPrint();
        } catch (Exception e) {
            out.println(message.toString());
        }

    }

    private void parseAndPrint() {
        ErrorMessage errorMessage = parseError(message.toString());
        boolean ignore = false;
        for (String aFilterOut : toExclude) {
            if (errorMessage.thread.contains(aFilterOut)) {
                ignore = true;
                break;
            }
        }
        if (!ignore)
            out.println(errorMessage);
    }

    static String lineSeparator = System.getProperty("line.separator");

    private static ErrorMessage parseError(String s) {
        ErrorMessage errorMessage = new ErrorMessage();
        int i1 = s.indexOf("*");
        int i2 = s.indexOf("*", i1 + 1);
        errorMessage.date = s.substring(0, i1).trim();
        errorMessage.level = Level.valueOf(s.substring(i1 + 1, i2));
        i1 = s.indexOf("[", i1 + 1);
        i2 = s.indexOf("]", i1 + 1);
        errorMessage.thread = s.substring(i1 + 1, i2);
        errorMessage.body = s.substring(i2 + 1).trim();
        return errorMessage;
    }

    public enum Level {
        INFO, ERROR, WARN, DEBUG, FATAL, TRACE;
    }

    static class ErrorMessage {
        String date;
        Level level;
        String thread;
        String body;

        @Override
        public String toString() {
            return date + " *" + level + "* [" + thread + "] " + body;
        }
    }
}
