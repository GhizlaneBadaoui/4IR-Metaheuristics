package jobshop.solvers;

import jobshop.Instance;
import jobshop.encodings.ResourceOrder;
import jobshop.encodings.Schedule;
import jobshop.encodings.Task;
import jobshop.solvers.neighborhood.Neighborhood;
import jobshop.solvers.neighborhood.Nowicki;
import net.sourceforge.argparse4j.inf.Namespace;

import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

/** An empty shell to implement a descent solver. */
public class DescentSolver implements Solver {

    final Neighborhood neighborhood;
    final Solver baseSolver;

    /** Creates a new descent solver with a given neighborhood and a solver for the initial solution.
     *
     * @param neighborhood Neighborhood object that should be used to generates neighbor solutions to the current candidate.
     * @param baseSolver A solver to provide the initial solution.
     */
    public DescentSolver(Neighborhood neighborhood, Solver baseSolver) {
        this.neighborhood = neighborhood;
        this.baseSolver = baseSolver;
    }

    @Override
    public Optional<Schedule> solve(Instance instance, long deadline) {
        GreedySolver greedy = new GreedySolver(GreedySolver.Priority.SPT);
        Optional<Schedule> result = greedy.solve(instance,1000);
        System.out.println("list --");
        Nowicki nowicki = new Nowicki();
        List<Nowicki.Swap> swapList = nowicki.allSwaps(new ResourceOrder(result.get()));
        System.out.println("list " + swapList);
//        ResourceOrder newOrder = Nowicki.Swap.generateFrom();


//
//        Optional<Schedule> optSchedule = manualRO.toSchedule();
//        Schedule schedule = optSchedule.get();
//        optSchedule = basic.solve(instance, 100);
//        List<Task> tskOfCriticalPath = optSchedule.criticalPath();
//
//        System.out.println(tskOfCriticalPath);

//        Nowicki n = new Nowicki();
//        List<Nowicki.Block> list = n.blocksOfCriticalPath(manualRO);
//        System.out.println(list);



        return Optional.of(result.get());
    }

}
