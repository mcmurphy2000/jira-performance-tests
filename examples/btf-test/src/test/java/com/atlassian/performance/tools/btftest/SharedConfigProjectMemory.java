package com.atlassian.performance.tools.btftest;


import javax.annotation.concurrent.GuardedBy;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class SharedConfigProjectMemory {
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final String userName;
    private final String password;
    @GuardedBy("lock")
    private Long projectId;
    private final AtomicInteger createCount = new AtomicInteger(1);

    SharedConfigProjectMemory(final String userName, final String password) {
        this.userName = userName;
        this.password = password;
    }

    public Optional<Long> getProjectId() {
        try {
            if (lock.readLock().tryLock(3, TimeUnit.SECONDS)) {
                return Objects.isNull(projectId) ? Optional.empty() : Optional.of(projectId);
            }
        } catch (InterruptedException ignored) {
        } finally {
            lock.readLock().unlock();
        }
        return Optional.empty();
    }

    public void setProjectId(final Long projectId) {
        try {
            lock.writeLock().lock();
            this.projectId = projectId;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public int getAndIncrementCreateCount() {
        return createCount.getAndIncrement();
    }

    public String getUserName() {
        return userName;
    }

    public String getPassword() {
        return password;
    }
}
