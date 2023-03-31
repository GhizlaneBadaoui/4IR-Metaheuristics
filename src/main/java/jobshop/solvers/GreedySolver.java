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

    public ResourceOrder SPT (Instance instance) {
        ResourceOrder manualRO = new ResourceOrder(instance);
        List<Task> possibleTasks = new ArrayList<>();
        for (int job=0; job<instance.numJobs; job++) {
            for(int task = 0 ; task < instance.numTasks ; task++) {
                possibleTasks.add(new Task(job, task));
            }
        }
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

            manualRO.addTaskToMachine(instance.machine(firstTasks.get(taskIndex)), firstTasks.get(taskIndex));
            possibleTasks.remove(firstTasks.get(taskIndex));

        }
        return manualRO;
    }

    public ResourceOrder LRPT (Instance instance) {
        ResourceOrder manualRO = new ResourceOrder(instance);
        List<Task> possibleTasks = new ArrayList<>();
        for (int job=0; job<instance.numJobs; job++) {
            for(int task = 0 ; task < instance.numTasks ; task++) {
                possibleTasks.add(new Task(job, task));
            }
        }

        while (!possibleTasks.isEmpty()) {
            int [] jobsDuration = new int [instance.numJobs];
            for (Task tsk:possibleTasks) {
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
                if (possibleTasks.contains(new Task(jobIndex, tsk))) {
                    Task taskToRemove = new Task(jobIndex, tsk);
                    manualRO.addTaskToMachine(instance.machine(taskToRemove), taskToRemove);
                    possibleTasks.remove(taskToRemove);
                    break;
                }
            }
        }
        return manualRO;
    }


    public ResourceOrder EST_STP (Instance instance) {
        ResourceOrder manualRO = new ResourceOrder(instance);
        int[] jobs = new int[instance.numJobs];
        for (int i = 0; i < instance.numJobs; i++) {
            jobs[i] = 0;
        }

        int[] machines = new int[instance.numMachines];
        for (int i = 0; i < instance.numMachines; i++) {
            machines[i] = 0;
        }

        HashMap<Integer,Task> possibleTasks = new HashMap<>();
        for (int i = 0; i < instance.numJobs; i++) {
            possibleTasks.put(i, new Task(i, 0));
        }

        while (!possibleTasks.isEmpty()) {
            int[] est = new int[instance.numJobs];
            for (int i: possibleTasks.keySet()) {
                est[i] = Math.max(jobs[i], machines[instance.machine(possibleTasks.get(i))]);
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

            // STP
            int smallDuration = instance.duration(estTab.get(0)); // first task of estTab
            int taskIndex = 0;
            for (Task tsk : estTab) {
                if (smallDuration > instance.duration(tsk)) {
                    smallDuration = instance.duration(tsk);
                    taskIndex = estTab.indexOf(tsk);
                }
            }

            manualRO.addTaskToMachine(instance.machine(estTab.get(taskIndex)), estTab.get(taskIndex));
            possibleTasks.remove(estTab.get(taskIndex).job, estTab.get(taskIndex));

            // update job's value of tasks whom machine is the same of the task removed
            for (Task task : possibleTasks.values()) {
                if (instance.machine(task) == instance.machine(estTab.get(taskIndex))) {
                    jobs[task.job] = Math.max(instance.duration(estTab.get(taskIndex))+jobs[estTab.get(taskIndex).job], jobs[task.job]);
                }
            }

            // add next task of the job where a task has removed
            if (estTab.get(taskIndex).task + 1 < instance.numTasks) {
                possibleTasks.put(estTab.get(taskIndex).job, new Task(estTab.get(taskIndex).job, estTab.get(taskIndex).task+1));
            }

            // update machine's value and job's value in machines and jobs of the task removed
            jobs[estTab.get(taskIndex).job] += instance.duration(estTab.get(taskIndex));
            machines[instance.machine(estTab.get(taskIndex))] = Math.max(jobs[estTab.get(taskIndex).job], machines[instance.machine(estTab.get(taskIndex))] + instance.duration(estTab.get(taskIndex)));
        }
        return manualRO;
    }


    public ResourceOrder EST_LRPT (Instance instance) {
        ResourceOrder manualRO = new ResourceOrder(instance);

        int [] jobs = new int [instance.numJobs];
        for (int job=0; job<instance.numJobs; job++) {
            for(int task = 0 ; task < instance.numTasks ; task++) {
                jobs[job] += instance.duration(new Task(job, task));
            }
        }

        int[] machines = new int[instance.numMachines];
        for (int i = 0; i < instance.numMachines; i++) {
            machines[i] = 0;
        }

        HashMap<Integer,Task> possibleTasks = new HashMap<>();
        for (int i = 0; i < instance.numJobs; i++) {
            possibleTasks.put(i, new Task(i, 0));
        }

        while (!possibleTasks.isEmpty()) {
            int[] est = new int[instance.numJobs];
            for (int i: possibleTasks.keySet()) {
                est[i] = Math.max(jobs[i], machines[instance.machine(possibleTasks.get(i))]);
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

            // LRPT
            int remainingTime = jobs[0]; // first job of estTab
            int jobIndex = 0;
            for (int job=1; job<jobs.length; job++) {
                if (remainingTime<jobs[job]) {
                    remainingTime = jobs[job];
                    jobIndex = job;
                }
            }

            int index = 0; //index of task to remove
            for (Task tsk: possibleTasks.values()){
                if (tsk.job == jobIndex) {
                    index = tsk.task;
                    break;
                }
            }
            Task taskToRemove = new Task(jobIndex, index);

            manualRO.addTaskToMachine(instance.machine(taskToRemove), taskToRemove);
            possibleTasks.remove(jobIndex, taskToRemove);
//            System.out.println("remove :" + taskToRemove);

            // add next task of the job where a task has removed
            if (taskToRemove.task + 1 < instance.numTasks) {
                possibleTasks.put(jobIndex, new Task(jobIndex, taskToRemove.task+1));
            }

            // update machine's value and job's value in machines and jobs of the task removed
            jobs[jobIndex] -= instance.duration(taskToRemove);
            machines[instance.machine(taskToRemove)] += instance.duration(taskToRemove);
//            for (int machine : machines) System.out.print("  machines :" + machine);
//            for (int job : jobs) System.out.print("   jobs :" + job);
//            System.out.println("possl :" + possibleTasks.toString());
//            System.out.println("\n");
        }
        return manualRO;
    }

    @Override
    public Optional<Schedule> solve(Instance instance, long deadline) {

        ResourceOrder manualRO = new ResourceOrder(instance);

        if (this.priority == Priority.SPT) {
            manualRO = SPT(instance);
        }

        if (this.priority == Priority.LRPT) {
            manualRO = LRPT(instance);
        }

        if (this.priority == Priority.EST_SPT) {
            manualRO = EST_STP(instance);
        }

        if (this.priority == Priority.EST_LRPT) {
            manualRO = EST_LRPT(instance);
        }

        return manualRO.toSchedule() ;
    }
}
