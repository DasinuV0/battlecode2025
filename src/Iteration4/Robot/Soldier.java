package Robot;

import battlecode.common.*;
import Navigation.Bug2.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;

// import Iteration3.TowerLogic;

import java.util.LinkedHashSet;

public class Soldier extends Robot {
    MapLocation targetTower;
    Direction dirToEnemy;
    MapLocation lastRuinWithPatternDamagedFoundPos; //ruinLoc 
    int lastRuinWithPatternDamagedFoundRound; // the round we see the ruin

    public Soldier(RobotController _rc) throws GameActionException {
        super(_rc);
        ruinWithPatternDamaged = new HashSet<>();
        lastRuinWithPatternDamagedFoundPos = new MapLocation(-1,-1);
        lastRuinWithPatternDamagedFoundRound = -1;
        originPos = new MapLocation(-1,-1);
        buildingTower = new MapLocation(-1,-1);

        // if (rc.getMapWidth() * rc.getMapHeight() < 450) {
            rushInitialise(_rc);
        // }
    }

    public void rushInitialise(RobotController _rc) {
        if (_rc.getRoundNum() < 10) {
            //.out.println("suicide robot generated");
            this.isSuicideRobot = 1;
        }
    }

    //instructions run at the beginning of each turn
    public void beginTurn() throws GameActionException {
        resetFlags();
        listenMessage();
        updateLowPaintFlag();


        //check if any tower is found
        RobotInfo[] nearbyRobots = rc.senseNearbyRobots();
        for (RobotInfo robot : nearbyRobots)
            //check if our paint/money tower is found
            if (isPaintTower(robot.type) && robot.team == rc.getTeam())
                paintTowersPos.add(robot.location);
            else if (isMoneyTower(robot.type) && robot.team == rc.getTeam())
                moneyTowersPos.add(robot.location);
            else if (robot.team == rc.getTeam() && robot.type == UnitType.MOPPER)
                friendMopperFound = true;
            else if (robot.team != rc.getTeam() && robot.type.isTowerType())
                enemyTowersPos.add(robot.location);


        // Sense information about all visible nearby tiles.
        MapInfo[] nearbyTiles = rc.senseNearbyMapInfos();
        for (MapInfo tile : nearbyTiles)
            // Search all nearby ruins
            if (tile.hasRuin() && rc.senseRobotAtLocation(tile.getMapLocation()) == null && (lastRuinWithPatternDamagedFoundPos.x != tile.getMapLocation().x || lastRuinWithPatternDamagedFoundPos.y != tile.getMapLocation().y) && originPos.x == -1){
                rc.setIndicatorDot(tile.getMapLocation(), 1,23,42);
                ruinsFound.add(tile);
            }
            //search of an emptyTile 
            //TODO: improve this, for now just one the first empty tile
            else if (emptyTile.x == -1 && tile.getPaint() == PaintType.EMPTY && tile.isPassable()){
                emptyTile = tile.getMapLocation();
            }

        //if we saw a ruin, and we ignored that ruin for 50 rounds, start to consider it again
        if (lastRuinWithPatternDamagedFoundRound != -1 && rc.getRoundNum() - lastRuinWithPatternDamagedFoundRound > 50){
            lastRuinWithPatternDamagedFoundPos = new MapLocation(-1,-1);
            lastRuinWithPatternDamagedFoundRound = -1;
        }


        if (lastRuinWithPatternDamagedFoundPos.x != -1)
            rc.setIndicatorDot(lastRuinWithPatternDamagedFoundPos, 255,0,0);
        
        // if suicideRobot, find the enemy tower
        // if (isSuicideRobot == 1) rushTowerFind(rc);
        
    }

    // void rushTowerFind(RobotController _rc) throws GameActionException {
    //     Team myTeam = rc.getTeam();
    //     MapLocation myLocation = rc.getLocation();
    //     if (this.checkedEnemyTower == 0) {
    //         RobotInfo[] nearbyAllyRobots = rc.senseNearbyRobots(-1, myTeam);
    //         if (nearbyAllyRobots[0].getType().isTowerType()) {
    //             //.out.println("ally Tower found at " + nearbyAllyRobots[0].location);
    //             this.checkedEnemyTower = 1;
    //             MapLocation allyTower = nearbyAllyRobots[0].getLocation();
    //             this.targetTower = Symmetry.getEnemyStartingTowerCoord(rc, allyTower);
    //             this.dirToEnemy = myLocation.directionTo(targetTower);
    //         }
    //     }
    // }

