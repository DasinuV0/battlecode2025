package babyTourist;

import battlecode.common.Direction;
import battlecode.common.RobotController;
import battlecode.common.Team;

// Global class to setup all path finding algorithms that we will use
// for now only Bug Nav is implemented

public class Globals{
    public static RobotController rc;

    public static int mapWidth;
    public static int mapHeight;

    public static int myId;
    public static Team myTeam;
    public static Team opponentTeam;

    public static Direction[] allDirections = Direction.values();
    public static Direction[] adjacentDirections = {
            Direction.NORTH,
            Direction.EAST,
            Direction.SOUTH,
            Direction.WEST,
            Direction.NORTHEAST,
            Direction.SOUTHEAST,
            Direction.SOUTHWEST,
            Direction.NORTHWEST
    };

    public static void init(RobotController robotController) {
        rc = robotController;

        mapWidth = rc.getMapWidth();
        mapHeight = rc.getMapHeight();

        myId = rc.getID();
        myTeam = rc.getTeam();
        opponentTeam = myTeam.opponent();
    }
}