package com.creanga.tail;


public interface LineProcessor {
    void lineReceived(String line);

    void end();

}
