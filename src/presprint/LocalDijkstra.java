package presprint;
import battlecode.common.*;
import java.util.*;

class Pair{
    MapLocation pos;
    int corr;
    public Pair(MapLocation pos, int corr){
        this.pos=pos;
        this.corr=corr;
    }
}

class PairComp implements Comparator{
    public int compare(Object o1, Object o2){
        Pair p = (Pair)o1;
        Pair p2 = (Pair)o2;
        return (p.corr < p2.corr ? 1 : -1);
    }
}

public class LocalDijkstra extends RobotPlayer{

    PriorityQueue<Pair> pq = new PriorityQueue<Pair>();
    /*
    public static int dijkstra_radius = 0;
    public static int[] dx = {-1,1,0,0,-1,1,-1,1};
    public static int[] dy = {0,0,1,-1,1,1,-1,-1};

    public static void create_graph(RobotController rc){
        HashMap<MapLocation, MapLocation> wts = new HashMap<MapLocation, MapLocation>();
        HashMap<MapLocation, ArrayList<MapLocation> > cnts = new HashMap<Map, ArrayList<MapLocation> >();

        LinkedList<MapLocation> l = new Queue();
        HashMap<MapLocation, Boolean> vis = new HashMap<MapLocation, Boolean>();
        l.add(rc.getLocation());
        while(!l.empty()){
            MapLocation tp = l.poll();
            for(int i = 0; i < dx.length; i++){
                MapLocation tmp = new MapLocation(tmp.x + dx[i],tmp.y + dy[i]);
                if(!vis.containsKey(tmp)){
                    vis.put(tmp,true);
                    cnts.put(tp,tmp);

                    l.add(tmp);
                }
            }
        }
    }
    public static void dij_path(RobotController rc){
        switch(rc.getType()){
            case SLANDERER:
                dijkstra_radius = 5;
               break;
            case POLITICIAN:
                dijkstra_radius = 4;
                break;
            case MUCKRAKER:
                dijkstra_radius = 5;
                break;
        }
        create_graph();
    }
     */
}