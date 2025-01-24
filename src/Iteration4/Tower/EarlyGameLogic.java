package Tower;

import battlecode.common.*;

public class EarlyGameLogic extends TowerLogic{
    private static final int CHIP_SAVE_AMOUNT = 1200;
    private static boolean spawnSplasherYet = false;
    static int x = 0;
    private static boolean hasDamagedPatternRobots = false;
    private static boolean hasEnemyTowerRobots = false;
    private static int saveTurn = 0;

    static void runEarlyGame(RobotController rc) throws GameActionException{
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
            saveTurn = 0;
        }
        else{ // switch back to default command
            isDefendTower = false;
            isDamagedPatternFound = false;
            isEnemyTowerFound = false;
            isDefault = true;
            saveTurn = 0;
        }

        if (!isDefendTower) handleMessages(rc);
        attackNearbyEnemies(rc);


        if (saveTurn > 0 && enemyLoc == null){ // after non-default command is done, set a delay of 5 turns so bots can move out of range first
            System.out.println("Currently is a saving turn: " + saveTurn);
            saveTurn--;
            //attackNearbyEnemies(rc);
            return;
        }

        if (isDefault){ // exploration mode
            int soldierCount = countUnitsInTowerRangeOnPaint(rc, UnitType.SOLDIER);
            int mopperCount = countUnitsInTowerRangeOnPaint(rc, UnitType.MOPPER);

            if (!canBuildRobotOnPaintTile(rc)){
                buildRobotOnRandomTile(rc, UnitType.SPLASHER);
                System.out.println("Spawned Splasher on a random tile pppppp");
                saveTurn = 5;
                return;
            }

            if (!randomSoldierSpawnedYet && currentTurn < 10){
                buildRobotOnRandomTile(rc, UnitType.SOLDIER);
                System.out.println("Spawned Soldier on a random  kkkk");
                randomSoldierSpawnedYet = true;
                //attackNearbyEnemies(rc);
                return;
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

                    sendToLocation(rc);
                    int splasherCountAfterSpawn = countUnitsInTowerRangeOnPaint(rc, UnitType.SPLASHER);
                    sendMessageToRobots(rc, MOVE_TO_ATTACK_USING_SPLASHER_COMMAND, targetLoc, UnitType.SPLASHER, splasherCountAfterSpawn);
                    saveTurn = 5;
                    System.out.println("Spawned Splasher on a paint tile and commanded it to move.");
                    spawnSplasherYet = true;
                    x++;
                }
                else{
                    if (!buildRobotOnPaintTile(rc, UnitType.SOLDIER)){
                        System.out.println("No tiles found that are friendly so far, not spawning soldier xxx");
                        //attackNearbyEnemies(rc);
                        return; // Exit early to avoid setting flag to true
                    }

                    sendToLocation(rc);
                    int soldierCountAfterSpawn = countUnitsInTowerRangeOnPaint(rc, UnitType.SOLDIER);
                    sendMessageToRobots(rc, MOVE_TO_LOCATION_COMMAND, targetLoc, UnitType.SOLDIER, soldierCountAfterSpawn);
                    saveTurn = 5;
                    System.out.println("Spawned Soldier on a paint tile and commanded it to move.");
                    x++;
                }
            }

            if (mopperCount >= 1){ // send them out
                sendToLocation(rc);
                sendMessageToRobots(rc, MOVE_TO_LOCATION_COMMAND, targetLoc, UnitType.MOPPER, mopperCount);
                saveTurn = 5;
                System.out.println("Sent a soldier out to explore: " + targetLoc);
            }

            if (soldierCount >= 1){ // send them out, but keep one
                sendToLocation(rc);
                sendMessageToRobots(rc, MOVE_TO_LOCATION_COMMAND, targetLoc, UnitType.SOLDIER, soldierCount);
                saveTurn = 5;
                System.out.println("Sent a soldier out to explore: " + targetLoc);
            }

            else { // if none, spawn one
                if (rc.getMoney() <= CHIP_SAVE_AMOUNT){ // prioritize building towers
                    System.out.println("Saving ruins");
                    return;
                }
                if (!buildRobotOnPaintTile(rc, UnitType.SOLDIER)){
                    System.out.println("No tiles found that are friendly so far, not spawning soldier xxx");
                    //attackNearbyEnemies(rc);
                    return; // Exit early to avoid setting flag to true
                }

                // Command the Soldier to stay put
                sendToLocation(rc);
                sendMessageToRobots(rc, MOVE_TO_LOCATION_COMMAND, targetLoc, UnitType.SOLDIER, 1);
                saveTurn = 5;
                System.out.println("Spawned Soldier on a paint tile and commanded it to stay put.");
                // return; // Exit early to avoid spawning Soldier in the same turn
            }
        }
        else if (isDefendTower){ // defend mode
            if (!canBuildRobotOnPaintTile(rc)){
                buildRobotOnRandomTile(rc, UnitType.SPLASHER);
                System.out.println("Spawned Splasher on a random tile");
            }

            int mopperCount = countUnitsInTowerRangeOnPaint(rc, UnitType.MOPPER);
            int soldierCount = countUnitsInTowerRangeOnPaint(rc, UnitType.SOLDIER);

            if (!isHealthBelowTwentyPercent(rc)){
                if (soldierCount >= 1){
                    sendToLocation(rc);
                    sendMessageToRobots(rc, MOVE_TO_LOCATION_COMMAND, targetLoc, UnitType.SOLDIER, soldierCount);
                }

                if (mopperCount >= 1){
                    sendMessageToRobots(rc, MOVE_TO_DEFEND_TOWER_COMMAND, enemyLoc, UnitType.MOPPER, mopperCount);
                    System.out.println("TOWER UNDER ATTACKED! SENDING ALL MOPPERS ON PAINT TO FIGHT");
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
                if (soldierCount >= 2){
                    sendToLocation(rc);
                    sendMessageToRobots(rc, MOVE_TO_LOCATION_COMMAND, targetLoc, UnitType.SOLDIER, soldierCount-1);
                }

                if (mopperCount >= 1){
                    sendMessageToRobots(rc, MOVE_TO_DEFEND_TOWER_COMMAND, enemyLoc, UnitType.MOPPER, mopperCount);
                    System.out.println("TOWER UNDER ATTACKED! SENDING ALL MOPPERS ON PAINT TO FIGHT");
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
                    sendMessageToRobots(rc, MOVE_TO_LOCATION_COMMAND, enemyLoc, UnitType.SOLDIER, countSoldierAfterSpawn);
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

            if (soldierCount >= 1){ // send at least 1 soldier out
                System.out.println("Sending out 2 soldiers to attack enemy tower!");
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

    }
}
