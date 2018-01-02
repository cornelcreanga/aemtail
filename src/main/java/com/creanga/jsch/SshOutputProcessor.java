package com.creanga.jsch;

public interface SshOutputProcessor {

    void dataReceived(byte[] data);

    void end();

}
