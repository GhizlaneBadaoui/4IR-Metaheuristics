package jobshop.solvers;

import jobshop.Instance;
import jobshop.encodings.ResourceOrder;
import jobshop.encodings.Schedule;
import jobshop.solvers.neighborhood.Neighborhood;
import jobshop.solvers.neighborhood.Nowicki;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** An empty shell to implement a descent solver. */
public class TabooSolver implements Solver {

    final Neighborhood neighborhood;
    final Solver baseSolver;
    final int maxIter;
    final int dureeTaboo;
    final List<Nowicki.Swap> tabooList;

    /** Creates a new taboo solver with a given neighborhood and a solver for the initial solution.
     *
     * @param neighborhood Neighborhood object that should be used to generate neighbor solutions to the current candidate.
     * @param baseSolver A solver to provide the initial solution.
     * @param maxIter maximal number of iterations
     * @param dureeTaboo the number of iterations for which a solution remains taboo
     *
     */
    public TabooSolver(Neighborhood neighborhood, Solver baseSolver, int maxIter, int dureeTaboo) {
        this.neighborhood = neighborhood;
        this.baseSolver = baseSolver;
        this.maxIter = maxIter;
        this.dureeTaboo = dureeTaboo;
        tabooList = new ArrayList<>();
    }

    /** Trouver le Swap qui a généré order2 à partir d'order1 */
    public Nowicki.Swap findSwap(Instance instance, ResourceOrder order1, ResourceOrder order2) {
        Nowicki.Swap swap = null;
        for(int m=0; m<instance.numMachines; m++) {
            for(int t=0; t<instance.numJobs; t++) {
                if(order1.getTaskOfMachine(m, t) != order2.getTaskOfMachine(m, t)) {
                    swap = new Nowicki.Swap(m,  order2.getTaskOfMachine(m, t).task, order1.getTaskOfMachine(m, t).task);
                    break;
                }
            }
        }
        return swap;
    }

    @Override
    public Optional<Schedule> solve(Instance instance, long deadline) {
        long startTime = System.currentTimeMillis();

        /* Générer une solution réalisable avec une heuristique gloutonne  */
        Optional<Schedule> order = this.baseSolver.solve(instance, deadline);
        assert order.isPresent();

        /* mémoriser la meilleure solution */
        ResourceOrder orderStar = new ResourceOrder(order.get());
        ResourceOrder currentOrder = orderStar.copy();

        /* Compteur d'itérations */
        int i=0;

        while(i<maxIter && startTime<deadline) {
            /* Affichage du makespan */
            System.out.println("makespan : "+ currentOrder.toSchedule().get().makespan());

            /* Explorer les voisins successivement */
            i++;

            /* Choisir le meilleur voisin non tabou */
            List<ResourceOrder> neighbors = this.neighborhood.generateNeighbors(currentOrder);
            ResourceOrder bestNeighbor = null;

            for(ResourceOrder neighbor: neighbors) {
                assert neighbor.toSchedule().isPresent();
                Nowicki.Swap swp = findSwap(instance, currentOrder, neighbor);
                if(!tabooList.contains(swp) && (bestNeighbor == null || (neighbor.toSchedule().get().makespan()<bestNeighbor.toSchedule().get().makespan() && bestNeighbor.toSchedule().isPresent()))) {
                    bestNeighbor = neighbor;
                }
            }

            /* Mettre à jour la liste des solutions Tabou */
            if(bestNeighbor != null) {
                tabooList.add(findSwap(instance, currentOrder, bestNeighbor));
                currentOrder = bestNeighbor;

                /* Supprimer la solution qui a dépassé dureeTaboo */
                if(tabooList.size()>dureeTaboo){
                    tabooList.remove(0);
                }

                assert currentOrder.toSchedule().isPresent();
                assert orderStar.toSchedule().isPresent();
                if(currentOrder.toSchedule().get().makespan()<orderStar.toSchedule().get().makespan()){
                    orderStar = currentOrder;
                }
            } else {
                currentOrder.swapTasks(tabooList.get(0).machine, tabooList.get(0).t1, tabooList.get(0).t2);
            }
        }
        return orderStar.toSchedule();
    }
}