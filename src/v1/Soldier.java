package Iteration3;

import battlecode.common.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.LinkedHashSet;

public class Soldier extends Robot {
    public Soldier(RobotController _rc) throws GameActionException {
        super(_rc);
        ruinWithPatternDamaged = new HashSet<>();
    }

    //instructions run at the beginning of each turn
    void beginTurn() throws GameActionException {
        lowPaintFlag = false;
        ruinsFound = new HashSet<>();
        enemyTowerFound = false;
        emptyTile = new MapLocation(-1,-1);

        listenMessage();

        //get the robotInfo of rc and then calculate the percetange of the remain paint 
        int remainPaint = calculatePaintPercentage(rc.senseRobotAtLocation(rc.getLocation()));
        //check if it is low paint
        if (remainPaint <= LOWPAINTTRESHHOLD)
            lowPaintFlag = true;
        else    
            lowPaintFlag = false;
            friendMopperFound = false;
        

        //check if any tower is found
        RobotInfo[] nearbyRobots = rc.senseNearbyRobots();
        for (RobotInfo robot : nearbyRobots)
            if (robot.type.isTowerType())
                towersPos.add(robot.location);
            else if (robot.team.isPlayer() && robot.type == UnitType.MOPPER)
                friendMopperFound = true;

        // Sense information about all visible nearby tiles.
        MapInfo[] nearbyTiles = rc.senseNearbyMapInfos();
        // Search for a nearby ruin to complete.
        for (MapInfo tile : nearbyTiles)
            if (tile.hasRuin() && rc.senseRobotAtLocation(tile.getMapLocation()) == null)
                ruinsFound.add(tile);
            else if (emptyTile.x == -1 && tile.getPaint() == PaintType.EMPTY && tile.isPassable()){
                emptyTile = tile.getMapLocation(); //TODO: improve this, for now just one the first empty tile
                rc.setIndicatorDot(emptyTile, 1,1,1);
            }
        
    }

    //Instructions at the end of each turn
    void endTurn() throws GameActionException {
       
    }

