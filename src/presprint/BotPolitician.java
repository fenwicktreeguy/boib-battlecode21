package presprint;
import java.util.*;
import battlecode.common.*;

//strategy for using politicians early-game is twofold: we need to get information about the map early on
//to give to future robots as well as attempt to take control of the neutral enlightenment centers
public class BotPolitician extends RobotPlayer{
    public static int[] dx = {-1,1,0,0,-1,1,-1,1};
    public static int[] dy = {0,0,1,-1,1,1,-1,-1};
    public static int turn_ptr = (int)(Math.random() * 289);
    public static boolean TRANSMIT_MESSAGE = false;
    public static int TANGENT_RADIUS;
    boolean peruse;
    public static MapLocation POL_DESTINATION;
    public static boolean navigating_to_empower = false;
    public static int CURRENT_EC_LOCATION = 0;//with this, we will use heuristic which trys to
    //explore the map by moving troops in directions which maximize the distance from their native EC
    public static int CURRENT_EC_DIST = -1;

    public static Direction[] correspond = {Direction.WEST,
            Direction.EAST,
            Direction.NORTH,
            Direction.SOUTH,
            Direction.NORTHWEST,
            Direction.NORTHEAST,
            Direction.SOUTHWEST,
            Direction.NORTHWEST};
    BugNav pot_nav;
    public RobotController prc;
    public BotPolitician(boolean peruse){
        this.peruse = peruse;
        prc=rc;
    }

    //potential heuristic idea: mucks act as guides to neutral ECs, polys follow and attempt to convert,
    //although this pathfinding potential should be conditional.
    public static void region_and_location_parser(RobotController rc, int code) throws GameActionException{
        String bincode = bin_conv(code);
        int len = bin_conv(encode(POL_DESTINATION.x,POL_DESTINATION.y)).length();
        String one = bincode.substring(0,len);
        String two = bincode.substring(len);
        TEMP_POSITION = dec_conv(one);
        TEMP_REGION = dec_conv(two);
    }

    //maybe try an idea where your some subportion of your flag times number of set bits XOR some set value equals some prime value, maybe
    public static boolean allied_politician(int flag){
        int cur_b = (int)( (double)(Math.log(flag)) / (double)(Math.log(2)));
        int f_b = (int)(Math.floor(cur_b)) + 1;
        int n_set = 0;
        for(int i = 0; i < f_b; i++){
            if( (flag & (1<<i)) == (1<<i) ) {
                ++n_set;
            }
        }
        return n_set%2==0;

    }
    public static ArrayList<Integer> numSetBits(int flg){
        ArrayList<Integer> bit_locs = new ArrayList<Integer>();
        int bt = (int)( Math.log(flg) / Math.log(2) );
        for(int i = 0; i < Math.floor(bt)+1; i++){
            if( ((int)(flg) & (int)(1 << i)) == (1<<i)){
                bit_locs.add(i);
            }
        }
        return bit_locs;
    }
    //attempting to find neutral hqs
    public static void patrol(RobotController rc) throws GameActionException{
        Direction dir = RobotPlayer.randomDirection();
        if(rc.canMove(dir)){
            if(dir==Direction.NORTH){

            }else if(dir==Direction.SOUTH){

            }else if(dir==Direction.EAST){

            }else if(dir==Direction.WEST){

            }
        }
    }

    public static boolean should_empower(RobotController rc) throws GameActionException{
        int inf_amt = rc.getInfluence();
        int radius = (rc.getInfluence()/12)  - 3;
        int amt = 0;
        int pol_locality = 0;

        if(turnCount >= 1420 && distance(rc.getLocation(),HQ_THAT_SPAWNED_ME) >= 144){
            return true;
        }
        int BOUND_AMT = 0;
        int MIN_DIST = 6;
        int MUCK_DIST = 1000000000;
        for(RobotInfo r : rc.senseNearbyRobots()){
            if(r.type==RobotType.MUCKRAKER && r.team != my_team){
                MUCK_DIST = Math.min(MUCK_DIST, distance(r.location,rc.getLocation()));
            }
        }
        if(distance(rc.getLocation(), HQ_THAT_SPAWNED_ME) <= 144 && MUCK_DIST <= MIN_DIST){
            rc.empower(MUCK_DIST);
        }

        for(RobotInfo r : rc.senseNearbyRobots(radius)){
            amt += (r.team != my_team ? 1 : 0);
            if(r.team==my_team && r.type==RobotType.POLITICIAN){
                inf_amt += 12*r.influence;
                ++BOUND_AMT;
            }
        }
        if(amt==0){
            return false;
        } else {
            if (BOUND_AMT >= 3) {
                return true;
            }
            int div_influence = (int)(Math.floor(inf_amt / amt));
            double killed_sum = 0;
            for (RobotInfo r : rc.senseNearbyRobots(radius)) {
                if(r.team != my_team && r.type==RobotType.POLITICIAN){
                    killed_sum += (r.influence <= div_influence ? 1 : 0);
                } else if(r.team != my_team && r.type==RobotType.MUCKRAKER){
                    killed_sum += (r.influence <= div_influence ? 2 : 0);
                }
            }
            boolean flag1 = ( ((killed_sum) >= amt/3) ? true : false);
            return flag1;
        }

    }

