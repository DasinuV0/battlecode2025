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
        lowPaintFlag = false;
        // friendMopperFound = false;

        listenMessage();

        //get the robotInfo of rc and then calculate the percetange of the remain paint 
        int remainPaint = calculatePaintPercentage(rc.senseRobotAtLocation(rc.getLocation()));
        //check if it is low paint
        if (remainPaint <= LOWPAINTTRESHHOLD)
            lowPaintFlag = true;
        else    
            lowPaintFlag = false;
        

        //check if any tower is found
        RobotInfo[] nearbyRobots = rc.senseNearbyRobots();
        for (RobotInfo robot : nearbyRobots)
            if (robot.type.isTowerType())
                towersPos.add(robot.location);
            else if (robot.team.isPlayer() && robot.type.isRobotType() && robot.type != UnitType.MOPPER && robotToHealSet.contains(robot.location) == false && calculatePaintPercentage(robot) <= LOWPAINTTRESHHOLD){
                rc.setIndicatorDot(robot.location, 255,0,0);
                robotToHealQueue.add(robot.location);
                robotToHealSet.add(robot.location);
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
            // else if (friendMopperFound)
            //     ; //stop here and wait for the mopper to give paint
            else{
                BugNavigator.moveTo(nearestTower);
            }
        }else if (false){
            //TODO: what if rc found a ruin

        }
        //if there is any robot to heal
        else if (!robotToHealQueue.isEmpty()){
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
                else
                    BugNavigator.moveTo(curr);
            }
        }

        //TODO: establish an algo to move/find ruin
        //TODO: attack only those that are clsoe to ruins
        // Move and attack randomly if no objective.
        // Direction dir = directions[rng.nextInt(directions.length)];
        // if (rc.canMove(dir)){
        //     rc.move(dir);
        // }

        // MapInfo currentTile = rc.senseMapInfo(rc.getLocation());
        // if (!currentTile.getPaint().isAlly() && rc.canAttack(rc.getLocation()))
        //     rc.attack(rc.getLocation());
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
        }
    }
}