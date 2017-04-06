package org.janelia.jacs2.asyncservice.common;


import org.apache.commons.javaflow.api.Continuation;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

class ServiceComputationTask<T> implements Runnable {

    @FunctionalInterface
    interface ContinuationSupplier<T> {
        T get();
    }

    static class ComputeResult<T> {
        final T result;
        final Throwable exc;

        public ComputeResult(T result, Throwable exc) {
            this.result = result;
            this.exc = exc;
        }
    }

    private static class Stack<E> {
        private static class Node <E> {
            public final E item;
            public Node<E> next;

            public Node(E item) {
                this.item = item;
            }
        }

        private AtomicReference<Node<E>> head = new AtomicReference<>();

        void push(E item) {
            Node<E> newHead = new Node<>(item);
            Node<E> oldHead;
            do {
                oldHead = head.get();
                newHead.next = oldHead;
            } while (!head.compareAndSet(oldHead, newHead));
        }

        E top() {
            Node<E> headContent = head.get();
            if (headContent == null) {
                return null;
            } else {
                return headContent.item;
            }
        }

        E pop() {
            Node<E> oldHead;
            Node<E> newHead;
            do {
                oldHead = head.get();
                if (oldHead == null) {
                    return null;
                }
                newHead = oldHead.next;
            } while (!head.compareAndSet(oldHead, newHead));
            return oldHead.item;
        }
    }

    private final CountDownLatch done = new CountDownLatch(1);
    private final Stack<ServiceComputation<?>> depStack = new Stack<>();
    private ContinuationSupplier<T> resultSupplier;
    private ComputeResult<T> result;
    private volatile boolean suspended;
    private volatile boolean canceled;
    private Continuation cont;
    private ContinuationCond contCond;

    ServiceComputationTask(ServiceComputation<?> dep) {
        push(dep);
    }

    ServiceComputationTask(ServiceComputation<?> dep, T result) {
        this(dep);
        complete(result);
    }

    ServiceComputationTask(ServiceComputation<?> dep, Throwable exc) {
        this(dep);
        completeExceptionally(exc);
    }

    @Override
    public void run() {
        exec();
    }

    void push(ServiceComputation<?> dep) {
        if (dep != null) depStack.push(dep);
    }

    void setResultSupplier(ContinuationSupplier<T> resultSupplier) {
        this.resultSupplier = resultSupplier;
    }

    void exec() {
        if (isDone()) {
            return;
        }
        if (cont != null) {
            System.out.println("!!!!!!!!!!! RESUME 1 " + cont);
            cont = cont.resume();
            return;
        }
        for (;;) {
            if (isReady()) {
                if (resultSupplier != null) {
                    try {
                        complete(resultSupplier.get());
                    } catch (SuspendedException e) {
                        if (cont == null && e.getCont() != null) {
                            cont = e.getCont();
                            contCond = e.getContCond();
                        }
                        return;
                    } catch (Exception e) {
                        completeExceptionally(e);
                    }
                    if (cont != null) {
                        System.out.println("!!!!!!!!!!! RESUME 2 " + cont);
                        cont = cont.resume();
                    }
                    return;
                } else {
                    throw new IllegalStateException("No result supplier has been provided");
                }
            } else if (isSuspended()) {
                return;
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                return;
            }
        }
    }

    boolean isReady() {
        if (isCanceled()) {
            return true;
        }
        if (contCond != null && !contCond.checkCond()) {
            return false;
        }
        for (ServiceComputation<?> dep = depStack.top(); ;) {
            if (dep == null) {
                return true;
            }
            if (dep.isDone()) {
                // the current dependency completed successfully - go to the next one
                depStack.pop();
                dep = depStack.top();
            } else {
                return false;
            }
        }
    }

    ComputeResult<T> get() {
        try {
            done.await();
        } catch (InterruptedException e) {
            throw new CompletionException(e);
        }
        return result;
    }

    boolean isDone() {
        return canceled || result != null;
    }

    boolean isCompletedExceptionally() {
        return isDone() && result.exc != null;
    }

    void complete(T result) {
        this.result = new ComputeResult<>(result, null);
        done.countDown();
        this.resume();
    }

    void completeExceptionally(Throwable exc) {
        this.result = new ComputeResult<>(null, exc);
        done.countDown();
        this.resume();
    }

    boolean isSuspended() {
        return suspended;
    }

    void suspend() {
        suspended = true;
    }

    void resume() {
        suspended = false;
    }

    boolean cancel() {
        if (isDone()) {
            return false;
        } else {
            completeExceptionally(new CancellationException());
            canceled = true;
            return true;
        }
    }

    boolean isCanceled() {
        return canceled;
    }
}
