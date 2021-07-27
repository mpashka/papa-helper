package org.mpashka.findme.services;


import io.reactivex.Observable;
import io.reactivex.subjects.BehaviorSubject;
import timber.log.Timber;

/**
 * Used to store some state
 */
public class MyState {
    private long started = System.currentTimeMillis();
    private int activity;
    private int accumActivity;
    private long lastCreateTime;
    private long lastTransmitTime;
    private long created;
    private long pending;
    private BehaviorSubject<Long> createdSubject = BehaviorSubject.create();
    private BehaviorSubject<Long> pendingSubject = BehaviorSubject.create();

    public long getStarted() {
        return started;
    }

    public void init(long created, long pending) {
        Timber.i("Init(%s, %s)", created, pending);
        this.created = created;
        this.pending = pending;
        createdSubject.onNext(this.created);
        pendingSubject.onNext(this.pending);
    }

    public int onCreateAndGetActivity() {
        created++;
        createdSubject.onNext(created);
        int lastAccumActivity = accumActivity;
        lastCreateTime = System.currentTimeMillis();
        accumActivity |= 1 << activity;
        return lastAccumActivity;
    }

    public void addActivity(int activity) {
        this.activity = activity;
        accumActivity |= 1 << activity;
    }

    public long getLastCreateTime() {
        return lastCreateTime;
    }

    public long getLastTransmitTime() {
        return lastTransmitTime;
    }

    public long getCreated() {
        return created;
    }

    public long getPending() {
        return pending;
    }

    public void addPending() {
        pending++;
    }

    public void onTransmit(long transmitted) {
        this.lastTransmitTime = System.currentTimeMillis();
        this.pending -= transmitted;
        if (pending < 0) {
            pending = 0;
        }
        pendingSubject.onNext(this.pending);
    }

    public Observable<Long> getCreatedSubject() {
        return createdSubject;
    }

    public Observable<Long> getPendingSubject() {
        return pendingSubject;
    }
}
