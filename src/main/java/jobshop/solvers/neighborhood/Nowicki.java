package jobshop.solvers.neighborhood;

import jobshop.encodings.ResourceOrder;
import jobshop.encodings.Schedule;
import jobshop.encodings.Task;

import java.util.*;
import java.util.stream.Collectors;

/** Implementation of the Nowicki and Smutnicki neighborhood.
 *
 * It works on the ResourceOrder encoding by generating two neighbors for each block
 * of the critical path.
 * For each block, two neighbors should be generated that respectively swap the first two and
 * last two tasks of the block.
 */
public class Nowicki extends Neighborhood {

    /** A block represents a subsequence of the critical path such that all tasks in it execute on the same machine.
     * This class identifies a block in a ResourceOrder representation.
     *
     * Consider the solution in ResourceOrder representation
     * machine 0 : (0,1) (1,2) (2,2)
     * machine 1 : (0,2) (2,1) (1,1)
     * machine 2 : ...
     *
     * The block with : machine = 1, firstTask= 0 and lastTask = 1
     * Represent the task sequence : [(0,2) (2,1)]
     *
     * */
    public static class Block {
        /** machine on which the block is identified */
        public final int machine;
        /** index of the first task of the block */
        public final int firstTask;
        /** index of the last task of the block */
        public final int lastTask;

        /** Creates a new block. */
        Block(int machine, int firstTask, int lastTask) {
            this.machine = machine;
            this.firstTask = firstTask;
            this.lastTask = lastTask;
        }
    }

    /**
     * Represents a swap of two tasks on the same machine in a ResourceOrder encoding.
     *
     * Consider the solution in ResourceOrder representation
     * machine 0 : (0,1) (1,2) (2,2)
     * machine 1 : (0,2) (2,1) (1,1)
     * machine 2 : ...
     *
     * The swap with : machine = 1, t1= 0 and t2 = 1
     * Represent inversion of the two tasks : (0,2) and (2,1)
     * Applying this swap on the above resource order should result in the following one :
     * machine 0 : (0,1) (1,2) (2,2)
     * machine 1 : (2,1) (0,2) (1,1)
     * machine 2 : ...
     */
    public static class Swap {
        /** machine on which to perform the swap */
        public final int machine;

        /** index of one task to be swapped (in the resource order encoding).
         * t1 should appear earlier than t2 in the resource order. */
        public final int t1;

        /** index of the other task to be swapped (in the resource order encoding) */
        public final int t2;

        /** Creates a new swap of two tasks. */
        Swap(int machine, int t1, int t2) {
            this.machine = machine;
            if (t1 < t2) {
                this.t1 = t1;
                this.t2 = t2;
            } else {
                this.t1 = t2;
                this.t2 = t1;
            }
        }


        /** Creates a new ResourceOrder order that is the result of performing the swap in the original ResourceOrder.
         *  The original ResourceOrder MUST NOT be modified by this operation.
         */
        public ResourceOrder generateFrom(ResourceOrder original) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Swap swap = (Swap) o;
            return machine == swap.machine && t1 == swap.t1 && t2 == swap.t2;
        }

        @Override
        public int hashCode() {
            return Objects.hash(machine, t1, t2);
        }
    }


    @Override
    public List<ResourceOrder> generateNeighbors(ResourceOrder current) {
        // convert the list of swaps into a list of neighbors (function programming FTW)
        return allSwaps(current).stream().map(swap -> swap.generateFrom(current)).collect(Collectors.toList());

    }

    /** Generates all swaps of the given ResourceOrder.
     * This method can be used if one wants to access the inner fields of a neighbors. */
    public List<Swap> allSwaps(ResourceOrder current) {
        List<Swap> neighbors = new ArrayList<>();
        // iterate over all blocks of the critical path
        for(var block : blocksOfCriticalPath(current)) {
            // for this block, compute all neighbors and add them to the list of neighbors
            neighbors.addAll(neighbors(block));
        }
        return neighbors;
    }

    /** Returns a list of all the blocks of the critical path. */
    public List<Block> blocksOfCriticalPath(ResourceOrder order) {
        Optional<Schedule> optSchedule = order.toSchedule();
        Schedule schedule = optSchedule.get();
        List<Task> tskOfCriticalPath = schedule.criticalPath();
        List<Block> blockList = new ArrayList<>();

        int tsk = 0;
        while (tsk != tskOfCriticalPath.size()){
            for(int nextTsk=tsk+1; nextTsk<tskOfCriticalPath.size(); nextTsk++) {
                if (order.instance.machine(tskOfCriticalPath.get(tsk)) != order.instance.machine(tskOfCriticalPath.get(nextTsk))) {
                    blockList.add(new Block(order.instance.machine(tskOfCriticalPath.get(tsk)), tsk, nextTsk - 1));
                    tsk = nextTsk;
                }
            }
        }




//        List<Block> blockList = new ArrayList<>();
//        Task[][] tasksByMachine = order.getTasksByMachine();
//        Map<Integer, List<Task>> taskMap = new HashMap<>();
//
//        Optional<Schedule> optSchedule = order.toSchedule();
//        Schedule schedule = optSchedule.get();
//        List<Task> tskOfCriticalPath = schedule.criticalPath();
//
//        for (Task tsk : tskOfCriticalPath) {
//            int machine = order.instance.machine(tsk);
//            if (taskMap.containsKey(machine)) {
//                taskMap.get(order.instance.machine(tsk)).add(tsk);
//            } else {
//                taskMap.put(machine, List.of(tsk));
//            }
//        }
//
//        for (int m=0; m<order.instance.numMachines; m++) {
//            if (taskMap.containsKey(m) && taskMap.get(m).size()>1) {
//                int firstTask = order.instance.numTasks;
//                int lastTask = 0;
//                for (int i=0; i<tasksByMachine[m].length; i++) {
//                    if (taskMap.containsValue(tasksByMachine[m][i])) {
//                        firstTask = Math.min(firstTask, i);
//                        lastTask = Math.max(lastTask, i);
//                    }
//                }
//                Block newBlock = new Block(m, firstTask, lastTask);
//                blockList.add(newBlock);
//            }
//        }
        return blockList;
    }

    /** For a given block, return the possible swaps for the Nowicki and Smutnicki neighborhood */
    List<Swap> neighbors(Block block) {
        List<Swap> swapList = new ArrayList<>();
        if (block.firstTask != block.lastTask) {  // block with more than one task
            Swap newSwap1 = new Swap(block.machine, block.firstTask, block.firstTask+1);
            Swap newSwap2 = new Swap(block.machine, block.lastTask-1, block.lastTask);
            if (!newSwap1.equals(newSwap2)) {  // block with two tasks
                swapList.add(newSwap1);
            } else {                           // block with more than two tasks
                swapList.add(newSwap1);
                swapList.add(newSwap2);
            }
        }
        return swapList;
    }
}
