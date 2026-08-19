package com.example.demo.store.application.port;

public interface ImageStoragePort {

    String upload(byte[] content, String contentType, String extension);
}
