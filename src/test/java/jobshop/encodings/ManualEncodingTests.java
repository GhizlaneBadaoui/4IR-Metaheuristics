package jobshop.encodings;

import jobshop.Instance;
import jobshop.solvers.BasicSolver;
import jobshop.solvers.Solver;
import org.junit.Before;
import org.junit.Test;

import java.nio.file.Paths;
import java.util.Optional;

public class ManualEncodingTests {

    /** Instance that we will be studied in these tests */
    private Instance instance;

    /** Reference scheduled (produced by the basic solver) that we will recreate manually. */
    private Schedule reference;

    @Before
    public void setUp() throws Exception {
        this.instance = Instance.fromFile(Paths.get("instances/aaa1"));

        Solver solver = new BasicSolver();
        Optional<Schedule> result = solver.solve(this.instance, System.currentTimeMillis() + 10);

        assert result.isPresent() : "The solver did not find a solution";
        // extract the schedule associated to the solution
        this.reference = result.get();
    }

    @Test
    public void testManualSchedule() {
        System.out.println("***** Reference schedule to reproduce ******");
        System.out.println("MAKESPAN: " + this.reference.makespan());
        System.out.println("SCHEDULE: " + this.reference.toString());
        System.out.println("GANTT: " + this.reference.asciiGantt());

        Schedule manualSchedule = new Schedule(instance);

        /*
         * La solution proposée pour l'instance aaa1 :
         * tâche J0,0 commence à 0 et termine à 3 sur machine 0
         * tâche J1,1 commence à 3 et termine à 5 sur machine 0
         * tâche J1,0 commence à 0 et termine à 2 sur machine 1
         * tâche J0,1 commence à 3 et termine à 6 sur machine 1
         * tâche J0,2 commence à 6 et termine à 8 sur machine 2
         * tâche J1,2 commence à 8 et termine à 12 sur machine 2
         */

        manualSchedule.setStartTime(0,0, 0);
        manualSchedule.setStartTime(0,1, 3);
        manualSchedule.setStartTime(0,2, 6);

        manualSchedule.setStartTime(1,0, 0);
        manualSchedule.setStartTime(1,1, 3);
        manualSchedule.setStartTime(1,2, 8);

        assert manualSchedule.equals(this.reference);
    }

    @Test
    public void testManualResourceOrder() {
        ResourceOrder manualRO = new ResourceOrder(instance);

        /*
         * La solution proposée pour l'instance aaa1 dans le bon ordre de passage :
         * tâche J0,0 commence à 0 et termine à 3 sur machine 0
         * tâche J1,1 commence à 3 et termine à 5 sur machine 0
         * tâche J1,0 commence à 0 et termine à 2 sur machine 1
         * tâche J0,1 commence à 3 et termine à 6 sur machine 1
         * tâche J0,2 commence à 6 et termine à 8 sur machine 2
         * tâche J1,2 commence à 8 et termine à 12 sur machine 2
         */

        manualRO.addTaskToMachine(0, new Task(0, 0));
        manualRO.addTaskToMachine(0, new Task(1, 1));

        manualRO.addTaskToMachine(1, new Task(1, 0));
        manualRO.addTaskToMachine(1, new Task(0, 1));

        manualRO.addTaskToMachine(2, new Task(0, 2));
        manualRO.addTaskToMachine(2, new Task(1, 2));

        Optional<Schedule> optSchedule = manualRO.toSchedule();
        assert optSchedule.isPresent() : "The resource order could not be converted to a schedule (probably invalid)";
        Schedule schedule = optSchedule.get();
        System.out.println("***** Solution non optimale de l'instance aaa1 ******");
        System.out.println("MAKESPAN: " + schedule.makespan());
        System.out.println("SCHEDULE: " + schedule);
        System.out.println("GANTT: " + schedule.asciiGantt());
        assert schedule.equals(this.reference) : "The manual resource order encoding did not produce the same schedule";
    }

    @Test
    public void testOptimalResourceOrder() {
        /*
         * La solution optimale pour l'instance aaa1 :
         * tâche J0,0 commence à 0 et termine à 3 sur machine 0
         * tâche J1,1 commence à 3 et termine à 5 sur machine 0
         * tâche J1,0 commence à 0 et termine à 2 sur machine 1
         * tâche J0,1 commence à 3 et termine à 6 sur machine 1
         * tâche J1,2 commence à 5 et termine à 9 sur machine 2
         * tâche J0,2 commence à 9 et termine à 11 sur machine 2
         */

        ResourceOrder manualRO = new ResourceOrder(instance);

        manualRO.addTaskToMachine(0, new Task(0, 0));
        manualRO.addTaskToMachine(0, new Task(1, 1));

        manualRO.addTaskToMachine(1, new Task(1, 0));
        manualRO.addTaskToMachine(1, new Task(0, 1));

        manualRO.addTaskToMachine(2, new Task(1, 2));
        manualRO.addTaskToMachine(2, new Task(0, 2));

        Optional<Schedule> optSchedule = manualRO.toSchedule();
        assert optSchedule.isPresent() : "The resource order could not be converted to a schedule (probably invalid)";
        Schedule schedule = optSchedule.get();
        System.out.println("***** Solution optimale de l'instance aaa1 ******");
        System.out.println("MAKESPAN: " + schedule.makespan());
        System.out.println("SCHEDULE: " + schedule);
        System.out.println("GANTT: " + schedule.asciiGantt());
        assert schedule.makespan() == 11 : "The manual resource order encoding did not produce the optimal schedule";
    }

    @Test
    public void testInvalidResourceOrder() {
        ResourceOrder manualRO = new ResourceOrder(instance);

        /*
         * Un exemple d'une représentation de tâches invalide :
         * on inverse dans la solution optimale l'ordre de passage des tâches J0,0 et J1,1 et des deux
         * tâches J1,0 et J0,1.
         */
        manualRO.addTaskToMachine(1, new Task(0, 1));
        manualRO.addTaskToMachine(0, new Task(1, 1));

        manualRO.addTaskToMachine(1, new Task(1, 0));
        manualRO.addTaskToMachine(0, new Task(0, 0));

        manualRO.addTaskToMachine(2, new Task(1, 2));
        manualRO.addTaskToMachine(2, new Task(0, 2));

        assert manualRO.toSchedule().isEmpty();
    }
}
