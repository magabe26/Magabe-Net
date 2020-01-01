package com.magabe.net;


import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;

public class MTcpThreadPool extends MTcpSocketHelper {

    private volatile boolean mRunRunnable = false;
    private volatile boolean mAllThreadsTerminatedChecked;
    private LinkedBlockingQueue<Runnable> mRunRunnableQueue = new LinkedBlockingQueue<>();
    private ArrayList<MThread> mStartedThreads = new ArrayList<>();
    private volatile int mActiveThreadCount = 0;
    private int mLastThreadId = 0;
    private String mThreadPoolName;
    private int mMinThreadCount;
    private int mMaxThreadCount;
    private ThreadsStatusCallbacks threadsStatusCallbacks = null;

    public MTcpThreadPool() {
        setMinThreadCount();
        setMaxThreadCount(mMinThreadCount);
    }

    public MTcpThreadPool(int maxThreadCount) {
        setMinThreadCount();
        setMaxThreadCount(maxThreadCount);
    }

    public String getThreadPoolName() {
        return mThreadPoolName;
    }

    public void setThreadPoolName(String name) {
        mThreadPoolName = name;
    }

    private void setMinThreadCount() {
        int processors = Runtime.getRuntime().availableProcessors();
        if (processors <= 0) processors = 1;//just in case
        mMinThreadCount = processors;
    }

    public void setMaxThreadCount(int maxThreadCount) {
        if (maxThreadCount < mMinThreadCount) {
            mMaxThreadCount = mMinThreadCount;
        } else {
            mMaxThreadCount = maxThreadCount;
        }
    }

    public MTcpThreadPool setThreadsStatusCallbacks(ThreadsStatusCallbacks threadsStatusCallbacks) {
        this.threadsStatusCallbacks = threadsStatusCallbacks;
        return this;
    }

    private synchronized boolean maxThreadCountExceeded() {
        return (getStartedThreadCount() > getMaxThreadCount());
    }

    public void startThreads() {
        if (mRunRunnable) return;
        mRunRunnable = true;
        mAllThreadsTerminatedChecked = false;
        initInitialThreads();
    }

    private void initInitialThreads() {
        for (int count = 0; count < mMinThreadCount; count++) {
            addExtraThread();
        }
    }


    private void addExtraThread() {
        new MThread().start();
    }

    private boolean hasIdealThreads() {
        return (getActiveThreadCount() < getStartedThreadCount());
    }

    public void stopThreads() {
        mRunRunnableQueue.clear();
        if (hasActiveThreads()) {
            terminateBlockedThreads();
        }
    }

    private synchronized void terminateBlockedThreads() {
        mRunRunnable = false;
        for (MThread thread : mStartedThreads) {
            MTcpSocket socket = thread.getSocket();
            if (socket != null) {
                forceCloseSocket(socket);
            } else {
                thread.interrupt();
            }
        }
        mStartedThreads.clear();//terminate threads with runnable that use loops
    }

    public void post(Runnable runnable) throws NullPointerException {
        if (runnable == null)
            throw new NullPointerException("param runnable is null");
        try {
            mRunRunnableQueue.put(runnable);
            if (!mRunRunnableQueue.isEmpty() && !hasIdealThreads()) {
                addExtraThread();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();//not gonna happen
        }
    }

    /**
     * Return the number of active threads in the thread pool.
     * Note: It is possible for this function to return a value that is greater than getMaxThreadCount()
     *
     * @return
     */
    public synchronized int getActiveThreadCount() {
        return mActiveThreadCount;
    }

    /**
     * Return the maximum number of threads used by the thread pool.
     * Note: The thread pool will always use at least 2 thread,when created using a void constructor
     *
     * @return
     */
    public synchronized int getMaxThreadCount() {
        return mMaxThreadCount;
    }

    private synchronized void incrementActiveThreadCount() {
        mActiveThreadCount++;
    }

    private synchronized void decrementActiveThreadCount() {
        mActiveThreadCount--;
    }

    private synchronized int getStartedThreadCount() {
        return mStartedThreads.size();
    }

    public synchronized boolean hasActiveThreads(){
       return (getStartedThreadCount() > 0);
    }

    private synchronized void addStartedThread(MThread thread) {
        mStartedThreads.add(thread);
    }

    private synchronized void removeStartedThread(MThread thread) {
        mStartedThreads.remove(thread);
    }

    /**
     * Class MThread
     */
    private class MThread extends Thread {
        private int mId;
        private MTcpSocket socket;

        MThread() {
            mId = ++mLastThreadId;
        }

        @Override
        public void start() {
            super.start();
            addStartedThread(this);
            if (threadsStatusCallbacks != null) {
                threadsStatusCallbacks.onThreadStarted(getThreadId());
            }
           /* System.out.println("NEW THREAD STARTED:: MTcpThreadPool id = " + mThreadPoolId + ", Thread with id = "
                    + mId + " started" + " , Total created thread = " + getStartedThreadCount());*/
        }

        public int getThreadId() {
            return mId;
        }

        public MTcpSocket getSocket() {
            return socket;
        }

        @Override
        public void run() {

            while (mRunRunnable) {
                try {

                    Runnable runnable = mRunRunnableQueue.take();

                    if (runnable instanceof MTcpInputStreamHandler) {
                        socket = ((MTcpInputStreamHandler) runnable).getSocket();
                    }

                    incrementActiveThreadCount();

                    /*System.out.println("RUNNING NEW RUNNABLE::ThreadPoolName =" + getThreadPoolName() + ", ActiveThreadCount = " + getActiveThreadCount() +
                            " , Total created thread = " + getStartedThreadCount()
                            + ", mMinThreadCount = " + mMinThreadCount
                            + ", mMaxThreadCount = " + getMaxThreadCount()
                            + " , hasIdealThreads = " + hasIdealThreads()
                            + ", mRunRunnableQueue empty = " + mRunRunnableQueue.isEmpty());*/

                    runnable.run();

                    /*-----------finished running a runnable---------*/
                    socket = null;
                    decrementActiveThreadCount();

                    if ((mRunRunnableQueue.isEmpty() || hasIdealThreads())
                            && maxThreadCountExceeded()) {
                        break; //terminate thread
                    }
                  /*-----------------------------------------------*/

                } catch (InterruptedException e) {
                    // e.printStackTrace();
                }
            }

            aboutToTerminate();
        }

        private void aboutToTerminate() {

            removeStartedThread(this);

            if (threadsStatusCallbacks != null) {
                threadsStatusCallbacks.onThreadStopped(getThreadId());
            }
           /* System.out.println("ThreadPoolName =" + getThreadPoolName() + " THREAD TERMINATED:: Thread with id = " + mId
                    + " terminated, remaing threads = " + getStartedThreadCount());*/

            if (!mAllThreadsTerminatedChecked) {
                if (getStartedThreadCount() == 0 && !mRunRunnable) {
                    mAllThreadsTerminatedChecked = true;
                    if (threadsStatusCallbacks != null) {
                        threadsStatusCallbacks.onAllThreadsStopped();
                    }
                 // System.out.println("ThreadPoolName =" + getThreadPoolName() + " ALL THREAD TERMINATED ");
                }
            }
        }
    }

    public interface ThreadsStatusCallbacks {
        void onThreadStarted(int threadId);

        void onThreadStopped(int threadId);

        void onAllThreadsStopped();
    }

}

