    //Core turn method
    void runTurn() throws GameActionException {
        //if in past round the bot found some tiles (which is part of a pattern) are paint by enemy
        if (ruinWithPatternDamaged.size() > 0){
            //DONE: go to the nearest tower 
            MapLocation nearestTower = new MapLocation(0,0);
            int minDist = 9999;
            for (MapLocation pos : towersPos){
                int currDist = pos.distanceSquaredTo(rc.getLocation());
                if (minDist > currDist){
                    nearestTower = pos;
                    minDist = currDist;
                }
            }
            BugNavigator.moveTo(nearestTower);
            if (rc.canSendMessage(nearestTower)){
                MapLocation firstElement = new MapLocation(0, 0);
                // Get an iterator
                Iterator<MapLocation> iterator = ruinWithPatternDamaged.iterator();
                if (iterator.hasNext()) {
                    firstElement = iterator.next();
                    // Remove the first element
                    iterator.remove();
                }
                rc.sendMessage(nearestTower, OptCode.ENEMYTOWERFOUND); // TODO: add first firstElement to the messag
                // System.out.println("message sent: enemytowerleft: " + ruinWithPatternDamaged.size());
            }
        }


        if (lowPaintFlag){
            //DONE: go to the nearest tower 
            MapLocation nearestTower = new MapLocation(0,0);
            int minDist = 9999;
            for (MapLocation pos : towersPos){
                int currDist = pos.distanceSquaredTo(rc.getLocation());
                if (minDist > currDist){
                    nearestTower = pos;
                    minDist = currDist;
                }
            }

            // rc.setIndicatorString(""+friendMopperFound);
            if (rc.canTransferPaint(nearestTower, PAINTTOTAKE)){
                stayPut = false; 
                rc.transferPaint(nearestTower, PAINTTOTAKE);
            }
            else if (friendMopperFound)
                stayPut = true; //stop here and wait for the mopper to give paint
            else{
                BugNavigator.moveTo(nearestTower);
                if (rc.canSendMessage(nearestTower))
                    rc.sendMessage(nearestTower, OptCode.NEEDPAINT);
            }
        }else if (ruinsFound.size() > 0){
            for (MapInfo curRuin : ruinsFound){
                MapLocation targetLoc = curRuin.getMapLocation();
                Direction dir = rc.getLocation().directionTo(targetLoc);

                // Mark the pattern we need to draw to build a tower here if we haven't already.
                MapLocation shouldBeMarked = curRuin.getMapLocation().subtract(dir);
                //if ruin is found, but pattern is not marked                    && just double check we can mark the tower pattern
                if (rc.senseMapInfo(shouldBeMarked).getMark() == PaintType.EMPTY && rc.canMarkTowerPattern(UnitType.LEVEL_ONE_PAINT_TOWER, targetLoc)){
                    //TODO: which tower should the bot build?
                    rc.markTowerPattern(UnitType.LEVEL_ONE_PAINT_TOWER, targetLoc);
                }

                // Fill in any spots in the pattern with the appropriate paint.
                for (MapInfo patternTile : rc.senseNearbyMapInfos(targetLoc, 8)){
                    //if we see an ally mark
                    if (patternTile.getMark().isAlly()){
                        //if the tile is not paint as expected (primary color instead of secondary or vice versa or empty paint)
                        if (patternTile.getMark() != patternTile.getPaint()){
                            boolean useSecondaryColor = patternTile.getMark() == PaintType.ALLY_SECONDARY;
                            if (rc.canAttack(patternTile.getMapLocation()))
                                rc.attack(patternTile.getMapLocation(), useSecondaryColor);
                        else{
                            BugNavigator.moveTo(patternTile.getMapLocation());
                            for (MapInfo patternTile2 : rc.senseNearbyMapInfos(targetLoc, 8)){
                                //if we see an ally mark
                                if (patternTile2.getMark().isAlly()){
                                    //if the tile is not paint as expected (primary color instead of secondary or vice versa or empty paint)
                                    if (patternTile2.getMark() != patternTile2.getPaint()){
                                        useSecondaryColor = patternTile2.getMark() == PaintType.ALLY_SECONDARY;
                                        if (rc.canAttack(patternTile2.getMapLocation()))
                                            rc.attack(patternTile2.getMapLocation(), useSecondaryColor);
                                        }
                                    }
                                }
                            }
                        }
                    }
                    //if there is a pattern and it's paing by enemy
                    else if (patternTile.getPaint().isEnemy()){
                        ruinWithPatternDamaged.add(curRuin.getMapLocation());
                        rc.setIndicatorString("pattern damaged, enemy paint found."); //TODO: find a way to avoid multiple bot find the same enemy tile and all go back to the tower
                        // System.out.println("pattern damaged, enemy paint found.");
                    }
                }
                                // Complete the ruin if we can.
                if (rc.canCompleteTowerPattern(UnitType.LEVEL_ONE_PAINT_TOWER, targetLoc)){
                    rc.completeTowerPattern(UnitType.LEVEL_ONE_PAINT_TOWER, targetLoc);
                    rc.setTimelineMarker("Tower built", 0, 255, 0);
                }
            }
        }else if (enemyTowerFound){
            //TODO: what to do once an enemy tower is found
        }


        if (stayPut){
                MapLocation nearestTower = new MapLocation(0,0);
                int minDist = 9999;
                for (MapLocation pos : towersPos){
                    int currDist = pos.distanceSquaredTo(rc.getLocation());
                    if (minDist > currDist){
                        nearestTower = pos;
                        minDist = currDist;
                    }
                }
                
                for (Direction dir : directions){
                    MapLocation newLoc = nearestTower.add(dir);
                    if (rc.canSenseLocation(newLoc) && rc.senseMapInfo(newLoc).getPaint() == PaintType.EMPTY && rc.canAttack(newLoc))
                        rc.attack(newLoc);
                }
            return;
        }else if (moveToTarget.x != -1){
            MapLocation temp = new MapLocation(10,10);
            BugNavigator.moveTo(moveToTarget);
            rc.setIndicatorString("move to " + moveToTarget.x + moveToTarget.y);

        }
        //DONE: establish an algo to move/find ruin (explore state)
        //if so far, it din't move, explore unpaint tile (TODO: that is whitin the region)
        if (emptyTile.x != -1)
            BugNavigator.moveTo(emptyTile);

        // Move and attack randomly if no objective.
        Direction dir = directions[rng.nextInt(directions.length)];
        if (rc.canMove(dir)){
            rc.move(dir);
        }

        MapInfo currentTile = rc.senseMapInfo(rc.getLocation());
        if (currentTile.getPaint() == PaintType.EMPTY && rc.canAttack(rc.getLocation())){
            // rc.attack(rc.getLocation());
            boolean useSecondaryColor = rc.senseMapInfo(rc.getLocation()).getMark() == PaintType.ALLY_SECONDARY;
            rc.attack(rc.getLocation(), useSecondaryColor);
        }


    }
}