    public static void discern_map(RobotController rc) throws GameActionException{
        MapLocation lc = rc.getLocation();
        if(my_team==null){
            my_team=rc.getTeam();
        }
        if(SCOUT_MAP_SIZE) {
            if (rc.getFlag(rc.getID()) == 0) {
                for (RobotInfo r : rc.senseNearbyRobots()) {
                    if (r.type == RobotType.ENLIGHTENMENT_CENTER && r.team == my_team) {
                        rc.setFlag(rc.getFlag(r.ID));
                    }
                }
            }
            int flg = rc.getFlag(rc.getID());
            System.out.println("POLITICIAN FLAG: " + flg);
            String team = my_team == Team.A ? "A" : "B";
            System.out.println("TEAM: " + team);
            if (!set_discern) {
                if (flg == 1) {
                    discern_dir = Direction.NORTH;
                    opp_dir = Direction.SOUTH;
                    System.out.println("NORTH");
                } else if (flg == 2) {
                    discern_dir = Direction.SOUTH;
                    opp_dir = Direction.NORTH;
                    System.out.println("SOUTH");
                } else if (flg == 3) {
                    discern_dir = Direction.EAST;
                    opp_dir = Direction.WEST;
                    System.out.println("EAST");
                } else if (flg == 4) {
                    discern_dir = Direction.WEST;
                    opp_dir = Direction.EAST;
                    System.out.println("WEST");
                }
                set_discern = true;
            }
            MapLocation ad = lc.add(discern_dir);
            MapLocation ad2 = lc.add(opp_dir);
            System.out.println("DIRECTION: " + discern_dir);
            System.out.println("OPPOSITE DIRECTION: " + opp_dir);
            System.out.println("DISTANCE TRAVERSED: " + MAP_TRAV);
            System.out.println(rc.onTheMap(ad));

            if (!RETURN_NATIVE_HQ) {
                if (rc.onTheMap(ad)) {
                    RobotInfo r = rc.senseRobotAtLocation(ad);
                    if (r != null) {
                        RETURN_NATIVE_HQ = true;
                    }
                }
                if (rc.onTheMap(ad)) {
                    rc.move(discern_dir);
                    ++MAP_TRAV;
                } else {
                    //encode information about a successful scout with ( 1 << (24 - bits used to store distance) )
                    String bs = RobotPlayer.bin_conv(MAP_TRAV);
                    rc.setFlag(MAP_TRAV);
                    RETURN_NATIVE_HQ = true;
                }
            } else {
                System.out.println("RETURNING TO HQ...");
                System.out.println("POLITICIAN CURRENT FLAG: " + rc.getFlag(rc.getID()));
                if (rc.canMove(opp_dir)) {
                    rc.move(opp_dir);
                }
                int DESIRED_FLAG = 0;
                if (opp_dir == Direction.SOUTH) {
                    DESIRED_FLAG = successful_keys[0];
                } else if (opp_dir == Direction.NORTH) {
                    DESIRED_FLAG = successful_keys[1];
                } else if (opp_dir == Direction.WEST) {
                    DESIRED_FLAG = successful_keys[2];
                } else if (opp_dir == Direction.EAST) {
                    DESIRED_FLAG = successful_keys[3];
                }
                for (RobotInfo r : rc.senseNearbyRobots()) {
                    int d = RobotPlayer.distance(r.location, rc.getLocation());
                    if (r.team == rc.getTeam() && r.type == RobotType.ENLIGHTENMENT_CENTER && d == 1) {
                        if (rc.getFlag(r.ID) == TOGGLE_POLITICIAN_MODE) {
                            System.out.println("INFORMATION TRANSMISSIBLE!");
                            SCOUT_MAP_SIZE = false;
                            SCOUT_NEUTRAL_HQ = true;
                        }
                    }
                }
            }
        }
    }
    //random walks about the map in clusters; report back to nearest friendly EC with information
    public static void scout_neutral_hq(RobotController rc) throws GameActionException{
        turn_ptr += 17;
        Random ran = new Random(rc.getID());
        if(ran.nextDouble() >= 0.5){
            turn_ptr += (int)(Math.random() * 289);
            turn_ptr %= 1e18;
        }
        System.out.println("CURRENT TURN POINTER: " + turn_ptr);
        for(RobotInfo r : rc.senseNearbyRobots()){
            if(r.type==RobotType.ENLIGHTENMENT_CENTER && r.team == my_team){
                int identif =  128 * ( (r.location.x) % 128) + ( (r.location.y)%128);
                if(!ec_id.contains(identif)) {
                    ec_id.add(identif);
                    raw_ec_id.add(r.ID);
                }
                if(HQ_THAT_SPAWNED_ME==null){
                    HQ_THAT_SPAWNED_ME=r.location;
                    HQ_ENCODED_POSITION = encode(r.location.x,r.location.y);
                    CURRENT_EC_LOCATION = HQ_ENCODED_POSITION;
                }
            } else if(r.type == RobotType.POLITICIAN && r.team != my_team){
                if(should_empower(rc)){
                    rc.empower( (rc.getInfluence()/12) - 3);
                }
            } else if(r.type==RobotType.ENLIGHTENMENT_CENTER && r.team == Team.NEUTRAL){
                if(navigating_to_empower && distance(r.location,rc.getLocation()) <= 1){
                    rc.empower(1);
                }
                /*
                if(rc.canEmpower(squared_dist(rc.getLocation(),r.location))) {
                    rc.empower(squared_dist(rc.getLocation(), r.location));
                }
                 */
                int identif =  128 * ( (r.location.x + r.location.y) % 128) + ( (r.location.x+r.location.y)%128);
                ec_id.add(identif);
                raw_ec_id.add(r.ID);
                rc.setFlag(ec_id.get(0));
            } else if(r.type==RobotType.MUCKRAKER && r.team == my_team){
                int flag = (rc.getFlag(r.ID));
                if(flag >  ( 1 << 9) ){

                }
            } else if(r.type==RobotType.ENLIGHTENMENT_CENTER && r.team != my_team){
                if(distance(r.location,rc.getLocation()) <= ((rc.getInfluence()/12) - 2)*((rc.getInfluence()/12) - 2)){
                    rc.empower( ((rc.getInfluence()/12) - 2)*((rc.getInfluence()/12) - 2));
                }
            }
        }
        if(!LATTICE_COMPUTED){
            precompute_lattice_positions(rc);
            LATTICE_COMPUTED=true;
        }
        if(rc.getFlag(rc.getID())==0){
            int multiplier = (turnCount <= 500 ? POL_RANDOM_MULTIPLIER : (POL_RANDOM_MULTIPLIER/2) );
            int gen_x = (int)(Math.random() * (multiplier) ) + HQ_THAT_SPAWNED_ME.x;
            int gen_y = (int)(Math.random() * (multiplier))  + HQ_THAT_SPAWNED_ME.y;
            int gen_rand = (int)(Math.random() * precomputed_lattice_codes.length);
            double seed1 = Math.random();
            double seed2 = Math.random();
            int gen = 0;
            if(seed1 <= 0.3){
                gen_x *= -1;
            }
            if(seed2 <= 0.7){
                gen_y *= -1;
            }
            gen = encode(gen_x,gen_y);
            double r = ran.nextDouble();
            /*
            if(r >= 0.5){
                rc.setFlag(gen);
            } else {

             */
            MapLocation param1 = decode(rc,precomputed_lattice_codes[turn_ptr%(precomputed_lattice_codes.length)]);
            MapLocation param2 = decode(rc, CURRENT_EC_LOCATION);
            while(distance(param1,param2) < CURRENT_EC_DIST) {
                ++turn_ptr;
            }
            rc.setFlag(precomputed_lattice_codes[turn_ptr % (precomputed_lattice_codes.length)]);
            //}
            //rc.setFlag(precomputed_lattice_codes[turn_ptr % (precomputed_lattice_codes.length)]);
            POL_DESTINATION = decode(rc, precomputed_lattice_codes[turn_ptr % (precomputed_lattice_codes.length)]);
            ++turn_ptr;
        }
        CURRENT_ALLOCATED_REGION = my_current_region(rc.getFlag(rc.getID()));
        System.out.println(rc.getFlag(rc.getID()));
        MapLocation scatter = decode(rc, rc.getFlag(rc.getID()));
        //take advantage of muckraker property of converging on neutral ECs (choose farthest muck)
        int farth_muck = 0;
        MapLocation tmp_scatter = new MapLocation(scatter.x,scatter.y);

        if(turnCount >= 350 && distance(HQ_THAT_SPAWNED_ME,rc.getLocation()) >= 16) {
            for (RobotInfo r : rc.senseNearbyRobots()) {
                if(r.type==RobotType.MUCKRAKER && r.team==my_team){
                    farth_muck = Math.max(farth_muck, distance(r.location,rc.getLocation()));
                    int farth_flag = rc.getFlag(r.ID);
                    boolean one = farth_flag == MUCKRAKER_ENEMY_OR_NEUTRAL_EC_SURROUND ? true : false;
                    if(farth_muck == distance(r.location,rc.getLocation()) && one) {
                        tmp_scatter = r.location;
                    } else {
                        String dec = bin_conv(rc.getFlag(r.ID) - 32);
                        int cv = dec_conv(dec);
                        if(cv != CURRENT_EC_LOCATION){
                            CURRENT_EC_LOCATION = cv;
                            MapLocation newd1 = decode(rc,CURRENT_EC_LOCATION);
                            CURRENT_EC_DIST = distance(newd1,rc.getLocation());
                        }
                    }
                }
            }
            scatter = tmp_scatter;
        }
        for(RobotInfo r : rc.senseNearbyRobots()){
            Team enemy = (my_team==Team.A ? Team.B : Team.A);
            if(r.team == Team.NEUTRAL){
                navigating_to_empower=true;
                scatter = r.location;
            } else if(r.team == enemy){
                scatter = r.location;
                navigating_to_empower=true;
            }
        }


        System.out.println("TARGET POSITION: " + scatter.x + " " + scatter.y);
        rc.setFlag(encode(scatter.x,scatter.y));//in case where we use farthest muck heuristic
        BugNav b = new BugNav(rc.getLocation(), scatter);
        boolean val = b.path(rc);

        if(val){
            int multiplier = (turnCount <= 500 ? POL_RANDOM_MULTIPLIER : (POL_RANDOM_MULTIPLIER/2) );
            int gen_x = (int)(Math.random() * (multiplier) ) + HQ_THAT_SPAWNED_ME.x;
            int gen_y = (int)(Math.random() * multiplier)  + HQ_THAT_SPAWNED_ME.y;
            int gen_rand = (int)(Math.random() * precomputed_lattice_codes.length);
            double seed1 = Math.random();
            double seed2 = Math.random();
            int gen = 0;
            if(seed1 <= 0.3){
                gen_x *= -1;
            }
            if(seed2 <= 0.7){
                gen_y *= -1;
            }
            gen = encode(gen_x,gen_y);
            double r = ran.nextDouble();
            MapLocation param1 = decode(rc,precomputed_lattice_codes[turn_ptr%(precomputed_lattice_codes.length)]);
            MapLocation param2 = decode(rc, CURRENT_EC_LOCATION);
            //heuristic on max distance from current considered EC
            while(distance(param1,param2) < CURRENT_EC_DIST) {
                ++turn_ptr;
            }
            //TODO: add heuristic for
            rc.setFlag(precomputed_lattice_codes[turn_ptr % (precomputed_lattice_codes.length)]);
            //}
            POL_DESTINATION = decode(rc, precomputed_lattice_codes[turn_ptr % (precomputed_lattice_codes.length)]);
        } else if(val && rc.canSenseRobot(raw_ec_id.get(0))){
            rc.setFlag(1 << 9);
        }
    }

    public static void act(RobotController rc) throws GameActionException{
        //politicians which scout the map to determine size
        if(my_team == null){
            my_team = rc.getTeam();
        }
        if(HQ_THAT_SPAWNED_ME==null){
            for(RobotInfo r : rc.senseNearbyRobots()){
                if(r.type==RobotType.ENLIGHTENMENT_CENTER && r.team==my_team){
                    HQ_THAT_SPAWNED_ME=r.location;
                    break;
                }
            }
        }


        if(SCOUT_MAP_SIZE){
            System.out.println("SCOUT");
            discern_map(rc);
        } else if(SCOUT_NEUTRAL_HQ){
            System.out.println("CONTROL");
            scout_neutral_hq(rc);
        }
        //BugNav.path(new MapLocation(20,20));
    }
    public static void commit() throws GameActionException{

    }

}
