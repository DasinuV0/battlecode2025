package Iteration3;

import battlecode.common.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.LinkedList;
import java.util.Queue;


public class Mopper extends Robot {
    
    static Queue<MapLocation> robotToHealQueue;
    static Set<MapLocation> robotToHealSet;

    public Mopper(RobotController _rc) throws GameActionException {
        super(_rc);
        robotToHealQueue = new LinkedList<>();
        robotToHealSet = new HashSet<>();

    }

    //instructions run at the beginning of each turn
    void beginTurn() throws GameActionException {
        resetFlags();
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
        }
        
        // Sense information about all visible nearby tiles.
        MapInfo[] nearbyTiles = rc.senseNearbyMapInfos();
        // Search for a nearby ruin to complete.
        for (MapInfo tile : nearbyTiles)
            if (tile.hasRuin() && rc.senseRobotAtLocation(tile.getMapLocation()) == null)
                ruinsFound.add(tile);

        // someoneNeedPaint();
    }

    //Instructions at the end of each turn
    void endTurn() throws GameActionException {
       
    }

    //Core turn method
    void runTurn() throws GameActionException {
        //stayPut, moveToTarget, ruinWithPatternDamaged, lowPaintFlag, ruinsFound, enemyTowerFound
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
        */  

        if (stayPut){
            rc.setIndicatorString("stay put");
            return;
        }

        if (exploreMode){
            //if he is not at the destination yet, don't do anything else
            if (tryToReachTargetLocation()){
                rc.setIndicatorString("explore mode: move to  " + targetLocation.x + " " + targetLocation.y);
                return;
            }
            
            //once reached the target loc
            if (lowPaintFlag){
                MapLocation nearestAllyPaintTower = getNearestAllyPaintTower();
                rc.setIndicatorString("need healing: go to (" + nearestAllyPaintTower.x + " " + nearestAllyPaintTower.y + ")");

                //if i can get paint from nearestAllyPaintTower
                if (rc.canTransferPaint(nearestAllyPaintTower, PAINTTOTAKE))
                    rc.transferPaint(nearestAllyPaintTower, PAINTTOTAKE);
                else
                    BugNavigator.moveTo(nearestAllyPaintTower);
                return;
            }

            if (ruinsFound.size() > 0){
                for (MapInfo curRuin : ruinsFound){
                    tryToMarkPattern(curRuin);
                    tryToBuildTower(curRuin);

                    //if eneny paint found near the ruin
                    for (MapInfo patternTile : rc.senseNearbyMapInfos(curRuin.getMapLocation(), 8)){
                        if (patternTile.getPaint().isEnemy()){
                            MapLocation newLoc = patternTile.getMapLocation();
                            BugNavigator.moveTo(newLoc);
                            rc.setIndicatorDot(newLoc, 0,0,255);
                            tryToPaintAtLoc(newLoc, PaintType.ENEMY_PRIMARY);
                            tryToPaintAtLoc(newLoc, PaintType.ENEMY_SECONDARY);
                        }
                    }
                
                }
            }


            if (enemyTowerFound){
            //TODO: what to do once an enemy tower is found
            }

            //if there is any robot to heal
            if (!robotToHealQueue.isEmpty()){
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
                        BugNavigator.moveTo(curr);
                        rc.setIndicatorString("heal bot: move to (" + curr.x + " " + curr.y + ")");
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
                for (MapInfo curRuin : ruinsFound){
                    tryToMarkPattern(curRuin);
                    tryToBuildTower(curRuin);

                    //if eneny paint found near the ruin
                    for (MapInfo patternTile : rc.senseNearbyMapInfos(curRuin.getMapLocation(), 8)){
                        if (patternTile.getPaint().isEnemy()){
                            MapLocation newLoc = patternTile.getMapLocation();
                            BugNavigator.moveTo(newLoc);
                            rc.setIndicatorDot(newLoc, 0,0,255);
                            tryToPaintAtLoc(newLoc, PaintType.ENEMY_PRIMARY);
                            tryToPaintAtLoc(newLoc, PaintType.ENEMY_SECONDARY);
                            rc.setIndicatorString("try to remove enemy paint");
                        }
                    }
                
                }
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