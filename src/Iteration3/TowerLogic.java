package Iteration3;

import battlecode.common.*;

import java.util.Random;
import java.util.List;
import java.util.ArrayList;

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
    static final int MOVE_TO_DEFEND_USING_SPLASHER_COMAMND = 9;

    static boolean isDefault = true;
    static boolean isRuinFound = false;
    static boolean isDamagedPatternFound = false;
    static boolean isEnemyTowerFound = false;
    static boolean isDefendTower = false;
    static boolean isMoveToSpecificLocation = false;
    static boolean isNeedMopper = false;
    static boolean isAttackSplasherNeeded = false;
    static boolean isDefendSplasherNeeded = false;
    static int saveTurn = 0;

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


//    private static void runEarlyGame(RobotController rc) throws GameActionException{
//        //System.out.println("Running Early Game Logic");
////        Direction dir = directions[rng.nextInt(directions.length)];
////        MapLocation nextLoc = rc.getLocation().add(dir);
//        int currentTurn = rc.getRoundNum();
//
//        // System.out.println(rc.getMoney());
//        // always spawn a soldier at a random tile first
//
//        MapLocation enemyLoc = detectEnemyOnDamage(rc);
//        if (enemyLoc != null){
//            isDefendTower = true;
//            isDefault = false;
//            isDamagedPatternFound = false;
//            isEnemyTowerFound = false;
//            saveTurn = 0;
//        }
//        else{ // switch back to default command
//            isDefendTower = false;
//            isDamagedPatternFound = false;
//            isEnemyTowerFound = false;
//            saveTurn = 0;
//            isDefault = true;
//        }
//
//        if (!isDefendTower) handleMessages(rc);
//
//
//        if (saveTurn > 0){ // after non-default command is done, set a delay of 5 turns so bots can move out of range first
//            System.out.println("Currently is a saving turn: " + saveTurn);
//            saveTurn--;
//            attackNearbyEnemies(rc);
//            return;
//        }
//
//        if (isDefault){ // exploration mode
//            int soldierCount = countUnitsInTowerRangeOnPaint(rc, UnitType.SOLDIER);
//            if (!randomSoldierSpawnedYet){
//                buildRobotOnRandomTile(rc, UnitType.SOLDIER);
//                System.out.println("Spawned Soldier on a random tile");
//                randomSoldierSpawnedYet = true;
//                attackNearbyEnemies(rc);
//                return;
//            }
//
//            if (soldierCount >= 2){
//                sendToLocation(rc);
//                sendMessageToRobots(rc, MOVE_TO_LOCATION_COMMAND, targetLoc, UnitType.SOLDIER, 1);
//                saveTurn = 3;
//                System.out.println("Sent a soldier out to explore: " + targetLoc);
//            }
//            else{
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
//        }
//        else if (isDefendTower){ // defend mode
//            int mopperCount = countUnitsInTowerRangeOnPaint(rc, UnitType.MOPPER);
//            int soldierCount = countUnitsInTowerRangeOnPaint(rc, UnitType.SOLDIER);
//
//            // while defending, if i have so many soldiers, just send them out
//            // since they are pointless in defending, but leave some for other tasks when
//            // defending is completed
//            if (soldierCount >= 2){
//                sendToLocation(rc);
//                sendMessageToRobots(rc, MOVE_TO_LOCATION_COMMAND, targetLoc, UnitType.SOLDIER, 1);
//            }
//
//            if (mopperCount >= 2){
//                sendMessageToRobots(rc, MOVE_TO_DEFEND_TOWER_COMMAND, enemyLoc, UnitType.MOPPER, mopperCount);
//                System.out.println("TOWER UNDER ATTACKED! SENDING ALL MOPPERS ON PAINT TO FIGHT");
//            }
//            else{ // try to have 2 moppers defend, if not enough, spawn if can
//                System.out.println("NOT ENOUGH MOPPER TO DEFEND, TRYING TO SPAWN MORE!!!!");
//                if (!buildRobotOnPaintTile(rc, UnitType.MOPPER)){
//                    System.out.println("NO PAINT TILE DETECTED, CAN'T SPAWN MOPPER, MAYDAY!!!");
//                    attackNearbyEnemies(rc);
//                    return;
//                }
//                System.out.println("SPAWNED A MOPPER TO DEFEND TOWER!!!");
//            }
//        }
//        else if (isEnemyTowerFound){ // attack tower mode
//            System.out.println("Enemy tower found, now preparing robots to attack");
//            int soldierCount = countUnitsInTowerRangeOnPaint(rc, UnitType.SOLDIER);
//
//            if (soldierCount >= 2){ // send 2 soldiers out
//                System.out.println("Sending out 2 soldiers to attack enemy tower!");
//                sendMessageToRobots(rc, MOVE_TO_ATTACK_TOWER_COMMAND, targetLoc, UnitType.SOLDIER, 2);
//                isEnemyTowerFound = false;
//                isDefault = true;
//                saveTurn = 5;
//            }
//            else{
//                if (!buildRobotOnPaintTile(rc, UnitType.SOLDIER)){
//                    System.out.println("No paint tile detected, not spawning soldier");
//                    attackNearbyEnemies(rc);
//                    return;
//                }
//
//                System.out.println("1 Soldier spawned on paint tile, preparing more soldiers...");
//            }
//
//        }
//        else if (isDamagedPatternFound){ // damage pattern found mode
//            System.out.println("Oh no! Damaged pattern is found!");
//            int mopperCount = countUnitsInTowerRangeOnPaint(rc, UnitType.MOPPER);
//            int soldierCount = countUnitsInTowerRangeOnPaint(rc, UnitType.SOLDIER);
//
//            // save resources if i have the unit already
//            if (mopperCount >= 2 && soldierCount >= 1){ // dont spawn any if we have the bots already
//                sendToLocation(rc);
//                sendMessageToRobots(rc, MOVE_TO_LOCATION_COMMAND, targetLoc, UnitType.MOPPER, 2);
//                sendMessageToRobots(rc, MOVE_TO_LOCATION_COMMAND, targetLoc, UnitType.SOLDIER, 1);
//                attackNearbyEnemies(rc);
//                saveTurn = 5;
//                return;
//            }
//
//            if (!calledSoldierStayPutYet){
//                sendMessageToRobots(rc, STAY_PUT_COMMAND, null, UnitType.SOLDIER, 1);
//                System.out.println("One soldier is stayed put now");
//                calledSoldierStayPutYet = true;
//            }
//
////            int mopperCount = sendMessageToRobots(rc, STAY_PUT_COMMAND, null, UnitType.MOPPER, 2);
//
//            if (mopperCount < 2){
//                if (!calledMoppersStayPutYet){
//                    sendMessageToRobots(rc, STAY_PUT_COMMAND, null, UnitType.MOPPER, mopperCount);
//                    if (mopperCount > 0) calledMoppersStayPutYet = true;
//                }
//
//                if (!buildRobotOnPaintTile(rc, UnitType.MOPPER)){
//                    System.out.println("Not enough paint, not spawning MOPPER");
//                    attackNearbyEnemies(rc);
//                    return; // Exit early to avoid sending wrong mopper message
//                }
//                System.out.println("Spawned MOPPER on a paint tile and commanded it to stay put.");
//                int currentMopperCountAfterSpawn = countUnitsInTowerRangeOnPaint(rc, UnitType.MOPPER);
//                sendMessageToRobots(rc, STAY_PUT_COMMAND, null, UnitType.MOPPER, currentMopperCountAfterSpawn);
//            }
//            else if (mopperCount >= 2){
//                sendMessageToRobots(rc, MOVE_TO_DAMAGED_PATTERN_COMMAND, targetLoc, UnitType.MOPPER, 2);
//                sendMessageToRobots(rc, MOVE_TO_DAMAGED_PATTERN_COMMAND, targetLoc, UnitType.SOLDIER, 1);
//                System.out.println("soldier + 2 moppers combo ready, sending out to " + targetLoc);
//                isDamagedPatternFound = false;
//                calledSoldierStayPutYet = false;
//                calledMoppersStayPutYet = false;
//                isDefault = true;
//                saveTurn = 5;
//            }
//
//        }
//
//        attackNearbyEnemies(rc);
//    }


