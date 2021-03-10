package org.uma.jmetal.parallel.asynchronous.jppf;

import org.jppf.JPPFException;
import org.jppf.client.JPPFClient;


import org.uma.jmetal.parallel.asynchronous.task.ParallelTask;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.solution.Solution;
import org.uma.jmetal.parallel.asynchronous.algorithm.AsynchronousParallelAlgorithm;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;


public abstract class AbstractJPPFBasedNSGAII<T extends ParallelTask<S>, S extends Solution<?>>
        implements AsynchronousParallelAlgorithm<T,S> {

    protected Problem<S> problem;

    private BlockingQueue<T> completedTaskQueue;

    protected final JPPFClient jppfClient;
    private final JPPFJobManager<S> jobManager;

    public AbstractJPPFBasedNSGAII(Problem<S> problem) {
        this.completedTaskQueue = new LinkedBlockingQueue<>();

        this.problem = problem ;

        jppfClient = new JPPFClient();
        jobManager = new JPPFJobManager<>(jppfClient, problem, this);
    }


    @Override
    public void submitInitialTasks(List<T> tasks) {
        tasks.forEach(this::submitTask);
    }

    @Override
    public T waitForComputedTask() {
        T evaluatedTask = null;
        try {
            evaluatedTask = (T) completedTaskQueue.take();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return evaluatedTask;
    }

    @Override
    public abstract void processComputedTask(T task);

    @Override
    public void submitTask(T task) {
        try {
            jobManager.executeJob(task);
        } catch (JPPFException e) {
            e.printStackTrace();
        }
    }

    public void completedTaskToQueue(T task) {
        completedTaskQueue.add(task);
    }

    @Override
    public abstract T createNewTask();

    @Override
    public boolean thereAreInitialTasksPending(List<T> tasks) {
        return tasks.size() > 0;
    }

    @Override
    public T getInitialTask(List<T> tasks) {
        T initialTask = tasks.get(0);
        tasks.remove(0);
        return initialTask;
    }

    @Override
    public abstract boolean stoppingConditionIsNotMet();

    @Override
    public void run() {
        AsynchronousParallelAlgorithm.super.run();
    }
}