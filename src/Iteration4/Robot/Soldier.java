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
    MapLocation resourceCenter = new MapLocation(-1,-1); //for SRP
    Set<MapLocation> invalidResourceCenter = new HashSet<>(); //for SRP, this saves centers that has walls/towers/ruins that make this center ivalid
    Map<MapLocation, Integer> temporaryInvalidResourceCenter = new HashMap<>(); //for SRP, location as key and round number as val
    Map<MapLocation, Integer> temporaryInvalidEnemyPaintZone = new HashMap<>(); //for SRP, location as key and round number as val
    MapLocation enemyPaintZone = new MapLocation(-1,-1); // for enemy paint zone
    int damagedPatternFoundMessageSentCount = 0;
    int enemyTowerMessageSentCount = 0;
    int mapArea;

    public Soldier(RobotController _rc) throws GameActionException {
        super(_rc);
        ruinWithPatternDamaged = new HashSet<>();
        lastRuinWithPatternDamagedFoundPos = new MapLocation(-1,-1);
        lastRuinWithPatternDamagedFoundRound = -1;
        originPos = new MapLocation(-1,-1);
        buildingTower = new MapLocation(-1,-1);
        mapArea = _rc.getMapHeight() * _rc.getMapWidth();

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
        tryToSendPaintPos(); //send nearest paint location to money tower when it's possible

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
        for (MapInfo tile : nearbyTiles){
            // Search all nearby ruins
            if (tile.hasRuin() && rc.senseRobotAtLocation(tile.getMapLocation()) == null && (lastRuinWithPatternDamagedFoundPos.x != tile.getMapLocation().x || lastRuinWithPatternDamagedFoundPos.y != tile.getMapLocation().y) && originPos.x == -1){
                rc.setIndicatorDot(tile.getMapLocation(), 1,23,42);
                ruinsFound.add(tile);
                
                //if we already found a resource center, ignore it if tile is too close to the center
                if (resourceCenter.x == -1){}
            
            }
            //search of an emptyTile 
            //TODO: improve this, for now just one the first empty tile
            else if (emptyTile.x == -1 && tile.getPaint() == PaintType.EMPTY && tile.isPassable()){
                emptyTile = tile.getMapLocation();
            }
        }

        //if we saw a ruin, and we ignored that ruin for 50 rounds, start to consider it again
        if (lastRuinWithPatternDamagedFoundRound != -1 && rc.getRoundNum() - lastRuinWithPatternDamagedFoundRound > 50){
            lastRuinWithPatternDamagedFoundPos = new MapLocation(-1,-1);
            lastRuinWithPatternDamagedFoundRound = -1;
        }

        //search for a "good" spot for resource centre, if resource centre is not already found
        final int THRESHOLD = mapArea <= 1200 ? 4 : 5;
        if (resourceCenter.x == -1 && rc.getNumberTowers() >= THRESHOLD){
            MapLocation nearestAllyTower = getNearestAllyTower();
            MapInfo[] surrMapInfos = rc.senseNearbyMapInfos();
            for (MapInfo mapInfo : surrMapInfos) {
                if (mapInfo.isPassable() == false)
                    continue;
                
        
                MapLocation currentTile = mapInfo.getMapLocation();
                if(currentTile.x % 4 == 2 && currentTile.y % 4 == 2) {
                    //skip completed resourcePatternCenter 
                    if (mapInfo.isResourcePatternCenter() && mapInfo.getPaint().isAlly())
                        continue;
                    //skip center that are at the edge of the map (we can't make SRP at edge)
                    if (rc.getMapWidth() - currentTile.x <= 2 || rc.getMapHeight() - currentTile.y <= 2)
                        continue;
                    // if currentTile is temporary considered as a invalid center, wait 50 rounds to check this tile again
                    if (temporaryInvalidResourceCenter.containsKey(currentTile) && rc.getRoundNum() - temporaryInvalidResourceCenter.get(currentTile) < 50)
                        continue;
                    // if currentTile is considered as a invalid center, never consider it again
                    if (invalidResourceCenter.contains(currentTile))
                        continue;

                    //if near currentTile there is a ruin. And the x dist is < 4 || y dist is < 4, ignore it
                    boolean flag = false; //this flag check whether there is a ruin near curretTile
                    for (MapInfo ruin : ruinsFound)
                        if(Math.abs(currentTile.x - ruin.getMapLocation().x) <= 4 || Math.abs(currentTile.y - ruin.getMapLocation().y) <= 4){
                            flag = true;
                            break;
                        }

                    if (flag)
                        continue;

                    resourceCenter = currentTile;
                    // paintSRP(resourceCenter);
                    break;
                }
            }
        }


        // if (lastRuinWithPatternDamagedFoundPos.x != -1)
        //     rc.setIndicatorDot(lastRuinWithPatternDamagedFoundPos, 255,0,0);
        
        
    }

    //Instructions at the end of each turn
    public void endTurn() throws GameActionException {
       
    }

    public static void buildMoneyTower(RobotController rc, MapLocation center) throws GameActionException {
        Navigation.Bug2.move(center);
        rc.mark(new MapLocation(center.x, center.y-1), false);
        int[][] attackPositions = {
                {-2, 2, 0}, {-1, 2, 1}, {0, 2, 1}, {1, 2, 1}, {2, 2, 0}, // 1st line
                {-2, 1, 1}, {-1, 1, 1}, {0, 1, 0}, {1, 1, 1}, {2, 1, 1}, // 2nd line
                {-2, 0, 1}, {-1, 0, 0}, {1, 0, 0}, {2, 0, 1}, // 3rd line
                {-2, -1, 1}, {-1, -1, 1}, {0, -1, 0}, {1, -1, 1}, {2, -1, 1}, // 4th line
                {-2, -2, 0}, {-1, -2, 1}, {0, -2, 1}, {1, -2, 1}, {2, -2, 0} // 5th line
        };
        for (int[] pos : attackPositions) {
            MapLocation target = new MapLocation(center.x + pos[0], center.y + pos[1]);
            if (rc.canAttack(target)) {
                boolean paintType = false;
                if(rc.senseMapInfo(target).getPaint() == PaintType.ALLY_SECONDARY)
                    paintType = true;
                else if(rc.senseMapInfo(target).getPaint() == PaintType.ALLY_PRIMARY)
                    paintType = false;
                if((paintType != (pos[2] == 1)) || (rc.senseMapInfo(target).getPaint() == PaintType.EMPTY))
                    rc.attack(target, pos[2] == 1);
            }
        }
    }

    public static void buildPaintTower(RobotController rc, MapLocation center) throws GameActionException {
        Navigation.Bug2.move(center);
        rc.mark(new MapLocation(center.x, center.y-1), true);
        int[][] attackPositions = {
                {-2, 2, 1}, {-1, 2, 0}, {0, 2, 0}, {1, 2, 0}, {2, 2, 1}, // 1st line
                {-2, 1, 0}, {-1, 1, 1}, {0, 1, 0}, {1, 1, 1}, {2, 1, 0}, // 2nd line
                {-2, 0, 0}, {-1, 0, 0}, {1, 0, 0}, {2, 0, 0}, // 3rd line
                {-2, -1, 0}, {-1, -1, 1}, {0, -1, 0}, {1, -1, 1}, {2, -1, 0}, // 4th line
                {-2, -2, 1}, {-1, -2, 0}, {0, -2, 0}, {1, -2, 0}, {2, -2, 1} // 5th line
        };
        for (int[] pos : attackPositions) {
            MapLocation target = new MapLocation(center.x + pos[0], center.y + pos[1]);
            if (rc.canAttack(target)) {
                boolean paintType = false;
                if(rc.senseMapInfo(target).getPaint() == PaintType.ALLY_SECONDARY)
                    paintType = true;
                else if(rc.senseMapInfo(target).getPaint() == PaintType.ALLY_PRIMARY)
                    paintType = false;
                if((paintType != (pos[2] == 1)) || (rc.senseMapInfo(target).getPaint() == PaintType.EMPTY))
                    rc.attack(target, pos[2] == 1);
            }
        }
    }

    public static void buildDefenseTower(RobotController rc, MapLocation center) throws GameActionException {
        Navigation.Bug2.move(center);
        rc.mark(new MapLocation(center.x, center.y-1), false);
        int[][] attackPositions = {
                {-2, 2, 0}, {-1, 2, 0}, {0, 2, 1}, {1, 2, 0}, {2, 2, 0}, // 1st line
                {-2, 1, 0}, {-1, 1, 1}, {0, 1, 1}, {1, 1, 1}, {2, 1, 0}, // 2nd line
                {-2, 0, 1}, {-1, 0, 1}, {1, 0, 1}, {2, 0, 1}, // 3rd line
                {-2, -1, 0}, {-1, -1, 1}, {0, -1, 1}, {1, -1, 1}, {2, -1, 0}, // 4th line
                {-2, -2, 0}, {-1, -2, 0}, {0, -2, 1}, {1, -2, 0}, {2, -2, 0} // 5th line
        };
        for (int[] pos : attackPositions) {
            MapLocation target = new MapLocation(center.x + pos[0], center.y + pos[1]);
            if (rc.canAttack(target)) {
                boolean paintType = false;
                if(rc.senseMapInfo(target).getPaint() == PaintType.ALLY_SECONDARY)
                    paintType = true;
                else if(rc.senseMapInfo(target).getPaint() == PaintType.ALLY_PRIMARY)
                    paintType = false;
                if((paintType != (pos[2] == 1)) || (rc.senseMapInfo(target).getPaint() == PaintType.EMPTY))
                    rc.attack(target, pos[2] == 1);
            }
        }
    }
    
    UnitType nextTowerToBuild() throws GameActionException{
        int knownTower = paintTowersPos.size() + moneyTowersPos.size();
        int totTower = rc.getNumberTowers();
        int percetange = knownTower / totTower * 100;

        if (percetange > 40){
            if (paintTowersPos.size() < moneyTowersPos.size())
                return UnitType.LEVEL_ONE_PAINT_TOWER;
            else
                return UnitType.LEVEL_ONE_MONEY_TOWER;
        }else{
            if (rc.getNumberTowers() % 2 == 0)
                return UnitType.LEVEL_ONE_MONEY_TOWER;
            else
                return UnitType.LEVEL_ONE_PAINT_TOWER;
        }
    }

    void paintSRP(MapLocation center) throws GameActionException {
        if (rc.canPaint(center)) {
            if(rc.senseMapInfo(center).getPaint() != PaintType.ALLY_SECONDARY)
                rc.attack(center, true);
        }
        // else if(rc.senseMapInfo(center).isPassable() && rc.senseMapInfo(center).getPaint() != PaintType.ENEMY_PRIMARY && rc.senseMapInfo(center).getPaint() != PaintType.ENEMY_SECONDARY) {
        //     Navigation.Bug2.move(center);
        //     if(rc.senseMapInfo(center).getPaint() != PaintType.ALLY_SECONDARY)
        //         rc.attack(center, true);
        // }
        rc.setIndicatorDot(center, 255,255,0);
        int[][] attackPositions = {
                {2, 2, 1}, {1, 2, 1}, {2, 1, 1},  // Quadrant 1
                {-2, 2, 1}, {-1, 2, 1}, {-2, 1, 1}, // Quadrant 2
                {-2, -2, 1}, {-1, -2, 1}, {-2, -1, 1}, // Quadrant 3
                {2, -2, 1}, {1, -2, 1}, {2, -1, 1}, // Quadrant 4
                {-1, 0, 0}, {-2, 0, 0}, {1, 0, 0}, {2, 0, 0}, // Horizontal
                {0, -1, 0}, {0, -2, 0}, {0, 1, 0}, {0, 2, 0}, // Vertical
                {1, 1, 0}, {-1, 1, 0}, {-1, -1, 0}, {1, -1, 0} // Diagonal
        };

        boolean paintTile = false;
        boolean unpaintableTile = false;
        boolean enemyTileFound = false;
        for (int[] pos : attackPositions) {
            MapLocation target = new MapLocation(center.x + pos[0], center.y + pos[1]);
            if (rc.canSenseLocation(target) == false)
                continue; //skip tiles that are not in the vision range

            //if target is a wall, ignore this center forever
            if (rc.senseMapInfo(target).isPassable() == false)
                unpaintableTile = true;

            //if target is paint by enemy, ignore this center  for 50 turns
            if (rc.senseMapInfo(target).getPaint().isEnemy())
                enemyTileFound = true;
            

            if (rc.canPaint(target)) {
                int paintType = -1;
                rc.setIndicatorDot(target,13,241,4);
                if(rc.senseMapInfo(target).getPaint() == PaintType.ALLY_SECONDARY)
                    paintType = 1;
                if(rc.senseMapInfo(target).getPaint() == PaintType.ALLY_PRIMARY)
                    paintType = 0;
                int expectedPaintType = (pos[2] == 1) ? 1 : 0;
                if(paintType != expectedPaintType && rc.canAttack(target)){
                    rc.attack(target, pos[2] == 1);
                    paintTile = true;
                }
            }

        }

        if (unpaintableTile){
            invalidResourceCenter.add(resourceCenter);
            resourceCenter = new MapLocation(-1,-1);
        }

        if (enemyTileFound){
            temporaryInvalidResourceCenter.put(resourceCenter, rc.getRoundNum());
            resourceCenter = new MapLocation(-1,-1);
        }
        
        // //if i'm at the center and i can't paint anything, skip this center and set it as a invalid center
        // if (rc.getLocation().x == center.x && rc.getLocation().y == center.y  && !paintTile){
        //     invalidResourceCenter.put(resourceCenter, rc.getRoundNum());
        //     resourceCenter = new MapLocation(-1,-1);
        // }
        Navigation.Bug2.move(center);
    }

    void paintSRPWithNoMoving() throws GameActionException {
        // if (rc.canPaint(center)) {
        //     if(rc.senseMapInfo(center).getPaint() != PaintType.ALLY_SECONDARY)
        //         rc.attack(center, true);
        // }
        // else if(rc.senseMapInfo(center).isPassable() && rc.senseMapInfo(center).getPaint() != PaintType.ENEMY_PRIMARY && rc.senseMapInfo(center).getPaint() != PaintType.ENEMY_SECONDARY) {
        //     Navigation.Bug2.move(center);
        //     if(rc.senseMapInfo(center).getPaint() != PaintType.ALLY_SECONDARY)
        //         rc.attack(center, true);
        // }
        // MapLocation center = rc.getLocation();//pls change this, so what if a the centre doesn't now exists
        // int[][] attackPositions = {
        //         {2, 2, 1}, {1, 2, 1}, {2, 1, 1},  // Quadrant 1
        //         {-2, 2, 1}, {-1, 2, 1}, {-2, 1, 1}, // Quadrant 2
        //         {-2, -2, 1}, {-1, -2, 1}, {-2, -1, 1}, // Quadrant 3
        //         {2, -2, 1}, {1, -2, 1}, {2, -1, 1}, // Quadrant 4
        //         {-1, 0, 0}, {-2, 0, 0}, {1, 0, 0}, {2, 0, 0}, // Horizontal
        //         {0, -1, 0}, {0, -2, 0}, {0, 1, 0}, {0, 2, 0}, // Vertical
        //         {1, 1, 0}, {-1, 1, 0}, {-1, -1, 0}, {1, -1, 0} // Diagonal
        // };
        // for (int[] pos : attackPositions) {
        //     MapLocation target = new MapLocation(center.x + pos[0], center.y + pos[1]);
        //     if (rc.canAttack(target)) {
        //         boolean paintType = false;
        //         if(rc.senseMapInfo(target).getPaint() == PaintType.ALLY_SECONDARY)
        //             paintType = true;
        //         if(rc.senseMapInfo(target).getPaint() == PaintType.ALLY_PRIMARY)
        //             paintType = false;
        //         if(paintType != (pos[2] == 1))
        //             rc.attack(target, pos[2] == 1);
        //     }
        //     // else if(rc.senseMapInfo(target).isPassable() && rc.senseMapInfo(target).getPaint() != PaintType.ENEMY_PRIMARY && rc.senseMapInfo(center).getPaint() != PaintType.ENEMY_SECONDARY) {
        //     //     Navigation.Bug2.move(target);
        //     //     boolean paintType = false;
        //     //     if(rc.senseMapInfo(target).getPaint() == PaintType.ALLY_SECONDARY)
        //     //         paintType = true;
        //     //     if(rc.senseMapInfo(target).getPaint() == PaintType.ALLY_PRIMARY)
        //     //         paintType = false;
        //     //     if(paintType != (pos[2] == 1))
        //     //         rc.attack(target, pos[2] == 1);
        //     // }
        // }

        // if (rc.senseMapInfo(rc.getLocation()).getPaint() == PaintType.EMPTY)
        //     rc.attack(rc.getLocation());
        if (rc.senseMapInfo(rc.getLocation()).getPaint() == PaintType.EMPTY && rc.canAttack(rc.getLocation()))
            rc.attack(rc.getLocation());
    }

    //Core turn method
    public void runTurn() throws GameActionException {  
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
            // UnitType towerToBuild = rc.senseRobotAtLocation(nearestAllyTower).type;
            // //if ruin is found, but pattern is not marked                    && just double check we can mark the tower pattern
            // if (rc.senseMapInfo(rc.getLocation()).getMark() == PaintType.EMPTY && rc.canMarkTowerPattern(towerToBuild, nearestAllyTower)){
            //     //DONE: which tower should the bot build?
            //     rc.markTowerPattern(towerToBuild, nearestAllyTower);
            // }
            //if robots pos is already paint, try to paint tiles near the tower
            for (Direction dir : directions){
                MapLocation newLoc = nearestAllyTower.add(dir);
                if (tryToPaintAtLoc(newLoc, PaintType.EMPTY))
                    return;
            }
            
            Navigation.Bug2.move(nearestAllyTower);
            //if tiles near the tower is already paint, paint the pattern
            for (MapInfo patternTile : rc.senseNearbyMapInfos(nearestAllyTower, 8)){
                //paint only tiles that are empty (this avoid us to destry any SRP)
                if (patternTile.getPaint() == PaintType.EMPTY){
                    // boolean useSecondaryColor = patternTile.getMark() == PaintType.ALLY_SECONDARY;
                    tryToPaintAtLoc(patternTile.getMapLocation(), false);
                }
            }

            //if i can get paint from nearestAllyPaintTower
            int localPaintToTake = rc.getPaint() - rc.getType().paintCapacity;

            if (localPaintToTake != 0 && rc.canTransferPaint(nearestAllyTower, localPaintToTake))
                rc.transferPaint(nearestAllyTower, localPaintToTake);

            return;
        }

        else if (exploreMode){
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
                // else if (!rc.canSenseLocation(nearestAllyTower) && friendMopperFound){
                //     return;//stop here and wait for the mopper to give paint
                // }
                else{
                    Navigation.Bug2.move(nearestAllyTower);
                    rc.setIndicatorString("move to (" + nearestAllyTower.x + " " + nearestAllyTower.y + ") to get healed");
                    if (rc.canSendMessage(nearestAllyTower)){
                        rc.sendMessage(nearestAllyTower, encodeMessage(OptCode.NEEDPAINT));
                        rc.setIndicatorString("message sent: " + encodeMessage(OptCode.NEEDPAINT));
                    }
                    // if (rc.canSendMessage(nearestAllyTower))
                        // rc.sendMessage(nearestAllyTower, OptCode.NEEDPAINT);
                }
                return;
            }

            //reset origin pos, if the bot is close enough to origin pos
            if (originPos.x != -1 && rc.getLocation().distanceSquaredTo(originPos) < 2)
                        originPos = new MapLocation(-1,-1);

            //if in past round the bot found some tiles (which is part of a pattern) are paint by enemy
            if (ruinWithPatternDamaged.size() > 0){
                //go to paint tower, if no paint tower, then go to money tower
                MapLocation nearestAllyTower = getNearestAllyPaintTower();
                if (nearestAllyTower.x == -1)
                    nearestAllyTower = getNearestAllyTower();
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
                        // Remove the first element only when nearestAllyTower is a paint tower
                        if (isPaintTower(rc.senseRobotAtLocation(nearestAllyTower).getType()))
                            iterator.remove();
                        else if (damagedPatternFoundMessageSentCount > 3)
                            iterator.remove();
                    }
                    rc.sendMessage(nearestAllyTower, encodeMessage(OptCode.DAMAGEDPATTERN,firstElement)); // DONE: add first firstElement to the messag
                    damagedPatternFoundMessageSentCount++; //count the number of times i sent this message to this tower
                    //.out.println("message sent to (" + nearestAllyTower.x + " " + nearestAllyTower.y + "): damaged pattern found");
                    
                    //reset to explore mode
                    resetMessageFlag();
                    exploreMode = true;

                    rc.setIndicatorDot(nearestAllyTower, 0,255,0);
                    rc.setIndicatorString("message sent: damaged pattern found, " + ruinWithPatternDamaged.size() + " pattern damaged left");
                }else{
                    damagedPatternFoundMessageSentCount = 0;
                }
                return;
            }

            if (enemyTowersPos.size() > 0){
                //DONE: what to do once an enemy tower is found
                MapLocation nearestAllyTower = getNearestAllyPaintTower();
                if (nearestAllyTower.x == -1)
                    nearestAllyTower = getNearestAllyTower();

                if (nearestAllyTower.x == -1){
                    rc.setIndicatorString("tower is destroyed, go back to explore mode and try to re-build the tower");
                    tryToRebuildTower();
                    return;
                }
                //reset the target location
                targetLocation = new MapLocation(-1,-1);
                originPos = new MapLocation(-1,-1);
                //go to the nearest ally tower 
                Navigation.Bug2.move(nearestAllyTower);
                rc.setIndicatorString("enemy tower found: going to (" + nearestAllyTower.x + " " + nearestAllyTower.y + ")");


                if (rc.canSendMessage(nearestAllyTower)){
                    MapLocation firstElement = new MapLocation(0, 0);
                    // Get an iterator
                    Iterator<MapLocation> iterator = enemyTowersPos.iterator();
                    if (iterator.hasNext()) {
                        firstElement = iterator.next();
                        // Remove the first element only when nearestAllyTower is a paint tower
                        if (isPaintTower(rc.senseRobotAtLocation(nearestAllyTower).getType()))
                            iterator.remove();
                        else if (enemyTowerMessageSentCount > 3)
                            iterator.remove();
                    }
                    rc.sendMessage(nearestAllyTower, encodeMessage(OptCode.ENEMYTOWERFOUND,firstElement)); // DONE: add first firstElement to the messag
                    enemyTowerMessageSentCount++;
                    
                    rc.setIndicatorDot(nearestAllyTower, 0,255,0);
                    rc.setIndicatorString("message sent: enemy tower found");
                }else{
                    enemyTowerMessageSentCount = 0;
                }
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
                    
                    // if more than 2 bot are building this tower, ignore it
                    if (buildingTower.x != -1 && rc.senseNearbyRobots(buildingTower, 8,rc.getTeam()).length >= 2)
                        buildingTower = new MapLocation(-1,-1);
                    else
                        break;//just take the first ruin found and build it
                }
            }

            // if more than 2 bot are building this tower, skip it
            if (buildingTower.x != -1 && rc.senseNearbyRobots(buildingTower, 8,rc.getTeam()).length >= 2)
                buildingTower = new MapLocation(-1,-1);
            //if bot already started to build a tower, try to complete that first
            if (buildingTower.x != -1){
                //reset target location
                targetLocation = new MapLocation(-1,-1);
                originPos = new MapLocation(-1,-1);

                rc.setIndicatorString("try to build the tower at (" + buildingTower.x + " " + buildingTower.y + ")");
                Navigation.Bug2.move(buildingTower);
                // tryToMarkPattern(buildingTower);

                boolean damagedPatternFound = false;

                
                UnitType whatIBuild = null;
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

                    //if someone is already bulding the tower, follow them
                    if (patternTile.getMark() == PaintType.ALLY_PRIMARY)
                        whatIBuild = UnitType.LEVEL_ONE_MONEY_TOWER;
                    if (patternTile.getMark() == PaintType.ALLY_SECONDARY)
                        whatIBuild = UnitType.LEVEL_ONE_PAINT_TOWER;
                    
                    // //if we see an ally mark
                    // else if (patternTile.getMark().isAlly()){
                    //     // Navigation.Bug2.move(buildingTower);

                    //     //if the tile is not paint as expected (primary color instead of secondary or vice versa or empty paint)
                    //     if (patternTile.getMark() != patternTile.getPaint()){
                    //         boolean useSecondaryColor = patternTile.getMark() == PaintType.ALLY_SECONDARY;
                    //         tryToPaintAtLoc(patternTile.getMapLocation(), useSecondaryColor);
                    //         rc.setIndicatorString("try to build the tower at (" + buildingTower.x + " " + buildingTower.y + ")");
                    //     }
                    // }
                }

                //if no one is building a tower at this ruin
                if (whatIBuild == null)
                    whatIBuild = nextTowerToBuild();

                if (whatIBuild == UnitType.LEVEL_ONE_MONEY_TOWER)
                    buildMoneyTower(rc,buildingTower);
                else
                    buildPaintTower(rc,buildingTower);

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

            // if Middle game, look for enemy paint zone and send message to tower
            if (rc.getRoundNum() > EARLY_GAME_TURNS) {
                if (enemyPaintZone.x == -1 && enemyPaintZone.y == -1) {
                    enemyPaintZone = getEnemyPaintZone(rc);
                    //ignore enemy paint zone that is already discovered within 50 round 
                    if (temporaryInvalidEnemyPaintZone.containsKey(enemyPaintZone) && rc.getRoundNum() - temporaryInvalidEnemyPaintZone.get(enemyPaintZone) < 50)
                        enemyPaintZone = new MapLocation(-1,-1);
                    else
                        temporaryInvalidEnemyPaintZone.put(enemyPaintZone,rc.getRoundNum());
                }  
    
                MapLocation nearestAllyTower = getNearestAllyTower();
                //if enemyPaintZone found, and nearesetTower exists 
                if (enemyPaintZone.x != -1 && enemyPaintZone.y != -1 && nearestAllyTower.x != -1 && nearestAllyTower.y != -1) {
                    rc.setIndicatorString("enemyPaintZoneFound at " + enemyPaintZone + " go back to " + nearestAllyTower);
                    Navigation.Bug2.move(nearestAllyTower);

                    int messageContent = encodeMessage(OptCode.SENDPAINTZONE, enemyPaintZone);
                    if (rc.canSendMessage(nearestAllyTower)) {
                        rc.setIndicatorString("message sent: enemyPaintZoneFound");
                        rc.sendMessage(nearestAllyTower, messageContent);
                        enemyPaintZone = new MapLocation(-1,-1);
                        //if message is sent go back to explore mode
                        resetMessageFlag();
                        exploreMode = true;
                    }
                    return;                     
                }    
            }


            //if a good spot for SRP is found, try to complete RSP
            if(resourceCenter.x != -1){
                // targetLocation = new MapLocation(-1,-1);
                rc.setIndicatorString("Trying to complete RSP with centere at " + resourceCenter.x + " " + resourceCenter.y + "(origin pos = " + originPos);
                System.out.println("Trying to complete RSP with centere at " + resourceCenter.x + " " + resourceCenter.y);
                if(!rc.senseMapInfo(resourceCenter).isResourcePatternCenter())
                    paintSRP(resourceCenter);

                if (rc.canSenseLocation(resourceCenter) && rc.senseMapInfo(resourceCenter).isResourcePatternCenter())
                    resourceCenter = new MapLocation(-1,-1);


                if(rc.canCompleteResourcePattern(resourceCenter)) {
                    rc.completeResourcePattern(resourceCenter);
                    resourceCenter = new MapLocation(-1,-1);
                }
                return; //focus on completing the SRP
            }
            else//paint while traveling
                paintSRPWithNoMoving();

            //if nothing interesting is found, try to reach the target location
            if(tryToReachTargetLocation())
                rc.setIndicatorString("explore mode: move to  " + targetLocation.x + " " + targetLocation.y);

            //if so far, it din't move, explore unpaint tile (TODO: that is whitin the region)
            if (emptyTile.x != -1 && rc.isMovementReady()){
                Navigation.Bug2.move(emptyTile);
                rc.setIndicatorString("move to a random empty tile");
                tryToPaintAtLoc(rc.getLocation(), PaintType.EMPTY);
                return;
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
            }else{
                resetMessageFlag();
                exploreMode = true;
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
