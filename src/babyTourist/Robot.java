package babyTourist;

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
    static int isSuicideRobot = 0;
    static int isDefenceRobot = 0;

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

    // void rush() throws GameActionException {
    //     Team enemy = rc.getTeam().opponent();

    //     MapLocation myLocation = rc.getLocation();
    //     RobotInfo[] nearbyRobots = rc.senseNearbyRobots(-1, enemy);
    //     rc.setIndicatorString("Number of robots: " + nearbyRobots.length);
    //     MapLocation[] enemyTowers = Arrays.stream(nearbyRobots)
    //                 .filter(robot -> robot.getType().isTowerType())
    //                 .map(RobotInfo::getLocation)
    //                 .toArray(MapLocation[]::new);

    //     if (enemyTowers.length > 0) {
    //         MapLocation target = enemyTowers[0];
    //         Direction dirToEnemy = myLocation.directionTo(target);

    //         if (rc.canMove(dirToEnemy)) {
    //             rc.move(dirToEnemy);
    //         }

    //         if (rc.canAttack(target)) {
    //             rc.attack(target);
    //         }
    //     }
    // }

    int calculatePaintPercentage(RobotInfo robot) {
        return (int)(((double)robot.paintAmount / robot.type.paintCapacity) * 100);
    }
}