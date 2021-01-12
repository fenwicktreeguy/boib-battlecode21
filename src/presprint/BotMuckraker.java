package presprint;
import java.util.*;
import battlecode.common.*;

public class BotMuckraker extends RobotPlayer{
    //note: always create muckrakers with 1 influence
    public static void rake(RobotController rc) throws GameActionException{

        if(my_team==null){
            my_team=rc.getTeam();
        }
        if(HQ_THAT_SPAWNED_ME==null){
            for(RobotInfo r : rc.senseNearbyRobots()){
                if(r.type==RobotType.ENLIGHTENMENT_CENTER && r.team==my_team){
                    HQ_THAT_SPAWNED_ME=r.location;
                    break;
                }
            }
        }

        System.out.println("DECODE LENGTH: " + decode.length);
        if(rc.getFlag(rc.getID())==0 || rc.getLocation().equals(decode[rc.getFlag(rc.getID())])){
            int multiplier = (turnCount <= 500 ? POL_RANDOM_MULTIPLIER : (POL_RANDOM_MULTIPLIER/2) );
            int gen_x = (int)(Math.random() * (multiplier) ) + HQ_THAT_SPAWNED_ME.x;
            int gen_y = (int)(Math.random() * (multiplier))  + HQ_THAT_SPAWNED_ME.y;
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
            rc.setFlag(gen);
        }
        MapLocation scatter = decode(rc.getFlag(rc.getID()));
        System.out.println(scatter.x + " " + scatter.y);
        for(MapLocation m : rc.detectNearbyRobots() ){
            if(rc.canExpose(m)) {
                rc.expose(m);
            }
        }
        for(RobotInfo r : rc.senseNearbyRobots()){
            if(r.type==RobotType.ENLIGHTENMENT_CENTER && r.team == my_team){
                int identif =  128 * ( (r.location.x + r.location.y) % 128) + ( (r.location.x+r.location.y)%128);
                if(!ec_id.contains(identif) ){
                    ec_id.add(identif);
                    raw_ec_id.add(r.ID);
                }
            } else if(r.type==RobotType.ENLIGHTENMENT_CENTER && r.team==my_team){
                int identif =  128 * ( (r.location.x + r.location.y) % 128) + ( (r.location.x+r.location.y)%128);
                ec_id.add(identif);
                raw_ec_id.add(r.ID);
                rc.setFlag(ec_id.get(0));
            }
        }
        BugNav b = new BugNav(rc.getLocation(),scatter);
        boolean val = b.path(rc);
        if(val){
            int multiplier = (turnCount <= 500 ? POL_RANDOM_MULTIPLIER : (POL_RANDOM_MULTIPLIER/2) );
            int gen_x = (int)(Math.random() * (multiplier) ) + HQ_THAT_SPAWNED_ME.x;
            int gen_y = (int)(Math.random() * multiplier)  + HQ_THAT_SPAWNED_ME.y;
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
            rc.setFlag(gen);
        }else if(val && rc.canSenseRobot(raw_ec_id.get(0))){

        }
    }


    public static void crunch(RobotController rc) throws GameActionException{

    }
}
