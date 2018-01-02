package com.creanga.tail;

import com.creanga.jsch.SshOutputProcessor;

public class LogMessageSshOutputProcessor implements SshOutputProcessor {

    LineProcessor lineProcessor;

    StringBuilder buffer = new StringBuilder();

    public LogMessageSshOutputProcessor(LineProcessor lineProcessor) {
        this.lineProcessor = lineProcessor;
    }

    @Override
    public void dataReceived(byte[] data) {
        buffer.append(new String(data));
        String lines[] = buffer.toString().split("\\R", -1);
        if (lines.length==1)
            return;
        String lastLine = lines[lines.length-1];
        for (int i = 0; i < lines.length-1; i++) {
            String line = lines[i];
            lineProcessor.lineReceived(line);
        }
        buffer.delete(0,buffer.length()-lastLine.length());


    }

    @Override
    public void end() {
        lineProcessor.end();
    }
}
