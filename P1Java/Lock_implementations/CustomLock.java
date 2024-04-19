public interface CustomLock {
    void lock(int threadId);
    void unlock(int threadId);
}
