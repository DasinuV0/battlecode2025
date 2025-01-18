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

    public Splasher(RobotController _rc) throws GameActionException {
        super(_rc);

    }

    public void beginTurn() throws GameActionException {
        resetFlags();
        updateLowPaintFlag();
        listenMessage();

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
            int zone = Symmetry.getRegion(rc, targetLocation);
            int currZone = Symmetry.getRegion(rc, rc.getLocation());
            if (currZone != zone) {
                Navigation.Bug2.move(targetLocation);
            } else {
                // move randomly within the region
                Random rand = new Random();
                Direction[] directions = Direction.values();
                Direction dir = directions[rand.nextInt(directions.length)];
                if (rc.canMove(dir)) {
                    rc.move(dir);
                }                
            }
        }        
        
        // if more than 3 tiles empty -> attack
        int emptyTiles = calculateEmptyTiles(rc);

        if (emptyTiles > 3) {
            rc.setIndicatorString("Attacking with splash");
            // attack
            if (rc.canAttack(rc.getLocation())) {
                rc.attack(rc.getLocation());
            }
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
                Random rand = new Random();
                Direction[] directions = Direction.values();
                Direction dir = directions[rand.nextInt(directions.length)];
                if (rc.canMove(dir)) {
                    rc.move(dir);
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
        MapInfo[] surrMapInfos = rc.senseNearbyMapInfos();
        int emptyTiles = 0;
        for (MapInfo mapInfo : surrMapInfos) {
            if (mapInfo.getPaint() != PaintType.ALLY_PRIMARY && mapInfo.getPaint() != PaintType.ALLY_SECONDARY) {
                emptyTiles++;
            }
        }
        return emptyTiles;
    }
    
}
