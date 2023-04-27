package jobshop.solvers;

import jobshop.Instance;
import jobshop.encodings.ResourceOrder;
import jobshop.encodings.Schedule;
import jobshop.solvers.neighborhood.Neighborhood;

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
        /* Générer une solution réalisable avec une heuristique gloutonne  */
        Optional<Schedule> initSchedule = this.baseSolver.solve(instance, deadline);
        assert initSchedule.isPresent();
        ResourceOrder order = new ResourceOrder(initSchedule.get());
        assert order.toSchedule().isPresent();

        while (true) {
            /* Générer la liste des ResourceOrders pour les différents voisinages */
            List<ResourceOrder> orderList = this.neighborhood.generateNeighbors(order);
            ResourceOrder bestOrder = orderList.get(0).copy();
            assert bestOrder.toSchedule().isPresent();

            /* Sélectionner la solution voisine améliorante dans orderList */
            for(ResourceOrder ord : orderList) {
                if (ord.toSchedule().isPresent() && ord.toSchedule().get().makespan() < bestOrder.toSchedule().get().makespan()) {
                    bestOrder = ord.copy();
                }
            }
            /* S'arrêter lorsque la solution voisine n'est plus améliorante par rapport à la solution sélectionnée à l'itération antérieure */
            if (bestOrder.toSchedule().get().makespan()<order.toSchedule().get().makespan()) {
                order = bestOrder.copy();
            } else {
                break;
            }
//            System.out.println("Makespan : "+ order.toSchedule().get().makespan());
            /* --> le makespan diminue au cours des itérations */
        }
        return order.toSchedule();
    }
}
