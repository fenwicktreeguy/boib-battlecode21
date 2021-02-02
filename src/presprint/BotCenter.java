package presprint;
import java.util.*;
import battlecode.common.*;

//figure out how to dynamically maintain information about existing hqs for team

public class BotCenter extends RobotPlayer {
    public static int turn_ptr = 0;
    public static void region_and_location_parser(RobotController rc, int code) throws GameActionException{
        String bincode = bin_conv(code);
        int len = bin_conv(HQ_ENCODED_POSITION).length();
        String one = bincode.substring(0,len);
        String two = bincode.substring(len);
        TEMP_POSITION = dec_conv(one);
        TEMP_REGION = dec_conv(two);
    }


    public static Direction[] correspond = {Direction.WEST,
            Direction.EAST,
            Direction.NORTH,
            Direction.SOUTH,
            Direction.NORTHWEST,
            Direction.NORTHEAST,
            Direction.SOUTHWEST,
            Direction.NORTHWEST};

    //detects muckrakers and suggests a different build order based on a return value
    public static int muckraker_data(RobotController rc) throws GameActionException{
        int muckraker_coming = 0;
        int tot_health = 0;
        for(RobotInfo r : rc.senseNearbyRobots()){
            if(r.team == my_team && r.type == RobotType.MUCKRAKER){
                int code = rc.getFlag(r.ID);
                String bin = bin_conv(code);
                String pert_reg = bin.substring(16);
                int conv = dec_conv(pert_reg);
                neutral_ecs.add(conv);
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

    public static void control_spawn_order(RobotController rc) throws GameActionException{
        if (!SCOUT_MAP_SIZE && RANDOM_MAP_HEURISTIC){
            HQ_ENCODED_POSITION = encode(rc.getLocation().x, rc.getLocation().y);
            int corr_val = turn_ptr % (potential_build_orders.get(BUILD_ORDER_INDEX).length);
            ++turn_ptr;
            /*
            String comb = bin_conv(HQ_ENCODED_POSITION);
            String comb2 = bin_conv(corr_val);
            int COMB_LENGTH = comb.length();
            int COMB_TWO_LENGTH = comb2.length();
            for(int i = 14-COMB_LENGTH; --i > 0;){
                comb = '0' + comb;
            }
            for(int i = 9-COMB_TWO_LENGTH; --i>0;){
                comb2 = '0' + comb2;
            }
            String bitpacked = comb + comb2; //occupies ~22 bits of space, should be transmissible
            rc.setFlag(dec_conv(bitpacked));
             */
            //if(swarm_locs.size()==0) {
            //  potential_enemy_ecs(rc);
            // }
            if(!LATTICE_COMPUTED){
                precompute_lattice_positions(rc);
                LATTICE_COMPUTED=true;
            }

            if(potential_build_orders.get(BUILD_ORDER_INDEX)[corr_val]==1){
                for(Direction dir: correspond) {
                    int het = politician_valid_health[turnCount % politician_valid_health.length];
                    if (rc.canBuildRobot(RobotType.POLITICIAN, dir, het)) {
                        rc.buildRobot(RobotType.POLITICIAN, dir, het);
                    }
                }
            } else if(potential_build_orders.get(BUILD_ORDER_INDEX)[corr_val]==0){
                for(Direction dir: correspond) {
                    if (rc.canBuildRobot(RobotType.MUCKRAKER, dir, 1)) {
                        rc.buildRobot(RobotType.MUCKRAKER, dir, 1);
                    }
                }
            }else if(potential_build_orders.get(BUILD_ORDER_INDEX)[corr_val]==2) {
                for(Direction dir: correspond) {
                    if (rc.canBuildRobot(RobotType.SLANDERER, dir, SLANDERER_HEALTH_CAP)) {
                        rc.buildRobot(RobotType.SLANDERER, dir, SLANDERER_HEALTH_CAP);
                    }
                }
            }
            //bidding is going to be suboptimal
            for(RobotInfo r : rc.senseNearbyRobots()){
                if(r.type==RobotType.MUCKRAKER && r.team==my_team){
                    int flag = rc.getFlag(r.ID);
                    //region_and_location_parser(rc,flag);
                    //find a way to distinguish muckrakers carrying neutral EC info from normal muckrakers

                }
            }


        } else if(!SCOUT_MAP_SIZE && !RANDOM_MAP_HEURISTIC){
            rc.setFlag(HQ_ASSIGNED_SUBREGION);
            HQ_ASSIGNED_SUBREGION = ((HQ_ASSIGNED_SUBREGION + 1) % (precomputed_lattice_codes.length));
            int corr_val = turnCount % (potential_build_orders.get(BUILD_ORDER_INDEX).length);
            if(potential_build_orders.get(BUILD_ORDER_INDEX)[corr_val]==1){
                for(Direction dir: correspond) {
                    int het = politician_valid_health[turnCount % politician_valid_health.length];
                    if (rc.canBuildRobot(RobotType.POLITICIAN, dir, het)) {
                        rc.buildRobot(RobotType.POLITICIAN, dir, het);
                    }
                }
            } else if(potential_build_orders.get(BUILD_ORDER_INDEX)[corr_val]==0){
                for(Direction dir: correspond) {
                    if (rc.canBuildRobot(RobotType.MUCKRAKER, dir, 1)) {
                        rc.buildRobot(RobotType.MUCKRAKER, dir, 1);
                    }
                }
            }else if(potential_build_orders.get(BUILD_ORDER_INDEX)[corr_val]==2) {
                for(Direction dir: correspond) {
                    if (rc.canBuildRobot(RobotType.SLANDERER, dir, SLANDERER_HEALTH_CAP)) {
                        rc.buildRobot(RobotType.SLANDERER, dir, SLANDERER_HEALTH_CAP);
                    }
                }
            }
        }
    }



    public static void construct(RobotController prc) throws GameActionException {
        if(my_team==null){
            my_team=prc.getTeam();
        }
        if(HQ_THAT_SPAWNED_ME == null){
            HQ_THAT_SPAWNED_ME = prc.getLocation();
            HQ_ENCODED_POSITION = (128 * (prc.getLocation().x % 128)) + (prc.getLocation().y % 128);
            rc.setFlag(HQ_ENCODED_POSITION);
        } else if(neutral_ecs.size() > 0){
            int idx = (turnCount % neutral_ecs.size());
            if(troops_sent_region[neutral_ecs.get(idx)] == INTERNAL_LIMIT_COUNTER ){
                return;
            }
            control_spawn_order(rc);
            ++troops_sent_region[neutral_ecs.get(idx)];
        }

        if(PREV_INFLUENCE==0){
            PREV_INFLUENCE = prc.getInfluence();
        } else {
            //rc.bid( (int)(BID_PERCENTAGE * Math.abs(PREV_INFLUENCE - prc.getInfluence())) );
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
        int ROT_VALUE = 1;

        if(turnCount >= 0 && turnCount <= 100){
            ROT_VALUE=7;
        } else if(turnCount >= 100 && turnCount <=300){
            ROT_VALUE=5;
        } else if(turnCount >= 300 && turnCount <= 600){
            ROT_VALUE=3;
        } else if(turnCount >= 600 && turnCount <= 1000){
            ROT_VALUE=2;
        }else if(turnCount >= 1000 && turnCount <=1500){
            ROT_VALUE=1;
        }

        if(turnCount % ROT_VALUE == 0) {
            control_spawn_order(rc);
        }

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
        }

        minimax_regret(rc);

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

    public static double bidding_activation_function(RobotController rc, int amt) throws GameActionException{
        double conv = (double)(amt)/(double)(750);
        double ret = Math.pow(Math.E, conv) - Math.pow(Math.E, -conv);
        double ret2 = Math.pow(Math.E,-conv) + Math.pow(Math.E, conv);
        double ans  = ret/ret2;
        return ans;
    }
    public static void maintain_formation(RobotController rc) throws GameActionException{

    }

    //bidding strategy will probably be minimax on regret
    //binary search on the amount that the enemy is bidding(works most of the time since most
    //current bidding strategies leads to bidding some constant amount)
    public static void minimax_regret(RobotController rc) throws GameActionException{
        int cur_votes = rc.getTeamVotes();

        /*
        if(CUR_VOTES < cur_votes){
            BID_PERCENTAGE = BID_PERCENTAGE/1.01;
        } else {
            BID_PERCENTAGE = BID_PERCENTAGE * 1.01;
        }
         */


        if(rc.getTeamVotes() < 751 && turnCount < 1200) {
            DYN_MULTIPLIER += (double)(turnCount)/(double)(120000);
            if (rc.canBid((int) (Math.ceil(DYN_MULTIPLIER * BID_PERCENTAGE * Math.abs(rc.getInfluence() - PREV_INFLUENCE))))) {
                rc.bid((int) (Math.ceil(DYN_MULTIPLIER * BID_PERCENTAGE * Math.abs(rc.getInfluence() - PREV_INFLUENCE))));
                System.out.println("BID: " + (int) (Math.ceil(DYN_MULTIPLIER * BID_PERCENTAGE * Math.abs(rc.getInfluence() - PREV_INFLUENCE))));
            } else {
                rc.bid(1 + (int)(Math.ceil((double)(turnCount)/(double)(300))));
            }
        } else if(rc.getTeamVotes() < 751 && turnCount >= 1200){
            rc.bid(22);
        }
        System.out.println("BID PERCENTAGE: " + BID_PERCENTAGE);
        CUR_VOTES=cur_votes;
        PREV_INFLUENCE = rc.getInfluence();
    }
    public static void binary_serch_on_bid(RobotController rc) throws GameActionException{
        int md = (LEFT_BID_PTR);
    }

    public static void modulate_build_order(RobotController rc) throws GameActionException{
        int get_count = rc.getRobotCount();
    }

}
