import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
class FilterLock implements CustomLock {
    private int numThreads;
    private AtomicInteger[] level;
    private AtomicInteger[] victim;

    public FilterLock(int numThreads) {
        this.numThreads = numThreads;
        level = new AtomicInteger[numThreads];
        victim = new AtomicInteger[numThreads];
        for (int i = 0; i < numThreads; i++) {
            level[i] = new AtomicInteger();
            victim[i] = new AtomicInteger();
        }
    }
    @Override
    public void lock(int threadId) {
        for (int i = 1; i < numThreads; i++) {
            level[threadId].set(i);
            victim[i].set(threadId);
            for (int k = 0; k < numThreads; k++) {
                while ((k != threadId) && (level[k].get() >= i && victim[i].get() == threadId)){
                    //busy wait
                }
            }
        }
    }
    @Override
    public void unlock(int threadId) {
        level[threadId].set(0);
    }
}




class GeneralizedPetersonLock {
    private int numThreads;
    private AtomicBoolean[] flags;
    private AtomicInteger victim;

    public GeneralizedPetersonLock(int numThreads) {
        this.numThreads = numThreads;
        flags = new AtomicBoolean[numThreads];
        victim = new AtomicInteger();
        for (int i = 0; i < numThreads; i++) {
            flags[i] = new AtomicBoolean();
            
        }
    }

    public void enter(int threadId) {
        int me = threadId;
        flags[me].set(true);
        victim.set(me);
        for (int k = 0; k < numThreads; k++) {
            while (k != me && flags[k].get() == true && victim.get() == me) {
                //busy wait
            }
        }
    }

    public void exit(int threadId) {
        flags[threadId].set(false);
    }
}


class FilterLock2 implements CustomLock {
    private int numThreads;
    private GeneralizedPetersonLock[] locks;

    public FilterLock2(int numThreads) {
        this.numThreads = numThreads;
        locks = new GeneralizedPetersonLock[numThreads - 1]; 
        for (int i = 0; i < numThreads - 1; i++) {
            locks[i] = new GeneralizedPetersonLock(numThreads);
        }
    }

    @Override
    public void lock(int threadId) {
        for(int i = 0 ; i < numThreads - 1; i++){
            locks[i].enter(threadId);
        }
    }

    @Override
    public void unlock(int threadId) {
        for (int i = numThreads - 2; i >= 0; i--) {
            locks[i].exit(threadId);
        }
    }
}


 class BakeryLock implements CustomLock {
    private AtomicBoolean[] flag;
    private AtomicInteger[] label;
    private int numThreads;

    public BakeryLock(int numThreads) {
        this.numThreads = numThreads;
        flag = new AtomicBoolean[numThreads];
        label = new AtomicInteger[numThreads];
        for (int i = 0; i < numThreads; i++) {
            flag[i] = new AtomicBoolean();
            label[i] = new AtomicInteger();
        }
    }

    @Override
    public void lock(int threadId) {
        flag[threadId].set(true);
        label[threadId].set(getMaxTicketNumber() + 1);
        for (int other = 0; other < numThreads; other++) {
            while (other !=threadId &&  flag[other].get() == true && (label[other].get() < label[threadId].get() || (label[other].get() == label[threadId].get() && other < threadId))) {
                // Wait while other threads with smaller ticket numbers or with same ticket but higher thread IDs are in the critical section
            }
        }
    }

    @Override
    public void unlock(int threadId) {
        flag[threadId].set(false); // Release the lock
    }

    private int getMaxTicketNumber() {
        int max = Integer.MIN_VALUE;
        for (AtomicInteger ticketNumber : label) {
            max = Math.max(max, ticketNumber.get());
        }
        return max;
    }
}


class TournamentTree implements CustomLock {
    private int numThreads;
    private BakeryLock[] locks;

    public TournamentTree(int numThreads){
        this.numThreads = numThreads;
        // int numLocks = 2*numThreads+1;
        locks = new BakeryLock[numThreads];
        for (int i = 0; i < numThreads; i++) {
            locks[i] = new BakeryLock(numThreads);
        }
    }

