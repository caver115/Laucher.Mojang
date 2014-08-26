package com.mojang.launcher.updater;

import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ExceptionalThreadPoolExecutor extends ThreadPoolExecutor {

    private static final Logger LOGGER = LogManager.getLogger();

    public ExceptionalThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, new LinkedBlockingQueue());
    }

    protected void afterExecute(Runnable r, Throwable t) {
        super.afterExecute(r, t);
        if (t == null && r instanceof Future) {
            try {
                Future ie = (Future) r;
                if (ie.isDone()) {
                    ie.get();
                }
            } catch (CancellationException var4) {
                ;
            } catch (ExecutionException var5) {
                t = var5.getCause();
            } catch (InterruptedException var6) {
                Thread.currentThread().interrupt();
            }
        }

    }

    protected <T extends Object> RunnableFuture<T> newTaskFor(Runnable runnable, T value) {
        return new ExceptionalThreadPoolExecutor.ExceptionalFutureTask(runnable, value);
    }

    protected <T extends Object> RunnableFuture<T> newTaskFor(Callable<T> callable) {
        return new ExceptionalThreadPoolExecutor.ExceptionalFutureTask(callable);
    }

    public class ExceptionalFutureTask extends FutureTask {

        public ExceptionalFutureTask(Callable callable) {
            super(callable);
        }

        public ExceptionalFutureTask(Runnable runnable, Object result) {
            super(runnable, result);
        }

        protected void done() {
            try {
                this.get();
            } catch (Throwable var2) {
                ExceptionalThreadPoolExecutor.LOGGER.error("Unhandled exception in executor " + this, var2);
            }

        }
    }
}
