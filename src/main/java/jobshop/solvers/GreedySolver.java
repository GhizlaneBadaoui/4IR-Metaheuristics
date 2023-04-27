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

    /** Renvoie la tâche la plus courte parmi les tâches de la liste possibleTasks */
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

    /** Renvoie la tâche appartenant au job ayant la plus grande durée restante parmi les tâches de la liste possibleTasks */
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

    /** Renvoie la/les tâche(s) pouvant commencer au plus tôt parmi les tâches de la liste possibleTasks */
    public List<Task> EST (Instance instance, int[] jobs, int[] machines, HashMap<Integer,Task> possibleTasks) {
        int[] est = new int[instance.numJobs];
        for (int i: possibleTasks.keySet()) {
            est[i] = Math.max(jobs[i], machines[instance.machine(possibleTasks.get(i))]);
        }

        /* Sélectionner la/les tâche(s) qui ont la petite valeur d'EST dans estTab et les ajouter à estTab */
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

    /** Ordonnancer les tâches d'une instance selon EST_SPT */
    public ResourceOrder EST_STP (Instance instance, ResourceOrder order) {
        int[] jobs = new int[instance.numJobs];
        int[] machines = new int[instance.numMachines];

        HashMap<Integer,Task> possibleTasks = new HashMap<>();
        for (int i = 0; i < instance.numJobs; i++) {
            possibleTasks.put(i, new Task(i, 0));
        }

        while (!possibleTasks.isEmpty()) {
            /* Choisir et enlever de possibleTasks la tâche à réaliser en premier en appliquant SPT sur la liste renvoyée
            par EST */
            Task taskToRemove = SPT(instance, EST(instance, jobs, machines, possibleTasks));

            order.addTaskToMachine(instance.machine(taskToRemove), taskToRemove);
            possibleTasks.remove(taskToRemove.job, taskToRemove);

            /* Mettre à jour la valeur du job des tâches qui vont être réalisées sur la même machine que la machine de
            la tâche précédemment réalisée */
            for (Task task : possibleTasks.values()) {
                if (instance.machine(task) == instance.machine(taskToRemove)) {
                    jobs[task.job] = Math.max(instance.duration(taskToRemove)+jobs[taskToRemove.job], jobs[task.job]);
                }
            }

            /* Ajouter la tâche qui suit dans le job où la tâche taskToRemove a été enlevée */
            if (taskToRemove.task + 1 < instance.numTasks) {
                possibleTasks.put(taskToRemove.job, new Task(taskToRemove.job, taskToRemove.task+1));
            }

            /* Mettre à jour la valeur de machine et job de la tâche enlevée */
            jobs[taskToRemove.job] += instance.duration(taskToRemove);
            machines[instance.machine(taskToRemove)] = Math.max(jobs[taskToRemove.job], machines[instance.machine(taskToRemove)] + instance.duration(taskToRemove));
        }
        return order;
    }

    /** Ordonnancer les tâches d'une instance selon EST_LRPT */
    public ResourceOrder EST_LRPT (Instance instance, ResourceOrder order) {
        int [] jobs = new int [instance.numJobs];
        int [] machines = new int[instance.numMachines];

        HashMap<Integer,Task> possibleTasks = new HashMap<>();
        for (int i = 0; i < instance.numJobs; i++) {
            possibleTasks.put(i, new Task(i, 0));
        }

        while (!possibleTasks.isEmpty()) {
            /* Choisir et enlever de possibleTasks la tâche à réaliser en premier en appliquant LRPT sur la liste des
             tâches renvoyées par EST */
            List<Task> estTab = EST(instance, jobs, machines, possibleTasks);
            List<Task> tasks = new ArrayList<>();
            for (Task tsk : estTab) {
                tasks.add(possibleTasks.get(tsk.job));
            }
            Task taskToRemove = LRPT(instance, tasks);

            order.addTaskToMachine(instance.machine(taskToRemove), taskToRemove);
            possibleTasks.remove(taskToRemove.job, taskToRemove);

            /* Ajouter la tâche qui suit dans le job où la tâche taskToRemove a été enlevée */
            if (taskToRemove.task + 1 < instance.numTasks) {
                possibleTasks.put(taskToRemove.job, new Task(taskToRemove.job, taskToRemove.task+1));
            }

            /* Mettre à jour la valeur de machine et job de la tâche enlevée */
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
                /* mettre la première tâche de chaque job dans la liste des tâches qui devront être réalisées en premier */
                for (int i = 0; i < instance.numJobs; i++) {
                    possibleTasks.add(new Task(i, 0));
                }
                while (!possibleTasks.isEmpty()) {
                    /* Choisir et enlever de la liste possibleTasks la tâche la plus prioritaire selon SPT */
                    Task taskToRemove = SPT(instance, possibleTasks);
                    possibleTasks.remove(taskToRemove);
                    order.addTaskToMachine(instance.machine(taskToRemove), taskToRemove);

                    /* Ajouter la tâche qui suit dans le job où la tâche taskToRemove a été enlevée, si elle existe */
                    if (taskToRemove.task + 1 < instance.numTasks) {
                        possibleTasks.add(new Task(taskToRemove.job, taskToRemove.task + 1));
                    }
                }
                break;

            case LRPT:
                /* ajouter toutes les tâches de l'instance à la liste possibleTasks */
                for (int job = 0; job < instance.numJobs; job++) {
                    for (int task = 0; task < instance.numTasks; task++) {
                        possibleTasks.add(new Task(job, task));
                    }
                }
                while (!possibleTasks.isEmpty()) {
                    /* enlever de la liste possibleTasks la tâche la plus prioritaire selon LRPT */
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