    // public static int getNumLocksNeeded(int index, int MaxN) {
    //     if (index > MaxN) {
    //         return 0;
    //     }
    //     if (index == MaxN) {
    //         return 1;
    //     }
    
    //     int locks = 2; // At least two locks for the current node and its left child
    //     if (2 * index + 1 <= MaxN) {
    //         locks += getNumLocksNeeded(2 * index, MaxN);
    //         locks += getNumLocksNeeded(2 * index + 1, MaxN);
    //     } else if (2 * index <= MaxN) {
    //         locks += getNumLocksNeeded(2 * index, MaxN);
    //     }
    //     return locks;
    // }

    @Override
    public void lock(int threadId) {
        int index = threadId;
        while(index != 0){
            locks[index].lock(threadId);
            index = index/2;
        }
        locks[0].lock(threadId);
    }

    @Override
    public void unlock(int threadId) {
        int index = threadId;
        while(index != 0){
            locks[index].unlock(threadId);
            index = index/2;
        }
        locks[0].unlock(threadId);
    }
}


public class Main {
    public static final int COUNTER_LIMIT = 10000;

    static class ThreadParam {
        CustomLock lock;
        int[] counter;
        int tid;

        ThreadParam(CustomLock lock, int[] counter, int tid) {
            this.lock = lock;
            this.counter = counter;
            this.tid = tid;
        }
    }

    static class Worker implements Runnable {
        ThreadParam params;

        Worker(ThreadParam params) {
            this.params = params;
        }

        @Override
        public void run() {
            for (int i = 0; i < COUNTER_LIMIT; i++) {
                params.lock.lock(params.tid);
                try {
                    params.counter[0]++; // shared non - atomic counter
                } finally {
                    params.lock.unlock(params.tid);
                }
            }
        }
    }

    static void simulateForNThreads(CustomLock lock, int numThreads, int numIterations, String locktype) throws InterruptedException {
        double elapsedTime = 0;
        double throughput = 0;

        for (int iteration = 0; iteration < numIterations; iteration++) {
            int[] counter = new int[1];

            long startTime = System.nanoTime();
            Thread[] threads = new Thread[numThreads];

            for (int i = 0; i < numThreads; i++) {
                ThreadParam param = new ThreadParam(lock, counter, i);
                threads[i] = new Thread(new Worker(param));
                threads[i].start();
            }

            for (int i = 0; i < numThreads; i++) {
                threads[i].join();
            }

            long endTime = System.nanoTime();
            double elapsedSeconds = (endTime - startTime) / 1e9;
            elapsedTime += elapsedSeconds;

            double iterationsPerSecond = (numThreads * COUNTER_LIMIT) / elapsedSeconds;
            throughput += iterationsPerSecond;

            if (counter[0] != numThreads * COUNTER_LIMIT) {
                throw new IllegalStateException("Mutual exclusion failed");
            }
        }
        System.out.printf("%s,%d,%.2f,%.6f\n",locktype,numThreads,throughput / numIterations, elapsedTime / numIterations);
    }

    public static void main(String[] args) throws InterruptedException {
        int numThreads = Integer.parseInt(args[0]);
        int numIterations = Integer.parseInt(args[1]);
        System.out.printf("locktype,numThreads,Throughput,ElapsedTime(s)\n");
        for (int i = 1; i <= numThreads ;i++){
            CustomLock lock = new FilterLock(i); 
            simulateForNThreads(lock, i, numIterations,"FilterLock");
        }
        for (int i = 1; i <= numThreads ;i++){
            CustomLock lock = new FilterLock2(i); 
            simulateForNThreads(lock, i, numIterations,"FilterLockWithGPL");
        }
        for (int i = 1; i <= numThreads ;i++){
            CustomLock lock = new BakeryLock(i); 
            simulateForNThreads(lock, i, numIterations,"BakeryLock");
        }
        for (int i = 1; i <= numThreads ;i++){
            CustomLock lock = new TournamentTree(i); 
            simulateForNThreads(lock, i, numIterations,"TournamentTree");
        }
        
    }
}


