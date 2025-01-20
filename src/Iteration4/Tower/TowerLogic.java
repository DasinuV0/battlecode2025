package Tower;

import battlecode.common.*;

import java.util.Random;
import java.util.List;
import java.util.ArrayList;

import Util.*;

// commands:
// 1 = stay put
// 2 = damaged pattern found
// 3 = explore
// 4 = defend tower
// 5 = attack location
// 8 = attack splasher
// 9 = defend splasher


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
    static final int LATE_GAME_TURNS = 2000;  // Example: Turns beyond 1000 are late game
    static final int STAY_PUT_COMMAND = 1;
    static final int MOVE_TO_DAMAGED_PATTERN_COMMAND = 2;
    static final int MOVE_TO_LOCATION_COMMAND = 3;
    static final int MOVE_TO_DEFEND_TOWER_COMMAND = 4;
    static final int MOVE_TO_ATTACK_TOWER_COMMAND = 5;
    static final int MOVE_TO_ATTACK_USING_SPLASHER_COMMAND = 8;
    static final int MOVE_TO_DEFEND_USING_SPLASHER_COMMAND = 9;
    static final int MOVE_TO_CLOSEST_PAINT_TOWER_COMMAND = 6;
    //static final int MOVE_TO_PAINT_LARGE_REGION_USING_SPLASHER_COMMAND = 10;

    static boolean isDefault = true;
    static boolean isRuinFound = false;
    static boolean isDamagedPatternFound = false;
    static boolean isEnemyTowerFound = false;
    static boolean isDefendTower = false;
    static boolean isMoveToSpecificLocation = false;
    static boolean isNeedMopper = false;
    static boolean isAttackSplasherNeeded = false;
    static boolean isDefendSplasherNeeded = false;
    static boolean isLargeRegionWithEnemyPaint = false;
    static int saveTurn = 0;

    private static MapLocation lastClosestTower = null;

    public static void run(RobotController rc) throws GameActionException {
        int currentTurn = rc.getRoundNum(); // Get the current game turn
        //System.out.println("Current Turn: " + currentTurn);

        // Determine the game phase
        String gamePhase = determineGamePhase(currentTurn);

        switch (gamePhase) {
            case "EARLY_GAME":
                EarlyGameLogic.runEarlyGame(rc);
                break;
            case "MID_GAME":
                MidGameLogic.runMidGame(rc);
                break;
            case "LATE_GAME":
                LateGameLogic.runLateGame(rc);
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
    static int soldiersSpawned = 0;

    // Boolean flags to track whether the robots have been spawned (default command)
    static boolean randomSoldierSpawnedYet = false;
    static MapLocation targetLoc = new MapLocation(0,0);

    // Bolean flags to track whether 2 moppers exists in vision range
    static boolean calledMoppersStayPutYet = false;
    static boolean calledSoldierStayPutYet = false;


    public static void buildRobotOnRandomTile(RobotController rc, UnitType unitType) throws GameActionException {
        Direction dir = directions[rng.nextInt(directions.length)];
        MapLocation nextLoc = rc.getLocation().add(dir);

        if (rc.canBuildRobot(unitType, nextLoc)) {
            rc.buildRobot(unitType, nextLoc);
            //System.out.println("BUILT A " + unitType);
        }
    }

    public static boolean buildRobotOnPaintTile(RobotController rc, UnitType unitType) throws GameActionException {
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
                    //System.out.println("BUILT A " + unitType + " on a paint tile (" + PaintType.ALLY_PRIMARY + ") at " + nextLoc);
                    return true; // Exit after successfully building the robot
                }
                else if (paintType == PaintType.ALLY_SECONDARY){
                    rc.buildRobot(unitType, nextLoc);
                    //System.out.println("BUILT A " + unitType + " on a paint tile (" + PaintType.ALLY_SECONDARY + ") at " + nextLoc);
                    return true; // Exit after successfully building the robot
                }
            }
        }

        // If no valid paint tile is found
        //System.out.println("FAILED to build " + unitType + ": No valid friendly paint tiles nearby.");
        return false;
    }

    public static boolean canBuildRobotOnPaintTile(RobotController rc) throws GameActionException {
        for (Direction dir : directions) {
            MapLocation nextLoc = rc.getLocation().add(dir);
            // System.out.println(nextLoc);
            // Check if the location is valid for building a robot
            if (rc.canBuildRobot(UnitType.MOPPER, nextLoc)){
                PaintType paintType = rc.senseMapInfo(nextLoc).getPaint();
                // System.out.println(paintType);
                if (paintType == PaintType.ALLY_PRIMARY){
                    //System.out.println("BUILT A " + unitType + " on a paint tile (" + PaintType.ALLY_PRIMARY + ") at " + nextLoc);
                    return true; // Exit after successfully building the robot
                }
                else if (paintType == PaintType.ALLY_SECONDARY){
                    //System.out.println("BUILT A " + unitType + " on a paint tile (" + PaintType.ALLY_SECONDARY + ") at " + nextLoc);
                    return true; // Exit after successfully building the robot
                }
                else if (paintType == PaintType.EMPTY){
                    return true;
                }

            }
        }

        // If no valid paint tile is found
        //System.out.println("FAILED to build " + unitType + ": No valid friendly paint tiles nearby.");
        return false;
    }


    public static void attackNearbyEnemies(RobotController rc) throws GameActionException {
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

    public static int countUnitsInTowerRangeOnPaint(RobotController rc, UnitType unitType) throws GameActionException {
        // Get all nearby robots within the tower's sensing range
        RobotInfo[] nearbyRobots = rc.senseNearbyRobots(-1, rc.getTeam());

        int unitCount = 0;

        // Loop through all nearby robots
        for (RobotInfo robot : nearbyRobots) {
            // Check if the robot is of the specified type
            if (robot.getType() == unitType) {
                MapLocation robotLoc = robot.getLocation();

                // Check if the robot is on a friendly paint tile
                PaintType paintType = rc.senseMapInfo(robotLoc).getPaint();
                if (paintType == PaintType.ALLY_PRIMARY || paintType == PaintType.ALLY_SECONDARY) {
                    unitCount++;
                }
            }
        }

        return unitCount;
    }

    public static void checkAndUpgradeTowers(RobotController rc, int chipThreshold) throws GameActionException {
        // Ensure this robot is a tower
        if (!rc.getType().isTowerType()){
            return; // Exit if not a tower
        }

        // Get the current money of the team
        int currentMoney = rc.getMoney();

        // Check if the chip threshold is met
        if (currentMoney < chipThreshold){
            return; // Not enough money, so exit
        }

        // Get the current tower's location
        MapLocation myLocation = rc.getLocation();

        // Get the next level for this tower
        UnitType nextTowerType = rc.getType().getNextLevel();

        // Prioritize paint towers
        if (isPaintTower(nextTowerType) && rc.canUpgradeTower(myLocation)){
            rc.upgradeTower(myLocation);
            System.out.println("Paint Tower upgraded to: " + nextTowerType + " at " + myLocation);
            return;
        }

        // Upgrade the tower if it's not a paint tower or the next level isn't paint-related
        if (rc.canUpgradeTower(myLocation)){
            rc.upgradeTower(myLocation);
            System.out.println("Tower upgraded to: " + nextTowerType + " at " + myLocation);
        }
    }

    // Helper function to check if a UnitType is a paint tower
    private static boolean isPaintTower(UnitType type){
        return type.toString().contains("PAINT_TOWER");
    }



    public static int sendMessageToRobots(RobotController rc, int command, MapLocation targetLoc, UnitType robotType, int maxRobots) throws GameActionException {
        int x = targetLoc != null ? targetLoc.x : 63; // Default x-coordinate if no target (out of bounds value)
        int y = targetLoc != null ? targetLoc.y : 63; // Default y-coordinate if no target (out of bounds value)

        int messageContent = (x << 6) | y | (command << 12);

        // Find nearby robots of the specified type
        RobotInfo[] nearbyAllies = rc.senseNearbyRobots(-1, rc.getTeam());

        // Filter for robots of the specified type and their paint levels
        List<RobotInfo> filteredRobots = new ArrayList<>();
        for (RobotInfo ally : nearbyAllies) {
            if (ally.getType() == robotType) {
                filteredRobots.add(ally);
            }
        }

        // Sort the robots by their paint levels in descending order
        filteredRobots.sort((a, b) -> Integer.compare(b.getPaintAmount(), a.getPaintAmount()));

        int sentCount = 0;

        // Send messages to the top maxRobots robots with the highest paint levels
        for (RobotInfo ally : filteredRobots) {
            MapLocation allyLoc = ally.getLocation();

            // Check if the message can be sent to this robot
            if (rc.canSendMessage(allyLoc, messageContent)) {
                rc.sendMessage(allyLoc, messageContent);
                sentCount++;
                System.out.println("Sent message to " + allyLoc + ": " + messageContent);

                // Stop if we've reached the maximum number of robots
                if (sentCount >= maxRobots) {
                    break;
                }
            }
        }

        return sentCount; // Return the number of robots that received the message
    }

    private static boolean isChipTower(RobotController rc) {
        // Check if the tower is one of the money or paint towers at any level
        UnitType towerType = rc.getType();
        return towerType == UnitType.LEVEL_ONE_MONEY_TOWER ||
                towerType == UnitType.LEVEL_TWO_MONEY_TOWER ||
                towerType == UnitType.LEVEL_THREE_MONEY_TOWER;
    }

    public static void checkIsChipTowerAndSendToPaintTower(RobotController rc, int senderID) throws GameActionException {
        if (isChipTower(rc) && rc.getPaint() > 350) {
            MapLocation closestPaintTower = TowerUtils.getClosestPaintTower();
            if (closestPaintTower != null) {
                // Get the RobotInfo for the robot with the given senderID
                RobotInfo robotInfo = rc.senseRobot(senderID);

                if (robotInfo != null) {
                    // Get the MapLocation of the robot
                    MapLocation robotLocation = robotInfo.getLocation();

                    // Construct a response message with the closest paint tower coordinates
                    int messageContent = (closestPaintTower.x << 6) | closestPaintTower.y | (MOVE_TO_CLOSEST_PAINT_TOWER_COMMAND << 12);
                    // Send the message back to the robot at its location
                    rc.sendMessage(robotLocation, messageContent);
                }
            }
            else {
                // Get the RobotInfo for the robot with the given senderID
                RobotInfo robotInfo = rc.senseRobot(senderID);

                if (robotInfo != null) {
                    // Get the MapLocation of the robot
                    MapLocation robotLocation = robotInfo.getLocation();

                    // Construct a response message with the closest paint tower coordinates
                    int messageContent = (63 << 6) | 63 | (MOVE_TO_CLOSEST_PAINT_TOWER_COMMAND << 12);
                    // Send the message back to the robot at its location
                    rc.sendMessage(robotLocation, messageContent);
                }
            }
        }
    }


    public static void handleMessages(RobotController rc) throws GameActionException {
        Message[] messages = rc.readMessages(-1); // Read all messages from the past 5 rounds
        int currentTurn = rc.getRoundNum();

        //if no message received in this turn, don't change any message flags
        if (messages.length == 0) return;

        for (Message message : messages) {
            int messageContent = message.getBytes();
            int senderID = message.getSenderID(); // Get the ID of the robot that sent the message

            // Decode command and coordinates
            int x = (messageContent >> 6) & 63; // Extract x-coordinate (bits 6-11)
            int y = messageContent & 63;       // Extract y-coordinate (bits 0-5)
            int command = (messageContent >> 12); // Extract command (bits 12+)

            System.out.println("Received command: " + command + " for location: (" + x + ", " + y + ")");
            //System.out.println(targetLoc);
            // Execute actions based on command
            switch (command) {
                case 6: // update closest paint tower set
                    MapLocation newTowerLocation = new MapLocation(x, y);
                    TowerUtils.updatePaintTowers(newTowerLocation);
                    break;

                case 7: // tower destroyed, remove from set.
                    MapLocation destroyedTowerLocation = new MapLocation(x, y);
                    TowerUtils.removeDestroyedPaintTower(destroyedTowerLocation);
                    checkIsChipTowerAndSendToPaintTower(rc, senderID);
                    break;
                case 3: // give robot paint command
                    checkIsChipTowerAndSendToPaintTower(rc, senderID);
                    break;

                case 10: // attack region with large amount of paint
                    checkIsChipTowerAndSendToPaintTower(rc, senderID);
                    if (determineGamePhase(currentTurn) != "EARLY_GAME" && isDefault){
                        System.out.println("Large region with paint command received. Now forming the required formation of troops");
                        targetLoc = new MapLocation(x, y);
                        // send 2 soldier : early game
                        isDefault = false;
                        isLargeRegionWithEnemyPaint = true;
                    }
                case 8: // attack Splasher
                    checkIsChipTowerAndSendToPaintTower(rc, senderID);
                    if (determineGamePhase(currentTurn) != "EARLY_GAME" && isDefault){
                        System.out.println("Attack splasher command received. Now forming the required formation of troops");
                        targetLoc = new MapLocation(x, y);
                        // send 2 soldier : early game
                        isDefault = false;
                        isAttackSplasherNeeded = true;
                    }
                case 9: // defend Splasher
                    checkIsChipTowerAndSendToPaintTower(rc, senderID);
                    if (determineGamePhase(currentTurn) != "EARLY_GAME" && isDefault){
                        System.out.println("Attack splasher command received. Now forming the required formation of troops");
                        targetLoc = new MapLocation(x, y);
                        // send 2 soldier : early game
                        isDefault = false;
                        isDefendSplasherNeeded = true;
                    }
                case 5: // attack tower
                    checkIsChipTowerAndSendToPaintTower(rc, senderID);
                    System.out.println("Received attack command, checking if tower is free...");
                    if (isDefault){
                        System.out.println("Attack tower command received. Now forming the required formation of troops");
                        targetLoc = new MapLocation(x, y);
                        // send 2 soldier : early game
                        isDefault = false;
                        isEnemyTowerFound = true;
                    }
                case 2: // damaged ruin found
                    checkIsChipTowerAndSendToPaintTower(rc, senderID);
                    if (isDefault){
                        targetLoc = new MapLocation(x, y);
                        // send 2 mopper + soldier : early game
                        isDefault = false;
                        isDamagedPatternFound = true;
                    }

            }
        }
    }

    static final Random rand = new Random(); // Class-level Random object

    public static void sendToLocation(RobotController rc) throws GameActionException {
        MapLocation myLocation = rc.getLocation();
        int randomInt = rand.nextInt(3) + 1;

        if (randomInt == 1){ // own region
            int myRegion = Symmetry.getRegion(rc, myLocation);
            setNewExploreTarget(myRegion, rc);
        }

        else if (randomInt == 2 ){ // adjacent region 1
            int AdjacentRegion1 = Symmetry.getAdjacentRegions(rc, myLocation)[0];
            setNewExploreTarget(AdjacentRegion1, rc);
        }

        else{ // another adjacent region
            int AdjacentRegion2 = Symmetry.getAdjacentRegions(rc, myLocation)[1];
            setNewExploreTarget(AdjacentRegion2, rc);
        }
    }

    public static void setNewExploreTarget(int myRegion, RobotController rc) throws GameActionException {
        int X = rc.getMapWidth() / 2;
        int Y = rc.getMapHeight() / 2;

        if (myRegion == 0){ // A, topLeft
            int randX = rand.nextInt(X);
            int randY = rand.nextInt(Y) + Y;
            targetLoc = new MapLocation(randX, randY);
        }

        else if (myRegion == 1){ // B, topRight
            int randX = rand.nextInt(X) + X;
            int randY = rand.nextInt(Y) + Y;
            targetLoc = new MapLocation(randX, randY);
        }
        else if (myRegion == 2){ // C, bottomLeft
            int randX = rand.nextInt(X);
            int randY = rand.nextInt(Y);
            targetLoc = new MapLocation(randX, randY);
        }
        else{ // D, bottomRight
            int randX = rand.nextInt(X) + X;
            int randY = rand.nextInt(Y);
            targetLoc = new MapLocation(randX, randY);
        }
    }


    static int previousHealth = -1; // Store the previous health of the tower

    public static MapLocation detectEnemyOnDamage(RobotController rc) throws GameActionException{
        int currentHealth = rc.getHealth();

        // Initialize the previous health if this is the first call
        if (previousHealth == -1){
            previousHealth = currentHealth;
            return null; // No detection on the first call
        }

        // Check if the tower has taken damage
        if (currentHealth < previousHealth){
            previousHealth = currentHealth; // Update the previous health

            // Sense nearby enemies
            RobotInfo[] nearbyEnemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
            for (RobotInfo enemy : nearbyEnemies){
                // If the enemy is within attack range, return its location
                MapLocation enemyLocation = enemy.getLocation();
                if (rc.canAttack(enemyLocation)){
                    return enemyLocation; // Return the first detected enemy
                }
            }
        }

        // Update the previous health in case no damage is taken
        previousHealth = currentHealth;
        return null; // Return null if no damage or no enemies are found
    }

    public static int getMapArea(RobotController rc) throws GameActionException {
        // Get the map's width and height
        int mapWidth = rc.getMapWidth();
        int mapHeight = rc.getMapHeight();

        // Calculate and return the map area
        return mapWidth * mapHeight;
    }

}

