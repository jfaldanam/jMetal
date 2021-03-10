package org.uma.jmetal.parallel.asynchronous.jppf;

import org.jppf.JPPFException;
import org.jppf.client.JPPFClient;
import org.uma.jmetal.parallel.asynchronous.algorithm.AsynchronousParallelAlgorithm;
import org.uma.jmetal.parallel.asynchronous.task.ParallelTask;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.solution.Solution;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public abstract class AbstractJPPFBasedNSGAII<S extends Solution<?>>
        implements AsynchronousParallelAlgorithm<ParallelTask<S>, List<S>> {

    protected Problem<S> problem;

    private BlockingQueue<ParallelTask<S>> completedTaskQueue;

    protected final JPPFClient jppfClient;
    private final JPPFJobManager<S> jobManager;

    public AbstractJPPFBasedNSGAII(Problem<S> problem) {
        this.completedTaskQueue = new LinkedBlockingQueue<>();

        this.problem = problem;

        jppfClient = new JPPFClient();
        jobManager = new JPPFJobManager<>(jppfClient, problem, this);
    }

    @Override
    public void submitInitialTasks(List<ParallelTask<S>> tasks) {
        tasks.forEach(this::submitTask);
    }

    @Override
    public ParallelTask<S> waitForComputedTask() {
        ParallelTask<S> evaluatedTask = null;
        try {
            evaluatedTask = completedTaskQueue.take();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return evaluatedTask;
    }

    @Override
    public abstract void processComputedTask(ParallelTask<S> task);

    @Override
    public void submitTask(ParallelTask<S> task) {
        try {
            jobManager.executeJob(task);
        } catch (JPPFException e) {
            e.printStackTrace();
        }
    }

    public void completedTaskToQueue(ParallelTask<S> task) {
        completedTaskQueue.add(task);
    }

    @Override
    public abstract ParallelTask<S> createNewTask();

    @Override
    public boolean thereAreInitialTasksPending(List<ParallelTask<S>> tasks) {
        return tasks.size() > 0;
    }

    @Override
    public ParallelTask<S> getInitialTask(List<ParallelTask<S>> tasks) {
        ParallelTask<S> initialTask = tasks.get(0);
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
