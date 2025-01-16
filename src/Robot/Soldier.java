package Robot;

import battlecode.common.*;
import Navigation.Bug1.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.LinkedHashSet;

public class Soldier extends Robot {
    MapLocation targetTower;
    Direction dirToEnemy;

    public Soldier(RobotController _rc) throws GameActionException {
        super(_rc);
        ruinWithPatternDamaged = new HashSet<>();

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

    //Core turn method
    public void runTurn() throws GameActionException {  
        // if (isSuicideRobot == 1) {
        //     rc.setIndicatorString("suicideRobot");
        //     Navigation.Bug2.move(targetTower);
            
        //     if (rc.canAttack(targetTower)) {
        //         rc.attack(targetTower);
        //     }
        // }     
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

        if (exploreMode){
            // rc.setIndicatorString("explore mode");
            if ((rc.getNumberTowers() >= 25 || ruinsFound.size() == 0) && tryToReachTargetLocation()){
                //paint while traveling
                MapInfo currentTile = rc.senseMapInfo(rc.getLocation());
                if (currentTile.getPaint() == PaintType.EMPTY && rc.canAttack(rc.getLocation())){
                    boolean useSecondaryColor = rc.senseMapInfo(rc.getLocation()).getMark() == PaintType.ALLY_SECONDARY;
                    rc.attack(rc.getLocation(), useSecondaryColor);
                }
                rc.setIndicatorString("explore mode: move to  " + targetLocation.x + " " + targetLocation.y);

            }
            //once reached the target loc

            //if in past round the bot found some tiles (which is part of a pattern) are paint by enemy
            if (ruinWithPatternDamaged.size() > 0){
                MapLocation nearestAllyTower = getNearestAllyTower();
                if (nearestAllyTower.x == -1){
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
                MapLocation nearestAllyTower = getNearestAllyTower();
                if (nearestAllyTower.x == -1){
                    rc.setIndicatorString("tower is destroyed, go back to explore mode and try to re-build the tower");
                    tryToRebuildTower();
                    return;
                }                


                //if i can get paint from nearestAllyTower
                int localPaintToTake = rc.getPaint() - rc.getType().paintCapacity;
                if (rc.canTransferPaint(nearestAllyTower, localPaintToTake)){
                    rc.transferPaint(nearestAllyTower, localPaintToTake);
                }// if bot is not in the vision range and a mopper is nearby
                else if (!rc.canSenseLocation(nearestAllyTower) && friendMopperFound){
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

            if (enemyTowerFound){
            //TODO: what to do once an enemy tower is found
                return;
            }

            if (ruinsFound.size() > 0 && rc.getNumberTowers() < 25){
                rc.setIndicatorString("ruins found");
                // targetLocation = new MapLocation(-1,-1);
                for (MapInfo curRuin : ruinsFound){
                    Navigation.Bug2.move(curRuin.getMapLocation());
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
                        else if (patternTile.getPaint().isEnemy()){
                            ruinWithPatternDamaged.add(curRuin.getMapLocation());
                            rc.setIndicatorDot(patternTile.getMapLocation(), 25,14,24); //TODO: find a way to avoid multiple bot find the same enemy tile and all go back to the tower
                            rc.setIndicatorString("pattern damaged, enemy paint found."); //TODO: find a way to avoid multiple bot find the same enemy tile and all go back to the tower
                        }
                    }

                    tryToBuildTower(curRuin);
                }
                return;
            }

            //if so far, it din't move, explore unpaint tile (TODO: that is whitin the region)
            if (emptyTile.x != -1 && rc.isMovementReady()){
                Navigation.Bug2.move(emptyTile);
                rc.setIndicatorString("move to a random empty tile");
                tryToPaintAtLoc(rc.getLocation(), PaintType.EMPTY);
            }
        
            // Move and attack randomly if no objective.
            Direction dir = directions[rng.nextInt(directions.length)];
            if (rc.canMove(dir)){
                rc.setIndicatorString("move randomly");
                rc.move(dir);
            }
        }
        
        if (removePatterMode){
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
                MapLocation nearestAllyTower = getNearestAllyTower();
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

            if (enemyTowerFound){
            //TODO: what to do once an enemy tower is found
                return;
            }

            if (ruinsFound.size() > 0){
                for (MapInfo curRuin : ruinsFound){
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
                        else if (patternTile.getPaint().isEnemy()){
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
        
            // Move and attack randomly if no objective.
            Direction dir = directions[rng.nextInt(directions.length)];
            if (rc.canMove(dir)){
                rc.setIndicatorString("move randomly");
                rc.move(dir);
            }
        }

        if (attackMode){
            if (tryToReachTargetLocation()){
                //paint while traveling
                MapInfo currentTile = rc.senseMapInfo(rc.getLocation());
                if (currentTile.getPaint() == PaintType.EMPTY && rc.canAttack(rc.getLocation())){
                    boolean useSecondaryColor = rc.senseMapInfo(rc.getLocation()).getMark() == PaintType.ALLY_SECONDARY;
                    rc.attack(rc.getLocation(), useSecondaryColor);
                }

                rc.setIndicatorString("attack mode: move to  " + targetLocation.x + " " + targetLocation.y);
                return;
            }
            
            Team enemy = rc.getTeam().opponent();
            RobotInfo[] enemyTowers = rc.senseNearbyRobots(-1, enemy);

            for (RobotInfo tower : enemyTowers)
                if (rc.canAttack(tower.location)){
                    rc.setIndicatorString("attack mode: attack enemy tower (" + tower.location.x + " " + tower.location.y + ")");
                    rc.attack(tower.location);
                }

        }

    }
}