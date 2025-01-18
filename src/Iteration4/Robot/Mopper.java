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
import java.util.LinkedList;
import java.util.Queue;


public class Mopper extends Robot {
    
    static Queue<MapLocation> robotToHealQueue;
    static Set<MapLocation> robotToHealSet;

    Direction dirToEnemy;
    MapLocation targetTower;
    boolean enemyFound = false;
    MapLocation tileToclean;

    public Mopper(RobotController _rc) throws GameActionException {
        super(_rc);
        robotToHealQueue = new LinkedList<>();
        robotToHealSet = new HashSet<>();

        // if (rc.getMapHeight() * rc.getMapWidth() < 450) {
            rushInitialise(_rc);
        // }

    }

    private void rushInitialise(RobotController _rc) {
        if (_rc.getRoundNum() < 10) {
            //.out.println("Defence mopper generated");
            this.isDefenceMopper = 1;
        }
    }

    //instructions run at the beginning of each turn
    public void beginTurn() throws GameActionException {
        resetFlags();
        robotToHealQueue = new LinkedList<>();
        robotToHealSet = new HashSet<>();
        listenMessage();
        updateLowPaintFlag();

        RobotInfo[] nearbyRobots = rc.senseNearbyRobots();
        for (RobotInfo robot : nearbyRobots){
            //check if our paint/money tower is found
            if (isPaintTower(robot.type) && robot.team == rc.getTeam()){
                paintTowersPos.add(robot.location);
            }
            
            if (isMoneyTower(robot.type) && robot.team == rc.getTeam()){
                moneyTowersPos.add(robot.location);
            }

            //check if any bot/tower needs heal
            if (robot.team == rc.getTeam() && robot.type != UnitType.MOPPER && isPaintTower(robot.type) == false && robotToHealSet.contains(robot.location) == false && calculatePaintPercentage(robot) <= LOWPAINTTRESHHOLD){
                rc.setIndicatorDot(robot.location, 255,0,0);
                robotToHealQueue.add(robot.location);
                robotToHealSet.add(robot.location);
            }
            if (robot.team != rc.getTeam() && robot.type.isTowerType())
                enemyTowersPos.add(robot.location);
        }
        
        // Sense information about all visible nearby tiles.
        MapInfo[] nearbyTiles = rc.senseNearbyMapInfos();
        // Search for a nearby ruin to complete.
        for (MapInfo tile : nearbyTiles)
            if (tile.hasRuin() && rc.senseRobotAtLocation(tile.getMapLocation()) == null)
                ruinsFound.add(tile);
            else if (tileToclean == null && tile.getPaint().isEnemy())
                tileToclean = tile.getMapLocation();

        // someoneNeedPaint();

        // if (isDefenceMopper == 1) rushTowerFind(rc);
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
    public  void endTurn() throws GameActionException {
       
    }

    void givePaintToTower(RobotController rc) throws GameActionException {
        MapLocation nearestTower = new MapLocation(0,0);
        int minDist = 9999;
        for (MapLocation pos : paintTowersPos){
            int currDist = pos.distanceSquaredTo(rc.getLocation());
            if (minDist > currDist){
                nearestTower = pos;
                minDist = currDist;
            }
        }
    
        if (rc.canTransferPaint(nearestTower, PAINTTOGIVE))
            rc.transferPaint(nearestTower, PAINTTOGIVE);        
    }

    //Core turn method
    public void runTurn() throws GameActionException {
        // if (isDefenceMopper == 1) {
        //     rc.setIndicatorString("Defense Mopper");

            
        //     if (enemyFound == false) {
        //         Navigation.Bug2.move(targetTower);
        //     }
        //     Team enemy = rc.getTeam().opponent();

        //     // sense and attack enemy robots
        //     RobotInfo[] nearbyRobots = rc.senseNearbyRobots(-1, enemy);
        //     for (RobotInfo robot : nearbyRobots) {
        //         Direction dirToEnemy = rc.getLocation().directionTo(robot.location);
        //         if (rc.canAttack(robot.location)) {
        //             rc.attack(robot.location);
        //         }
        //         Navigation.Bug2.move(robot.location);
        //         enemyFound = true;
        //     }
        //     givePaintToTower(rc);
        // }
        //stayPut, moveTarget, ruinWithPatternDamaged, lowPaintFlag, ruinsFound, enemyTowerFound
        /*

        STAYPUT MODE
        don't do anything else

        EXPLORE MODE
        once reached the target loc
        1) if lowPaintFlag
        2) ruinsFound (try to build)
            - ruinWithPatternDamaged (clean the tile)
        3) if enemyTowerFound (go back to tower and tell the tower about it then go back to ruindsFound the explore mode) 
        4) if he found a bot with low paint (heal)
        
        HEAL MODE
        if there is a tower nearby, send the message to the tower
        otherwise go to heal the bot

        REMOVE PATTERN MODE
        once reached the target loc
        clean tile
        go back to the tower

        DEFENDMODE
        go to the target location 
        stop and attack when he can attack an enemy
     
        */  

        if (defendMode){
            if (targetLocation.x != -1){
                rc.setIndicatorString("defend tower: command received, go to " + targetLocation.x + " " + targetLocation.y);
                Navigation.Bug2.move(targetLocation);
                // targetLocation = new MapLocation(-1,-1);
            }
            
            Team enemy = rc.getTeam().opponent();
            // sense and attack enemy robots
            RobotInfo[] nearbyRobots = rc.senseNearbyRobots(-1, enemy);
            for (RobotInfo robot : nearbyRobots){
                rc.setIndicatorDot(robot.location, 0,255,0);
                if (rc.canAttack(robot.location) && robot.paintAmount > 0){
                    rc.attack(robot.location);
                    rc.setIndicatorString("defend tower: attack enemy bot at: " + robot.location.x + " " + robot.location.y + " with paint amount = " + robot.paintAmount);
                    return;
                }
                    // Navigation.Bug2.move(nearbyRobots[0].location);
            }

            //if no command received
            if (targetLocation.x == -1){
                rc.setIndicatorString("no command received, go to the tower and stay put");
                if (rc.senseMapInfo(rc.getLocation()).getPaint().isAlly()){
                    //go to stay put, when defend mode is finished
                    resetMessageFlag();
                    stayPut = true;
                }else{
                    MapLocation nearestAllyTower = getNearestAllyTower();
                    if (nearestAllyTower.x != -1)
                        Navigation.Bug2.move(nearestAllyTower);
                    else{
                        rc.setIndicatorString("tower is destroyed, try to re-build the tower");
                        tryToRebuildTower();
                        return; 
                    }

                }
            }else//reset targetLocation
                targetLocation = new MapLocation(-1,-1);
            
            return;
        }

        if (stayPut){
            //if i can get paint from nearestAllyPaintTower
            MapLocation nearestAllyTower = getNearestAllyTower();
            rc.setIndicatorString("stay put near the tower (" + nearestAllyTower.x + " " + nearestAllyTower.y + ")");
            if (nearestAllyTower.x == -1){
                // resetMessageFlag();
                // exploreMode = true;
                rc.setIndicatorString("tower is destroyed, try to re-build the tower");
                tryToRebuildTower();

                //find a new ally tower
                nearestAllyTower = getNearestAllyTower();
                // return; just try to build the tower once, so if it's not possible, go to the nearset one
            }
        
            // int localPaintToTake = rc.getPaint() - rc.getType().paintCapacity;
            // if (localPaintToTake != 0 && rc.canTransferPaint(nearestAllyTower, localPaintToTake))
            //     rc.transferPaint(nearestAllyTower, localPaintToTake);
            // else if (nearestAllyTower.x != -1)
            //     Navigation.Bug2.move(nearestAllyTower);
        
                     //try to clean enemy tile
            if (tileToclean != null){
                    Navigation.Bug2.move(tileToclean);
                    rc.setIndicatorDot(tileToclean, 0,0,255);
                    if (tryToPaintAtLoc(tileToclean, PaintType.ENEMY_PRIMARY) || tryToPaintAtLoc(tileToclean, PaintType.ENEMY_SECONDARY))
                        tileToclean = null;
            }

            if (!rc.canSendMessage(nearestAllyTower))
                Navigation.Bug2.move(nearestAllyTower);

        }


        if (exploreMode){
            //if he is not at the destination yet, don't do anything else
            if (tryToReachTargetLocation()){
                rc.setIndicatorString("explore mode: move to  " + targetLocation.x + " " + targetLocation.y);
            }
            
            //once reached the target loc
            if (lowPaintFlag){
                //reset target location 
                targetLocation = new MapLocation(-1,-1);
                MapLocation nearestAllyPaintTower = getNearestAllyPaintTower();
                if (nearestAllyPaintTower.x == -1){
                    rc.setIndicatorString("tower is destroyed, go back to explore mode and try to re-build the tower");
                    tryToRebuildTower();
                    return;
                }
                rc.setIndicatorString("need healing: go to (" + nearestAllyPaintTower.x + " " + nearestAllyPaintTower.y + ")");

                //if i can get paint from nearestAllyPaintTower
                int localPaintToTake = rc.getPaint() - rc.getType().paintCapacity;
                if (rc.canTransferPaint(nearestAllyPaintTower, localPaintToTake))
                    rc.transferPaint(nearestAllyPaintTower, localPaintToTake);
                else
                    Navigation.Bug2.move(nearestAllyPaintTower);
                return;
            }

            //if there is any robot to heal
            if (!robotToHealQueue.isEmpty()){
                //reset target location 
                targetLocation = new MapLocation(-1,-1);
                MapLocation curr = robotToHealQueue.peek();
                //if curr is within the vision range && if at curr the robot is gone
                while(rc.canSenseLocation(curr) && rc.canSenseRobotAtLocation(curr) == false && !robotToHealQueue.isEmpty()){ 
                    robotToHealQueue.remove();
                    robotToHealSet.remove(curr);
                    if (!robotToHealQueue.isEmpty())
                        curr = robotToHealQueue.peek();
                }

                if (!robotToHealQueue.isEmpty()){
                    if (rc.canTransferPaint(curr, PAINTTOGIVE)){
                        rc.transferPaint(curr, PAINTTOGIVE);
                        robotToHealQueue.remove();
                        robotToHealSet.remove(curr);
                    }
                    else{
                        Navigation.Bug2.move(curr);
                        rc.setIndicatorString("heal bot: move to (" + curr.x + " " + curr.y + ")");
                    }
                }
            }

            if (ruinsFound.size() > 0){
                //reset target location
                targetLocation = new MapLocation(-1,-1);

                for (MapInfo curRuin : ruinsFound){
                    tryToMarkPattern(curRuin);
                    tryToBuildTower(curRuin);

                    //if eneny paint found near the ruin
                    for (MapInfo patternTile : rc.senseNearbyMapInfos(curRuin.getMapLocation(), 8)){
                        if (patternTile.getPaint().isEnemy()){
                            MapLocation newLoc = patternTile.getMapLocation();
                            Navigation.Bug2.move(newLoc);
                            rc.setIndicatorDot(newLoc, 0,0,255);
                            tryToPaintAtLoc(newLoc, PaintType.ENEMY_PRIMARY);
                            tryToPaintAtLoc(newLoc, PaintType.ENEMY_SECONDARY);
                        }
                    }
                
                }
            }

                            //try to clean enemy tile
            if (tileToclean != null){
                //reset target location 
                targetLocation = new MapLocation(-1,-1);
                Navigation.Bug2.move(tileToclean);
                rc.setIndicatorDot(tileToclean, 0,0,255);
                if (tryToPaintAtLoc(tileToclean, PaintType.ENEMY_PRIMARY) || tryToPaintAtLoc(tileToclean, PaintType.ENEMY_SECONDARY))
                    tileToclean = null;
            }

            if (enemyTowersPos.size() > 0){
                //Done: what to do once an enemy tower is found
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
                System.out.println("enemy tower found: going to (" + nearestAllyTower.x + " " + nearestAllyTower.y + ")");


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


            // Move randomly if no objective.
            Direction dir = directions[rng.nextInt(directions.length)];
            if (rc.canMove(dir)){
                rc.move(dir);
                rc.setIndicatorString("move randomly");
            }
        }


        if (healMode){

        }

        if (removePatterMode){
            //if he is not at the destination yet, don't do anything else
            if (tryToReachTargetLocation()){
                rc.setIndicatorString("removePatter mode: move to  " + targetLocation.x + " " + targetLocation.y);
                return;
            }

            if (ruinsFound.size() > 0){
                //if action is in cooldown, wait for the cooldown
                if (!rc.isActionReady())
                    return;
                MapLocation enemyTilePos = new MapLocation(-1,-1);
                boolean tileIsCleaned = false;
                for (MapInfo curRuin : ruinsFound){
                    // tryToMarkPattern(curRuin);
                    // tryToBuildTower(curRuin);

                    //if eneny paint found near the ruin
                    for (MapInfo patternTile : rc.senseNearbyMapInfos(curRuin.getMapLocation(), 8)){
                        if (patternTile.getPaint().isEnemy()){
                            MapLocation newLoc = patternTile.getMapLocation();

                            rc.setIndicatorDot(newLoc, 0,0,255);
                            if (tryToPaintAtLoc(newLoc, PaintType.ENEMY_PRIMARY) || tryToPaintAtLoc(newLoc, PaintType.ENEMY_SECONDARY)){
                                tileIsCleaned = true;
                                rc.setIndicatorDot(newLoc, 0,255,255);
                            }else
                                enemyTilePos = newLoc;
                        }
                    }
                }

                //if, by looking all the ruin, some enemy tile is cleaned
                if (tileIsCleaned){
                    rc.setIndicatorString("enemy paint removed");
                }else{
                    if (enemyTilePos.x == -1){
                        MapLocation nearestAllyTower = getNearestAllyTower();
                        if (nearestAllyTower.x == -1){
                            resetMessageFlag();
                            exploreMode = true;
                            rc.setIndicatorString("tower is destroyed, go back to explore mode and try to re-build the tower");
                            tryToRebuildTower();
                            return;
                        }
                        Navigation.Bug2.move(nearestAllyTower);
                        rc.setIndicatorString("enemy paint not found, go to the nearestAllyTower (" + nearestAllyTower.x + " " + nearestAllyTower.y + ")");
                    }
                    else{
                        Navigation.Bug2.move(enemyTilePos);
                        rc.setIndicatorString("move closer to the enemy paint (" + enemyTilePos.x + " " + enemyTilePos.y + ")");
                    } 
                }
            }else{
                //if ruins are not found, go back to the tower
                MapLocation nearestAllyTower = getNearestAllyTower();
                if (nearestAllyTower.x == -1){
                    resetMessageFlag();
                    exploreMode = true;
                    rc.setIndicatorString("tower is destroyed, go back to explore mode and try to re-build the tower");
                    tryToRebuildTower();
                    return;
                }
                Navigation.Bug2.move(nearestAllyTower);
                rc.setIndicatorString("enemy paint not found, go to the nearestAllyTower (" + nearestAllyTower.x + " " + nearestAllyTower.y + ")");
            }
        }
        
    }

    // custom lowpaintflag
    void updateLowPaintFlag() throws GameActionException{
        if (rc.getPaint() <= 60)
            lowPaintFlag = true;
    }


    // void someoneNeedPaint(int x, int y){

    // }
}