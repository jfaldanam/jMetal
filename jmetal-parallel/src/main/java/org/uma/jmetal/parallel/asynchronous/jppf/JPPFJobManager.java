package org.uma.jmetal.parallel.asynchronous.jppf;

import org.jppf.JPPFException;
import org.jppf.client.JPPFClient;
import org.jppf.client.JPPFJob;
import org.jppf.client.event.JobEvent;
import org.jppf.client.event.JobListenerAdapter;
import org.jppf.node.protocol.Task;
import org.uma.jmetal.parallel.asynchronous.task.ParallelTask;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.solution.Solution;

import java.util.List;

public class JPPFJobManager<S extends Solution<?>> {
    private final JPPFClient jppfClient;
    private Problem problem;
    private AbstractJPPFBasedNSGAII algorithm;

    public JPPFJobManager(JPPFClient jppfClient, Problem problem, AbstractJPPFBasedNSGAII algorithm) {
        this.jppfClient = jppfClient;
        this.problem = problem;
        this.algorithm = algorithm;
    }

    private JPPFJob createJob(final String jobName, ParallelTask taskToExecute) throws JPPFException {
        final JPPFJob job = new JPPFJob();

        job.setName(jobName);

        // add a task to the job.
        final org.jppf.node.protocol.Task<?> task =
                job.add(new JPPFTaskWrapper<S>(problem, taskToExecute));
        task.setId(jobName + " - Task");

        return job;
    }

    public void executeJob(ParallelTask task) throws JPPFException {
        final JPPFJob job = createJob("Template job", task);
        // https://www.jppf.org/doc/6.0/index.php?title=Submitting_multiple_jobs_concurrently#Fully_asynchronous_processing
        job.addJobListener(
                new JobListenerAdapter() {
                    @Override
                    public void jobEnded(JobEvent event) {
                        List<Task<?>> results = event.getJob().getAllResults();
                        for (org.jppf.node.protocol.Task t : results)
                            algorithm.completedTaskToQueue((ParallelTask) t.getResult());
                    }
                });
        jppfClient.submitAsync(job);
    }
}