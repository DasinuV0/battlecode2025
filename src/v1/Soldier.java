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
        resetFlags();
        listenMessage();
        updateLowPaintFlag();


        //check if any tower is found
        RobotInfo[] nearbyRobots = rc.senseNearbyRobots();
        for (RobotInfo robot : nearbyRobots)
            if (robot.type.isTowerType())
                towersPos.add(robot.location);
            else if (robot.team.isPlayer() && robot.type == UnitType.MOPPER)
                friendMopperFound = true;


        // Sense information about all visible nearby tiles.
        MapInfo[] nearbyTiles = rc.senseNearbyMapInfos();
        for (MapInfo tile : nearbyTiles)
            // Search all nearby ruins
            if (tile.hasRuin() && rc.senseRobotAtLocation(tile.getMapLocation()) == null){
                ruinsFound.add(tile);
            }
            //search of an emptyTile 
            //TODO: improve this, for now just one the first empty tile
            else if (emptyTile.x == -1 && tile.getPaint() == PaintType.EMPTY && tile.isPassable()){
                emptyTile = tile.getMapLocation();
            }
        
    }

    //Instructions at the end of each turn
    void endTurn() throws GameActionException {
       
    }

    //Core turn method
    void runTurn() throws GameActionException {       
        //stayPut, moveToTarget, ruinWithPatternDamaged, lowPaintFlag, ruinsFound, enemyTowerFound
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
            MapLocation nearestTower = getNearestTower();
         
            //paint the robots pos
            if (rc.senseMapInfo(rc.getLocation()).getPaint() == PaintType.EMPTY)
                tryToPaintAtLoc(rc.getLocation());
            
            //if robots pos is already paint, try to paint tiles near the tower
            for (Direction dir : directions){
                MapLocation newLoc = nearestTower.add(dir);
                if (rc.senseMapInfo(newLoc).getPaint() == PaintType.EMPTY)
                    tryToPaintAtLoc(newLoc);
            }
            return;
        }

        if (exploreMode){
            if (tryToReachTargetLocation()){
                //paint while traveling
                MapInfo currentTile = rc.senseMapInfo(rc.getLocation());
                if (currentTile.getPaint() == PaintType.EMPTY && rc.canAttack(rc.getLocation())){
                    boolean useSecondaryColor = rc.senseMapInfo(rc.getLocation()).getMark() == PaintType.ALLY_SECONDARY;
                    rc.attack(rc.getLocation(), useSecondaryColor);
                }
                return;
            }
            //once reached the target loc

            //if in past round the bot found some tiles (which is part of a pattern) are paint by enemy
            if (ruinWithPatternDamaged.size() > 0){
                MapLocation nearestTower = getNearestTower();
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
                    rc.sendMessage(nearestTower, encodeMessage(OptCode.DAMAGEDPATTERN,firstElement)); // DONE: add first firstElement to the messag
                    rc.setIndicatorDot(nearestTower, 0,255,0);
                    rc.setIndicatorString("message sent: damaged pattern find");
                }
            }

            if (lowPaintFlag){
                rc.setIndicatorString("need healing");
                MapLocation nearestTower = getNearestTower();

                //if i can get paint from nearestTower
                if (rc.canTransferPaint(nearestTower, PAINTTOTAKE)){
                    stayPut = false; 
                    rc.transferPaint(nearestTower, PAINTTOTAKE);
                }
                else if (friendMopperFound){
                    return;//stop here and wait for the mopper to give paint
                }
                else{
                    BugNavigator.moveTo(nearestTower);
                    if (rc.canSendMessage(nearestTower))
                        rc.sendMessage(nearestTower, OptCode.NEEDPAINT);
                    rc.setIndicatorString("wait for heal");
                }
            }

            if (enemyTowerFound){
            //TODO: what to do once an enemy tower is found
            }

            if (ruinsFound.size() > 0){
                for (MapInfo curRuin : ruinsFound){
                    tryToMarkPattern(curRuin);

                    // Fill in any spots in the pattern with the appropriate paint.
                    for (MapInfo patternTile : rc.senseNearbyMapInfos(curRuin.getMapLocation(), 8)){
                        //if we see an ally mark
                        if (patternTile.getMark().isAlly()){
                            //try to move to empty tile
                            if (patternTile.getPaint() == PaintType.EMPTY)
                                BugNavigator.moveTo(patternTile.getMapLocation());

                            //if the tile is not paint as expected (primary color instead of secondary or vice versa or empty paint)
                            if (patternTile.getMark() != patternTile.getPaint()){
                                boolean useSecondaryColor = patternTile.getMark() == PaintType.ALLY_SECONDARY;
                                tryToPaintAtLoc(patternTile.getMapLocation(), useSecondaryColor);
                                rc.setIndicatorString("try to build the tower");
                            }
                        }
                        //if there is a pattern and it's paint by enemy
                        else if (patternTile.getPaint().isEnemy()){
                            ruinWithPatternDamaged.add(curRuin.getMapLocation());
                            rc.setIndicatorString("pattern damaged, enemy paint found."); //TODO: find a way to avoid multiple bot find the same enemy tile and all go back to the tower
                        }
                    }

                    tryToBuildTower(curRuin);
                }
            }

            //if so far, it din't move, explore unpaint tile (TODO: that is whitin the region)
            if (emptyTile.x != -1){
                BugNavigator.moveTo(emptyTile);
                rc.setIndicatorString("move to a random empty tile");

            }
        
            // Move and attack randomly if no objective.
            Direction dir = directions[rng.nextInt(directions.length)];
            if (rc.canMove(dir)){
                rc.setIndicatorString("move randomly");
                rc.move(dir);
            }
        }
        





    }
}