package v1;

import battlecode.common.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;


public class Robot {
    class OptCode {
        static final int NEEDMOPPER = 0;
    }   

    /**
     * Core robot class. Contains all necessary info for other classes and all high level instructions.
     */
    static final int LOWPAINTTRESHHOLD = 20;
    static final int PAINTTOTAKE = -20;
    static final int PAINTTOGIVE = 20;
    static final Random rng = new Random(6147);

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

    static RobotController rc;

    static Set<MapLocation> towersPos = new HashSet<>();
    
    static boolean lowPaintFlag;
    static boolean friendMopperFound;
   

    public Robot(RobotController _rc) throws GameActionException {
        this.rc = _rc;
    }

    //instructions run at the beginning of each turn
    void beginTurn() throws GameActionException {

    }

    //Instructions at the end of each turn
    void endTurn() throws GameActionException {
       
    }

    //Core turn method
    void runTurn() throws GameActionException {

    }


    int calculatePaintPercentage(RobotInfo robot) {
        return (int)(((double)robot.paintAmount / robot.type.paintCapacity) * 100);
    }
}