    //Instructions at the end of each turn
    public void endTurn() throws GameActionException {
       
    }

    public static void paintSRP(RobotController rc, MapLocation center) throws GameActionException {
        if (rc.canAttack(center)) {
            if(rc.senseMapInfo(center).getPaint() != PaintType.ALLY_SECONDARY)
                rc.attack(center, true);
        }
        else if(rc.senseMapInfo(center).isPassable() && rc.senseMapInfo(center).getPaint() != PaintType.ENEMY_PRIMARY && rc.senseMapInfo(center).getPaint() != PaintType.ENEMY_SECONDARY) {
            Navigation.Bug2.move(center);
            if(rc.senseMapInfo(center).getPaint() != PaintType.ALLY_SECONDARY)
                rc.attack(center, true);
        }

        int[][] attackPositions = {
                {2, 2, 1}, {1, 2, 1}, {2, 1, 1},  // Quadrant 1
                {-2, 2, 1}, {-1, 2, 1}, {-2, 1, 1}, // Quadrant 2
                {-2, -2, 1}, {-1, -2, 1}, {-2, -1, 1}, // Quadrant 3
                {2, -2, 1}, {1, -2, 1}, {2, -1, 1}, // Quadrant 4
                {-1, 0, 0}, {-2, 0, 0}, {1, 0, 0}, {2, 0, 0}, // Horizontal
                {0, -1, 0}, {0, -2, 0}, {0, 1, 0}, {0, 2, 0}, // Vertical
                {1, 1, 0}, {-1, 1, 0}, {-1, -1, 0}, {1, -1, 0} // Diagonal
        };
        for (int[] pos : attackPositions) {
            MapLocation target = new MapLocation(center.x + pos[0], center.y + pos[1]);
            if (rc.canAttack(target)) {
                boolean paintType = false;
                if(rc.senseMapInfo(target).getPaint() == PaintType.ALLY_SECONDARY)
                    paintType = true;
                if(rc.senseMapInfo(target).getPaint() == PaintType.ALLY_PRIMARY)
                    paintType = false;
                if(paintType != (pos[2] == 1))
                    rc.attack(target, pos[2] == 1);
            }
            else if(rc.senseMapInfo(target).isPassable() && rc.senseMapInfo(target).getPaint() != PaintType.ENEMY_PRIMARY && rc.senseMapInfo(center).getPaint() != PaintType.ENEMY_SECONDARY) {
                Navigation.Bug2.move(target);
                boolean paintType = false;
                if(rc.senseMapInfo(target).getPaint() == PaintType.ALLY_SECONDARY)
                    paintType = true;
                if(rc.senseMapInfo(target).getPaint() == PaintType.ALLY_PRIMARY)
                    paintType = false;
                if(paintType != (pos[2] == 1))
                    rc.attack(target, pos[2] == 1);
            }
        }
    }

