package presprint;
import java.util.*;
import battlecode.common.*;

//figure out how to dynamically maintain information about existing hqs for team

public class BotCenter extends RobotPlayer {
    public static Direction[] correspond = {Direction.WEST,
            Direction.EAST,
            Direction.NORTH,
            Direction.SOUTH,
            Direction.NORTHWEST,
            Direction.NORTHEAST,
            Direction.SOUTHWEST,
            Direction.NORTHWEST};

    //detects muckrakers and suggests a different build order based on a return value
    public static int muckraker_counter(RobotController rc) throws GameActionException{
        int muckraker_coming = 0;
        int tot_health = 0;
        for(RobotInfo r : rc.senseNearbyRobots()){
            if(r.team != my_team && r.type== RobotType.MUCKRAKER){
                ++muckraker_coming;
                tot_health += r.conviction;
            }
        }
        return 0;
    }


    public static void potential_enemy_ecs(RobotController rc) throws GameActionException{
        ArrayList<MapLocation> candidates= new ArrayList<MapLocation>();
        if(OFFSET_X != 0 && OFFSET_Y != 0){
            int vert_x = 2 * (int)(Math.abs(rc.getLocation().x  - (MAP_X)/2)) + rc.getLocation().x;
            int hort_y = 2 * (int)(Math.abs(rc.getLocation().y - (MAP_Y)/2)) + rc.getLocation().y;
            for(int i = 0; i < vert_x; i++){
                for(int j = 0; j < hort_y; j++){
                    candidates.add(new MapLocation(rc.getLocation().x+i,rc.getLocation().y+j));
                }
            }
        }
        swarm_locs = candidates;
    }

    public static void gather_intel(RobotController rc) throws GameActionException{
        return;
    }



