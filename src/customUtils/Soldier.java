package v1;

import battlecode.common.*;
import Iteration2.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class Soldier extends Robot {
    public Soldier(RobotController _rc) throws GameActionException {
        super(_rc);
    }

    //instructions run at the beginning of each turn
    void beginTurn() throws GameActionException {
        lowPaintFlag = false;
        // friendMopperFound = false;

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
            else if (robot.team.isPlayer() && robot.type == UnitType.MOPPER){
                rc.setIndicatorDot(robot.location, 1,1,1);
                friendMopperFound = true;
            }
    }

    //Instructions at the end of each turn
    void endTurn() throws GameActionException {
       
    }

    //Core turn method
    void runTurn() throws GameActionException {
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
            if (rc.canTransferPaint(nearestTower, PAINTTOTAKE))
                rc.transferPaint(nearestTower, PAINTTOTAKE);
            else if (friendMopperFound)
                ; //stop here and wait for the mopper to give paint
            else{
                BugNavigator.moveTo(nearestTower);
            }
        }else if (false){
            //TODO: what if rc found a ruin

        }
        else{
            //TODO: establish an algo to move/find ruin
            // Move and attack randomly if no objective.
            Direction dir = directions[rng.nextInt(directions.length)];
            if (rc.canMove(dir)){
                rc.move(dir);
            }

            MapInfo currentTile = rc.senseMapInfo(rc.getLocation());
            if (!currentTile.getPaint().isAlly() && rc.canAttack(rc.getLocation())){
                rc.attack(rc.getLocation());
            }
        }


    }
}