    MapLocation NearestAllyTower = rc.getLocation();
    MapLocation resourceCenter = new MapLocation(-1,-1);
    //Core turn method
    public void runTurn() throws GameActionException {  
        // if (isSuicideRobot == 1) {
        //     rc.setIndicatorString("suicideRobot");
        //     Navigation.Bug2.move(targetTower);
            
        //     if (rc.canAttack(targetTower)) {
        //         rc.attack(targetTower);
        //     }
        // }     
        //stayPut, moveTarget, ruinWithPatternDamaged, lowPaintFlag, ruinsFound, enemyTowerFound
        /*
        STAYPUT MODE
        paint tower nearby tiles
        don't do anything else

        EXPLORE MODE
        once reached the target loc
        1) if lowPaintFlag or ruinWithPatternDamaged (go back to the tower)
        2) if enemyTowerFound (go back to tower and tell the tower about it then go back to ruindsFound the explore mode) 
        3) ruinsFound (try to build)

        HEAL MODE
        n/a (soldier can't heal)

        REMOVE PATTERN MODE
        n/a (soldier can't clean tile)

         */
        if (stayPut){
            rc.setIndicatorString("stay put");
            MapLocation nearestAllyTower = getNearestAllyTower();
            if (nearestAllyTower.x == -1){
                resetMessageFlag();
                exploreMode = true;
                rc.setIndicatorString("tower is destroyed, go back to explore mode and try to re-build the tower");
                tryToRebuildTower();
                return;
            }
            //paint the robots pos
            tryToPaintAtLoc(rc.getLocation(), PaintType.EMPTY);
            UnitType towerToBuild = rc.senseRobotAtLocation(nearestAllyTower).type;
            //if ruin is found, but pattern is not marked                    && just double check we can mark the tower pattern
            if (rc.senseMapInfo(rc.getLocation()).getMark() == PaintType.EMPTY && rc.canMarkTowerPattern(towerToBuild, nearestAllyTower)){
                //DONE: which tower should the bot build?
                rc.markTowerPattern(towerToBuild, nearestAllyTower);
            }
            //if robots pos is already paint, try to paint tiles near the tower
            for (Direction dir : directions){
                MapLocation newLoc = nearestAllyTower.add(dir);
                if (tryToPaintAtLoc(newLoc, PaintType.EMPTY))
                    return;
            }
            
            Navigation.Bug2.move(nearestAllyTower);
            //if tiles near the tower is already paint, paint the pattern
            for (MapInfo patternTile : rc.senseNearbyMapInfos(nearestAllyTower, 8)){
                if (patternTile.getMark() != patternTile.getPaint()){
                    boolean useSecondaryColor = patternTile.getMark() == PaintType.ALLY_SECONDARY;
                    tryToPaintAtLoc(patternTile.getMapLocation(), useSecondaryColor);
                }
            }

            //if i can get paint from nearestAllyPaintTower
            int localPaintToTake = rc.getPaint() - rc.getType().paintCapacity;

            if (localPaintToTake != 0 && rc.canTransferPaint(nearestAllyTower, localPaintToTake))
                rc.transferPaint(nearestAllyTower, localPaintToTake);

            return;
        }

        else if (exploreMode){
            rc.setIndicatorString("explore mode: move to  " + targetLocation.x + " " + targetLocation.y);

           if (lowPaintFlag == false && (rc.getNumberTowers() >= 25 || ruinsFound.size() == 0) && tryToReachTargetLocation()){
                //reset origin pos, if the bot is close enough to origin pos
                if (originPos.x != -1){
                    rc.setIndicatorDot(originPos, 0,0,244);
                    if (rc.getLocation().distanceSquaredTo(originPos) < 2)
                            originPos = new MapLocation(-1,-1);
                }

                if(rc.getLocation().distanceSquaredTo(getNearestAllyTower()) < rc.getLocation().distanceSquaredTo(NearestAllyTower))
                    NearestAllyTower = getNearestAllyTower();
                //paint while traveling
                MapInfo[] surrMapInfos = rc.senseNearbyMapInfos();
                for (MapInfo mapInfo : surrMapInfos) {
                    MapLocation currentTile = mapInfo.getMapLocation();
                    if(currentTile.x % 5 == 2 && currentTile.y % 5 == 2) {
                        paintSRP(rc, currentTile);
                        resourceCenter = currentTile;
                    }
                }
                if(resourceCenter.x != -1)
                    rc.setIndicatorString("Trying to complete RSP");
                if(rc.canCompleteResourcePattern(resourceCenter) && rc.getLocation().distanceSquaredTo(NearestAllyTower) < 200)
                    rc.completeResourcePattern(resourceCenter);
                rc.setIndicatorString("explore mode: move to  " + targetLocation.x + " " + targetLocation.y);
            }

            if (lowPaintFlag){
                //save origin pos when i'm building the tower and runs out of paint
                if (originPos.x == -1 && (buildingTower.x != -1 || resourceCenter.x != -1)){
                    //reset building tower
                    // buildingTower = new MapLocation(-1,-1);
                    resourceCenter = new MapLocation(-1,-1);
                    originPos = rc.getLocation();
                }
                rc.setIndicatorString("need healing");
                MapLocation nearestAllyTower = getNearestAllyPaintTower();
                if (nearestAllyTower.x == -1)
                    nearestAllyTower = getNearestAllyTower();

                if (nearestAllyTower.x == -1){
                    rc.setIndicatorString("tower is destroyed, go back to explore mode and try to re-build the tower");
                    tryToRebuildTower();
                    return;
                }                


                //if i can get paint from nearestAllyTower
                int localPaintToTake = rc.getPaint() - rc.getType().paintCapacity;
                if (rc.canTransferPaint(nearestAllyTower, localPaintToTake)){
                    rc.transferPaint(nearestAllyTower, localPaintToTake);
                    targetLocation = originPos;
                }
                // if bot is not in the vision range and a mopper is nearby
                else if (!rc.canSenseLocation(nearestAllyTower) && friendMopperFound){
                    return;//stop here and wait for the mopper to give paint
                }
                else{
                    Navigation.Bug2.move(nearestAllyTower);
                    // if (rc.canSendMessage(nearestAllyTower))
                        // rc.sendMessage(nearestAllyTower, OptCode.NEEDPAINT);
                    rc.setIndicatorString("move to (" + nearestAllyTower.x + " " + nearestAllyTower.y + ") to get healed");
                }
                return;
            }

            //if in past round the bot found some tiles (which is part of a pattern) are paint by enemy
            if (ruinWithPatternDamaged.size() > 0){
                MapLocation nearestAllyTower = getNearestAllyTower();
                if (nearestAllyTower.x == -1){
                    rc.setIndicatorString("tower is destroyed, go back to explore mode and try to re-build the tower");
                    tryToRebuildTower();
                    return;
                }
                //reset the target location
                targetLocation = new MapLocation(-1,-1);
                //go to the nearest ally tower 
                Navigation.Bug2.move(nearestAllyTower);
                rc.setIndicatorString("damaged pattern find: going to (" + nearestAllyTower.x + " " + nearestAllyTower.y + ")");

                if (rc.canSendMessage(nearestAllyTower)){
                    MapLocation firstElement = new MapLocation(0, 0);
                    // Get an iterator
                    Iterator<MapLocation> iterator = ruinWithPatternDamaged.iterator();
                    if (iterator.hasNext()) {
                        firstElement = iterator.next();
                        // Remove the first element
                        iterator.remove();
                    }
                    rc.sendMessage(nearestAllyTower, encodeMessage(OptCode.DAMAGEDPATTERN,firstElement)); // DONE: add first firstElement to the messag
                    //.out.println("message sent to (" + nearestAllyTower.x + " " + nearestAllyTower.y + "): damaged pattern found");
                    
                    rc.setIndicatorDot(nearestAllyTower, 0,255,0);
                    rc.setIndicatorString("message sent: damaged pattern found");
                }
                return;
            }

            if (enemyTowersPos.size() > 0){
                //DONE: what to do once an enemy tower is found
                MapLocation nearestAllyTower = getNearestAllyTower();
                if (nearestAllyTower.x == -1){
                    rc.setIndicatorString("tower is destroyed, go back to explore mode and try to re-build the tower");
                    tryToRebuildTower();
                    return;
                }
                //reset the target location
                targetLocation = new MapLocation(-1,-1);
                //go to the nearest ally tower 
                Navigation.Bug2.move(nearestAllyTower);
                rc.setIndicatorString("enemy tower found: going to (" + nearestAllyTower.x + " " + nearestAllyTower.y + ")");


                if (rc.canSendMessage(nearestAllyTower)){
                    MapLocation firstElement = new MapLocation(0, 0);
                    // Get an iterator
                    Iterator<MapLocation> iterator = enemyTowersPos.iterator();
                    if (iterator.hasNext()) {
                        firstElement = iterator.next();
                        // Remove the first element
                        iterator.remove();
                    }
                    rc.sendMessage(nearestAllyTower, encodeMessage(OptCode.ENEMYTOWERFOUND,firstElement)); // DONE: add first firstElement to the messag
                    //.out.println("message sent to (" + nearestAllyTower.x + " " + nearestAllyTower.y + "): damaged pattern found");
                    
                    rc.setIndicatorDot(nearestAllyTower, 0,255,0);
                    rc.setIndicatorString("message sent: enemy tower found");
                }
                return;
            }

            // if more than 2 bot are building this tower, skip it
            if (buildingTower.x != -1 && rc.senseNearbyRobots(buildingTower, 8,rc.getTeam()).length >= 2)
                buildingTower = new MapLocation(-1,-1);
            //if bot already started to build a tower, try to complete that first
            if (buildingTower.x != -1){
                //reset target location
                targetLocation = new MapLocation(-1,-1);

                rc.setIndicatorString("try to build the tower at (" + buildingTower.x + " " + buildingTower.y + ")");
                Navigation.Bug2.move(buildingTower);
                tryToMarkPattern(buildingTower);

                boolean damagedPatternFound = false;

                // Fill in any spots in the pattern with the appropriate paint.
                MapInfo[] nearbyTiles = rc.senseNearbyMapInfos(buildingTower, 8);
                for (MapInfo patternTile : nearbyTiles){
                    //stop building this tower
                    if (patternTile.getPaint().isEnemy())
                        damagedPatternFound = true;

                    //if there is a pattern and it's paint by enemy and it's not the ruin i've seen before
                    if (patternTile.getPaint().isEnemy() && buildingTower.x != lastRuinWithPatternDamagedFoundPos.x && buildingTower.y != lastRuinWithPatternDamagedFoundPos.y){
                        lastRuinWithPatternDamagedFoundPos = buildingTower;
                        lastRuinWithPatternDamagedFoundRound = rc.getRoundNum();
                        ruinWithPatternDamaged.add(buildingTower);
                        damagedPatternFound = true;
                        rc.setIndicatorString("pattern damaged, enemy paint found."); //TODO: find a way to avoid multiple bot find the same enemy tile and all go back to the tower
                    }
                    //if we see an ally mark
                    else if (patternTile.getMark().isAlly()){
                        // Navigation.Bug2.move(buildingTower);

                        //if the tile is not paint as expected (primary color instead of secondary or vice versa or empty paint)
                        if (patternTile.getMark() != patternTile.getPaint()){
                            boolean useSecondaryColor = patternTile.getMark() == PaintType.ALLY_SECONDARY;
                            tryToPaintAtLoc(patternTile.getMapLocation(), useSecondaryColor);
                            rc.setIndicatorString("try to build the tower at (" + buildingTower.x + " " + buildingTower.y + ")");
                        }
                    }
                }

                tryToBuildTower(buildingTower);
                //if building tower is within the vision range, check if the tower is built
                if (rc.canSenseLocation(buildingTower) && rc.canSenseRobotAtLocation(buildingTower)){
                    buildingTower = new MapLocation(-1,-1);
                    lastRuinWithPatternDamagedFoundPos = new MapLocation(-1,-1);
                    lastRuinWithPatternDamagedFoundRound = -1;
                }

                if (damagedPatternFound)
                    buildingTower = new MapLocation(-1,-1);
                    
                return;
            }

            if (buildingTower.x == -1 && ruinsFound.size() > 0 && rc.getNumberTowers() < 25){
                Iterator<MapInfo> iterator = ruinsFound.iterator();
                targetLocation = new MapLocation(-1,-1);
                // targetLocation = new MapLocation(-1,-1);
                while (iterator.hasNext()) {
                    MapInfo curRuin = iterator.next();
                    //ignore curRuin if it is the lastRuinWithPatternDamagedFound
                    if (curRuin.getMapLocation().x == lastRuinWithPatternDamagedFoundPos.x && curRuin.getMapLocation().y == lastRuinWithPatternDamagedFoundPos.y){
                        iterator.remove();
                        continue;
                    }

                    if (rc.senseNearbyRobots(curRuin.getMapLocation(), 8,rc.getTeam()).length >= 2){
                        iterator.remove();
                        continue;
                    }
                    
                    rc.setIndicatorString("ruins found");
                    buildingTower = curRuin.getMapLocation();
                    Navigation.Bug2.move(curRuin.getMapLocation());
                    tryToMarkPattern(curRuin);

                    // Fill in any spots in the pattern with the appropriate paint.
                    for (MapInfo patternTile : rc.senseNearbyMapInfos(curRuin.getMapLocation(), 8)){
                        //if there is a pattern and it's paint by enemy
                        if (patternTile.getPaint().isEnemy() && curRuin.getMapLocation().x != lastRuinWithPatternDamagedFoundPos.x && curRuin.getMapLocation().y != lastRuinWithPatternDamagedFoundPos.y){
                            lastRuinWithPatternDamagedFoundPos = curRuin.getMapLocation();
                            lastRuinWithPatternDamagedFoundRound = rc.getRoundNum();
                            ruinWithPatternDamaged.add(curRuin.getMapLocation());
                            rc.setIndicatorDot(curRuin.getMapLocation(), 25,14,24); //TODO: find a way to avoid multiple bot find the same enemy tile and all go back to the tower
                            rc.setIndicatorString("pattern damaged, enemy paint found."); //TODO: find a way to avoid multiple bot find the same enemy tile and all go back to the tower
                            return;
                        }
                        //if we see an ally mark
                        else if (patternTile.getMark().isAlly()){
                            Navigation.Bug2.move(curRuin.getMapLocation());

                            //if the tile is not paint as expected (primary color instead of secondary or vice versa or empty paint)
                            if (patternTile.getMark() != patternTile.getPaint()){
                                boolean useSecondaryColor = patternTile.getMark() == PaintType.ALLY_SECONDARY;
                                tryToPaintAtLoc(patternTile.getMapLocation(), useSecondaryColor);
                                rc.setIndicatorString("try to build the tower");
                            }
                        }
                    }

                    tryToBuildTower(curRuin);
                    return;//just take the first ruin found and build it
                }
            
            }

            //if so far, it din't move, explore unpaint tile (TODO: that is whitin the region)
            if (emptyTile.x != -1 && rc.isMovementReady()){
                Navigation.Bug2.move(emptyTile);
                rc.setIndicatorString("move to a random empty tile");
                tryToPaintAtLoc(rc.getLocation(), PaintType.EMPTY);
            }

            // if Middle game, look for enemy paint zone and send message to tower
            if (rc.getRoundNum() > EARLY_GAME_TURNS) {
                MapLocation enemyPaintZone = getEnemyPaintZone(rc);
                if (enemyPaintZone.x != -1 && enemyPaintZone.y != -1) {
                    MapLocation nearestAllyTower = getNearestAllyTower();
                    if (nearestAllyTower.x != -1 && nearestAllyTower.y != -1) {
                        Navigation.Bug2.move(nearestAllyTower);

                        int messageContent = encodeMessage(10, enemyPaintZone);
                        if (rc.canSendMessage(nearestAllyTower)) {
                            rc.sendMessage(nearestAllyTower, messageContent);
                        }
                    }
                }
            }
        
            //if it still didn't move, bugnav to a random pos
            if (rc.isMovementReady() && targetLocation.x == -1){
                int x = rng.nextInt(rc.getMapWidth());
                int y = rng.nextInt(rc.getMapHeight());
                targetLocation = new MapLocation(x,y);
                rc.setIndicatorString("move to" + x + " " + y);
                Navigation.Bug2.move(targetLocation);
            }
        }
        
        else if (removePatterMode){
            rc.setIndicatorString("remove pattern mode");
            if (tryToReachTargetLocation()){
                //paint while traveling
                MapInfo currentTile = rc.senseMapInfo(rc.getLocation());
                if (currentTile.getPaint() == PaintType.EMPTY && rc.canAttack(rc.getLocation())){
                    boolean useSecondaryColor = rc.senseMapInfo(rc.getLocation()).getMark() == PaintType.ALLY_SECONDARY;
                    rc.attack(rc.getLocation(), useSecondaryColor);
                }

                rc.setIndicatorString("removePatternMode mode: move to  " + targetLocation.x + " " + targetLocation.y);
                return;
            }
            //once reached the target loc

            //if in past round the bot found some tiles (which is part of a pattern) are paint by enemy
            if (ruinWithPatternDamaged.size() > 0){
                MapLocation nearestAllyTower = getNearestAllyTower();
                if (nearestAllyTower.x == -1){
                    resetMessageFlag();
                    exploreMode = true;
                    rc.setIndicatorString("tower is destroyed, go back to explore mode and try to re-build the tower");
                    tryToRebuildTower();
                    return;
                }
                Navigation.Bug2.move(nearestAllyTower);
                rc.setIndicatorString("damaged pattern find: going to (" + nearestAllyTower.x + " " + nearestAllyTower.y + ")");

                if (rc.canSendMessage(nearestAllyTower)){
                    MapLocation firstElement = new MapLocation(0, 0);
                    // Get an iterator
                    Iterator<MapLocation> iterator = ruinWithPatternDamaged.iterator();
                    if (iterator.hasNext()) {
                        firstElement = iterator.next();
                        // Remove the first element
                        iterator.remove();
                    }
                    rc.sendMessage(nearestAllyTower, encodeMessage(OptCode.DAMAGEDPATTERN,firstElement)); // DONE: add first firstElement to the messag
                    //.out.println("message sent to (" + nearestAllyTower.x + " " + nearestAllyTower.y + "): damaged pattern found");
                    
                    rc.setIndicatorDot(nearestAllyTower, 0,255,0);
                    rc.setIndicatorString("message sent: damaged pattern found");
                }
                return;
            }

            if (lowPaintFlag){
                rc.setIndicatorString("need healing");
                MapLocation nearestAllyTower = getNearestAllyPaintTower();
                if (nearestAllyTower.x == -1)
                    nearestAllyTower = getNearestAllyTower();
                if (nearestAllyTower.x == -1){
                    resetMessageFlag();
                    exploreMode = true;
                    rc.setIndicatorString("tower is destroyed, go back to explore mode and try to re-build the tower");
                    tryToRebuildTower();
                    return;
                }                


                //if i can get paint from nearestAllyTower
                int localPaintToTake = rc.getPaint() - rc.getType().paintCapacity;
                if (rc.canTransferPaint(nearestAllyTower, localPaintToTake)){
                    rc.transferPaint(nearestAllyTower, localPaintToTake);
                }
                else if (friendMopperFound){
                    return;//stop here and wait for the mopper to give paint
                }
                else{
                    Navigation.Bug2.move(nearestAllyTower);
                    // if (rc.canSendMessage(nearestAllyTower))
                        // rc.sendMessage(nearestAllyTower, OptCode.NEEDPAINT);
                    rc.setIndicatorString("move to get healed");
                }
                return;
            }

            // if (enemyTowerFound){//don't do anything when enemytower is found
            //     //TODO: what to do once an enemy tower is found
            //     return;
            // 

            if (ruinsFound.size() > 0){
                Iterator<MapInfo> iterator = ruinsFound.iterator();
                while (iterator.hasNext()) {
                    MapInfo curRuin = iterator.next();
                    //ignore curRuin if it is the lastRuinWithPatternDamagedFound
                    if (curRuin.getMapLocation().x == lastRuinWithPatternDamagedFoundPos.x && curRuin.getMapLocation().y == lastRuinWithPatternDamagedFoundPos.y){
                        iterator.remove();
                        continue;
                    }

                    if (rc.senseNearbyRobots(curRuin.getMapLocation(), 8,rc.getTeam()).length >= 2)
                        continue;

                    tryToMarkPattern(curRuin);

                    // Fill in any spots in the pattern with the appropriate paint.
                    for (MapInfo patternTile : rc.senseNearbyMapInfos(curRuin.getMapLocation(), 8)){
                        //if we see an ally mark
                        if (patternTile.getMark().isAlly()){
                            Navigation.Bug2.move(curRuin.getMapLocation());

                            //if the tile is not paint as expected (primary color instead of secondary or vice versa or empty paint)
                            if (patternTile.getMark() != patternTile.getPaint()){
                                boolean useSecondaryColor = patternTile.getMark() == PaintType.ALLY_SECONDARY;
                                tryToPaintAtLoc(patternTile.getMapLocation(), useSecondaryColor);
                                rc.setIndicatorString("try to build the tower");
                            }
                        }
                        //if there is a pattern and it's paint by enemy
                        else if (patternTile.getPaint().isEnemy() && curRuin.getMapLocation().x != lastRuinWithPatternDamagedFoundPos.x && curRuin.getMapLocation().y != lastRuinWithPatternDamagedFoundPos.y){
                            lastRuinWithPatternDamagedFoundPos = curRuin.getMapLocation();
                            lastRuinWithPatternDamagedFoundRound = rc.getRoundNum();
                            ruinWithPatternDamaged.add(curRuin.getMapLocation());
                            rc.setIndicatorDot(patternTile.getMapLocation(), 25,14,24); //TODO: find a way to avoid multiple bot find the same enemy tile and all go back to the tower
                            rc.setIndicatorString("pattern damaged, enemy paint found."); //TODO: find a way to avoid multiple bot find the same enemy tile and all go back to the tower
                        }
                    }

                    tryToBuildTower(curRuin);
                }
            }

            //if so far, it din't move, explore unpaint tile (TODO: that is whitin the region)
            if (emptyTile.x != -1 && rc.isMovementReady()){
                Navigation.Bug2.move(emptyTile);
                rc.setIndicatorString("move to a random empty tile (" + emptyTile.x + " " + emptyTile.y + ")s");
                tryToPaintAtLoc(rc.getLocation(), PaintType.EMPTY);
            }
        
            //if it still didn't move, bugnav to a random pos
            if (rc.isMovementReady() && targetLocation.x == -1){
                int x = rng.nextInt(rc.getMapWidth());
                int y = rng.nextInt(rc.getMapHeight());
                targetLocation = new MapLocation(x,y);
                rc.setIndicatorString("move to" + x + " " + y);
                Navigation.Bug2.move(targetLocation);
            }
        }

        else if (attackMode){
            rc.setIndicatorString("attack mode");
            if (tryToReachTargetLocation(8)){
                //paint while traveling
                MapInfo currentTile = rc.senseMapInfo(rc.getLocation());
                if (currentTile.getPaint() == PaintType.EMPTY && rc.canAttack(rc.getLocation())){
                    boolean useSecondaryColor = rc.senseMapInfo(rc.getLocation()).getMark() == PaintType.ALLY_SECONDARY;
                    rc.attack(rc.getLocation(), useSecondaryColor);
                }

                rc.setIndicatorString("attack mode: move to  " + targetLocation.x + " " + targetLocation.y + " with distance: " + rc.getLocation().distanceSquaredTo(targetLocation));
            }else{
                // RobotInfo enemyTower = rc.senseRobotAtLocation(targetLocation);

                rc.setIndicatorString("attack mode: attack enemy tower (" + targetLocation.x + " " + targetLocation.y + ")");
                //attack -> move away, next turn move -> attack (attack twice faster than towers)
                if (rc.canAttack(targetLocation))
                    rc.attack(targetLocation);
                
                Direction dir = rc.getLocation().directionTo(targetLocation).opposite();
                if (rc.canMove(dir))
                    rc.move(dir);
                else if (rc.canMove(dir.rotateLeft()))//if can't move try to move to adjacente 
                    rc.move(dir.rotateLeft());
                else if (rc.canMove(dir.rotateRight()))
                    rc.move(dir.rotateRight());//TODO: smarter way to go outside the attack range

                //if enemy tower is destroyed
                if (rc.canSenseRobotAtLocation(targetLocation) == false){
                    rc.setIndicatorString("Tower destroyed, go to explore mode");
                    resetMessageFlag();
                    exploreMode = true;
                }
                
            }
        }
        
        else{
            rc.setIndicatorString("No command");
        }

    }
}
