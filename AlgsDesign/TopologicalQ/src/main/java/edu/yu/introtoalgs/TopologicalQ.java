package edu.yu.introtoalgs;
import java.util.*;

public class TopologicalQ {

    private Digraph G;
    private int E;
    private Queue<Integer>topOrder;

    public TopologicalQ(Digraph G){
        if(G == null)
            throw new IllegalArgumentException("No Null values Please");

        this.G = G;
        this.E = this.G.E();

        int[] indegree = new int[G.V()];
        Queue<Integer> sources = new LinkedList<Integer>();
        HashSet<Integer> set = new HashSet<Integer>();

        for(int i = 0; i < G.V(); i++) {
            sources.add(i);
            for (int adj : G.adj(i)){
                set.add(adj);
                indegree[adj]++;
            }
        }

        sources.removeAll(set);
        topOrder = new LinkedList<Integer>();

        while(sources.peek() != null) {
            int remVertex = sources.remove();
            topOrder.add(remVertex);
            for (int temp : G.adj(remVertex)) {
                E--;
                if (--indegree[temp] == 0)
                    sources.add(temp);
            }
        }
    }

    public boolean hasOrder() {
       // return topOrder.size() == G.V();
        return this.E == 0;
    }


    public Iterable <Integer > order(){
        if (this.hasOrder()) {
            return topOrder;
        }
        else
            return null;
    }

    public static void main (String[] args) {
        Digraph d1 = new Digraph(6);

        d1.addEdge(5,2);
        d1.addEdge(2,3);
        d1.addEdge(3,1);
        d1.addEdge(4,1);




        TopologicalQ top1 = new TopologicalQ(d1);
        System.out.println(top1.hasOrder());
        System.out.print(top1.order());

    }
}
