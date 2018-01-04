package com.creanga.tail;

import java.io.PrintStream;
import java.util.Collections;
import java.util.List;

public class SimpleLineProcessor implements LineProcessor {

    private PrintStream out;
    private List<String> toExclude = Collections.emptyList();

    public SimpleLineProcessor(PrintStream out) {
        this.out = out;
    }

    public SimpleLineProcessor(List<String> toExclude, PrintStream out) {
        this.toExclude = toExclude;
        this.out = out;
    }

    @Override
    public void lineReceived(String line) {
        boolean ignore = false;
        for (String aFilterOut : toExclude) {
            if (line.contains(aFilterOut)) {
                ignore = true;
                break;
            }
        }
        if (!ignore)
            out.println(line);
    }

    @Override
    public void end() {

    }
}
