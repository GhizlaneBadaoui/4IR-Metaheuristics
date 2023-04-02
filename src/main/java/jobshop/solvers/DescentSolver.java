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
        Optional<Schedule> result = this.baseSolver.solve(instance, deadline);

        ResourceOrder original = new ResourceOrder(result.get());
        List<ResourceOrder> orderList = this.neighborhood.generateNeighbors(original);

        ResourceOrder bestOrder = orderList.get(0).copy();
        //System.out.println("mks " +orderList.get(0).toSchedule().get().makespan() );
        for(ResourceOrder ord : orderList) {
            //System.out.println("mks " +ord.toSchedule().get().makespan() );
            if (ord.toSchedule().get().makespan() < bestOrder.toSchedule().get().makespan()) {
                bestOrder = ord.copy();
            }
        }
        return bestOrder.toSchedule();
    }
}
