package com.cgi.nm.radius.poc;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;
import org.quartz.SchedulerConfigException;
import org.quartz.spi.ThreadPool;

/**
 * FixedThreadPool
 */
public class FixedThreadPool implements ThreadPool, ThreadFactory, UncaughtExceptionHandler {

    /** Log4j logger for this class. */
    private static final Logger logger = Logger.getLogger(FixedThreadPool.class);

    // Constants
    private static final int DEFAULT_POOL_SIZE = 32;

    // ThreadPool
    private ThreadPoolExecutor pool;
    private int poolSize = DEFAULT_POOL_SIZE;
    private ThreadGroup group = new ThreadGroup("group2");
    private AtomicInteger count = new AtomicInteger(0);
    

    @Override
    public boolean runInThread(Runnable pRunnable) {

        if (pool.getActiveCount() == poolSize)
            return false;

        final StringBuffer lResult = new StringBuffer();
        pool.setRejectedExecutionHandler(new RejectedExecutionHandler() {

            @Override
            public void rejectedExecution(Runnable pRunnable, ThreadPoolExecutor pExecutor) {

                logger.error("Rejected execution");
                lResult.append("REJECTED");
            }
        });

        pool.execute(pRunnable);
        return (lResult.length() == 0);
    }

    @Override
    public void initialize() throws SchedulerConfigException {

        pool = new ThreadPoolExecutor(poolSize, poolSize, 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(), this);
    }

    @Override
    public int blockForAvailableThreads() {

        while (true) {
            int lFreeCount = (poolSize - pool.getActiveCount());
            if (lFreeCount > 0)
                return lFreeCount;

            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                // Do nothing
            }
        }
    }

    @Override
    public void shutdown(boolean pWaitForJobsToComplete) {

        if (pWaitForJobsToComplete)
            pool.shutdown();
        else
            pool.shutdownNow();
    }

    /**
     * Set pool size.
     * 
     * @param pSize new size.
     */
    public void setPoolSize(int pSize) {

        poolSize = pSize;
    }

	@Override
    public int getPoolSize() {

        return poolSize;
    }

    @Override
    public void uncaughtException(Thread pThread, Throwable pError) {

        logger.error("Uncaught exception in thread '" + pThread.getName() + "'", pError);
    }

    @Override
    public Thread newThread(Runnable pRunnable) {

        Thread lThread = new Thread(group, pRunnable, group.getName() + count.incrementAndGet());
        lThread.setDaemon(false);
        lThread.setUncaughtExceptionHandler(this);
        lThread.setPriority(Thread.NORM_PRIORITY);
        return lThread;
    }
    

	@Override
	public void setInstanceId(String schedInstId) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setInstanceName(String schedName) {
		// TODO Auto-generated method stub
	}
}
