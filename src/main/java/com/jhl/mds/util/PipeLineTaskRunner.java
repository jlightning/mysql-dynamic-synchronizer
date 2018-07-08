package com.jhl.mds.util;

import java.util.function.Consumer;

public interface PipeLineTaskRunner<T, I, R> {

    void queue(T context, I input, Consumer<R> next, Consumer<Exception> errorHandler) throws Exception;

    interface SelfHandleThread {
    }
}
