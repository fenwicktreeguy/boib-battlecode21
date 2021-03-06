package presprint;
import java.util.*;
import battlecode.common.*;

class SortedPair{
    MapLocation loc;
    int dist;
    public SortedPair(MapLocation loc, int dist){
        this.loc=loc;
        this.dist=dist;
    }
}

class PairComparator implements Comparator{
    public int compare(Object o1, Object o2){
        SortedPair s1=(SortedPair)o1;
        SortedPair s2=(SortedPair)o2;

        if(s1.dist < s2.dist){
            return 1;
        } else if(s1.dist==s2.dist){
            return 0;
        }
        return -1;
    }
}

public class BotSlanderer extends RobotPlayer{
    public static int turn_ptr = (int)(Math.random() * 289);
    public static boolean TRANSMIT_MESSAGE = false;
    public static MapLocation SLAND_DESTINATION;

    public static int manhattan_distance(MapLocation one, MapLocation two){
        return (int)(Math.abs(one.x-two.x)) + (int)(Math.abs(one.y-two.y));
    }

    public static void region_and_location_parser(RobotController rc, int code) throws GameActionException{
        String bincode = bin_conv(code);
        int len = bin_conv(encode(SLAND_DESTINATION.x,SLAND_DESTINATION.y)).length();
        String one = bincode.substring(0,len);
        String two = bincode.substring(len);
        TEMP_POSITION = dec_conv(one);
        TEMP_REGION = dec_conv(two);
    }
    public static void populate_top(int i, ArrayList<SortedPair> m) {
        int y_coord = 0;
        boolean flg = false;
        int cnt = 0;
        for (int j = i; j >= -i; j--) {
            ++cnt;
            MapLocation mp = new MapLocation(j, y_coord);
            m.add(new SortedPair(mp,manhattan_distance(new MapLocation(0,0), mp)));
            if (y_coord == i) {
                flg=true;
            }
            if(!flg){
                y_coord++;
            }else{
                y_coord--;
            }
        }
    }
    public static void populate_low(int i, ArrayList<SortedPair> m) {
        int y_coord = -1;
        int cnt = 0;
        boolean flg = false;
        for (int j = -i+1; j <= i; j++) {
            ++cnt;
            MapLocation mp = new MapLocation(j, y_coord);
            m.add(new SortedPair(mp,manhattan_distance(new MapLocation(0,0), mp)));
            if(y_coord <= -i){
                flg=true;
            }
            if(!flg){
                y_coord--;
            }else{
                y_coord++;
            }
        }
    }

    public static void populate(){
        for (Integer i : SLANDERER_RADII) {
            ArrayList<SortedPair> m = new ArrayList<SortedPair>();
            //for(int rd = i ; i >= 1; i--) {
            populate_top(i, m);
            populate_low(i, m);

            if(!slanderer_circles.containsKey(i)) {
                slanderer_circles.put(i, m);
            }
        }
        for(Integer i : SLANDERER_RADII){
            slanderer_circles.get(i).remove(slanderer_circles.get(i).size()-1);
            for(SortedPair p : slanderer_circles.get(i)){
                System.out.println(p.loc.x + " " + p.loc.y);
            }
            System.out.println("-----------------------------------------");
        }
    }
    public static void init_encircle(RobotController prc) throws GameActionException{
        //make sure to modify slanderer_circles to accomodate for roads
        if(my_team==null){
            my_team=prc.getTeam();
        }
        for(RobotInfo rb : prc.senseNearbyRobots()){
            if(rb.team==my_team && rb.type==RobotType.ENLIGHTENMENT_CENTER){
                int hq_flag = prc.getFlag(rb.ID);
                if(hq_flag < slanderer_circles.get(5).size()){
                    MapLocation dest = slanderer_circles.get(5).get(hq_flag).loc;
                    dest = new MapLocation(dest.x+rb.location.x, dest.y+rb.location.y);
                    System.out.println("DEST X:  " + dest.x + " " + "DEST Y: " + dest.y);
                    BugNav b = new BugNav(prc.getLocation(), dest);
                    b.path(prc);
                }
            }
        }
    }