    public static void construct(RobotController prc) throws GameActionException {
        if(my_team==null){
            my_team=prc.getTeam();
        }

        if(PREV_INFLUENCE==0){
            PREV_INFLUENCE = prc.getInfluence();
        } else {
            rc.bid( (int)(BID_PERCENTAGE * Math.abs(PREV_INFLUENCE - prc.getInfluence())) );
            PREV_INFLUENCE = prc.getInfluence();
        }

        System.out.println("FLAG: " + prc.getFlag(prc.getID()));
        MapLocation loc = prc.getLocation();
        //allow map scouts to move out of sensing radius before being detected again
        //finding offset of map is very important

        if(MAP_NORTH!=-1 && MAP_SOUTH!=-1 && MAP_EAST!=-1 && MAP_WEST!=-1){
            System.out.println("MAP UNITS DETERMINED!");
            System.out.print(MAP_NORTH + " " + MAP_SOUTH + " " + MAP_WEST + " " + MAP_EAST);
            OFFSET_X = rc.getLocation().x-MAP_WEST;
            OFFSET_Y = rc.getLocation().y-MAP_SOUTH;
            MAP_X = MAP_EAST + MAP_WEST + 1;
            MAP_Y = MAP_NORTH + MAP_SOUTH + 1;
            if(MAP_Y>MAP_X){
                MAP_X=MAP_Y;
            }else if(MAP_X > MAP_Y){
                MAP_Y=MAP_X;
            }
            System.out.println("OFFSETS:");
            System.out.println(OFFSET_X + " " + OFFSET_Y);
            if(SCOUT_MAP_SIZE){
                SCOUT_NEUTRAL_HQ=true;
            }
            rc.setFlag(TOGGLE_POLITICIAN_MODE);
            SCOUT_MAP_SIZE = false;
        }

        if(turn_ptr >= 4 && SCOUT_MAP_SIZE) {
            if ((MAP_NORTH == -1  || MAP_SOUTH == -1 || MAP_EAST == -1 || MAP_WEST == -1)) {
                for (RobotInfo r : rc.senseNearbyRobots()) {
                    if (r.team != my_team) {
                        continue;
                    }
                    if (r.team == my_team && r.type == RobotType.POLITICIAN && rc.getFlag(r.ID) != 0) {
                        //coming from north
                        if (r.getLocation().y == rc.getLocation().y + 1 && key_ptr==0) {
                            MAP_NORTH = prc.getFlag(r.getID()) + 1;
                            System.out.println("NORTH DISTANCE FOUND!: " + prc.getFlag(r.getID()));
                            prc.setFlag(successful_keys[key_ptr]);
                            ++key_ptr;
                            //coming from south
                        } else if (r.getLocation().y == rc.getLocation().y - 1 && key_ptr==1) {
                            MAP_SOUTH = prc.getFlag(r.getID()) + 1;
                            prc.setFlag(successful_keys[key_ptr]);
                            ++key_ptr;
                            System.out.println("SOUTH DISTANCE FOUND!: " + prc.getFlag(r.getID()));
                            //coming from east
                        } else if (r.getLocation().x == rc.getLocation().x + 1 && key_ptr==2) {
                            MAP_EAST = prc.getFlag(r.getID()) + 1;
                            prc.setFlag(successful_keys[key_ptr]);
                            ++key_ptr;
                            System.out.println("EAST DISTANCE FOUND!: " + prc.getFlag(r.getID()));
                            //coming from west
                        } else if (r.getLocation().x == rc.getLocation().x - 1 && key_ptr==0) {
                            MAP_WEST = prc.getFlag(r.getID()) + 1;
                            prc.setFlag(successful_keys[key_ptr]);
                            System.out.println("WEST DISTANCE FOUND!: " + prc.getFlag(r.getID()));
                        }
                    }
                }
            }
        }
        //send out 6 politicians for scouting the map

        System.out.println("TURN POINTER: " + turn_ptr);
        System.out.println("SCOUT_MAP_SIZE: " + SCOUT_MAP_SIZE);
        System.out.println("SLANDERER COOLDOWN: " + SLANDERER_COOLDOWN);
        System.out.println("INITIATE SLANDERER HORDE: " + INITIATE_SLANDERER_HORDE);

        //get a gauge of the map size

        if(turn_ptr < 4 && SCOUT_MAP_SIZE){
            if(rc.canBuildRobot(RobotType.POLITICIAN,sub[turn_ptr],1)){
                //print out direction built here for debugging
                System.out.println("DIRECTION: " + sub[turn_ptr]);
                prc.buildRobot(RobotType.POLITICIAN,sub[turn_ptr],1);
                ++turn_ptr;
                prc.setFlag(turn_ptr);
            }
        } else if (!SCOUT_MAP_SIZE){
            int corr_val = turnCount % (build_order.length);
            //if(swarm_locs.size()==0) {
              //  potential_enemy_ecs(rc);
           // }
            if(potential_build_orders.get(1)[corr_val]==1){
                for(Direction dir: correspond) {
                    int het = politician_valid_health[turnCount % politician_valid_health.length];
                    if (rc.canBuildRobot(RobotType.POLITICIAN, dir, het)) {
                        rc.buildRobot(RobotType.POLITICIAN, dir, het);
                    }
                }
            } else if(potential_build_orders.get(1)[corr_val]==0){
                for(Direction dir: correspond) {
                    if (rc.canBuildRobot(RobotType.MUCKRAKER, dir, 1)) {
                        rc.buildRobot(RobotType.MUCKRAKER, dir, 1);
                    }
                }
            }else if(potential_build_orders.get(1)[corr_val]==2) {
                for(Direction dir: correspond) {
                    if (rc.canBuildRobot(RobotType.SLANDERER, dir, SLANDERER_HEALTH_CAP)) {
                        rc.buildRobot(RobotType.SLANDERER, dir, SLANDERER_HEALTH_CAP);
                    }
                }
            }
            //bidding is going to be suboptimal


        }

        //send out politicians and muckrakers to candidate hq locations; if we find neutral or enemy hqs
        //it should somehow be transmitted back to our ECs

        /*
        //maintaining slanderer formations around ECs
        if(!SCOUT_MAP_SIZE && INITIATE_SLANDERER_HORDE) {
            for (Direction dir : correspond) {
                if (SLANDERER_COOLDOWN == 6) {
                    if (rc.canBuildRobot(RobotType.SLANDERER, dir, 1)) {
                        prc.setFlag(SLANDERER_PTR);
                        prc.buildRobot(RobotType.SLANDERER, dir, 1);
                        ++SLANDERER_PTR;
                        SLANDERER_COOLDOWN = 0;
                    }
                } else {
                    SLANDERER_COOLDOWN += 1;
                    break;
                }
            }
        }

        for(RobotInfo formation : rc.senseNearbyRobots()){
            if(prc.getFlag(formation.ID) == (1 << 24) ){
                SUCCESSFUL_SLANDERER_HORDE = true;
            }
        }
         */


    }
    public static void maintain_formation(RobotController rc) throws GameActionException{

    }

    //bidding strategy will probably be minimax on regret
    public static void bidSprint(RobotController rc) throws GameActionException{
        rc.bid(1);
    }

}
