package com.creanga.tail;

public class LogLineProcessor implements LineProcessor{

    private StringBuilder message = new StringBuilder();

    @Override
    public void lineRecieved(String line) {
        if (line.contains("*ERROR*") || line.contains("*WARN*") || line.contains("*INFO*")) {
            if (message.length() > 0) {
                ErrorMessage errorMessage = parseError(message.toString());
                System.out.println(errorMessage);
                boolean ignore = false;

                message.setLength(0);
            }
            message.append(line).append(lineSeparator);
        } else {
            message.append(line).append(lineSeparator);
        }
    }

    @Override
    public void end() {
        ErrorMessage errorMessage = parseError(message.toString());
        System.out.println(errorMessage);
    }

    static String lineSeparator = System.getProperty("line.separator");

    private static ErrorMessage parseError(String s) {
        ErrorMessage errorMessage = new ErrorMessage();
        int i1 = s.indexOf("*");
        int i2 = s.indexOf("*", i1+1);
        errorMessage.date = s.substring(0, i1).trim();
        errorMessage.level = Level.valueOf(s.substring(i1 + 1, i2));
        i1 = s.indexOf("[", i1+1);
        i2 = s.indexOf("]", i1+1);
        errorMessage.thread = s.substring(i1 + 1, i2);
        errorMessage.body = s.substring(i2+1).trim();
        return errorMessage;
    }

    public enum Level {
        INFO, ERROR, WARN,DEBUG,FATAL,TRACE;
    }

    static class ErrorMessage {
        String date;
        Level level;
        String thread;
        String body;

        @Override
        public String toString() {
            return date+" *"+level+"* ["+thread+"] "+body;
        }
    }
}
