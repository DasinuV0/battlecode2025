package Robot;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.ArrayList;
import java.util.LinkedList;

import Util.*;
import Navigation.Bug2;

import battlecode.common.*;

public class Splasher extends Robot {

    static boolean locationReached = false;
    private LinkedList<MapLocation> recentLocations = new LinkedList<MapLocation>();

    public Splasher(RobotController _rc) throws GameActionException {
        super(_rc);

    }

    public void beginTurn() throws GameActionException {
        resetFlags();
        listenMessage();
        updateLowPaintFlag();
        // updateLowHealthFlag();

        //check if any tower is found
        RobotInfo[] nearbyRobots = rc.senseNearbyRobots();
        for (RobotInfo robot : nearbyRobots) {
            //check if our paint/money tower is found
            if (isPaintTower(robot.type) && robot.team == rc.getTeam())
                    paintTowersPos.add(robot.location);
            else if (isMoneyTower(robot.type) && robot.team == rc.getTeam())
                    moneyTowersPos.add(robot.location);
            else if (robot.team == rc.getTeam() && robot.type == UnitType.MOPPER)
                friendMopperFound = true;
        }
    }

    public void endTurn() throws GameActionException {
        // if current location is in recent locations, move randomly
        if (recentLocations.contains(rc.getLocation())) {
             moveRandomly();
        }

        // if current location paint type is enemy, move away
        MapInfo mapInfo = rc.senseMapInfo(rc.getLocation());
        if (mapInfo.getPaint().isEnemy()) {
            moveRandomly();
        }

        recentLocations.add(rc.getLocation());
        if (recentLocations.size() > 5) {
            recentLocations.removeFirst();
        }

    }

    public void runTurn() throws GameActionException {
        // isAttackSplasher | isDefenseSplasher | lowPaintFlag
         if (lowPaintFlag) {
            runLowPaintSplasher();
        } else if (isAttackSplasher) {
            runAttackSplasher();
        } else if (isDefenseSplasher) {
            runDefenseSplasher();
        }

    }

    void runAttackSplasher() throws GameActionException {
        // A=0,B=1,C=2,D=3       
        if (!locationReached) {            
            if (rc.getLocation().distanceSquaredTo(targetLocation) <= 1) {
                locationReached = true;
            }
            Navigation.Bug2.move(targetLocation);
        } else {
            // stay in the same region
            int x = rng.nextInt(rc.getMapWidth());
            int y = rng.nextInt(rc.getMapHeight());
            targetLocation = new MapLocation(x,y);
            rc.setIndicatorString("move to" + x + " " + y);
            Navigation.Bug2.move(targetLocation);           
        }        
    
        // if more than 3 tiles empty -> attack
        int emptyTiles = calculateEmptyTiles(rc);
    
        if (emptyTiles > 3) {
            rc.setIndicatorString("Attacking with splash");
            MapLocation attackLocation = getEnemyPaintZone(rc);
            // attack
            // Navigation.Bug2.move(attackLocation);
            // rc.setIndicatorString("Moving towards" + attackLocation);
            if (rc.canAttack(rc.getLocation())) {
                rc.attack(rc.getLocation());
            }
            if (calculateHealthPercentage(rc.senseRobotAtLocation(rc.getLocation())) > 30 && rc.getActionCooldownTurns() <= 1) {
                targetLocation = attackLocation;
                locationReached = false;
                rc.setIndicatorString("Moving towards" + attackLocation);
            } else {
                targetLocation = attackLocation.add(rc.getLocation().directionTo(attackLocation).opposite());
                locationReached = false;
                rc.setIndicatorString("Moving towards" + attackLocation.add(rc.getLocation().directionTo(attackLocation).opposite()));
            }
        }
    
        // Update recent locations
        recentLocations.add(rc.getLocation());
        if (recentLocations.size() > 5) {
            recentLocations.removeFirst();
        }
    }

    void moveRandomly() throws GameActionException {
        Random rand = new Random();
        Direction[] directions = Direction.values();
        Direction dir = directions[rand.nextInt(directions.length)];
        if (rc.canMove(dir) && !recentLocations.contains(rc.adjacentLocation(dir))) {
            rc.move(dir);
        } else if (rc.canMove(dir.rotateLeft()) && !recentLocations.contains(rc.adjacentLocation(dir.rotateLeft()))) {
            rc.move(dir.rotateLeft());
        } else if (rc.canMove(dir.rotateRight()) && !recentLocations.contains(rc.adjacentLocation(dir.rotateRight()))) {
            rc.move(dir.rotateRight());
        } else if (rc.canMove(dir.opposite()) && !recentLocations.contains(rc.adjacentLocation(dir.opposite()))) {
            rc.move(dir.opposite());
        }
    }
    

    void runDefenseSplasher() throws GameActionException {
        // A=0,B=1,C=2,D=3
        if (!locationReached) {            
            if (rc.getLocation().distanceSquaredTo(targetLocation) <= 1) {
                locationReached = true;
            }
            Navigation.Bug2.move(targetLocation);
        } else {
            // stay in the same region
            int zone = Symmetry.getRegion(rc, targetLocation);
            int currZone = Symmetry.getRegion(rc, rc.getLocation());
            if (currZone != zone) {
                Navigation.Bug2.move(targetLocation);
            } else {
                // move randomly within the region
                Random rand = new Random(6147);
                Direction[] directions = Direction.values();
                Direction dir = directions[rand.nextInt(directions.length)];
                if (rc.canMove(dir)) {
                    rc.move(dir);
                } else if (rc.canMove(dir.rotateLeft())) {
                    rc.move(dir.rotateLeft());
                } else if (rc.canMove(dir.rotateRight())) {
                    rc.move(dir.rotateRight());
                }
            }
        }        
        
        // if more than 3 tiles empty -> attack
        int emptyTiles = calculateEmptyTiles(rc);

        if (emptyTiles > 3) {
            
            // attack
            if (rc.canAttack(rc.getLocation())) {
                rc.attack(rc.getLocation());
            }
        }
    }

    void runLowPaintSplasher() throws GameActionException {
        rc.setIndicatorString("need healing");
            MapLocation nearestAllyTower = getNearestAllyTower();
            if (nearestAllyTower.x == -1){
                resetMessageFlag();
                exploreMode = true;
                rc.setIndicatorString("tower is destroyed, go back to explore mode and try to re-build the tower");
                return;
            }                


            //if i can get paint from nearestAllyTower
            if (rc.canTransferPaint(nearestAllyTower, PAINTTOTAKE)){
                rc.transferPaint(nearestAllyTower, PAINTTOTAKE);
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

    int calculateEmptyTiles(RobotController rc) throws GameActionException {
        MapInfo[] surrMapInfos = rc.senseNearbyMapInfos(4);
        int emptyTiles = 0;
        for (MapInfo mapInfo : surrMapInfos) {
            if (!mapInfo.getPaint().isAlly() && mapInfo.isPassable() && !mapInfo.isWall()) {
                rc.setIndicatorDot(mapInfo.getMapLocation(), 255, 255, 0);
                emptyTiles++;
            }
        }
        return emptyTiles;
    }
    
}
