package Iteration3;

import battlecode.common.*;

import java.util.Random;

public class TowerLogic {
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

    // Define thresholds for game phases
    static final int EARLY_GAME_TURNS = 200;   // Example: First 200 turns are early game
    static final int MID_GAME_TURNS = 1000;   // Example: Turns 201-1000 are mid-game
    static final int LATE_GAME_TURNS = 1500;  // Example: Turns beyond 1000 are late game
    private static boolean isDefault = true;
    private static boolean isRuinFound = false;
    private static boolean isEnemyTowerFound = false;
    private static boolean isMoveToSpecificLocation = false;
    private static boolean isNeedMopper = false;
    private static boolean isAttack = false;

    public static void run(RobotController rc) throws GameActionException {
        int currentTurn = rc.getRoundNum(); // Get the current game turn
        System.out.println("Current Turn: " + currentTurn);

        // Determine the game phase
        String gamePhase = determineGamePhase(currentTurn);

        switch (gamePhase) {
            case "EARLY_GAME":
                runEarlyGame(rc);
                break;
            case "MID_GAME":
                runMidGame(rc);
                break;
            case "LATE_GAME":
                runLateGame(rc);
                break;
        }
    }

    private static String determineGamePhase(int currentTurn) {
        if (currentTurn <= EARLY_GAME_TURNS) {
            return "EARLY_GAME";
        } else if (currentTurn <= MID_GAME_TURNS) {
            return "MID_GAME";
        } else {
            return "LATE_GAME";
        }
    }

    // Boolean flags to track whether the robots have been spawned
    private static boolean mopperSpawnedYet = false;
    private static boolean soldierSpawnedYet = false;

    private static void runEarlyGame(RobotController rc) throws GameActionException{
        System.out.println("Running Early Game Logic");
        Direction dir = directions[rng.nextInt(directions.length)];
        MapLocation nextLoc = rc.getLocation().add(dir);

        // Spawn Mopper if it hasn't been spawned yet
        if (!mopperSpawnedYet && rc.canBuildRobot(UnitType.MOPPER, nextLoc)){
            buildRobot(rc, UnitType.MOPPER);
            mopperSpawnedYet = true;

            // Command the Mopper to stay put
            int stayPutCommand = 1; // Command: Stay put
            sendMessageToRobots(rc, "EARLY_GAME", stayPutCommand, null);
            System.out.println("Spawned Mopper and commanded it to stay put.");
            return; // Exit early to avoid spawning Soldier in the same turn
        }

        // Spawn Soldier if it hasn't been spawned yet
        if (mopperSpawnedYet && !soldierSpawnedYet && rc.canBuildRobot(UnitType.SOLDIER, nextLoc)) {
            buildRobot(rc, UnitType.SOLDIER);
            soldierSpawnedYet = true;

            // Command the Soldier to stay put
            int stayPutCommand = 1; // Command: Stay put
            sendMessageToRobots(rc, "EARLY_GAME", stayPutCommand, null);
            System.out.println("Spawned Soldier and commanded it to stay put.");
            return; // Exit early to avoid sending movement commands prematurely
        }

        // Once both robots are spawned, command them to move to a target location
        if (mopperSpawnedYet && soldierSpawnedYet) {
            int moveCommand = 3; // Command: Move to target location
            MapLocation target = new MapLocation(5, 5); // Example target location
            sendMessageToRobots(rc, "EARLY_GAME", moveCommand, target);
            System.out.println("Commanded robots to move to target location.");
            mopperSpawnedYet = false; soldierSpawnedYet = false;
        }

        attackNearbyEnemies(rc);
    }


    private static void runMidGame(RobotController rc) throws GameActionException {
        //System.out.println("Running Mid Game Logic");

        // Balance between attacking and expanding
//        if (rng.nextBoolean()) {
//            buildRobot(rc, UnitType.MOPPER); // Example: Build moppers for area control
//        } else {
//            attackNearbyEnemies(rc);
//        }
    }

    private static void runLateGame(RobotController rc) throws GameActionException {
        //System.out.println("Running Late Game Logic");

        // Focus on strong units and defending
//        buildRobot(rc, UnitType.SPLASHER); // Example: Build splashers for AoE attacks
//        attackNearbyEnemies(rc); // Simultaneously attack enemies
    }

    private static void buildRobot(RobotController rc, UnitType unitType) throws GameActionException {
        Direction dir = directions[rng.nextInt(directions.length)];
        MapLocation nextLoc = rc.getLocation().add(dir);

        if (rc.canBuildRobot(unitType, nextLoc)) {
            rc.buildRobot(unitType, nextLoc);
            System.out.println("BUILT A " + unitType);
        }
    }

    private static void attackNearbyEnemies(RobotController rc) throws GameActionException {
        Team enemy = rc.getTeam().opponent();
        RobotInfo[] nearbyEnemies = rc.senseNearbyRobots(-1, enemy); // Sense all nearby robots.

        for (RobotInfo enemyRobot : nearbyEnemies) {
            MapLocation enemyLoc = enemyRobot.getLocation();
            if (rc.canAttack(enemyLoc)) {
                rc.attack(enemyLoc); // Attack the enemy.
                System.out.println("Tower attacked enemy at " + enemyLoc);
                break; // Attack one enemy per turn.
            }
        }
    }

//    private static void sendMessageToRobots(RobotController rc, String gamePhase) throws GameActionException {
//        MapLocation towerLoc = rc.getLocation(); // Tower's location
//        // commands start from the 12th bit onwards, bits 1 to 12 are for coordinates.
//        // 2^12=4096: stay put until spawned 1 mopper + 1 soldier within EARLY_GAME_TURNS
//        // 2^12+2^13=12288: once the combo is done, send k, which is the command to
//        // signal the 2 robots to go to a location, the x-y coordinate uses the
//        // first 12 bits, so k = (x-coodinate << 6) + y-coordinate + 4097, again
//        // for within EARLY_GAME_TURNS
//
//        int messageContent = 1;
//
//        // Find nearby robots to send the message to
//        RobotInfo[] nearbyAllies = rc.senseNearbyRobots(-1, rc.getTeam());
//
//        for (RobotInfo ally : nearbyAllies){
//            MapLocation allyLoc = ally.getLocation();
//
//            // Check if the message can be sent to this robot
//            if (rc.canSendMessage(allyLoc, messageContent)){
//                rc.sendMessage(allyLoc, messageContent);
//                System.out.println("Sent message to " + allyLoc + ": " + gamePhase);
//            }
//        }
//    }

    private static void sendMessageToRobots(RobotController rc, String gamePhase, int command, MapLocation targetLoc) throws GameActionException {
        int x = targetLoc != null ? targetLoc.x : 0; // Default x-coordinate if no target
        int y = targetLoc != null ? targetLoc.y : 0; // Default y-coordinate if no target

        // Encode coordinates and command into messageContent
        int messageContent = (x << 6) | y | (command << 12);

        // Find nearby robots to send the message to
        RobotInfo[] nearbyAllies = rc.senseNearbyRobots(-1, rc.getTeam());

        for (RobotInfo ally : nearbyAllies) {
            MapLocation allyLoc = ally.getLocation();

            // Check if the message can be sent to this robot
            if (rc.canSendMessage(allyLoc, messageContent)) {
                rc.sendMessage(allyLoc, messageContent);
                System.out.println("Sent message to " + allyLoc + ": " + messageContent);
            }
        }
    }

}
