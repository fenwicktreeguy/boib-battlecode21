package presprint;
import java.util.*;
import battlecode.common.*;

public strictfp class RobotPlayer{

    //NOTE: FIND WAY TO DYNAMICALLY MAINTAIN EC IDS, EVEN IF ONE IS LOST(done,also maintains compressed map info)

    static RobotController rc;

    static final RobotType[] spawnableRobot = {
            RobotType.POLITICIAN,
            RobotType.SLANDERER,
            RobotType.MUCKRAKER,
    };
    public static int[] SLANDERER_RADII = {5,12,15}; //by default let it be
    static Direction[] sub = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
    public static ArrayList<int[]> potential_build_orders = new ArrayList<int[]>(5);
    //1 is a politician, 0 is a muckraker, and 2 is a slanderer
    public static final int[] build_order = {1,1,1,1,2,2,2,2,0,0};
    public static final int[] build_order_two = {1,1,1,1,1,2,2,2,2,0,0,0};
    public static final int[] build_order_three = {1,1,2,2,0,0,0,0};
    public static final int[] build_order_four = {0,0,0,0,0,1,1,2};
    public static final int[] build_order_five = {1,1,1,1,1,0,0,0,2,2};


    public static void initialize_build_orders(){
        potential_build_orders.add(build_order);
        potential_build_orders.add(build_order_two);
        potential_build_orders.add(build_order_three);
        potential_build_orders.add(build_order_four);
        potential_build_orders.add(build_order_five);
    }


    public static int squared_dist(MapLocation one, MapLocation two){
        return (int)(Math.pow(one.x-two.x,2)) + (int)(Math.pow(one.y-two.y,2));
    }

    static MapLocation HQ_THAT_SPAWNED_ME;

    MapLocation[] index_to_position;
    //every 6 turns, we try to spawn 3 politicians, 2 muckrakers, and 1 slanderer,
    //after we have discerned
    static Team my_team;
    //stores precomputed information about the slanderer formations
    public static HashMap<Integer, ArrayList<SortedPair> > slanderer_circles;
    public static ArrayList<MapLocation> pol_centers;
    public static ArrayList<Integer> ec_id;//stores compressed information about the map locations
    public static ArrayList<Integer> raw_ec_id; //stores actual ID values
    public static ArrayList<Integer> pol_id;
    public static ArrayList<Integer> potential_enemy_ecs;
    public static MapLocation[] decode;
    public static double BID_PERCENTAGE = 0.1;
    public static int PREV_INFLUENCE = 0;
    //storing map information/dimensions
    static int OFFSET_X;
    static int OFFSET_Y;
    static int MAP_X, MAP_Y;
    static int MAP_NORTH, MAP_SOUTH, MAP_EAST, MAP_WEST;
    static int MAP_TRAV;
    static int turnCount;
    static int turn_ptr;

    //for discerning map size(obsolete for the moment)
    static boolean RETURN_NATIVE_HQ;
    static Direction discern_dir, opp_dir;
    static boolean set_discern;

    //set different statuses for different game mutators
    static boolean SCOUT_MAP_SIZE, SCOUT_NEUTRAL_HQ, SCOUT_ENEMY_MUCKRAKER;
    static int[] successful_keys = { (1 << 20), (1 << 21), (1 << 22), (1 << 23) };
    static int TOGGLE_POLITICIAN_MODE = (1 << 12);
    static int SLANDERER_AND_MUCKRAKER_SWARM = (1 << 13);
    static int TRANSMIT_NEUTRAL_HQ_INFO = (1 << 9);
    static int key_ptr;

    //storing pertinent enlightenment center information
    static ArrayList<MapLocation> swarm_locs;
    static int swarm_ptr;

    //set statuses for slanderer horde
    static boolean INITIATE_SLANDERER_HORDE;
    static boolean SUCCESSFUL_SLANDERER_HORDE;
    static boolean IN_SLANDERER_HORDE;
    static int SLANDERER_PTR;
    static int SLANDERER_COOLDOWN;


    static int[] politician_valid_health = {48,60,72,84};
    static int POL_RANDOM_MULTIPLIER = 2048;
    static int SLANDERER_HEALTH_CAP = 50;
    static int MUCK_HEALTH_CAP = 1;
    static int MIN_TROOPS = 180;
    static int MIN_INFLUENCE = 3500;



    public static MapLocation decode(int value){
        if(HQ_THAT_SPAWNED_ME != null) {
            for (int i = HQ_THAT_SPAWNED_ME.x - 64; i >= HQ_THAT_SPAWNED_ME.x + 64; i++) {
                for (int j = HQ_THAT_SPAWNED_ME.y-64; i >= HQ_THAT_SPAWNED_ME.y+64; j++){
                    if(encode(i,j)==value){
                        return new MapLocation(i,j);
                    }
                }
            }
        }
        return HQ_THAT_SPAWNED_ME;
    }
    public static int encode(int x, int y){
        return 128 * ( (x)%128) + (y)%128;
    }

    /*

    public static void encode_position(int HQ_X, int HQ_Y){
        for(int i = HQ_X - 64; i <= HQ_X + 64; i++){
            for(int j = HQ_Y - 64; j <= HQ_Y + 64; j++){
                decode[128 * ( (i+j)%128) + ( (i+j)%128)] = new MapLocation(i,j);
                //System.out.println(i + " " + j);
            }
        }
    }

     */

    public static int find_compressed(MapLocation comp){
        for(int i = 0; i < decode.length; i++){
            if(decode[i].equals(comp)){
                return i;
            }
        }
        return -1;
    }
    //based on information revealed in 01/09/21 lecture

    public static int distance(MapLocation one, MapLocation two){
        return (int)(Math.pow((one.x-two.x),2) + Math.pow((one.y-two.y),2));
    }

    public static Direction[] correspond = {Direction.WEST,
            Direction.EAST,
            Direction.NORTH,
            Direction.SOUTH,
            Direction.NORTHWEST,
            Direction.NORTHEAST,
            Direction.SOUTHWEST,
            Direction.NORTHWEST};

    //make sure to pad the char array so it represents 2^6
    public static String bin_conv(int amt){
        int cur_b = (int)( (double)(Math.log(amt)) / (double)(Math.log(2)));
        int f_b = (int)(Math.floor(cur_b)) + 1;
        System.out.println(f_b);
        char[] ans = new char[f_b];
        for(int i = 0; i < f_b; i++){
            ans[i] = '0';
        }
        while(amt > 0){
            System.out.println(f_b);
            if( (1 << f_b)  <= amt){
                amt -= (1 << f_b);
                ans[cur_b-f_b] = '1';
            };
            --f_b;
        }
        String vl = String.valueOf(ans);
        return vl;
    }

    public static int dec_conv(String bitString){
        int ans = 0;
        for(int i = 0; i < bitString.length(); i++){
            ans += (1 << (bitString.length() - 1 - i) );
        }
        return ans;
    }


    static final Direction[] directions = {
            Direction.NORTH,
            Direction.NORTHEAST,
            Direction.EAST,
            Direction.SOUTHEAST,
            Direction.SOUTH,
            Direction.SOUTHWEST,
            Direction.WEST,
            Direction.NORTHWEST,
    };


    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * If this method returns, the robot dies!
     **/
    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {

        // This is the RobotController object. You use it to perform actions from this robot,
        // and to get information on its current status.
        RobotPlayer.rc = rc;
        raw_ec_id = new ArrayList<Integer>();
        turnCount = 0;
        OFFSET_X=0;
        OFFSET_Y=0;
        MAP_TRAV = 0;
        MAP_NORTH=-1;
        MAP_SOUTH=-1;
        MAP_EAST=-1;
        MAP_WEST=-1;
        pol_centers = new ArrayList<MapLocation>();
        potential_enemy_ecs = new ArrayList<Integer>();
        slanderer_circles = new HashMap<Integer, ArrayList<SortedPair> >();
        ec_id = new ArrayList<Integer>();
        pol_id = new ArrayList<Integer>();
        my_team = null;
        RETURN_NATIVE_HQ = false;
        INITIATE_SLANDERER_HORDE=false;
        discern_dir = null;
        set_discern=false;
        opp_dir=null;
        SCOUT_MAP_SIZE=false;
        SCOUT_NEUTRAL_HQ=true;
        SCOUT_ENEMY_MUCKRAKER=false;
        SUCCESSFUL_SLANDERER_HORDE=false;
        turn_ptr=0;
        key_ptr=0;
        SLANDERER_PTR=0;
        SLANDERER_COOLDOWN=0;
        swarm_locs = new ArrayList<MapLocation>();
        swarm_ptr=0;
        decode = new MapLocation[17000];
        BotSlanderer.populate();
        initialize_build_orders();
        HQ_THAT_SPAWNED_ME=null;
        if(turnCount==0 && rc.getType() ==RobotType.ENLIGHTENMENT_CENTER) {
            //encode_position(rc.getLocation().x, rc.getLocation().y);
        }
        System.out.println(rc.getLocation().x + " " + rc.getLocation().y);
        System.out.println("SUCCESS");

        System.out.println("I'm a " + rc.getType() + " and I just got created!");

        while (true) {
            turnCount += 1;
            // Try/catch blocks stop unhandled exceptions, which cause your robot to freeze
            try {
                // Here, we've separated the controls into a different method for each RobotType.
                // You may rewrite this into your own control structure if you wish.
                System.out.println("I'm a " + rc.getType() + "! Location " + rc.getLocation());
                switch (rc.getType()) {
                    case ENLIGHTENMENT_CENTER:
                        System.out.println("one");
                        BotCenter.construct(rc);
                        break;
                    case POLITICIAN:
                        System.out.println("two");
                        BotPolitician.act(rc);
                        break;
                    case SLANDERER:
                        System.out.println("three");
                        BotSlanderer.encircle(rc);
                        break;
                    case MUCKRAKER:
                        System.out.println("four");
                        BotMuckraker.rake(rc);
                        break;
                }

                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                System.out.println(rc.getType() + " Exception");
                e.printStackTrace();
            }
        }
    }

    static void runEnlightenmentCenter() throws GameActionException {
        RobotType toBuild = randomSpawnableRobotType();
        int influence = 50;
        for (Direction dir : directions) {
            if (rc.canBuildRobot(toBuild, dir, influence)) {
                rc.buildRobot(toBuild, dir, influence);
            } else {
                break;
            }
        }
    }

    static void runPolitician() throws GameActionException {
        Team enemy = rc.getTeam().opponent();
        int actionRadius = rc.getType().actionRadiusSquared;
        RobotInfo[] attackable = rc.senseNearbyRobots(actionRadius, enemy);
        if (attackable.length != 0 && rc.canEmpower(actionRadius)) {
            System.out.println("empowering...");
            rc.empower(actionRadius);
            System.out.println("empowered");
            return;
        }
        if (tryMove(randomDirection()))
            System.out.println("I moved!");
    }

    static void runSlanderer() throws GameActionException {
        if (tryMove(randomDirection()))
            System.out.println("I moved!");
    }

    static void runMuckraker() throws GameActionException {
        Team enemy = rc.getTeam().opponent();
        int actionRadius = rc.getType().actionRadiusSquared;
        for (RobotInfo robot : rc.senseNearbyRobots(actionRadius, enemy)) {
            if (robot.type.canBeExposed()) {
                // It's a slanderer... go get them!
                if (rc.canExpose(robot.location)) {
                    System.out.println("e x p o s e d");
                    rc.expose(robot.location);
                    return;
                }
            }
        }
        if (tryMove(randomDirection()))
            System.out.println("I moved!");
    }

    /**
     * Returns a random Direction.
     *
     * @return a random Direction
     */
    static Direction randomDirection() {
        return directions[(int) (Math.random() * directions.length)];
    }

    /**
     * Returns a random spawnable RobotType
     *
     * @return a random RobotType
     */
    static RobotType randomSpawnableRobotType() {
        return spawnableRobot[(int) (Math.random() * spawnableRobot.length)];
    }

    /**
     * Attempts to move in a given direction.
     *
     * @param dir The intended direction of movement
     * @return true if a move was performed
     * @throws GameActionException
     */
    static boolean tryMove(Direction dir) throws GameActionException {
        System.out.println("I am trying to move " + dir + "; " + rc.isReady() + " " + rc.getCooldownTurns() + " " + rc.canMove(dir));
        if (rc.canMove(dir)) {
            rc.move(dir);
            return true;
        } else return false;
    }
}
