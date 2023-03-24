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

//    public ResourceOrder EST_SPT(Instance instance) {
//        ResourceOrder manualRO = new ResourceOrder(instance);
//
//        List<Integer> finishTime_machines = new ArrayList<>();
//        List<Integer> jobs = new ArrayList<>();
//
//        for (int job=0; job<instance.numJobs; job++) {
//            jobs.add(0);
//        }
//        for (int machine=0; machine<instance.numMachines; machine++) {
//            finishTime_machines.add(0);
//        }
//
//        List<Task> possibleTasks = new ArrayList<>();
//        for (int job=0; job<instance.numJobs; job++) {
//            for(int task = 0 ; task < instance.numTasks ; task++) {
//                possibleTasks.add(new Task(job, task));
//            }
//        }
//        while (!possibleTasks.isEmpty()) {
//            List<Task> firstTasks = new ArrayList<>();
//
//            for (int job = 0; job < instance.numJobs; job++) {
//                for (int task = 0; task < instance.numTasks; task++) {
//                    if (possibleTasks.contains(new Task(job, task))) {
//                        firstTasks.add(new Task(job, task));
//                        break;
//                    }
//                }
//            }
//
//            int[] max = new int[instance.numJobs];
//            for(int job=0; job<instance.numJobs; job++) {
//                for(int machine=0; machine< instance.numTasks; machine++) {
//                    if (jobs.get(job) > finishTime_machines.get(job))
//                        //
//                }
//
//            }
//
//        }
//
//
//        return manualRO;
//    }

    @Override
    public Optional<Schedule> solve(Instance instance, long deadline) {

        ResourceOrder manualRO = new ResourceOrder(instance);

        /* ********************** 3.1 Premières heuristiques : SPT ************************ */
        if (this.priority == Priority.SPT) {
            manualRO = SPT(instance);
        }

        /* ********************** 3.1 Premières heuristiques : LRPT ************************ */
        if (this.priority == Priority.LRPT) {
            manualRO = LRPT(instance);
        }

        /* ********************** 3.2 amélioration : EST-SPT ************************ */
        if (this.priority == Priority.EST_SPT) {
            int [] jobs = new int[instance.numJobs];
            for (int i=0; i< instance.numJobs; i++) {
                jobs[i] = 0;
            }

            int [] machines = new int[instance.numMachines];
            for (int i=0; i< instance.numMachines; i++) {
                machines[i] = 0;
            }

            List<Task> possibleTasks = new ArrayList<>();
            for (int i=0; i< instance.numJobs; i++) {
                possibleTasks.add(new Task(i, 0));
            }

            while (!possibleTasks.isEmpty()) {
                int[] est = new int[instance.numJobs];
                for (int i = 0; i < instance.numJobs; i++) {
                    //System.out.println("machiiiiiine : "+instance.machine(possibleTasks.get(i)) + " -------- " + possibleTasks.get(i).toString());

                    est[i] = Math.max(jobs[possibleTasks.get(i).job], machines[instance.machine(possibleTasks.get(i))]);
                }

                int smallEst = est[0];
                List<Task> estTab = new ArrayList<>();
                for (int i=0; i<est.length; i++) {
                    if (smallEst > est[i]) {
                        smallEst = est[i];
                        estTab.clear();
                        estTab.add(possibleTasks.get(i));
                    } else if (smallEst == est[i]) {
                        estTab.add(possibleTasks.get(i));
                    }
                }


                int smallDuration = instance.duration(estTab.get(0));
                int taskIndex = 0;
                for (Task tsk : estTab) {
                    if (smallDuration > instance.duration(tsk)) {
                        smallDuration = instance.duration(tsk);
                        taskIndex = estTab.indexOf(tsk);
                    }
                }

                manualRO.addTaskToMachine(instance.machine(estTab.get(taskIndex)), estTab.get(taskIndex));
                possibleTasks.remove(possibleTasks.get(taskIndex));

                for (Task task : possibleTasks) {
                    if (instance.machine(task) == instance.machine(estTab.get(taskIndex))) {
                        jobs[instance.machine(estTab.get(taskIndex))] += instance.duration(estTab.get(taskIndex));
                    }
                }

                System.out.println("psb old :" + possibleTasks);
                if (estTab.get(taskIndex).task+1 <= instance.numTasks) {
                    possibleTasks.add(new Task(estTab.get(taskIndex).job, estTab.get(taskIndex).task+1));
                }
                System.out.println("psb new :" + possibleTasks);

                //jobs[estTab.get(taskIndex).job] += instance.duration(estTab.get(taskIndex));
                machines[instance.machine(estTab.get(taskIndex))] += instance.duration(estTab.get(taskIndex));


///
                for (int job=0; job<instance.numJobs; job++) {

                    System.out.print("jb new :" + jobs[job]+ " --");
                }

                for (int job=0; job<instance.numMachines; job++) {

                    System.out.print("mch new :" + machines[job]+ " --");
                }
            }


//            List<Task> possibleTasks = new ArrayList<>();
//
//            for (int job=0; job<instance.numJobs; job++) {
//                for(int task=0 ; task<instance.numTasks ; task++) {
//                    possibleTasks.add(new Task(job, task));
//                }
//            }
//
//            while (!possibleTasks.isEmpty()) {
//                List<Task> availableTasks = new ArrayList<>();
//
//                for (int job=0; job<instance.numJobs; job++) {
//                    for (int task=0; task<instance.numTasks; task++) {
//                        if (possibleTasks.contains(new Task(job, task))) {
//                            availableTasks.add(new Task(job, task));
//                            break;
//                        }
//                    }
//                }
//
//               Map<Task, Integer> estMap = new HashMap<>();
//               for (Task task : availableTasks) {
//                   estMap.put(task, EST_SPT(task, availableTasks));
//               }
//
//               Task earliestTask = null;
//               int earliestStartTime = Integer.MAX_VALUE;
//               for (Task task : availableTasks) {
//                   int startTime = estMap.get(task);
//                   if (startTime < earliestStartTime) {
//                       if (isMachineAvailable(instance, instance.machine(task), startTime, availableTasks)) {
//                           earliestTask = task;
//                           earliestStartTime = startTime;
//                       }
//                   }
//               }
//
//               manualRO.addTaskToMachine(instance.machine(earliestTask), earliestTask);
//               possibleTasks.remove(earliestTask);
//            }
        }
        return manualRO.toSchedule() ;
    }

    private static int EST_SPT(Task task, List<Task> scheduledTasks) {
        int est = 0;

        return est;
    }


    private static boolean isMachineAvailable(Instance instance, int machine, int time, List<Task> scheduledTasks) {
        for (Task task : scheduledTasks) {
            if (instance.machine(task)==machine) {
                return false;
            }
        }
        return true;
    }
}