//    private static void runMidGame(RobotController rc) throws GameActionException {
//        //System.out.println("Running Mid Game Logic");
//
//        // Balance between attacking and expanding
////        if (rng.nextBoolean()) {
////            buildRobot(rc, UnitType.MOPPER); // Example: Build moppers for area control
////        } else {
////            attackNearbyEnemies(rc);
////        }
//    }

//    private static void runLateGame(RobotController rc) throws GameActionException {
//        //System.out.println("Running Late Game Logic");
//
//        // Focus on strong units and defending
////        buildRobot(rc, UnitType.SPLASHER); // Example: Build splashers for AoE attacks
////        attackNearbyEnemies(rc); // Simultaneously attack enemies
//    }

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


    public static void handleMessages(RobotController rc) throws GameActionException {
        Message[] messages = rc.readMessages(-1); // Read all messages from the past 5 rounds
        int currentTurn = rc.getRoundNum();

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
                case 8: // attack Splasher
                    if (determineGamePhase(currentTurn) != "EARLY_GAME" && isDefault){
                        System.out.println("Attack splasher command received. Now forming the required formation of troops");
                        targetLoc = new MapLocation(x, y);
                        // send 2 soldier : early game
                        isDefault = false;
                        isAttackSplasherNeeded = true;
                    }
                case 9: // defend Splasher
                    if (determineGamePhase(currentTurn) != "EARLY_GAME" && isDefault){
                        System.out.println("Attack splasher command received. Now forming the required formation of troops");
                        targetLoc = new MapLocation(x, y);
                        // send 2 soldier : early game
                        isDefault = false;
                        isDefendSplasherNeeded = true;
                    }
                case 5: // attack tower
                    System.out.println("Received attack command, checking if tower is free...");
                    if (isDefault){
                        System.out.println("Attack tower command received. Now forming the required formation of troops");
                        targetLoc = new MapLocation(x, y);
                        // send 2 soldier : early game
                        isDefault = false;
                        isEnemyTowerFound = true;
                    }
                case 2: // damaged ruin found
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
