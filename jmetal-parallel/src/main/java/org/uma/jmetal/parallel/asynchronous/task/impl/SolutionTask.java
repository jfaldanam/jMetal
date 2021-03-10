package org.uma.jmetal.parallel.asynchronous.task.impl;

import org.uma.jmetal.parallel.asynchronous.task.ParallelTask;
import org.uma.jmetal.solution.Solution;

/**
 * Task representing a solution to be evaluated by a problem
 *
 * @param <S>
 */
public class SolutionTask<S extends Solution<?>> implements ParallelTask<S> {
    private final S solution ;
    private final long taskIdentifier ;

    public SolutionTask(int taskIdentifier, S solution) {
        this.solution = solution ;
        this.taskIdentifier = taskIdentifier ;
    }

    @Override
    public S getContents() {
        return solution;
    }

    @Override
    public long getIdentifier() {
        return taskIdentifier;
    }
}