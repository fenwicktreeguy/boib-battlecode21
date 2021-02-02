package presprint;
import java.util.*;
import battlecode.common.*;

public class BotMuckraker extends RobotPlayer{
    //note: always create muckrakers with 1 influence
    public static int turn_ptr = (int)(Math.random() * 289);
    public static boolean TRANSMIT_MESSAGE= false;
    public static MapLocation MUCK_DESTINATION;
    public static int DESIRED_FLAG = -1;
    public static HashMap<Integer,Team> prev_team = new HashMap<Integer,Team>();
    public static MapLocation CURRENT_HQ_LOCATION;
    public static int CURRENT_EC_DIST = 0;

    public static void region_and_location_parser(RobotController rc, int code) throws GameActionException{
        String bincode = bin_conv(code);
        int len = bin_conv(encode(MUCK_DESTINATION.x,MUCK_DESTINATION.y)).length();
        String one = bincode.substring(0,len);
        String two = bincode.substring(len);
        TEMP_POSITION = dec_conv(one);
        TEMP_REGION = dec_conv(two);
    }
    public static int region_and_location_encoder(RobotController rc, MapLocation pos) throws GameActionException{
        String encode = bin_conv(encode(pos.x,pos.y)) + bin_conv(CURRENT_ALLOCATED_REGION);
        int ret = dec_conv(encode);
        return ret;
    }

