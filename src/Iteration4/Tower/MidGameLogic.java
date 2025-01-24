package Tower;

import battlecode.common.*;

// add splashers for default command as well.

public class MidGameLogic extends TowerLogic{
    private static final int CHIP_SAVE_AMOUNT = 2600;
    private static boolean spawnSplasherYet = false;
    static int x = 0;
    private static boolean hasDamagedPatternRobots = false;
    private static boolean hasEnemyTowerRobots = false;

    static void runMidGame(RobotController rc) throws GameActionException{
        //System.out.println("Running Early Game Logic");
//        Direction dir = directions[rng.nextInt(directions.length)];
//        MapLocation nextLoc = rc.getLocation().add(dir);
        int currentTurn = rc.getRoundNum();

        // System.out.println(rc.getMoney());
        // always spawn a soldier at a random tile first

        MapLocation enemyLoc = detectEnemyOnDamage(rc);
        if (enemyLoc != null && isHealthBelowFiftyPercent(rc)){
            isDefendTower = true;
            isDefault = false;
            isDamagedPatternFound = false;
            isEnemyTowerFound = false;
            isAttackSplasherNeeded = false;
            isDefendSplasherNeeded = false;
            saveTurn = 0;
        }
        else{ // switch back to default command
            isDefendTower = false;
            isDamagedPatternFound = false;
            isEnemyTowerFound = false;
            isAttackSplasherNeeded = false;
            isDefendSplasherNeeded = false;
            saveTurn = 0;
            isDefault = true;
        }

        if (!isDefendTower) handleMessages(rc);
        attackNearbyEnemies(rc);


        if (saveTurn > 0){ // after non-default command is done, set a delay of 5 turns so bots can move out of range first
//          checkAndUpgradeTowers(rc, CHIP_TRESHOLD);
            System.out.println("Currently is a saving turn: " + saveTurn);
            saveTurn--;
            //attackNearbyEnemies(rc);
            return;
        }

        if (isDefault){ // exploration mode
            if (rc.canUpgradeTower(rc.getLocation())) checkAndUpgradeTowers(rc, CHIP_SAVE_AMOUNT);
            int soldierCount = countUnitsInTowerRangeOnPaint(rc, UnitType.SOLDIER);
            int mopperCount = countUnitsInTowerRangeOnPaint(rc, UnitType.MOPPER);
            int splasherCount = countUnitsInTowerRangeOnPaint(rc, UnitType.SPLASHER);

            if (!canBuildRobotOnPaintTile(rc)){
                buildRobotOnRandomTile(rc, UnitType.SPLASHER);
                System.out.println("Spawned Splasher on a random tile to paint surrounding enemy paint on friendly tower");
                saveTurn = 3;
                return;
            }

//            if (!randomSoldierSpawnedYet && currentTurn < 10){
//                buildRobotOnRandomTile(rc, UnitType.SOLDIER);
//                System.out.println("Spawned Soldier on a random tile");
//                randomSoldierSpawnedYet = true;
//                //attackNearbyEnemies(rc);
//                return;
//            }

            if (mopperCount >= 1){ // send them out
                sendToLocation(rc);
                sendMessageToRobots(rc, MOVE_TO_LOCATION_COMMAND, targetLoc, UnitType.MOPPER, mopperCount);
                saveTurn = 5;
                System.out.println("Sent a soldier out to explore: " + targetLoc);
            }

            if (soldierCount >= 1){
                sendToLocation(rc);
                sendMessageToRobots(rc, MOVE_TO_LOCATION_COMMAND, targetLoc, UnitType.SOLDIER, soldierCount);
                saveTurn = 5;
                System.out.println("Sent a soldier out to explore: " + targetLoc);
            }
            if (splasherCount >= 1){
                sendToLocation(rc);
                sendMessageToRobots(rc, MOVE_TO_ATTACK_USING_SPLASHER_COMMAND, targetLoc, UnitType.SPLASHER, splasherCount);
                saveTurn = 5;
                System.out.println("Sent a splasher out to explore: " + targetLoc);
            }

            if (isChipTower(rc) && getMapArea(rc) > 1200){ // just spawn soldiers on chip towers
                buildRobotOnPaintTile(rc, UnitType.SOLDIER);
                sendToLocation(rc);
                int soldierCountAfterSpawn = countUnitsInTowerRangeOnPaint(rc, UnitType.SOLDIER);
                sendMessageToRobots(rc, MOVE_TO_LOCATION_COMMAND, targetLoc, UnitType.SOLDIER, soldierCountAfterSpawn);
                saveTurn = 5;
                System.out.println("Spawned Soldier on a paint tile and commanded it to move.");
            }


            if (isChipTower(rc) && x < 2 && getMapArea(rc) <= 1200){ // be more aggresive, spawn soldier && splasher out
                if (!spawnSplasherYet){
                    if (!buildRobotOnPaintTile(rc, UnitType.SPLASHER)){
                        System.out.println("No tiles found that are friendly so far, not spawning splasher xxx");
                        //attackNearbyEnemies(rc);
                        return; // Exit early to avoid setting flag to true
                    }

                    // Command the Soldier to stay put
                    sendToLocation(rc);
                    int splasherCountAfterSpawn = countUnitsInTowerRangeOnPaint(rc, UnitType.SPLASHER);
                    sendMessageToRobots(rc, MOVE_TO_ATTACK_USING_SPLASHER_COMMAND, targetLoc, UnitType.SPLASHER, splasherCountAfterSpawn);
                    saveTurn = 5;
                    System.out.println("Spawned Splasher on a paint tile and commanded it to move xxxxxxxxxxxyyyyyyyyyyyyyyyyy.");
                    spawnSplasherYet = true;
                    x++;
                }
                else{
                    //int splasherCountAfterSpawn = countUnitsInTowerRangeOnPaint(rc, UnitType.SPLASHER);
                    //sendMessageToRobots(rc, MOVE_TO_ATTACK_USING_SPLASHER_COMMAND, targetLoc, UnitType.SPLASHER, splasherCountAfterSpawn);
                    if (!buildRobotOnPaintTile(rc, UnitType.SOLDIER)){
                        System.out.println("No tiles found that are friendly so far, not spawning soldier xxxyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyy");
                        //attackNearbyEnemies(rc);
                        return; // Exit early to avoid setting flag to true
                    }

                    // Command the Soldier to stay put
                    sendToLocation(rc);
                    int soldierCountAfterSpawn = countUnitsInTowerRangeOnPaint(rc, UnitType.SOLDIER);
                    sendMessageToRobots(rc, MOVE_TO_LOCATION_COMMAND, targetLoc, UnitType.SOLDIER, soldierCountAfterSpawn);
                    saveTurn = 5;
                    System.out.println("Spawned Soldier on a paint tile and commanded it to move xxxxxxxxxxxxzzzzzzzzzzzzzzz to: " + targetLoc);
                }
            }

            if (rc.getMoney() <= CHIP_SAVE_AMOUNT || rc.getPaint() < 350){ // save chips regardless of tower
                System.out.println("Saving ruins regardless of tower type");
                return;
            }
            else if (isChipTower(rc) && rc.getPaint() < 350){ // save paint for chip towers
                System.out.println("Is chip tower, and paint <= 200: Saving ruins");
                return;
            }


            int randomInt = rand.nextInt(3) + 1;

            if (randomInt == 1) { // spawn splasher
                if (!buildRobotOnPaintTile(rc, UnitType.SPLASHER)) {
                    System.out.println("No tiles found that are friendly so far, not spawning soldier xxx");
                    //attackNearbyEnemies(rc);
                    return; // Exit early to avoid setting flag to true
                }

                sendToLocation(rc);
                int countSplasherAfterSpawn = countUnitsInTowerRangeOnPaint(rc, UnitType.SPLASHER);
                sendMessageToRobots(rc, MOVE_TO_ATTACK_USING_SPLASHER_COMMAND, targetLoc, UnitType.SPLASHER, countSplasherAfterSpawn);
                saveTurn = 5;
                System.out.println("Spawned Splasher on a paint tile and commanded to go random loc.");
            }
            else if (randomInt == 2) { // spawn soldier
                if (!buildRobotOnPaintTile(rc, UnitType.SOLDIER)) {
                    System.out.println("No tiles found that are friendly so far, not spawning soldier xxx");
                    //attackNearbyEnemies(rc);
                    return; // Exit early to avoid setting flag to true
                }

                sendToLocation(rc);
                int countSoldierAfterSpawn = countUnitsInTowerRangeOnPaint(rc, UnitType.SOLDIER);
                sendMessageToRobots(rc, MOVE_TO_LOCATION_COMMAND, targetLoc, UnitType.SOLDIER, countSoldierAfterSpawn);
                saveTurn = 5;
                System.out.println("Spawned Soldier on a paint tile and commanded to go random loc.");
            }
            else{ // spawn mopper
                if (!buildRobotOnPaintTile(rc, UnitType.MOPPER)) {
                    System.out.println("No tiles found that are friendly so far, not spawning soldier xxx");
                    //attackNearbyEnemies(rc);
                    return; // Exit early to avoid setting flag to true
                }

                sendToLocation(rc);
                int countMopperAfterSpawn = countUnitsInTowerRangeOnPaint(rc, UnitType.MOPPER);
                sendMessageToRobots(rc, MOVE_TO_LOCATION_COMMAND, targetLoc, UnitType.MOPPER, countMopperAfterSpawn);
                saveTurn = 5;
                System.out.println("Spawned Soldier on a paint tile and commanded to go random loc.");
            }

        }
        else if (isDefendTower){ // defend mode
            if (!canBuildRobotOnPaintTile(rc)){
                buildRobotOnRandomTile(rc, UnitType.SPLASHER);
                System.out.println("Spawned Splasher on a random tile");
            }

            int mopperCount = countUnitsInTowerRangeOnPaint(rc, UnitType.MOPPER);
            int soldierCount = countUnitsInTowerRangeOnPaint(rc, UnitType.SOLDIER);
            int splasherCount = countUnitsInTowerRangeOnPaint(rc, UnitType.SPLASHER);

            if (!isHealthBelowTwentyPercent(rc)){
                if (soldierCount >= 1){
                    sendToLocation(rc);
                    sendMessageToRobots(rc, MOVE_TO_LOCATION_COMMAND, targetLoc, UnitType.SOLDIER, soldierCount);
                }

                if (mopperCount >= 1){
                    sendMessageToRobots(rc, MOVE_TO_DEFEND_TOWER_COMMAND, enemyLoc, UnitType.MOPPER, mopperCount);
                    System.out.println("TOWER UNDER ATTACKED! SENDING ALL MOPPERS ON PAINT TO FIGHT");
                }

                if (splasherCount >= 1){
                    sendToLocation(rc);
                    sendMessageToRobots(rc, MOVE_TO_ATTACK_USING_SPLASHER_COMMAND, targetLoc, UnitType.SPLASHER, splasherCount);
                }

                if (mopperCount < 2){ // try to have 2 moppers defend, if not enough, spawn if can
                    System.out.println("NOT ENOUGH MOPPER TO DEFEND, TRYING TO SPAWN MORE!!!!");
                    if (!buildRobotOnPaintTile(rc, UnitType.MOPPER)){
                        System.out.println("NO PAINT TILE DETECTED, CAN'T SPAWN MOPPER, MAYDAY!!!");
                        //attackNearbyEnemies(rc);
                        return;
                    }
                    int countMopperAfterSpawn = countUnitsInTowerRangeOnPaint(rc, UnitType.MOPPER);
                    sendMessageToRobots(rc, MOVE_TO_DEFEND_TOWER_COMMAND, enemyLoc, UnitType.MOPPER, countMopperAfterSpawn);
                    System.out.println("SPAWNED A MOPPER TO DEFEND TOWER!!!");
                }
            }
            else{
                // while defending, if i have so many soldiers, just send them out
                // since they are pointless in defending, but leave some for other tasks when
                // defending is completed
                if (soldierCount >= 1){
                    sendToLocation(rc);
                    sendMessageToRobots(rc, MOVE_TO_LOCATION_COMMAND, enemyLoc, UnitType.SOLDIER, soldierCount);
                }

                if (mopperCount >= 1){
                    sendMessageToRobots(rc, MOVE_TO_DEFEND_TOWER_COMMAND, enemyLoc, UnitType.MOPPER, mopperCount);
                    System.out.println("TOWER UNDER ATTACKED! SENDING ALL MOPPERS ON PAINT TO FIGHT");
                }

                if (splasherCount >= 1){
                    sendToLocation(rc);
                    sendMessageToRobots(rc, MOVE_TO_ATTACK_USING_SPLASHER_COMMAND, targetLoc, UnitType.SPLASHER, splasherCount);
                }

                if (mopperCount < 1){ // try to have 1 mopper defend, if not enough, spawn if can
                    System.out.println("NOT ENOUGH MOPPER TO DEFEND, TRYING TO SPAWN MORE!!!!");
                    if (!buildRobotOnPaintTile(rc, UnitType.MOPPER)){
                        System.out.println("NO PAINT TILE DETECTED, CAN'T SPAWN MOPPER, MAYDAY!!!");
                        //attackNearbyEnemies(rc);
                        return;
                    }
                    int countMopperAfterSpawn = countUnitsInTowerRangeOnPaint(rc, UnitType.MOPPER);
                    sendMessageToRobots(rc, MOVE_TO_DEFEND_TOWER_COMMAND, enemyLoc, UnitType.MOPPER, countMopperAfterSpawn);
                    System.out.println("SPAWNED A MOPPER TO DEFEND TOWER!!!");
                }
                if (soldierCount < 1){ // try to have 1 soldier defend, if not enough, spawn if can
                    System.out.println("NOT ENOUGH SOLDIER TO DEFEND, TRYING TO SPAWN MORE!!!!");
                    if (!buildRobotOnPaintTile(rc, UnitType.SOLDIER)){
                        System.out.println("NO PAINT TILE DETECTED, CAN'T SPAWN SOLDIER, MAYDAY!!!");
                        //attackNearbyEnemies(rc);
                        return;
                    }
                    int countSoldierAfterSpawn = countUnitsInTowerRangeOnPaint(rc, UnitType.SOLDIER);
                    sendMessageToRobots(rc,  MOVE_TO_LOCATION_COMMAND, enemyLoc, UnitType.SOLDIER, countSoldierAfterSpawn);
                    System.out.println("SPAWNED A SOLDIER TO REBUILD TOWER IN CASE ITS DESTROYED!!!");
                }

            }
        }
        else if (isEnemyTowerFound){ // attack tower mode
            if (hasEnemyTowerRobots){
                int currentSoldierCountAfterSpawn = countUnitsInTowerRangeOnPaint(rc, UnitType.SOLDIER);
                sendMessageToRobots(rc, MOVE_TO_ATTACK_TOWER_COMMAND, targetLoc, UnitType.SOLDIER, currentSoldierCountAfterSpawn);
                isDefault = true;
                hasEnemyTowerRobots = false;
                isEnemyTowerFound = false;
                saveTurn = 4;
                return;
            }
            System.out.println("Enemy tower found, now preparing robots to attack");

            if (!canBuildRobotOnPaintTile(rc)){
                buildRobotOnRandomTile(rc, UnitType.SPLASHER);
                System.out.println("Spawned Splasher on a random tile");
                saveTurn = 3;
                return;
            }

            int soldierCount = countUnitsInTowerRangeOnPaint(rc, UnitType.SOLDIER);
            int splasherCount = countUnitsInTowerRangeOnPaint(rc, UnitType.SPLASHER);

            if (splasherCount >= 1) sendMessageToRobots(rc, MOVE_TO_ATTACK_USING_SPLASHER_COMMAND, targetLoc, UnitType.SPLASHER, splasherCount);

            if (soldierCount >= 1){ // send at least 1 soldier out
                System.out.println("Sending out 1 soldier to attack enemy tower!");
                sendMessageToRobots(rc, MOVE_TO_ATTACK_TOWER_COMMAND, targetLoc, UnitType.SOLDIER, soldierCount);
                isEnemyTowerFound = false;
                isDefault = true;
                saveTurn = 5;
            }
            else{
                if (!buildRobotOnPaintTile(rc, UnitType.SOLDIER)){
                    System.out.println("No paint tile detected, not spawning soldier");
                    //attackNearbyEnemies(rc);
                    return;
                }

                System.out.println("1 Soldier spawned on paint tile, sending out to attack");
                hasEnemyTowerRobots = true;
            }
        }
        else if (isDamagedPatternFound){ // damage pattern found mode
            if (hasDamagedPatternRobots){
                int currentMopperCountAfterSpawn = countUnitsInTowerRangeOnPaint(rc, UnitType.MOPPER);
                sendMessageToRobots(rc, MOVE_TO_DAMAGED_PATTERN_COMMAND, targetLoc, UnitType.MOPPER, currentMopperCountAfterSpawn);
                isDefault = true;
                hasDamagedPatternRobots = false;
                isDamagedPatternFound = false;
                saveTurn = 4;
                return;
            }

            System.out.println("Oh no! Damaged pattern is found!");

            if (!canBuildRobotOnPaintTile(rc)){
                buildRobotOnRandomTile(rc, UnitType.SPLASHER);
                System.out.println("Spawned Splasher on a random tile");
                saveTurn = 3;
                return;
            }

            int mopperCount = countUnitsInTowerRangeOnPaint(rc, UnitType.MOPPER);
            int soldierCount = countUnitsInTowerRangeOnPaint(rc, UnitType.SOLDIER);

            // save resources if i have the unit already
            if (mopperCount >= 1 && soldierCount >= 1){ // dont spawn any if we have the bots already
                sendMessageToRobots(rc, MOVE_TO_DAMAGED_PATTERN_COMMAND, targetLoc, UnitType.MOPPER, mopperCount);
                sendMessageToRobots(rc, MOVE_TO_DAMAGED_PATTERN_COMMAND, targetLoc, UnitType.SOLDIER, soldierCount);
                isDamagedPatternFound = false;
                isDefault = true;
                //attackNearbyEnemies(rc);
                saveTurn = 5;
                return;
            }
            else if (soldierCount >= 1){
                sendMessageToRobots(rc, MOVE_TO_DAMAGED_PATTERN_COMMAND, targetLoc, UnitType.SOLDIER, soldierCount);
            }
            else if (mopperCount >= 1){
                sendMessageToRobots(rc, MOVE_TO_DAMAGED_PATTERN_COMMAND, targetLoc, UnitType.MOPPER, mopperCount);
                isDamagedPatternFound = false;
            }

//            if (!calledSoldierStayPutYet){
//                sendMessageToRobots(rc, STAY_PUT_COMMAND, null, UnitType.SOLDIER, 1);
//                System.out.println("One soldier is stayed put now");
//                calledSoldierStayPutYet = true;
//            }

//            int mopperCount = sendMessageToRobots(rc, STAY_PUT_COMMAND, null, UnitType.MOPPER, 2);

            if (mopperCount < 1){
//                if (!calledMoppersStayPutYet){
//                    sendMessageToRobots(rc, STAY_PUT_COMMAND, null, UnitType.MOPPER, mopperCount);
//                    if (mopperCount > 0) calledMoppersStayPutYet = true;
//                }

                if (!buildRobotOnPaintTile(rc, UnitType.MOPPER)){
                    System.out.println("Not enough paint, not spawning MOPPER");
                    //attackNearbyEnemies(rc);
                    return; // Exit early to avoid sending wrong mopper message
                }
                System.out.println("Spawned MOPPER on a paint tile and commanded it to stay put.");
                hasDamagedPatternRobots = true;
            }
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

        }
        else if (isAttackSplasherNeeded){
            int splasherCount = countUnitsInTowerRangeOnPaint(rc, UnitType.SPLASHER);

            if (!canBuildRobotOnPaintTile(rc)){
                buildRobotOnRandomTile(rc, UnitType.SPLASHER);
                System.out.println("Spawned Splasher on a random tile");
                saveTurn = 3;
                return;
            }

            if (splasherCount >= 1) {
                sendMessageToRobots(rc, MOVE_TO_ATTACK_USING_SPLASHER_COMMAND, targetLoc, UnitType.SPLASHER, splasherCount);
                System.out.println("Sending an attack splasher to: " + targetLoc);
                isAttackSplasherNeeded = false;
                isDefault = true;
                saveTurn = 5;
            }
            else{
                if (!buildRobotOnPaintTile(rc, UnitType.SPLASHER)){
                    System.out.println("Not enough paint, not spawning SPLASHER");
                    //attackNearbyEnemies(rc);
                    return; // Exit early to avoid sending wrong splasher message
                }
                System.out.println("Spawned ATTACK SPLASHER on a paint tile and commanded it go to: " + targetLoc);
                int splasherCountAfterSpawn = countUnitsInTowerRangeOnPaint(rc, UnitType.SPLASHER);
                sendMessageToRobots(rc, MOVE_TO_ATTACK_USING_SPLASHER_COMMAND, targetLoc, UnitType.SPLASHER, splasherCountAfterSpawn);
                isDefault = true;
                isAttackSplasherNeeded = false;
                saveTurn = 5;
            }
        }
        else if (isDefendSplasherNeeded){
            int splasherCount = countUnitsInTowerRangeOnPaint(rc, UnitType.SPLASHER);

            if (!canBuildRobotOnPaintTile(rc)){
                buildRobotOnRandomTile(rc, UnitType.SPLASHER);
                System.out.println("Spawned Splasher on a random tile");
                saveTurn = 3;
                return;
            }

            if (splasherCount >= 1) {
                sendMessageToRobots(rc, MOVE_TO_ATTACK_USING_SPLASHER_COMMAND, targetLoc, UnitType.SPLASHER, splasherCount);
                System.out.println("Sending a defend splasher to: " + targetLoc);
                isDefendSplasherNeeded = false;
                isDefault = true;
                saveTurn = 5;
            }
            else{
                if (!buildRobotOnPaintTile(rc, UnitType.SPLASHER)){
                    System.out.println("Not enough paint, not spawning SPLASHER");
                    //attackNearbyEnemies(rc);
                    return; // Exit early to avoid sending wrong mopper message
                }
                System.out.println("Spawned DEFEND SPLASHER on a paint tile and commanded it to go to: " + targetLoc);
                int splasherCountAfterSpawn = countUnitsInTowerRangeOnPaint(rc, UnitType.SPLASHER);
                sendMessageToRobots(rc, MOVE_TO_DEFEND_USING_SPLASHER_COMMAND, targetLoc, UnitType.SPLASHER, splasherCountAfterSpawn);
                isDefault = true;
                isDefendSplasherNeeded = false;
                saveTurn = 5;
            }
        }
        else if (isLargeRegionWithEnemyPaint){
            if (!canBuildRobotOnPaintTile(rc)){
                buildRobotOnRandomTile(rc, UnitType.SPLASHER);
                System.out.println("Spawned Splasher on a random tile");
                saveTurn = 3;
                return;
            }

            int splasherCount = countUnitsInTowerRangeOnPaint(rc, UnitType.SPLASHER);

            if (splasherCount >= 1) {
                sendMessageToRobots(rc, MOVE_TO_ATTACK_USING_SPLASHER_COMMAND, targetLoc, UnitType.SPLASHER, splasherCount);
                System.out.println("Sending a defend splasher to: " + targetLoc);
                isLargeRegionWithEnemyPaint = false;
                isDefault = true;
                saveTurn = 5;
            }
            else{
                if (!buildRobotOnPaintTile(rc, UnitType.SPLASHER)){
                    System.out.println("Not enough paint, not spawning SPLASHER");
                    //attackNearbyEnemies(rc);
                    return; // Exit early to avoid sending wrong mopper message
                }
                System.out.println("Spawned CLEAR SPLASHER on a paint tile and commanded it to go to: " + targetLoc);
                int splasherCountAfterSpawn = countUnitsInTowerRangeOnPaint(rc, UnitType.SPLASHER);
                sendMessageToRobots(rc, MOVE_TO_ATTACK_USING_SPLASHER_COMMAND, targetLoc, UnitType.SPLASHER, splasherCountAfterSpawn);
                isDefault = true;
                isLargeRegionWithEnemyPaint = false;
                saveTurn = 5;
            }
        }
    }
}
