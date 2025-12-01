package com.dream11.flocker.engine.modules.sink;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;

public interface Sink<T> {

    void write(T data) throws Exception;

    default void writeDataset(Dataset<Row> dataset) throws Exception {
        throw new UnsupportedOperationException("writeDataset not supported by this sink implementation");
    }

    default void flush() throws Exception {}

}