    public static void mindful_swarm(RobotController rc) throws GameActionException{
        //attempting to get an emergent behavior for examining the map and converting neutral ECs
        turn_ptr += 2;
        Random ran = new Random(rc.getID());
        if(ran.nextDouble() >= 0.5){
            turn_ptr += (int)(Math.random() * 289);
            turn_ptr %= 1e18;
        }
        System.out.println("CURRENT TURN POINTER: " + turn_ptr);
        if(my_team==null){
            my_team=rc.getTeam();
        }
        if(!LATTICE_COMPUTED){
            precompute_lattice_positions(rc);
            LATTICE_COMPUTED=true;
        }
        if(rc.getFlag(rc.getID())==0){
            int gen_x = (int)(Math.random() * (POL_RANDOM_MULTIPLIER/4) ) + HQ_THAT_SPAWNED_ME.x;
            int gen_y = (int)(Math.random() * (POL_RANDOM_MULTIPLIER/4) ) + HQ_THAT_SPAWNED_ME.y;
            double seed1 = Math.random();
            double seed2 = Math.random();
            int gen_rand = (int)(Math.random() * precomputed_lattice_codes.length);
            int gen = 0;
            if(seed1 <= 0.4){
                gen_x *= -1;
            }
            if(seed2 <= 0.6){
                gen_y *= -1;
            }
            gen = encode(gen_x,gen_y);
            double r = ran.nextDouble();
            if(r >= 0.5){
                rc.setFlag(gen);
            } else {
                rc.setFlag(precomputed_lattice_codes[turn_ptr % (precomputed_lattice_codes.length)]);
            }
            SLAND_DESTINATION = decode(rc,precomputed_lattice_codes[turn_ptr % (precomputed_lattice_codes.length)]);
            ++turn_ptr;
            //rc.setFlag(gen);
        }

        //as a rule of thumb, neutral hqs are more important than enemy hqs, since the former gives leverage for control,
        //and the other is mainly for efficient defense (which is a fall-back plan)
        for(RobotInfo r : rc.senseNearbyRobots()){
            if(r.type == RobotType.ENLIGHTENMENT_CENTER && r.team == my_team) {
                int identif =  128 * ( (r.location.x + r.location.y) % 128) + ( (r.location.x+r.location.y)%128);
                if (!ec_id.contains(identif)) {
                    ec_id.add(identif);
                    raw_ec_id.add(r.ID);
                }
                if(HQ_THAT_SPAWNED_ME==null){
                    HQ_THAT_SPAWNED_ME=r.location;
                    HQ_ENCODED_POSITION = encode(r.location.x,r.location.y);
                }
            } else if(r.type== RobotType.ENLIGHTENMENT_CENTER && r.team == Team.NEUTRAL){
                int identif =  128 * ( (r.location.x + r.location.y) % 128) + ( (r.location.x+r.location.y)%128);
                ec_id.add(identif);
                raw_ec_id.add(r.ID);
                rc.setFlag(ec_id.get(0));
                //send muckraker message along chain
            } else if(r.type==RobotType.MUCKRAKER && r.team == my_team){
                //TODO: add clustering behavior to avoid muckrakers
            }
        }
        CURRENT_ALLOCATED_REGION = my_current_region(rc.getFlag(rc.getID()));
        /*
        int ENEMY_NW = 0;
        int ENEMY_NE = 0;
        int ENEMY_SW = 0;
        int ENEMY_SE = 0;
        for(RobotInfo r : rc.senseNearbyRobots()){
            if(r.type==RobotType.MUCKRAKER && r.team != my_team){
                boolean one = (r.location.x > r.location)
            }
        }
         */
        MapLocation scatter = decode(rc, rc.getFlag(rc.getID()));
        for(RobotInfo r : rc.senseNearbyRobots()){
            if(r.type==RobotType.ENLIGHTENMENT_CENTER && r.team==Team.NEUTRAL){
                scatter = r.location;
            }
            if(r.type==RobotType.MUCKRAKER && r.team != my_team){
                int x_diff = r.location.x - rc.getLocation().x;
                int y_diff = r.location.y-rc.getLocation().y;
                scatter = new MapLocation(rc.getLocation().x-x_diff,rc.getLocation().y-y_diff);
                break;
            }
        }
        System.out.println("Target point: " + scatter.x + " " + scatter.y);
        BugNav b = new BugNav(rc.getLocation(), scatter);
        boolean val = b.path(rc);
        if(val){
            System.out.println("SUCCESSFUL NAVIGATION!");
            int gen_x = (int)(Math.random() * (POL_RANDOM_MULTIPLIER/4) ) + HQ_THAT_SPAWNED_ME.x;
            int gen_y = (int)(Math.random() * (POL_RANDOM_MULTIPLIER/4))  + HQ_THAT_SPAWNED_ME.y;
            double seed1 = Math.random();
            double seed2 = Math.random();
            int gen_rand = (int)(Math.random() * precomputed_lattice_codes.length);
            int gen = 0;
            if(seed1 <= 0.4){
                gen_x *= -1;
            }
            if(seed2 <= 0.6){
                gen_y *= -1;
            }
            gen = encode(gen_x,gen_y);
            double r = ran.nextDouble();
            if(r >= 0.5){
                rc.setFlag(gen);
            } else {
                rc.setFlag(precomputed_lattice_codes[turn_ptr % (precomputed_lattice_codes.length)]);
            }
            //rc.setFlag(precomputed_lattice_codes[gen_rand]);
            rc.setFlag(precomputed_lattice_codes[turn_ptr % (precomputed_lattice_codes.length)]);
            SLAND_DESTINATION = decode(rc,precomputed_lattice_codes[turn_ptr % (precomputed_lattice_codes.length)]);
            ++turn_ptr;
        }else if(val && rc.canSenseRobot(raw_ec_id.get(0))){

        }

    }

    public static void encircle(RobotController prc) throws GameActionException{
        if(my_team==null){
            my_team=prc.getTeam();
        }
        if(HQ_THAT_SPAWNED_ME==null){
            for(RobotInfo r : prc.senseNearbyRobots()){
                if(r.type==RobotType.ENLIGHTENMENT_CENTER && r.team==my_team){
                    HQ_THAT_SPAWNED_ME=r.location;
                    break;
                }
            }
        }
        if(!SCOUT_MAP_SIZE && INITIATE_SLANDERER_HORDE) {
            init_encircle(prc);
            /*
            int idx = rc.getFlag(rc.getID());
            BugNav b = new BugNav(prc.getLocation(), slanderers.get(8).get(idx));
            b.path(prc);
             */
        } else {
            mindful_swarm(rc);
        }

    }
}