//            if (!randomSoldierSpawnedYet){
//                buildRobotOnRandomTile(rc, UnitType.SOLDIER);
//                System.out.println("Spawned Soldier on a random tile");
//                randomSoldierSpawnedYet = true;
//                attackNearbyEnemies(rc);
//                return;
//            }
//
//            //System.out.println("BACK IN DEFAULT CMD");
//            int mopperCount = countUnitsInTowerRangeOnPaint(rc, UnitType.MOPPER);
//            int soldierCount = countUnitsInTowerRangeOnPaint(rc, UnitType.SOLDIER);
//
//            // save resources if i have the unit already
//            if (mopperCount >= 6 && soldierCount >= 2){ // dont spawn any if we have the bots already
//                sendToLocation(rc);
//                sendMessageToRobots(rc, MOVE_TO_LOCATION_COMMAND, targetLoc, UnitType.MOPPER, 1);
//                sendMessageToRobots(rc, MOVE_TO_LOCATION_COMMAND, targetLoc, UnitType.SOLDIER, 1);
//                attackNearbyEnemies(rc);
//                return;
//            }
//
//            // Spawn Soldier if it hasn't been spawned yet
//            if (soldierCount < 2){
//                if (!buildRobotOnPaintTile(rc, UnitType.SOLDIER)){
//                    System.out.println("No tiles found that are friendly so far, not spawning soldier xxx");
//                    attackNearbyEnemies(rc);
//                    return; // Exit early to avoid setting flag to true
//                }
//
//                // Command the Soldier to stay put
//                sendMessageToRobots(rc, STAY_PUT_COMMAND, null, UnitType.SOLDIER, 1);
//                System.out.println("Spawned Soldier on a paint tile and commanded it to stay put.");
//                // return; // Exit early to avoid spawning Soldier in the same turn
//            }
//
//            // Spawn Mopper if it hasn't been spawned yet
//            else if (mopperCount < 1){
//                if (!buildRobotOnPaintTile(rc, UnitType.MOPPER)){
//                    System.out.println("No paint tile detected, not spawning mopper dddd");
//                    attackNearbyEnemies(rc);
//                    return;
//                }
//
//                // Command the Mopper to stay put
//                sendMessageToRobots(rc, STAY_PUT_COMMAND, null, UnitType.MOPPER, 1);
//                System.out.println("Spawned Mopper on a paint tile and commanded it to stay put.");
//                //return; // Exit early to avoid sending movement commands prematurely
//            }
//
//            // Once both robots are spawned, command them to move to a target location
//            else{
//                //MapLocation target = new MapLocation(29, 29); // Example target location
//                sendToLocation(rc); // create random destination to explore using probability
//                //System.out.println("new randOM loc created");
//                // sendMessageToRobots(rc, MOVE_TO_LOCATION_COMMAND, target, UnitType.MOPPER, 1);
//                //sendMessageToRobots(rc, MOVE_TO_LOCATION_COMMAND, targetLoc, UnitType.MOPPER, 1);
//                // TODO: add a function to check for how many soldiers
//                // if there is >2, we send 2, otherwise send 1
//                // sendMessageToRobots(rc, MOVE_TO_LOCATION_COMMAND, target, UnitType.MOPPER, 1);
//                if (soldierCount > 1){
//                    sendMessageToRobots(rc, MOVE_TO_LOCATION_COMMAND, targetLoc, UnitType.SOLDIER, 1);
//                }
//                System.out.println("Commanded robots to move to target location:" + targetLoc);
//            }
