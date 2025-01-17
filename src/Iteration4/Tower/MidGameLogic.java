package Tower;

import battlecode.common.*;

public class MidGameLogic extends TowerLogic{

    static void runMidGame(RobotController rc) throws GameActionException{
        //System.out.println("Running Early Game Logic");
//        Direction dir = directions[rng.nextInt(directions.length)];
//        MapLocation nextLoc = rc.getLocation().add(dir);
        int currentTurn = rc.getRoundNum();

        // System.out.println(rc.getMoney());
        // always spawn a soldier at a random tile first

        MapLocation enemyLoc = detectEnemyOnDamage(rc);
        if (enemyLoc != null){
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


        if (saveTurn > 0){ // after non-default command is done, set a delay of 5 turns so bots can move out of range first
            System.out.println("Currently is a saving turn: " + saveTurn);
            saveTurn--;
            attackNearbyEnemies(rc);
            return;
        }

        if (isDefault){ // exploration mode
            int soldierCount = countUnitsInTowerRangeOnPaint(rc, UnitType.SOLDIER);
            if (!randomSoldierSpawnedYet){
                buildRobotOnRandomTile(rc, UnitType.SOLDIER);
                System.out.println("Spawned Soldier on a random tile");
                randomSoldierSpawnedYet = true;
                attackNearbyEnemies(rc);
                return;
            }

            if (soldierCount >= 2){
                sendToLocation(rc);
                sendMessageToRobots(rc, MOVE_TO_LOCATION_COMMAND, targetLoc, UnitType.SOLDIER, 1);
                saveTurn = 3;
                System.out.println("Sent a soldier out to explore: " + targetLoc);
            }
            else{
                if (!buildRobotOnPaintTile(rc, UnitType.SOLDIER)){
                    System.out.println("No tiles found that are friendly so far, not spawning soldier xxx");
                    attackNearbyEnemies(rc);
                    return; // Exit early to avoid setting flag to true
                }

                // Command the Soldier to stay put
                sendMessageToRobots(rc, STAY_PUT_COMMAND, null, UnitType.SOLDIER, 1);
                System.out.println("Spawned Soldier on a paint tile and commanded it to stay put.");
                // return; // Exit early to avoid spawning Soldier in the same turn
            }
        }
        else if (isDefendTower){ // defend mode
            int mopperCount = countUnitsInTowerRangeOnPaint(rc, UnitType.MOPPER);
            int soldierCount = countUnitsInTowerRangeOnPaint(rc, UnitType.SOLDIER);

            // while defending, if i have so many soldiers, just send them out
            // since they are pointless in defending, but leave some for other tasks when
            // defending is completed
            if (soldierCount >= 2){
                sendToLocation(rc);
                sendMessageToRobots(rc, MOVE_TO_LOCATION_COMMAND, targetLoc, UnitType.SOLDIER, 1);
            }

            if (mopperCount >= 2){
                sendMessageToRobots(rc, MOVE_TO_DEFEND_TOWER_COMMAND, enemyLoc, UnitType.MOPPER, mopperCount);
                System.out.println("TOWER UNDER ATTACKED! SENDING ALL MOPPERS ON PAINT TO FIGHT");
            }
            else{ // try to have 2 moppers defend, if not enough, spawn if can
                System.out.println("NOT ENOUGH MOPPER TO DEFEND, TRYING TO SPAWN MORE!!!!");
                if (!buildRobotOnPaintTile(rc, UnitType.MOPPER)){
                    System.out.println("NO PAINT TILE DETECTED, CAN'T SPAWN MOPPER, MAYDAY!!!");
                    attackNearbyEnemies(rc);
                    return;
                }
                System.out.println("SPAWNED A MOPPER TO DEFEND TOWER!!!");
            }
        }
        else if (isEnemyTowerFound){ // attack tower mode
            System.out.println("Enemy tower found, now preparing robots to attack");
            int soldierCount = countUnitsInTowerRangeOnPaint(rc, UnitType.SOLDIER);
            int splasherCount = countUnitsInTowerRangeOnPaint(rc, UnitType.SPLASHER);

            if (splasherCount < 1){
                if (!buildRobotOnPaintTile(rc, UnitType.SPLASHER)){
                    System.out.println("No paint tile detected, not spawning soldier");
                    attackNearbyEnemies(rc);
                    return;
                }

                System.out.println("1 Splasher spawned on paint tile...");
                sendMessageToRobots(rc, STAY_PUT_COMMAND, targetLoc, UnitType.SPLASHER, 1);
            }
            else if (soldierCount < 2){
                if (!buildRobotOnPaintTile(rc, UnitType.SOLDIER)){
                    System.out.println("No paint tile detected, not spawning soldier");
                    attackNearbyEnemies(rc);
                    return;
                }

                System.out.println("1 Soldier spawned on paint tile, preparing more soldiers...");
                sendMessageToRobots(rc, STAY_PUT_COMMAND, targetLoc, UnitType.SOLDIER, 2);
            }
            else{ // send 2 soldiers + 1 splasher out
                System.out.println("Sending out 2 soldiers + 1 splasher to attack enemy tower!");
                sendMessageToRobots(rc, MOVE_TO_ATTACK_TOWER_COMMAND, targetLoc, UnitType.SOLDIER, 2);
                sendMessageToRobots(rc, MOVE_TO_ATTACK_TOWER_COMMAND, targetLoc, UnitType.SPLASHER, 1);
                isEnemyTowerFound = false;
                isDefault = true;
                saveTurn = 5;
            }

        }
        else if (isDamagedPatternFound){ // damage pattern found mode
            System.out.println("Oh no! Damaged pattern is found!");
            int mopperCount = countUnitsInTowerRangeOnPaint(rc, UnitType.MOPPER);
            int soldierCount = countUnitsInTowerRangeOnPaint(rc, UnitType.SOLDIER);

            // save resources if i have the unit already
            if (mopperCount >= 2 && soldierCount >= 1){ // dont spawn any if we have the bots already
                sendToLocation(rc);
                sendMessageToRobots(rc, MOVE_TO_LOCATION_COMMAND, targetLoc, UnitType.MOPPER, 2);
                sendMessageToRobots(rc, MOVE_TO_LOCATION_COMMAND, targetLoc, UnitType.SOLDIER, 1);
                attackNearbyEnemies(rc);
                saveTurn = 5;
                return;
            }

            if (!calledSoldierStayPutYet){
                sendMessageToRobots(rc, STAY_PUT_COMMAND, null, UnitType.SOLDIER, 1);
                System.out.println("One soldier is stayed put now");
                calledSoldierStayPutYet = true;
            }

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
                int currentMopperCountAfterSpawn = countUnitsInTowerRangeOnPaint(rc, UnitType.MOPPER);
                sendMessageToRobots(rc, STAY_PUT_COMMAND, null, UnitType.MOPPER, currentMopperCountAfterSpawn);
            }
            else if (mopperCount >= 2){
                sendMessageToRobots(rc, MOVE_TO_DAMAGED_PATTERN_COMMAND, targetLoc, UnitType.MOPPER, 2);
                sendMessageToRobots(rc, MOVE_TO_DAMAGED_PATTERN_COMMAND, targetLoc, UnitType.SOLDIER, 1);
                System.out.println("soldier + 2 moppers combo ready, sending out to " + targetLoc);
                isDamagedPatternFound = false;
                calledSoldierStayPutYet = false;
                calledMoppersStayPutYet = false;
                isDefault = true;
                saveTurn = 5;
            }
        }
        else if (isAttackSplasherNeeded){
            int splasherCount = countUnitsInTowerRangeOnPaint(rc, UnitType.SPLASHER);

            if (splasherCount >= 1) {
                sendMessageToRobots(rc, MOVE_TO_ATTACK_USING_SPLASHER_COMMAND, targetLoc, UnitType.SPLASHER, 1);
                System.out.println("Sending an attack splasher to: " + targetLoc);
                isAttackSplasherNeeded = false;
                isDefault = true;
                saveTurn = 5;
            }
            else{
                if (!buildRobotOnPaintTile(rc, UnitType.SPLASHER)){
                    System.out.println("Not enough paint, not spawning SPLASHER");
                    attackNearbyEnemies(rc);
                    return; // Exit early to avoid sending wrong splasher message
                }
                System.out.println("Spawned SPLASHER on a paint tile and commanded it to stay put.");
                sendMessageToRobots(rc, STAY_PUT_COMMAND, null, UnitType.SPLASHER, 1);
            }
        }
        else if (isDefendSplasherNeeded){
            int splasherCount = countUnitsInTowerRangeOnPaint(rc, UnitType.SPLASHER);

            if (splasherCount >= 1) {
                sendMessageToRobots(rc, MOVE_TO_ATTACK_USING_SPLASHER_COMMAND, targetLoc, UnitType.SPLASHER, 1);
                System.out.println("Sending a defend splasher to: " + targetLoc);
                isDefendSplasherNeeded = false;
                isDefault = true;
                saveTurn = 5;
            }
            else{
                if (!buildRobotOnPaintTile(rc, UnitType.SPLASHER)){
                    System.out.println("Not enough paint, not spawning SPLASHER");
                    attackNearbyEnemies(rc);
                    return; // Exit early to avoid sending wrong mopper message
                }
                System.out.println("Spawned SPLASHER on a paint tile and commanded it to stay put.");
                sendMessageToRobots(rc, STAY_PUT_COMMAND, null, UnitType.SPLASHER, 1);
            }
        }

        attackNearbyEnemies(rc);
    }
}
