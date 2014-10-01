package com.couchbase.lite.support;

import com.couchbase.lite.Database;
import com.couchbase.lite.LiteTestCase;
import com.couchbase.lite.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class BatcherTest extends LiteTestCase {

    /**
     * Submit 101 objects to batcher, and make sure that batch
     * of first 100 are processed "immediately" (as opposed to being
     * subjected to a delay which would add latency)
     *
     * Disabled because this is failing on Jenkins.  Needs investigation.
     * https://github.com/couchbase/couchbase-lite-android/issues/388
     *
     */
    public void disabledTestBatcherLatencyInitialBatch() throws Exception {

        final CountDownLatch doneSignal = new CountDownLatch(1);

        ScheduledExecutorService workExecutor = new ScheduledThreadPoolExecutor(1);

        int inboxCapacity = 100;
        int processorDelay = 500;

        final AtomicLong timeProcessed = new AtomicLong();

        Batcher batcher = new Batcher<String>(workExecutor, inboxCapacity, processorDelay, new BatchProcessor<String>() {

            @Override
            public void process(List<String> itemsToProcess) {
                Log.v(Database.TAG, "process called with: " + itemsToProcess);

                timeProcessed.set(System.currentTimeMillis());

                doneSignal.countDown();
            }

        });

        ArrayList<String> objectsToQueue = new ArrayList<String>();
        for (int i=0; i < inboxCapacity + 1; i++) {
            objectsToQueue.add(Integer.toString(i));
        }

        long timeQueued = System.currentTimeMillis();
        batcher.queueObjects(objectsToQueue);


        boolean didNotTimeOut = doneSignal.await(35, TimeUnit.SECONDS);
        assertTrue(didNotTimeOut);

        long delta = timeProcessed.get() - timeQueued;
        assertTrue(delta >= 0);

        // we want the delta between the time it was queued until the
        // time it was processed to be as small as possible.  since
        // there is some overhead, rather than using a hardcoded number
        // express it as a ratio of the processor delay, asserting
        // that the entire processor delay never kicked in.
        int acceptableDelta = processorDelay - 1;

        Log.v(Log.TAG, "delta: %d", delta);

        assertTrue(delta < acceptableDelta);


    }

    /**
     * Set batch processing delay to 500 ms, and every second, add a new item
     * to the batcher queue.  Make sure that each item is processed immediately.
     */
    public void testBatcherLatencyTrickleIn() throws Exception {

        final CountDownLatch doneSignal = new CountDownLatch(10);

        ScheduledExecutorService workExecutor = new ScheduledThreadPoolExecutor(1);

        int inboxCapacity = 100;
        int processorDelay = 500;

        final AtomicLong maxObservedDelta = new AtomicLong(-1);

        Batcher batcher = new Batcher<Long>(workExecutor, inboxCapacity, processorDelay, new BatchProcessor<Long>() {

            @Override
            public void process(List<Long> itemsToProcess) {

                if (itemsToProcess.size() != 1) {
                    throw new RuntimeException("Unexpected itemsToProcess");
                }

                Long timeSubmitted = itemsToProcess.get(0);
                long delta = System.currentTimeMillis() - timeSubmitted.longValue();
                if (delta > maxObservedDelta.get()) {
                    maxObservedDelta.set(delta);
                }

                doneSignal.countDown();

            }

        });


        ArrayList<Long> objectsToQueue = new ArrayList<Long>();
        for (int i=0; i < 10; i++) {
            batcher.queueObjects(Arrays.asList(System.currentTimeMillis()));
            Thread.sleep(1000);
        }

        boolean didNotTimeOut = doneSignal.await(35, TimeUnit.SECONDS);
        assertTrue(didNotTimeOut);

        Log.v(Log.TAG, "maxDelta: %d", maxObservedDelta.get());

        // we want the max observed delta between the time it was queued until the
        // time it was processed to be as small as possible.  since
        // there is some overhead, rather than using a hardcoded number
        // express it as a ratio of 1/4th the processor delay, asserting
        // that the entire processor delay never kicked in.
        int acceptableMaxDelta = processorDelay -1;

        Log.v(Log.TAG, "maxObservedDelta: %d", maxObservedDelta.get());

        assertTrue((maxObservedDelta.get() < acceptableMaxDelta));

    }

    /**
     * Add 100 items in a batcher and make sure that the processor
     * is correctly called back with the first batch.
     *
     */
    public void testBatcherSingleBatch() throws Exception {

        final CountDownLatch doneSignal = new CountDownLatch(10);

        ScheduledExecutorService workExecutor = new ScheduledThreadPoolExecutor(1);

        int inboxCapacity = 10;
        int processorDelay = 1000;

        Batcher batcher = new Batcher<String>(workExecutor, inboxCapacity, processorDelay, new BatchProcessor<String>() {

            @Override
            public void process(List<String> itemsToProcess) {
                Log.v(Database.TAG, "process called with: " + itemsToProcess);

                assertEquals(10, itemsToProcess.size());

                assertNumbersConsecutive(itemsToProcess);

                doneSignal.countDown();
            }

        });

        ArrayList<String> objectsToQueue = new ArrayList<String>();
        for (int i=0; i<inboxCapacity * 10; i++) {
            objectsToQueue.add(Integer.toString(i));
        }
        batcher.queueObjects(objectsToQueue);

        boolean didNotTimeOut = doneSignal.await(35, TimeUnit.SECONDS);
        assertTrue(didNotTimeOut);

    }

    /**
     * With a batcher that has an inbox of size 10, add 100 items in batches
     * of 5.  Make sure that the processor is called back with all 100 items.
     * Also make sure that they appear in the correct order within a batch.
     */
    public void testBatcherBatchSize5() throws Exception {


        ScheduledExecutorService workExecutor = new ScheduledThreadPoolExecutor(1);

        int inboxCapacity = 10;
        int numItemsToSubmit = inboxCapacity * 10;
        final int processorDelay = 0;

        final CountDownLatch doneSignal = new CountDownLatch(numItemsToSubmit);

        Batcher batcher = new Batcher<String>(workExecutor, inboxCapacity, processorDelay, new BatchProcessor<String>() {

            @Override
            public void process(List<String> itemsToProcess) {
                Log.v(Database.TAG, "process called with: " + itemsToProcess);

                assertNumbersConsecutive(itemsToProcess);

                for (String item : itemsToProcess) {
                    doneSignal.countDown();
                }

                Log.v(Database.TAG, "doneSignal: " + doneSignal.getCount());

            }

        });

        ArrayList<String> objectsToQueue = new ArrayList<String>();
        for (int i=0; i<numItemsToSubmit; i++) {
            objectsToQueue.add(Integer.toString(i));
            if (objectsToQueue.size() == 5) {
                batcher.queueObjects(objectsToQueue);
                objectsToQueue = new ArrayList<String>();
            }

        }

        boolean didNotTimeOut = doneSignal.await(35, TimeUnit.SECONDS);
        assertTrue(didNotTimeOut);

    }

    /**
     * Reproduce issue:
     * https://github.com/couchbase/couchbase-lite-java-core/issues/283
     *
     * This sporadically fails on the genymotion emulator and Nexus 5 device.
     */
    public void testBatcherThreadSafe() throws Exception {

        // 10 threads using the same batcher

        // each thread queues a bunch of items and makes sure they were all processed

        ScheduledExecutorService workExecutor = new ScheduledThreadPoolExecutor(1);
        int inboxCapacity = 10;
        final int processorDelay = 1000;

        int numThreads = 5;
        final int numItemsPerThread = 200;
        int numItemsTotal = numThreads * numItemsPerThread;
        final AtomicInteger numItemsProcessed = new AtomicInteger(0);

        final CountDownLatch allItemsProcessed = new CountDownLatch(numItemsTotal);

        final Batcher batcher = new Batcher<String>(workExecutor, inboxCapacity, processorDelay, new BatchProcessor<String>() {

            @Override
            public void process(List<String> itemsToProcess) {
                for (String item : itemsToProcess) {
                    int curVal = numItemsProcessed.incrementAndGet();
                    Log.d(Log.TAG, "%d items processed so far", curVal);
                    try {
                        Thread.sleep(5);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    allItemsProcessed.countDown();
                }
            }

        });


        for (int i=0; i<numThreads; i++) {
            final String iStr = Integer.toString(i);
            Runnable runnable = new Runnable() {
                @Override
                public void run() {

                    for (int j=0; j<numItemsPerThread; j++) {
                        try {
                            Thread.sleep(5);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        String item = String.format("%s-item:%d", iStr, j);
                        batcher.queueObject(item);
                    }
                }
            };
            new Thread(runnable).start();


        }

        Log.d(TAG, "waiting for allItemsProcessed");
        boolean success = allItemsProcessed.await(120, TimeUnit.SECONDS);
        assertTrue(success);
        Log.d(TAG, "/waiting for allItemsProcessed");

        assertEquals(numItemsTotal, numItemsProcessed.get());
        assertEquals(0, batcher.count());

        Log.d(TAG, "waiting for pending futures");
        batcher.waitForPendingFutures();
        Log.d(TAG, "/waiting for pending futures");


    }


    private static void assertNumbersConsecutive(List<String> itemsToProcess) {
        int previousItemNumber = -1;
        for (String itemString : itemsToProcess) {
            if (previousItemNumber == -1) {
                previousItemNumber = Integer.parseInt(itemString);
            } else {
                int curItemNumber = Integer.parseInt(itemString);
                assertTrue(curItemNumber == previousItemNumber + 1);
                previousItemNumber = curItemNumber;
            }
        }
    }

}
