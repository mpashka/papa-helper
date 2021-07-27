package org.mpashka.findme.services;

public interface MyListenableServiceInterface {
    String[] getPermissions();
    void startListen();
    void stopListen();
}
