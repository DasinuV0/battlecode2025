package babyTourist;

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
        if (isSuicideRobot == 1) {
            Team enemy = rc.getTeam().opponent();
            Team myTeam = rc.getTeam();
            MapLocation myLocation = rc.getLocation();
            RobotInfo[] nearbyRobots = rc.senseNearbyRobots(-1, myTeam);
            MapLocation[] allyTowers = Arrays.stream(nearbyRobots)
                    .filter(robot -> robot.getType().isTowerType())
                    .map(RobotInfo::getLocation)
                    .toArray(MapLocation[]::new);

            // Guess enemy tower locations based on symmetry
            MapLocation[] guessedEnemyTowers = new MapLocation[allyTowers.length];
            int mapWidth = rc.getMapWidth();
            int mapHeight = rc.getMapHeight();

            for (int i = 0; i < allyTowers.length; i++) {
                int mirroredX = mapWidth - allyTowers[i].x - 1;
                int mirroredY = mapHeight - allyTowers[i].y - 1;
                guessedEnemyTowers[i] = new MapLocation(mirroredX, mirroredY);
            }

            // Send soldiers to the closest guessed enemy tower
            MapLocation targetTower = guessedEnemyTowers[0];
            Direction dirToEnemy = myLocation.directionTo(targetTower);

            // Move towards the guessed enemy tower
            if (rc.canMove(dirToEnemy)) {
                rc.move(dirToEnemy);
            }

            // Attack the guessed enemy tower if in range
            if (rc.canAttack(targetTower)) {
                rc.attack(targetTower);
            }
        } else {
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