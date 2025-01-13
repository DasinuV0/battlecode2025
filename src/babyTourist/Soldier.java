package babyTourist;

import battlecode.common.*;
// import Iteration2.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.ArrayList;


public class Soldier extends Robot {
    MapLocation targetTower;
    Direction dirToEnemy;

    public Soldier(RobotController _rc) throws GameActionException {
        super(_rc);
        Team enemy = rc.getTeam().opponent();
        Team myTeam = rc.getTeam();
        MapLocation myLocation = rc.getLocation();
        
        rushInitialise(_rc);
    }

    public void rushInitialise(RobotController _rc) {
        if (_rc.getRoundNum() < 10) {
            System.out.println("suicide robot generated");
            this.isSuicideRobot = 1;
        }        
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
        for (RobotInfo robot : nearbyRobots) {
            if (robot.type.isTowerType())
                towersPos.add(robot.location);
            else if (robot.team.isPlayer() && robot.type == UnitType.MOPPER){
                rc.setIndicatorDot(robot.location, 1,1,1);
                friendMopperFound = true;
            }
        }
        
        // if suicideRobot, find the enemy tower
        if (isSuicideRobot == 1) rushTowerFind(rc);
    }

    void rushTowerFind(RobotController _rc) throws GameActionException {
        Team myTeam = rc.getTeam();
        MapLocation myLocation = rc.getLocation();
        if (this.checkedEnemyTower == 0) {
            RobotInfo[] nearbyAllyRobots = rc.senseNearbyRobots(-1, myTeam);
            if (nearbyAllyRobots[0].getType().isTowerType()) {
                System.out.println("ally Tower found at " + nearbyAllyRobots[0].location);
                this.checkedEnemyTower = 1;
                MapLocation allyTower = nearbyAllyRobots[0].getLocation();
                this.targetTower = Symmetry.getEnemyStartingTowerCoord(rc, allyTower);
                this.dirToEnemy = myLocation.directionTo(targetTower);
            }
        }
    }

    //Instructions at the end of each turn
    void endTurn() throws GameActionException {

    }

    //Core turn method
    void runTurn() throws GameActionException {
        if (isSuicideRobot == 1) {
            rc.setIndicatorString("suicideRobot");
            // Move towards the guessed enemy tower
            // System.out.println("Moving towards enemy tower: " + targetTower.toString());
            
            BugNavigator.moveTo(targetTower);
            

            // Attack the guessed enemy tower if in range
            if (rc.canAttack(targetTower)) {
                rc.attack(targetTower);
            }
        }else{
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
}