    public static void rake(RobotController rc) throws GameActionException {
        turn_ptr += 17;
        Random ran = new Random(rc.getID());
        if(ran.nextDouble() >= 0.5){
            turn_ptr += (int)(Math.random() * 289);
            turn_ptr %= 1e18;
        }
        System.out.println("CURRENT TURN POINTER: " + turn_ptr);
        if (my_team == null) {
            my_team = rc.getTeam();
        }
        if (HQ_THAT_SPAWNED_ME == null) {
            for (RobotInfo r : rc.senseNearbyRobots()) {
                if (r.type == RobotType.ENLIGHTENMENT_CENTER && r.team == my_team) {
                    HQ_THAT_SPAWNED_ME = r.location;
                    HQ_ENCODED_POSITION = encode(r.location.x, r.location.y);
                    CURRENT_HQ_LOCATION = HQ_THAT_SPAWNED_ME;
                }
            }
        }
        if (!LATTICE_COMPUTED) {
            precompute_lattice_positions(rc);
            LATTICE_COMPUTED = true;
        }
        for (RobotInfo r : rc.senseNearbyRobots()) {
            if (r.type == RobotType.ENLIGHTENMENT_CENTER && r.team == my_team) {
                int identif = 128 * ((r.location.x % 128)) + ((r.location.y) % 128);
                if (!ec_id.contains(identif)) {
                    ec_id.add(identif);
                    raw_ec_id.add(r.ID);
                }
            }else if(r.type==RobotType.MUCKRAKER && r.team == my_team){
                int flag = (rc.getFlag(r.ID));
                if(flag >  ( 1 << 9) ){
                    TRANSMIT_MESSAGE = true;
                    int encode_loc = encode(r.location.x, r.location.y);
                    int g_flag = rc.getFlag(r.ID);

                }
            }
        }
        for(RobotInfo r : rc.senseNearbyRobots(30)){
            if(r.type==RobotType.ENLIGHTENMENT_CENTER && r.team != my_team){
                if(!prev_team.containsKey(encode(r.location.x,r.location.y))) {
                    ENCODED_MESSAGE = region_and_location_encoder(rc, r.location);
                    rc.setFlag(MUCKRAKER_ENEMY_OR_NEUTRAL_EC_SURROUND);
                    MUCKRAKER_NAVIGATING_BACK = true;
                    prev_team.put(encode(r.location.x, r.location.y), r.team);
                }
            }
            if(r.type==RobotType.ENLIGHTENMENT_CENTER && prev_team.get(encode(r.location.x,r.location.y)) == my_team){
                //communicate information to passing polis that this EC is taken, and update heuristic
                //for pathing away from closest converted EC
                MUCKRAKER_NAVIGATING_BACK=false;
                CURRENT_HQ_LOCATION = r.location;
                String one = bin_conv(encode(CURRENT_HQ_LOCATION.x, CURRENT_HQ_LOCATION.y) + 32);
                int COMB_DEC = dec_conv(one);
                rc.setFlag(COMB_DEC);
            }
        }
        if (rc.getFlag(rc.getID()) == 0 && RANDOM_MAP_HEURISTIC) {
            int multiplier = (turnCount <= 500 ? POL_RANDOM_MULTIPLIER : (POL_RANDOM_MULTIPLIER / 2));
            int gen_x = (int) (Math.random() * (multiplier)) + HQ_THAT_SPAWNED_ME.x;
            int gen_y = (int) (Math.random() * (multiplier)) + HQ_THAT_SPAWNED_ME.y;
            int gen_rand = (int) (Math.random() * precomputed_lattice_codes.length);
            double seed1 = Math.random();
            double seed2 = Math.random();
            int gen = 0;
            if (seed1 <= 0.3) {
                gen_x *= -1;
            }
            if (seed2 <= 0.7) {
                gen_y *= -1;
            }
            gen = encode(gen_x, gen_y);
            gen = encode(gen_x,gen_y);
            double r = ran.nextDouble();
            if(r >= 0.5){
                rc.setFlag(gen);
            } else {
                rc.setFlag(precomputed_lattice_codes[turn_ptr % (precomputed_lattice_codes.length)]);
            }
            rc.setFlag(precomputed_lattice_codes[turn_ptr % (precomputed_lattice_codes.length)]);
            MUCK_DESTINATION = decode(rc,precomputed_lattice_codes[turn_ptr % (precomputed_lattice_codes.length)]);
            ++turn_ptr;
            //rc.setFlag(gen);
        }
        CURRENT_ALLOCATED_REGION = my_current_region(rc.getFlag(rc.getID()));

        for (MapLocation m : rc.detectNearbyRobots()) {
            if (rc.canExpose(m)) {
                rc.expose(m);
            }
        }
        MapLocation norm = decode(rc,rc.getFlag(rc.getID()));
        if(MUCKRAKER_NAVIGATING_BACK){
            return;
            /*
            System.out.println("BRINGING INFORMATION BACK...");
            rc.setFlag(ENCODED_MESSAGE);
            region_and_location_parser(rc,ENCODED_MESSAGE);
            System.out.println("MESSAGE: " + ENCODED_MESSAGE);
            System.out.println(TEMP_POSITION + " " + TEMP_REGION);
            norm = decode(rc,TEMP_POSITION);
            CURRENT_ALLOCATED_REGION = TEMP_REGION;
            MUCK_DESTINATION = HQ_THAT_SPAWNED_ME;
             */
        }
        MapLocation scatter = norm;
        System.out.println("TARGET POSITION: " + scatter.x + " " + scatter.y);
        BugNav b = new BugNav(rc.getLocation(), scatter);
        boolean val = b.path(rc);
        if (val) {
            int multiplier = (turnCount <= 500 ? POL_RANDOM_MULTIPLIER : (POL_RANDOM_MULTIPLIER / 2));
            int gen_x = (int) (Math.random() * (multiplier)) + HQ_THAT_SPAWNED_ME.x;
            int gen_y = (int) (Math.random() * multiplier) + HQ_THAT_SPAWNED_ME.y;
            double seed1 = Math.random();
            double seed2 = Math.random();
            int gen_rand = (int) (Math.random() * precomputed_lattice_codes.length);
            int gen = 0;
            if (seed1 <= 0.3) {
                gen_x *= -1;
            }
            if (seed2 <= 0.7) {
                gen_y *= -1;
            }
            //rc.setFlag(precomputed_lattice_codes[gen_rand]);

            MUCK_DESTINATION = decode(rc, precomputed_lattice_codes[turn_ptr % (precomputed_lattice_codes.length)]);
            ++turn_ptr;
            gen = encode(gen_x, gen_y);
            double r = ran.nextDouble();
            while(distance( decode(rc,precomputed_lattice_codes[turn_ptr]),CURRENT_HQ_LOCATION) < CURRENT_EC_DIST) {
                ++turn_ptr;
            }
            /*
            if(r >= 0.5){
                rc.setFlag(gen);
            } else {

             */
                rc.setFlag(precomputed_lattice_codes[turn_ptr % (precomputed_lattice_codes.length)]);
            //}
            //rc.setFlag(gen);
            if(MUCKRAKER_NAVIGATING_BACK){
                MUCKRAKER_NAVIGATING_BACK=false;
            }
        } else if (val && rc.canSenseRobot(raw_ec_id.get(0))) {

        }
    }


    public static void crunch(RobotController rc) throws GameActionException{

    }

    public static void muckraker_advanced_comms(RobotController rc) throws GameActionException{

    }
}
