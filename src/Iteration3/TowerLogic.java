package Iteration3;

import battlecode.common.*;

import java.util.Random;

// commands:
// 1 = stay put
// 2 = attack location
// 4 = request paint

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
    static final int EARLY_GAME_TURNS = 900;   // Example: First 200 turns are early game
    static final int MID_GAME_TURNS = 1000;   // Example: Turns 201-1000 are mid-game
    static final int LATE_GAME_TURNS = 1500;  // Example: Turns beyond 1000 are late game
    static final int STAY_PUT_COMMAND = 1;
    static final int MOVE_TO_DAMAGED_PATTERN_COMMAND = 2;
    static final int MOVE_TO_LOCATION_COMMAND = 3;
    private static boolean isDefault = true;
    private static boolean isRuinFound = false;
    private static boolean isDamagedPatternFound = false;
    private static boolean isEnemyTowerFound = false;
    private static boolean isMoveToSpecificLocation = false;
    private static boolean isNeedMopper = false;
    private static boolean isAttack = false;

    public static void run(RobotController rc) throws GameActionException {
        int currentTurn = rc.getRoundNum(); // Get the current game turn
        //System.out.println("Current Turn: " + currentTurn);

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

    // rush phase variables
    private static int soldiersSpawned = 0;

    // Boolean flags to track whether the robots have been spawned (default command)
    private static boolean mopperSpawnedYet = false;
    private static boolean soldierSpawnedYet = false;
    private static boolean randomSoldierSpawnedYet = false;
    private static MapLocation targetLoc = new MapLocation(0,0);

    // Bolean flags to track whether 2 moppers exists in vision range
    private static boolean calledMoppersStayPutYet = false;
    private static boolean calledSoldierStayPutYet = false;



    private static void runEarlyGame(RobotController rc) throws GameActionException{
        //System.out.println("Running Early Game Logic");
        checkAndRequestPaint(rc); // check paint capacity and request for more paint if needed
        Direction dir = directions[rng.nextInt(directions.length)];
        MapLocation nextLoc = rc.getLocation().add(dir);

//        if (getMapArea(rc) <= 450 && rc.getRoundNum() < 10) {
//            MapLocation myLocation = rc.getLocation();
//            MapLocation enemyTower = Symmetry.getEnemyStartingTowerCoord(rc, myLocation);
//            Direction dirToEnemy = myLocation.directionTo(enemyTower);
//            MapLocation spawnLocation = myLocation.add(dirToEnemy);
//
//            while (soldiersSpawned < 2) {
//                if (rc.canBuildRobot(UnitType.SOLDIER, spawnLocation)) {
//                    rc.buildRobot(UnitType.SOLDIER, spawnLocation);
//                    soldiersSpawned++;
//                }
//            }
//
//            if (soldiersSpawned < 3) {
//                if (rc.canBuildRobot(UnitType.MOPPER, spawnLocation)) {
//                    rc.buildRobot(UnitType.MOPPER, spawnLocation);
//                    soldiersSpawned++;
//                }
//            }
//        }

//        if (isEnemyTowerFound){
//            if (!findMopper()){
//                buildRobotOnPaintTile(rc, UnitType.MOPPER);
//                int stayPutCommand = 1;
//                sendMessageToRobots(rc, stayPutCommand, targetLoc);
//
//            }
//
//        }
        // System.out.println(rc.getMoney());
        // always spawn a soldier at a random tile first

        handleMessages(rc);

        if (isDefault){
            if (!randomSoldierSpawnedYet){
                buildRobotOnRandomTile(rc, UnitType.SOLDIER);
                System.out.println("Spawned Soldier on a random tile");
                randomSoldierSpawnedYet = true;
                attackNearbyEnemies(rc);
                return;
            }
            // Spawn Soldier if it hasn't been spawned yet
            if (!soldierSpawnedYet){
                if (!buildRobotOnPaintTile(rc, UnitType.SOLDIER)){
                    System.out.println("No tiles found that are friendly so far, not spawning soldier");
                    attackNearbyEnemies(rc);
                    return; // Exit early to avoid setting flag to true
                }

                soldierSpawnedYet = true;

                // Command the Soldier to stay put
                sendMessageToRobots(rc, STAY_PUT_COMMAND, null, UnitType.SOLDIER, 1);
                System.out.println("Spawned Soldier on a paint tile and commanded it to stay put.");
                // return; // Exit early to avoid spawning Soldier in the same turn
            }

            // Spawn Mopper if it hasn't been spawned yet
            else if (!mopperSpawnedYet){
                if (!buildRobotOnPaintTile(rc, UnitType.MOPPER)){
                    System.out.println("No paint tile detected, not spawning mopper");
                    attackNearbyEnemies(rc);
                    return;
                }

                mopperSpawnedYet = true;

                // Command the Mopper to stay put
                sendMessageToRobots(rc, STAY_PUT_COMMAND, null, UnitType.MOPPER, 1);
                System.out.println("Spawned Mopper on a paint tile and commanded it to stay put.");
                //return; // Exit early to avoid sending movement commands prematurely
            }

            // Once both robots are spawned, command them to move to a target location
            else if (mopperSpawnedYet && soldierSpawnedYet){
                int moveCommand = 3; // Command: Move to target location
                MapLocation target = new MapLocation(27, 28); // Example target location
                sendMessageToRobots(rc, MOVE_TO_LOCATION_COMMAND, target, UnitType.MOPPER, 1);
                sendMessageToRobots(rc, MOVE_TO_LOCATION_COMMAND, target, UnitType.SOLDIER, 1);
                System.out.println("Commanded robots to move to target location.");
                mopperSpawnedYet = false; soldierSpawnedYet = false;
            }
        }
        else if (isDamagedPatternFound){
            System.out.println("Oh no! Damaged pattern is found!");
            if (!calledSoldierStayPutYet){
                sendMessageToRobots(rc, STAY_PUT_COMMAND, null, UnitType.SOLDIER, 1);
                System.out.println("One soldier is stayed put now");
                calledSoldierStayPutYet = true;
            }

            int mopperCount = countMoppersInTowerRangeOnPaint(rc);
//            int mopperCount = sendMessageToRobots(rc, STAY_PUT_COMMAND, null, UnitType.MOPPER, 2);

            if (mopperCount < 2){
                if (!calledMoppersStayPutYet){
                    sendMessageToRobots(rc, STAY_PUT_COMMAND, null, UnitType.MOPPER, mopperCount);
                    if (mopperCount > 0) calledMoppersStayPutYet = true;
                }

                if (!buildRobotOnPaintTile(rc, UnitType.MOPPER)){
                    System.out.println("Not enough paint, not spawning MOPPER");
                    attackNearbyEnemies(rc);
                    return; // Exit early to avoid sending wrong mopper message
                }
                System.out.println("Spawned MOPPER on a paint tile and commanded it to stay put.");
                int currentMopperCountAfterSpawn = countMoppersInTowerRangeOnPaint(rc);
                sendMessageToRobots(rc, STAY_PUT_COMMAND, null, UnitType.MOPPER, currentMopperCountAfterSpawn);
            }
            else if (mopperCount >= 2){
                sendMessageToRobots(rc, MOVE_TO_DAMAGED_PATTERN_COMMAND, targetLoc, UnitType.MOPPER, 2);
                sendMessageToRobots(rc, MOVE_TO_DAMAGED_PATTERN_COMMAND, targetLoc, UnitType.SOLDIER, 1);
                System.out.println("soldier + 2 moppers combo ready, sending out to " + targetLoc);
                isDamagedPatternFound = false; calledSoldierStayPutYet = false; isDefault = true;
            }
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

    private static void buildRobotOnRandomTile(RobotController rc, UnitType unitType) throws GameActionException {
        Direction dir = directions[rng.nextInt(directions.length)];
        MapLocation nextLoc = rc.getLocation().add(dir);

        if (rc.canBuildRobot(unitType, nextLoc)) {
            rc.buildRobot(unitType, nextLoc);
            System.out.println("BUILT A " + unitType);
        }
    }

    private static boolean buildRobotOnPaintTile(RobotController rc, UnitType unitType) throws GameActionException {
        for (Direction dir : directions) {
            MapLocation nextLoc = rc.getLocation().add(dir);
            // System.out.println(nextLoc);
            // Check if the location is valid for building a robot
            if (rc.canBuildRobot(unitType, nextLoc)){
                PaintType paintType = rc.senseMapInfo(nextLoc).getPaint();
                // System.out.println(paintType);
                if (paintType == PaintType.ALLY_PRIMARY){
                    // Ensure the tile is painted with ALLY_PRIMARY
                    rc.buildRobot(unitType, nextLoc);
                    System.out.println("BUILT A " + unitType + " on a paint tile (" + PaintType.ALLY_PRIMARY + ") at " + nextLoc);
                    return true; // Exit after successfully building the robot
                }
                else if (paintType == PaintType.ALLY_SECONDARY){
                    rc.buildRobot(unitType, nextLoc);
                    System.out.println("BUILT A " + unitType + " on a paint tile (" + PaintType.ALLY_SECONDARY + ") at " + nextLoc);
                    return true; // Exit after successfully building the robot
                }
            }
        }

        // If no valid paint tile is found
        System.out.println("FAILED to build " + unitType + ": No valid friendly paint tiles nearby.");
        return false;
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

    private static int countMoppersInTowerRangeOnPaint(RobotController rc) throws GameActionException {
        // Get all nearby robots within the tower's sensing range
        RobotInfo[] nearbyRobots = rc.senseNearbyRobots(-1, rc.getTeam());

        int mopperCount = 0;

        // Loop through all nearby robots
        for (RobotInfo robot : nearbyRobots) {
            // Check if the robot is a mopper
            if (robot.getType() == UnitType.MOPPER) {
                MapLocation robotLoc = robot.getLocation();

                // Check if the robot is on a friendly paint tile
                PaintType paintType = rc.senseMapInfo(robotLoc).getPaint();
                if (paintType == PaintType.ALLY_PRIMARY || paintType == PaintType.ALLY_SECONDARY){
                    mopperCount++;
                }
            }
        }

        return mopperCount;
    }



    private static void checkAndRequestPaint(RobotController rc) throws GameActionException {
        // Threshold for low paint
        int lowPaintThreshold = 400;  // adjust as needed, but i think this is a good number

        // Check if tower has low paint
        if (rc.getPaint() <= lowPaintThreshold) {
            // Find a nearby robot to send the message to
            RobotInfo[] nearbyRobots = rc.senseNearbyRobots();

            for (RobotInfo robot : nearbyRobots) {
                // Only send to robots that are not on the same team and are not towers
                if (robot.team != rc.getTeam() && !robot.type.isTowerType()) {
                    // Send a message to the robot to request paint
                    int requestPaintCommand = 4; // Command to request paint
                    MapLocation towerLocation = rc.getLocation();
                    sendMessageToRobots(rc, requestPaintCommand, towerLocation, UnitType.SOLDIER, 1);
                    break;  // Send to only one robot at a time
                }
            }
        }
    }

//    private static void sendMessageToRobots(RobotController rc, int command, MapLocation targetLoc) throws GameActionException {
//        int x = targetLoc != null ? targetLoc.x : 0; // Default x-coordinate if no target
//        int y = targetLoc != null ? targetLoc.y : 0; // Default y-coordinate if no target
//
//        // Encode coordinates and command into messageContent
//        int messageContent = (x << 6) | y | (command << 12);
//
//        // Find nearby robots to send the message to
//        RobotInfo[] nearbyAllies = rc.senseNearbyRobots(-1, rc.getTeam());
//
//        for (RobotInfo ally : nearbyAllies) {
//            MapLocation allyLoc = ally.getLocation();
//            // Check if the message can be sent to this robot
//            if (rc.canSendMessage(allyLoc, messageContent) && isDefault){
//                rc.sendMessage(allyLoc, messageContent);
//                System.out.println("Sent message to " + allyLoc + ": " + messageContent);
//            }
//        }
//    }

    private static int sendMessageToRobots(RobotController rc, int command, MapLocation targetLoc, UnitType robotType, int maxRobots) throws GameActionException {
        int x = targetLoc != null ? targetLoc.x : 63; // Default x-coordinate if no target (out of bounds value)
        int y = targetLoc != null ? targetLoc.y : 63; // Default y-coordinate if no target (out of bounds value)

        // Encode coordinates and command into messageContent
        int messageContent = (x << 6) | y | (command << 12);

        // Find nearby robots of the specified type
        RobotInfo[] nearbyAllies = rc.senseNearbyRobots(-1, rc.getTeam());
        int sentCount = 0;

        for (RobotInfo ally : nearbyAllies) {
            // Check if the robot matches the specified type
            if (ally.getType() == robotType) {
                MapLocation allyLoc = ally.getLocation();

                // Check if the message can be sent to this robot
                if (rc.canSendMessage(allyLoc, messageContent)) {
                    rc.sendMessage(allyLoc, messageContent);
                    sentCount++;
                    if (isDamagedPatternFound) System.out.println("sent to mopper + soldier bruh");
                    System.out.println("Sent message to " + allyLoc + ": " + messageContent);

                    // Stop if we've reached the maximum number of robots
                    if (sentCount >= maxRobots) {
                        break;
                    }
                }
            }
        }

        return sentCount; // Return the number of robots that received the message
    }

    private static void handleMessages(RobotController rc) throws GameActionException {
        Message[] messages = rc.readMessages(-1); // Read all messages from the past 5 rounds

        //if no message received in this turn, don't change any message flags
        if (messages.length == 0) return;

        for (Message message : messages) {
            int messageContent = message.getBytes();

            // Decode command and coordinates
            int x = (messageContent >> 6) & 63; // Extract x-coordinate (bits 6-11)
            int y = messageContent & 63;       // Extract y-coordinate (bits 0-5)
            int command = (messageContent >> 12); // Extract command (bits 12+)

            System.out.println("Received command: " + command + " for location: (" + x + ", " + y + ")");
            //System.out.println(targetLoc);
            // Execute actions based on command
            switch (command) {
                case 0: // need paint command
                    break;
                case 1: //
                    System.out.println("Staying put as per command.");
                    break;
                case 2: // damaged ruin found
                    if (isDefault){
                        targetLoc = new MapLocation(x, y);
                        // send 2 mopper + soldier
                        isDefault = false;
                        isDamagedPatternFound = true;
                    }

            }
        }
    }

    private static int getMapArea(RobotController rc) {
        // Get the map's width and height
        int mapWidth = rc.getMapWidth();
        int mapHeight = rc.getMapHeight();

        // Calculate and return the map area
        return mapWidth * mapHeight;
    }


}
