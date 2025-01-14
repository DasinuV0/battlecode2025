package babyTourist;

import battlecode.common.*;
// import Iteration2.*;

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

    Direction dirToEnemy;
    MapLocation targetTower;
    boolean enemyFound = false;

    public Mopper(RobotController _rc) throws GameActionException {
        super(_rc);
        robotToHealQueue = new LinkedList<>();
        robotToHealSet = new HashSet<>();

        rushInitialise(_rc);

    }

    private void rushInitialise(RobotController _rc) {
        if (_rc.getRoundNum() < 10) {
            System.out.println("Defence mopper generated");
            this.isDefenceMopper = 1;
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


        //check if any tower is found
        RobotInfo[] nearbyRobots = rc.senseNearbyRobots();
        for (RobotInfo robot : nearbyRobots) {
            if (robot.type.isTowerType())
                towersPos.add(robot.location);
            else if (robot.team.isPlayer() && robot.type.isRobotType() && robot.type != UnitType.MOPPER && robotToHealSet.contains(robot.location) == false && calculatePaintPercentage(robot) <= LOWPAINTTRESHHOLD){
                rc.setIndicatorDot(robot.location, 255,0,0);
                robotToHealQueue.add(robot.location);
                robotToHealSet.add(robot.location);
            }
        }

        if (isDefenceMopper == 1) rushTowerFind(rc);
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

    void givePaintToTower(RobotController rc) throws GameActionException {
        MapLocation nearestTower = new MapLocation(0,0);
        int minDist = 9999;
        for (MapLocation pos : towersPos){
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
    void runTurn() throws GameActionException {
        if (isDefenceMopper == 1) {
            rc.setIndicatorString("Defense Mopper");

            
            // if (enemyFound == false) {
            //     BugNavigator.moveTo(targetTower);
            // }
            Team enemy = rc.getTeam().opponent();

            // sense and attack enemy robots
            RobotInfo[] nearbyRobots = rc.senseNearbyRobots(-1, enemy);
            for (RobotInfo robot : nearbyRobots) {
                Direction dirToEnemy = rc.getLocation().directionTo(robot.location);
                if (rc.canAttack(robot.location)) {
                    rc.attack(robot.location);
                }
                BugNavigator.moveTo(robot.location);
                enemyFound = true;
            }
            givePaintToTower(rc);
        } else {

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
            else{
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
                        else
                            BugNavigator.moveTo(curr);
                    }
                }
    
                //TODO: establish an algo to move/find ruin
                // Move and attack randomly if no objective.
                Direction dir = directions[rng.nextInt(directions.length)];
                if (rc.canMove(dir)){
                    rc.move(dir);
                }
    
                MapInfo currentTile = rc.senseMapInfo(rc.getLocation());
                if (!currentTile.getPaint().isAlly() && rc.canAttack(rc.getLocation()))
                    rc.attack(rc.getLocation());
    
    
    
            }
        }



    }
}