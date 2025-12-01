package com.dream11.flocker.engine.modules.source;

public interface Source<T> {

    T read() throws Exception;
}

