package org.uma.jmetal.parallel.asynchronous.jppf;

import org.jppf.node.protocol.AbstractTask;
import org.uma.jmetal.parallel.asynchronous.task.ParallelTask;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.solution.Solution;

/**
 * Class to wrap a {@link ParallelTask} to be accepted as a {@link org.jppf.node.protocol.AbstractTask}.
 *
 * @author Jose Francisco Aldana Martin <jfaldanam@gmail.com>
 */
public class JPPFTaskWrapper<S extends Solution<?>> extends AbstractTask<ParallelTask> {
    private static final long serialVersionUID = 1L;
    private ParallelTask task;
    private Problem<S> problem;

    /** Perform initializations on the client side, before the task is executed by the node. */
    public JPPFTaskWrapper(Problem problem, ParallelTask task) {
        this.problem = problem;
        this.task = task;
    }

    /**
     * This method contains the code that will be executed by a node. Any uncaught {@link Throwable
     * Throwable} will be stored in the task via a call to {@link
     * org.jppf.node.protocol.Task#setThrowable(Throwable) Task.setThrowable(Throwable)}.
     */
    @Override
    public void run() {
        problem.evaluate((S) task.getContents());

        setResult(this.task);
    }
}
