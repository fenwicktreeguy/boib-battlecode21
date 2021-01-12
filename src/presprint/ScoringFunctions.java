package presprint;
import battlecode.common.*;
import java.util.*;

public class ScoringFunctions {
    //ideally, these functions should represent local maxima or minima; the needful will be specified
    ArrayList<MapLocation> exclude;
    public ScoringFunctions(ArrayList<MapLocation> exclude){
        this.exclude=exclude;
    }
    //use f(x,y) = 1 - Math.sqrt( Math.pow( Math.sin(Math.PI * x), 2) + Math.pow( ( Math.cos(Math.PI * x), 2)));
    public static int slanderer_distribute(RobotController rc, int x, int y) throws GameActionException{
        return 0;
    }
    public static int map_swarm(RobotController rc, int x, int y) throws GameActionException{
        return 0;
    }

}