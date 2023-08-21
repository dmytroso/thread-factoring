package com.epam.rd.autotasks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class ThreadUnionImpl implements ThreadUnion {

    private final String threadUnionName;
    private volatile boolean shutdownRequested;
    private final List<FinishedThreadResult> threadResults;
    private final AtomicInteger threadCounter;
    private final List<Thread> threadList;

    public ThreadUnionImpl(String threadUnionName) {
        super();
        this.threadUnionName = threadUnionName;
        this.threadResults = Collections.synchronizedList(new ArrayList<>());
        this.threadCounter = new AtomicInteger(0);
        this.threadList = Collections.synchronizedList(new ArrayList<>());
    }

    @Override
    public int totalSize() {
        return threadCounter.get();
    }

    @Override
    public int activeSize() {
        return (int) threadList.stream().filter(Thread::isAlive).count();
    }

    @Override
    public boolean isShutdown() {
        return shutdownRequested;
    }

    @Override
    public void shutdown() {
        for (Thread thread : threadList) {
            thread.interrupt();
        }
        shutdownRequested = true;
    }

    @Override
    public void awaitTermination() {
        for(Thread thread : threadList) {
            if (thread.isAlive()) {
                try {
                    thread.join();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.out.println("Error" + Thread.currentThread());
                }
            }
        }
    }

    @Override
    public boolean isFinished() {
        return shutdownRequested && activeSize() == 0;
    }

    @Override
    public List<FinishedThreadResult> results() {
        return new ArrayList<>(threadResults);
    }

    @Override
    public synchronized Thread newThread(Runnable r) {
        if (shutdownRequested) {
            throw new IllegalStateException("Cannot create new threads after shutdown");
        }
        Thread thread = new Thread(r) {
            public void run() {
                super.run();
                threadResults.add(new FinishedThreadResult(this.getName()));
            }
        };
        thread.setUncaughtExceptionHandler((threadProc, throwable) -> {
            FinishedThreadResult finishedThreadResult = new FinishedThreadResult(threadProc.getName(), throwable);
            threadResults.add(finishedThreadResult);
        });
        String threadName = threadUnionName + "-worker-" + threadCounter.getAndIncrement();
        thread.setName(threadName);
        threadList.add(thread);
        return thread;
    }
}
