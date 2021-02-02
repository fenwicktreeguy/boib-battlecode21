package presprint;
import battlecode.common.*;
import java.util.*;

class NodeObj{
    MapLocation pos;
    double corr;
    public NodeObj(MapLocation pos, double corr){
        this.pos=pos;
        this.corr=corr;
    }
}

class NodeComparator implements Comparator{
    public int compare(Object o1, Object o2){
        NodeObj p = (NodeObj)o1;
        NodeObj p2 = (NodeObj)o2;
        return (p.corr < p2.corr ? 1 : -1);
    }
}

public class LocalDijkstra extends RobotPlayer {
    public static HashMap<MapLocation, ArrayList<NodeObj>> adj;
    public static HashMap<MapLocation, Double> intermediate_sp;
    public static HashMap<MapLocation, Boolean> vis;
    public static HashMap<MapLocation, MapLocation> ucs_predecessor;
    public static PriorityQueue<NodeObj> pq;

    public LocalDijkstra() {
        adj = new HashMap<MapLocation, ArrayList<NodeObj>>();
        intermediate_sp = new HashMap<MapLocation, Double>();
        vis = new HashMap<MapLocation, Boolean>();
        ucs_predecessor = new HashMap<MapLocation, MapLocation>();
        pq = new PriorityQueue<NodeObj>(new NodeComparator());
    }

    public static void create_adjacency_list(RobotController rc) throws GameActionException {
        int det_radius = 0;
        switch (rc.getType()) {
            case MUCKRAKER:
                det_radius = 6;
                break;
            case SLANDERER:
                det_radius = 4;
                break;
            case POLITICIAN:
                det_radius = 5;
                break;
        }
        Queue<MapLocation> q = new LinkedList<MapLocation>();
        q.add(rc.getLocation());
        while (q.size() > 0) {
            MapLocation tp = q.poll();
            int[] dx = {1, 0, -1, 0};
            int[] dy = {0, 1, 0, -1};
            for (int i = 0; i < dx.length; i++) {
                if (adj.get(tp) == null) {
                    ArrayList<NodeObj> a = new ArrayList<NodeObj>();
                    a.add(new NodeObj( new MapLocation(tp.x + dx[i], tp.y + dy[i]), rc.sensePassability(rc.getLocation())));
                    adj.put(tp, a);
                } else {
                    ArrayList<NodeObj> a = adj.get(tp);
                    a.add(new NodeObj( new MapLocation(tp.x + dx[i], tp.y + dy[i]), rc.sensePassability(rc.getLocation())));
                    adj.put(tp, a);
                }
            }
        }

    }

    public static ArrayList<MapLocation> optimal_path_ucs(int end_node) {
        ArrayList<MapLocation> ans = new ArrayList<MapLocation>();
        MapLocation pt = ucs_predecessor.get(end_node);
        ans.add(pt);
        while (pt != new MapLocation(-1, -1)) {
            ans.add(pt);
            pt = ucs_predecessor.get(pt);
        }
        Collections.reverse(ans);
        return ans;
    }

    public static void UCS(NodeObj startNode) {
        pq.add(startNode);
        ArrayList<MapLocation> seen = new ArrayList<MapLocation>();
        while (pq.size() > 0) {
            NodeObj tmp = pq.poll();
            //System.out.println(tmp.pos + " " + tmp.cost);
            LinkedList<NodeObj> adj_list = (LinkedList<NodeObj>) (adj.get(tmp.pos).clone());
            if (!seen.contains(tmp.pos)) {
                seen.add(tmp.pos);
            }
            int prev_nd = 0;
            int relax_v = 100000000;
            while (!adj_list.isEmpty()) {
                NodeObj tp = adj_list.poll();
                double wt = tp.corr;
                NodeObj addend = new NodeObj(tp.pos, tmp.corr + wt);
                double cst = tmp.corr + wt;
                if (!intermediate_sp.containsKey(tp.pos)) {
                    intermediate_sp.put(tp.pos, tmp.corr + wt);
                    ucs_predecessor.put(tp.pos,tmp.pos);
                    pq.add(addend);
                } else {
                    intermediate_sp.put(tp.pos, Math.min(intermediate_sp.get(tp.pos), cst));
                    if (Math.min(intermediate_sp.get(tp.pos), cst) == cst) {
                        ucs_predecessor.put(tp.pos,tmp.pos);
                        pq.add(addend);
                    }
                }
            }

        }
    }
}