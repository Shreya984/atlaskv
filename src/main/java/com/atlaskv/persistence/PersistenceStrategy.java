package com.atlaskv.persistence;

import java.io.IOException;

public interface PersistenceStrategy {

    void appendPut(String key, String value) throws IOException;

    void appendDelete(String key) throws IOException;
}