package jobshop.solvers;

import jobshop.Instance;
import jobshop.encodings.ResourceOrder;
import jobshop.encodings.Schedule;
import jobshop.encodings.Task;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** An empty shell to implement a greedy solver. */
public class GreedySolver implements Solver {

    /** All possible priorities for the greedy solver. */
    public enum Priority {
        SPT, LPT, SRPT, LRPT, EST_SPT, EST_LPT, EST_SRPT, EST_LRPT
    }

    /** Priority that the solver should use. */
    final Priority priority;

    /** Creates a new greedy solver that will use the given priority. */
    public GreedySolver(Priority p) {
        this.priority = p;
    }

    @Override
    public Optional<Schedule> solve(Instance instance, long deadline) {
        ResourceOrder manualRO = new ResourceOrder(instance);
        List<Task> possibleTasks = new ArrayList<>();
        for (int job=0; job<instance.numJobs; job++) {
            for(int task = 0 ; task < instance.numTasks ; task++) {
                possibleTasks.add(new Task(job, task));
            }
        }

        /* ********************** SPT ************************ */
        if (this.priority == Priority.SPT) {
            while (!possibleTasks.isEmpty()) {
                List<Task> firstTasks = new ArrayList<>();
                for (int job=0; job<instance.numJobs; job++) {
                    for (int task=0; task<instance.numTasks; task++) {
                        if (possibleTasks.contains(new Task(job, task))) {
                            firstTasks.add(new Task(job, task));
                            break;
                        }
                    }
                }

                int smallDuration = instance.duration(firstTasks.get(0));
                int taskIndex = 0;
                for (Task tsk : firstTasks) {
                    if (smallDuration>instance.duration(tsk)) {
                        smallDuration = instance.duration(tsk);
                        taskIndex = firstTasks.indexOf(tsk);
                    }
                }
                manualRO.addTaskToMachine(instance.machine(possibleTasks.get(taskIndex)), possibleTasks.get(taskIndex));
                possibleTasks.remove(taskIndex);
            }
        }

        /* ********************** LRPT ************************ */
        if (this.priority == Priority.LRPT) {
            while (!possibleTasks.isEmpty()) {
                int [] jobsDuration = new int [instance.numJobs];
                for (Task tsk:possibleTasks) {
                    jobsDuration[tsk.job] += instance.duration(tsk);
                }

                int longDuration = jobsDuration[0];
                int jobIndex = 0;
                for (int job=1; job<jobsDuration.length; job++) {
                    if (longDuration<job) {
                        longDuration = jobsDuration[job];
                        jobIndex = job;
                    }
                }

                for (int tsk=0; tsk<instance.numTasks; tsk++){
                    if (possibleTasks.contains(new Task(jobIndex, tsk))) {
                        Task taskToRemove = new Task(jobIndex, tsk);
                        manualRO.addTaskToMachine(instance.machine(taskToRemove), taskToRemove);
                        possibleTasks.remove(taskToRemove);
                        break;
                    }
                }
            }
        }

        return manualRO.toSchedule() ;
    }
}
