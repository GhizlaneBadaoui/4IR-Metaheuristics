package jobshop.solvers;

import jobshop.Instance;
import jobshop.encodings.ResourceOrder;
import jobshop.encodings.Schedule;
import jobshop.encodings.Task;

import java.util.*;

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

    public Task SPT (Instance instance, List<Task> possibleTasks) {
        int small = instance.duration(possibleTasks.get(0));
        Task taskToRemove = possibleTasks.get(0);
        for (Task tsk : possibleTasks) {
            if (small>instance.duration(tsk)) {
                small = instance.duration(tsk);
                taskToRemove = tsk;
            }
        }
        return taskToRemove;
    }

    public Task LRPT (Instance instance, List<Task> Tasks) {
        int [] jobsDuration = new int [instance.numJobs];
        for (Task tsk:Tasks) {
            jobsDuration[tsk.job] += instance.duration(tsk);
        }
        int longDuration = jobsDuration[0];
        int jobIndex = 0;
        for (int job=1; job<jobsDuration.length; job++) {
            if (longDuration<jobsDuration[job]) {
                longDuration = jobsDuration[job];
                jobIndex = job;
            }
        }
        for (int tsk=0; tsk<instance.numTasks; tsk++){
            if (Tasks.contains(new Task(jobIndex, tsk))) {
                return new Task(jobIndex, tsk);
            }
        }
        return null;
    }

    public List<Task> EST (Instance instance, int[] jobs, int[] machines, HashMap<Integer,Task> possibleTasks) {
        int[] est = new int[instance.numJobs];
        for (int i: possibleTasks.keySet()) {
            est[i] = Math.max(jobs[i], machines[instance.machine(possibleTasks.get(i))]);
            //System.out.println("**" + est[i]);
        }

        // tasks of the smallest est value in estTab
        int smallEst = Integer.MAX_VALUE;
        List<Task> estTab = new ArrayList<>();
        for (int i: possibleTasks.keySet()) {
            if (smallEst > est[i]) {
                smallEst = est[i];
                estTab.clear();
                estTab.add(possibleTasks.get(i));
            } else if (smallEst == est[i]) {
                estTab.add(possibleTasks.get(i));
            }
        }
        return estTab;
    }

    public ResourceOrder EST_STP (Instance instance, ResourceOrder order) {
        int[] jobs = new int[instance.numJobs];
        int[] machines = new int[instance.numMachines];

        HashMap<Integer,Task> possibleTasks = new HashMap<>();
        for (int i = 0; i < instance.numJobs; i++) {
            possibleTasks.put(i, new Task(i, 0));
        }

        while (!possibleTasks.isEmpty()) {
            Task taskToRemove = SPT(instance, EST(instance, jobs, machines, possibleTasks));

            order.addTaskToMachine(instance.machine(taskToRemove), taskToRemove);
            possibleTasks.remove(taskToRemove.job, taskToRemove);

            // update job's value of tasks whom machine is the same of the task removed
            for (Task task : possibleTasks.values()) {
                if (instance.machine(task) == instance.machine(taskToRemove)) {
                    jobs[task.job] = Math.max(instance.duration(taskToRemove)+jobs[taskToRemove.job], jobs[task.job]);
                }
            }

            // add next task of the job where a task has removed
            if (taskToRemove.task + 1 < instance.numTasks) {
                possibleTasks.put(taskToRemove.job, new Task(taskToRemove.job, taskToRemove.task+1));
            }

            // update machine's value and job's value in machines and jobs of the task removed
            jobs[taskToRemove.job] += instance.duration(taskToRemove);
            machines[instance.machine(taskToRemove)] = Math.max(jobs[taskToRemove.job], machines[instance.machine(taskToRemove)] + instance.duration(taskToRemove));
        }
        return order;
    }

    public ResourceOrder EST_LRPT (Instance instance, ResourceOrder order) {
        int [] jobs = new int [instance.numJobs];
        int [] machines = new int[instance.numMachines];

        HashMap<Integer,Task> possibleTasks = new HashMap<>();
        for (int i = 0; i < instance.numJobs; i++) {
            possibleTasks.put(i, new Task(i, 0));
        }

        while (!possibleTasks.isEmpty()) {
            List<Task> estTab = EST(instance, jobs, machines, possibleTasks);
            List<Task> tasks = new ArrayList<>();
            for (Task tsk : estTab) {
                tasks.add(possibleTasks.get(tsk.job));
            }
            Task taskToRemove = LRPT(instance, tasks);

            order.addTaskToMachine(instance.machine(taskToRemove), taskToRemove);
            possibleTasks.remove(taskToRemove.job, taskToRemove);

            // add next task of the job where a task has removed
            if (taskToRemove.task + 1 < instance.numTasks) {
                possibleTasks.put(taskToRemove.job, new Task(taskToRemove.job, taskToRemove.task+1));
            }

            // update machine's value and job's value in machines and jobs of the task removed
            jobs[taskToRemove.job] += instance.duration(taskToRemove);
            machines[instance.machine(taskToRemove)] += instance.duration(taskToRemove);
        }
        return order;
    }

    @Override
    public Optional<Schedule> solve(Instance instance, long deadline) {
        ResourceOrder order = new ResourceOrder(instance);

        /* ************* Heuristiques gloutonnes *************** */
        List<Task> possibleTasks = new ArrayList<>();
        switch (this.priority) {
            case SPT:
                for (int i = 0; i < instance.numJobs; i++) {
                    possibleTasks.add(new Task(i, 0));
                }
                while (!possibleTasks.isEmpty()) {
                    Task taskToRemove = SPT(instance, possibleTasks);
                    possibleTasks.remove(taskToRemove);
                    order.addTaskToMachine(instance.machine(taskToRemove), taskToRemove);
                    if (taskToRemove.task + 1 < instance.numTasks) {
                        possibleTasks.add(new Task(taskToRemove.job, taskToRemove.task + 1));
                    }
                }
                break;

            case LRPT:
                for (int job = 0; job < instance.numJobs; job++) {
                    for (int task = 0; task < instance.numTasks; task++) {
                        possibleTasks.add(new Task(job, task));
                    }
                }
                while (!possibleTasks.isEmpty()) {
                    Task taskToRemove = LRPT(instance, possibleTasks);
                    order.addTaskToMachine(instance.machine(taskToRemove), taskToRemove);
                    possibleTasks.remove(taskToRemove);
                }
                break;

            /* ************* Amélioration : EST *************** */
            case EST_SPT:
                order = EST_STP(instance, order);
                break;

            case EST_LRPT:
                order = EST_LRPT(instance, order);
                break;
        }
        return order.toSchedule() ;
    }
}
