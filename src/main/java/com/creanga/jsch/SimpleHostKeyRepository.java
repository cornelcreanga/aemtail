package com.creanga.jsch;

import com.jcraft.jsch.HostKey;
import com.jcraft.jsch.HostKeyRepository;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.UserInfo;

public class SimpleHostKeyRepository implements HostKeyRepository {
    String key;


    public SimpleHostKeyRepository(final byte[] key) {
        this.key = new String(key);
    }

    @Override
    public HostKey[] getHostKey(final String host, final String type) {
        return getHostKey();
    }

    @Override
    public HostKey[] getHostKey() {
        HostKey hostKey = null;

        try {
            hostKey = new HostKey("unused", this.key.getBytes());
        } catch (final JSchException e) {
            //todo
        }

        return new HostKey[]{hostKey};
    }
    
    public int check(final String host, final byte[] key) {
        String incomingKey = null;

        try {
            incomingKey = new HostKey(host, key).getKey();
        } catch (final JSchException e) {
            return NOT_INCLUDED;
        }

        if (this.key.compareTo(incomingKey) == 0) {
            return OK;
        }

        return NOT_INCLUDED;
    }
    
    public String getKnownHostsRepositoryID() {
        return null;
    }
    
    public void remove(final String host, final String type, final byte[] key) {
    }
    
    public void remove(final String host, final String type) {
    }
    
    public void add(final HostKey hostkey, final UserInfo ui) {
    }
}
