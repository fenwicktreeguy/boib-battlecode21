package presprint;
import java.util.*;
import battlecode.common.*;

class CustPair{
    MapLocation lc;
    int DST;
    public CustPair(MapLocation lc, int DST){
        this.lc=lc;
        this.DST=DST;
    }
}

public class BugNav extends RobotPlayer {
    public static MapLocation CUR, END;
    public static int[] dx = {-1,1,0,0,-1,1,-1,1};
    public static int[] dy = {0,0,1,-1,1,1,-1,-1};
    public static int mv = 0;
    public static final int BOUNDED_MOVE_LIMIT = 5;//heuristic for saying if we can't move for x turns,
    //choose another setpoint

    public static int TANGENT_RADIUS_SQUARED;
    HashMap<MapLocation, MapLocation> path;
    public static Direction[] correspond = {Direction.WEST,
            Direction.EAST,
            Direction.NORTH,
            Direction.SOUTH,
            Direction.NORTHWEST,
            Direction.NORTHEAST,
            Direction.SOUTHWEST,
            Direction.NORTHWEST};
    public static int distance(MapLocation one, MapLocation two){
        return (int)(Math.pow((one.x-two.x),2) + Math.pow((one.y-two.y),2));
    }

    public BugNav(MapLocation CUR, MapLocation END) {
        this.CUR = CUR;
        this.END = END;
    }
    public static boolean path(RobotController prc) throws GameActionException{
        int m_dist = (int)(1e18);
        Direction mp = Direction.NORTH;
        for(int i = 0; i < dx.length; i++){
            MapLocation one = prc.getLocation().add(correspond[i]);
            int relax_dist = distance(one,END);
            if(prc.onTheMap(one) && prc.sensePassability(one) >= 0.7){
                //continue;
            }
            if(prc.onTheMap(prc.getLocation().add(correspond[i])) && !prc.isLocationOccupied(prc.getLocation().add(correspond[i])) && Math.min(m_dist,relax_dist)==relax_dist) {
                m_dist = relax_dist;
                mp = correspond[i];
            }
        }

        if(!rc.onTheMap(prc.getLocation().add(mp)) || rc.getLocation().equals(END)){
            return true;
        }

        if(prc.canMove(mp)){
            prc.move(mp);
            return false;
        } else {
            ++mv;
            if (mv >= BOUNDED_MOVE_LIMIT) {
                mv = 0;
                return true;
            }
            return false;
        }
        /*
        int m_dist = (int)(1e18);
        Direction mp = Direction.NORTH;
        int det_radius = 0;

        switch(prc.getType()) {
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

        for(int i = 0; i < dx.length; i++) {
            int corres_x = dx[i] * det_radius;
            int corres_y = dy[i] * det_radius;
            int relax_dist = distance(new MapLocation(rc.getLocation().x + dx[i], rc.getLocation().y + dy[i]), END);
            if (Math.min(m_dist, relax_dist) == relax_dist) {
                m_dist = relax_dist;
                mp = correspond[i];
            }
        }

        if(!rc.onTheMap(prc.getLocation().add(mp)) || rc.getLocation().equals(END)){
            return true;
        }
        if(prc.canMove(mp)){
            prc.move(mp);
            return false;
        } else if (!prc.canMove(mp)) {
            return true;
        }
        return true;
         */

    }
    //more of a discretized tangent bug, only looks in cardinal directions since preprocessing space is hard
    public static void tangent_path(RobotController prc) throws GameActionException{
        switch(prc.getType()){
            case POLITICIAN:
                TANGENT_RADIUS_SQUARED = 5;
                break;
            case SLANDERER:
                TANGENT_RADIUS_SQUARED = 4;
                break;
            case MUCKRAKER:
                TANGENT_RADIUS_SQUARED = 5;
                break;
        }
        Direction mp = Direction.NORTH;
        TreeSet<CustPair> t= new TreeSet<CustPair>( new Comparator<CustPair>(){
            @Override
            public int compare(CustPair o1, CustPair o2){
                if(o1.DST < o2.DST){
                    return 1;
                } else if(o1.DST==o2.DST){
                    try {
                        return (rc.sensePassability(o1.lc) < rc.sensePassability(o2.lc) ? 1 : -1);
                    }catch(GameActionException e){

                    }
                }
                return 0;
            }
        }
        );//stores and sorts impassability values
        for(int i = 0; i < dx.length; i++){
            MapLocation one = new MapLocation(prc.getLocation().x+dx[i],prc.getLocation().y+dy[i]);
            one = new MapLocation(one.x*TANGENT_RADIUS_SQUARED,one.y*TANGENT_RADIUS_SQUARED);
            int relax_dist = distance(one,END);
            int orig_dist = distance(CUR,END);
            //of the locations which are movable to and which minimize distance, choose the one which
            //has lowest impassability (this protocol can be changed in emergencies to a more normal nav)
            if(!prc.isLocationOccupied(one) && Math.min(orig_dist,relax_dist)==relax_dist) {
                mp = correspond[i];
                t.add(new CustPair(one,relax_dist));
            }
        }

        try {
            prc.move(mp);
        }catch(GameActionException e){
            e.printStackTrace();
        }


    }

}
