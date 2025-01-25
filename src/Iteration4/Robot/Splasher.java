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
    // static boolean hasTarget = false;
    // static boolean exploreMode = true;
    private LinkedList<MapLocation> recentLocations = new LinkedList<MapLocation>();
    private MapLocation attackLocation = new MapLocation(-1,-1);   
    private MapLocation interMediateLocation = new MapLocation(-1,-1); 
    private boolean hasIntermediateLocation = false;
    private int TOWERTHRESHOLD = 0;
    private int defenseTowerAttacked = 0;
    private RobotInfo currDefenseTower = null;

    public Splasher(RobotController _rc) throws GameActionException {
        super(_rc);

    }

    public void beginTurn() throws GameActionException {
        resetFlags();
        if (exploreMode || lowPaintTower) { listenMessage(); }
        updateLowPaintFlag();
        tryToSendPaintPos();
        // updateLowHealthFlag();

        if ((targetLocation.x >= 0 && targetLocation.y >= 0) || receivedTarget ) {
            hasTarget = true;
            exploreMode = false;
        } else {
            hasTarget = false;
            exploreMode = true;
        }

        if (hasIntermediateLocation && interMediateLocation.x != -1) {
            targetLocation = interMediateLocation;
            interMediateLocation = new MapLocation(-1,-1);
            hasIntermediateLocation = false;
        }

        int splasherCount = 0;
        boolean defenseTowerDetected = false;
        MapLocation defenseTowerLocation = new MapLocation(-1,-1);

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
            else if (robot.team == rc.getTeam() && robot.type == UnitType.SPLASHER)
                splasherCount++;
            else if (robot.team != rc.getTeam() && isDefendTower(robot.type))
                defenseTowerDetected = true;
                defenseTowerLocation = robot.location;
                RobotInfo prevDefenseTower = currDefenseTower;
                currDefenseTower = robot;
                if (prevDefenseTower != null && prevDefenseTower.getID() != currDefenseTower.getID()) {
                    defenseTowerAttacked = 0;
                }
        }

        MapInfo[] surrMapInfos = rc.senseNearbyMapInfos(-1);
        for (MapInfo mapInfo : surrMapInfos) {
            if (mapInfo.isResourcePatternCenter()) {
                detectedSRP = true;
            }
        }

        if (isSmallerMap()) {
            TOWERTHRESHOLD = 5;
        } else {
            TOWERTHRESHOLD = 9;
        }

        if (defenseTowerDetected) {
            if (currDefenseTower.getHealth() <= 800 && splasherCount >= 2 && defenseTowerAttacked < 1) {
                // Move towards or attack the defense tower
                if (rc.canAttack(defenseTowerLocation)) {
                    rc.attack(defenseTowerLocation);
                    defenseTowerAttacked++;
                } else {
                    Navigation.Bug2.move(defenseTowerLocation);
                }
            } else if (splasherCount > 5 && defenseTowerAttacked < 1) {
                // Move towards or attack the defense tower
                if (rc.canAttack(defenseTowerLocation)) {
                    rc.attack(defenseTowerLocation);
                } else {
                    Navigation.Bug2.move(defenseTowerLocation);
                }
            } else {
                // Move randomly until out of range of the defense tower 
                while (rc.canSenseLocation(defenseTowerLocation) && isDefendTower(rc.senseRobotAtLocation(defenseTowerLocation).getType())) {
                    moveRandomly();
                }
            }
        }

    }

    public void endTurn() throws GameActionException {
        // if current location is in recent locations, move randomly
        // if (recentLocations.contains(rc.getLocation())) {
        //      moveRandomly();
        // }

        // // if current location paint type is enemy, move away
        // MapInfo mapInfo = rc.senseMapInfo(rc.getLocation());
        // if (mapInfo.getPaint().isEnemy()) {
        //     moveRandomly();
        // }

        // recentLocations.add(rc.getLocation());
        // if (recentLocations.size() > 5) {
        //     recentLocations.removeFirst();
        // }
        if (rc.senseMapInfo(rc.getLocation()).getPaint().isEnemy()) {
            interMediateLocation = targetLocation;
            targetLocation = rc.getLocation().add(rc.getLocation().directionTo(targetLocation).opposite());
            hasIntermediateLocation = true;
            rc.setIndicatorString("Moving away to: " + targetLocation);
        }

    }

    public void runTurn() throws GameActionException {
        // isAttackSplasher | isDefenseSplasher | lowPaintFlag
         if (lowPaintFlag) {
            runLowPaintSplasher();
        } else if (isAttackSplasher) {
            rc.setIndicatorString("target of splash is " + targetLocation);
            runDefenseSplasher();
        } else if (isDefenseSplasher) {
            runDefenseSplasher();
        } else {
            runDefenseSplasher();
        }

    }

    void runAttackSplasher() throws GameActionException {
        // A=0,B=1,C=2,D=3
        if (targetLocation.x >= 0) {            
            rc.setIndicatorString("target location is " + targetLocation);
            if (!locationReached) {            
                if (rc.getLocation().distanceSquaredTo(targetLocation) <= 1) {
                    locationReached = true;
                }
                Navigation.Bug2.move(targetLocation);
            } else {
                // move to random location
                if (!hasTarget) {
                    int x = rng.nextInt(rc.getMapWidth());
                    int y = rng.nextInt(rc.getMapHeight());
                    targetLocation = new MapLocation(x,y);
                    rc.setIndicatorString("move to" + x + " " + y);
                    Navigation.Bug2.move(targetLocation);   
                    hasTarget = true;        
                }
            }        
        }
        // if more than 3 tiles empty -> attack
        // int emptyTiles = calculateEmptyTiles(rc);
        MapLocation attackLocation = getEnemyPaintZone(rc);
    
        // if (emptyTiles > 3) {
        if (attackLocation.x >= 0) {
            rc.setIndicatorString("Attacking with splash");
            // MapLocation attackLocation = getEnemyPaintZone(rc);
            // attack
            Navigation.Bug2.move(attackLocation);
            // rc.setIndicatorString("Moving towards" + attackLocation);
            if (rc.canAttack(attackLocation) && detectedSRP == false) {
                rc.attack(attackLocation);
            } 
            // if (calculateHealthPercentage(rc.senseRobotAtLocation(rc.getLocation())) > 30 && rc.getActionCooldownTurns() <= 1) {
            //     targetLocation = attackLocation;
            //     locationReached = false;
            //     hasTarget = true;
            //     rc.setIndicatorString("Moving towards" + attackLocation);
            // } else {
            //     targetLocation = rc.getLocation().add(rc.getLocation().directionTo(attackLocation).opposite());
            //     locationReached = false;
            //     hasTarget = true;
            //     rc.setIndicatorString("(Opposite) Moving towards" + rc.getLocation().add(rc.getLocation().directionTo(attackLocation).opposite()));
            // }
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
        if (rc.canMove(dir)) {
            rc.move(dir);
        } else if (rc.canMove(dir.rotateLeft())) {
            rc.move(dir.rotateLeft());
        } else if (rc.canMove(dir.rotateRight())) {
            rc.move(dir.rotateRight());
        } else if (rc.canMove(dir.opposite())) {
            rc.move(dir.opposite());
        }
    }
    

    void runDefenseSplasher() throws GameActionException {
        /*
         * if has target
         *     if reached target
         *        has target = false
         * 
         * if not has target
         *    set target to random location
         *      has target = true
         *      can listen message
         * 
         * get paint zone
         * if paint zone is found
         *      can not listen message
         *      if distance to paint zone is greater than 4
         *          target paint zone
         *          has target = true
         *     else
         *         attack paint zone
         * 
         *      if health is greater than 30 and action cooldown is less than 1
         *          target paint zone
         *          has target = true
         *      else
         *          target opposite direction of paint zone
         *          has target = true
         * 
         * move to target
         */
        if (lowPaintFlag) {
            runLowPaintSplasher();
            return;
        }
        if (hasTarget) {
            if (exploreMode) {
                if (rc.getLocation().distanceSquaredTo(targetLocation) < 1) {
                    // hasTarget = false;
                    int x = rng.nextInt(rc.getMapWidth());
                    int y = rng.nextInt(rc.getMapHeight());
                    targetLocation = new MapLocation(x,y);
                    hasTarget = true;
                    exploreMode = false;

                    attackLocation = getEnemyPaintZone(rc);
                    if (attackLocation.x >= 0 && attackLocation.y >= 0 && attackLocation.x < rc.getMapWidth() && attackLocation.y < rc.getMapHeight()) {
                        exploreMode = false;
                        if (rc.canAttack(attackLocation)) {
                            rc.setIndicatorString("(rand targ + explore) attacking diff loc: " + attackLocation);
                            if (detectedSRP == false) {
                                rc.attack(attackLocation);
                            }
                            hasTarget = true;
                            if (rc.getNumberTowers() > TOWERTHRESHOLD) {
                                if (rng.nextDouble() < 0.7) {
                                    targetLocation = rc.getLocation().add(rc.getLocation().directionTo(attackLocation).opposite());
                                } else {
                                    targetLocation = rc.getLocation().add(rc.getLocation().directionTo(attackLocation));
                                }
                            } else {
                                if (calculateHealthPercentage(rc.senseRobotAtLocation(rc.getLocation())) > 30 && rc.getActionCooldownTurns() <= 1) {
                                    targetLocation = rc.getLocation().add(rc.getLocation().directionTo(attackLocation));
                                } else {
                                    targetLocation = rc.getLocation().add(rc.getLocation().directionTo(attackLocation).opposite());
                                }
                            }
                            attackLocation = new MapLocation(-1,-1);
                        } else {
                            rc.setIndicatorString("(rand targ + explore) found attacking loc (moving towards it): " + attackLocation);
                            targetLocation = attackLocation;
                            hasTarget = true; 
                            Navigation.Bug2.move(targetLocation);
                            return;                       
                        }
                        rc.setIndicatorString("(rand targ + explore) moving to target location: " + targetLocation);
                        Navigation.Bug2.move(targetLocation);
                    } else {
                        rc.setIndicatorString("(rand targ + explore) moving to rand point " + targetLocation);
                        Navigation.Bug2.move(targetLocation);
                        return;
                    }
                } else {
                    if (attackLocation.x == -1) {
                        attackLocation = getEnemyPaintZone(rc);
                    }
                    if (attackLocation.x >= 0 && attackLocation.y >= 0 && attackLocation.x < rc.getMapWidth() && attackLocation.y < rc.getMapHeight()) {
                        exploreMode = false;
                        if (rc.canAttack(attackLocation)) {
                            rc.setIndicatorString("(has targ + explore) Attacking location is " + attackLocation);
                            if (detectedSRP == false) {
                                rc.attack(attackLocation);
                            }
                            hasTarget = true;
                            if (rc.getNumberTowers() > TOWERTHRESHOLD) {
                                if (rng.nextDouble() < 0.7) {
                                    targetLocation = rc.getLocation().add(rc.getLocation().directionTo(attackLocation).opposite());
                                } else {
                                    targetLocation = rc.getLocation().add(rc.getLocation().directionTo(attackLocation));
                                }
                            } else {
                                if (calculateHealthPercentage(rc.senseRobotAtLocation(rc.getLocation())) > 30 && rc.getActionCooldownTurns() <= 1) {
                                    targetLocation = rc.getLocation().add(rc.getLocation().directionTo(attackLocation));
                                } else {
                                    targetLocation = rc.getLocation().add(rc.getLocation().directionTo(attackLocation).opposite());
                                }
                            }
                            attackLocation = new MapLocation(-1,-1);
                        } else {
                            targetLocation = attackLocation;
                            rc.setIndicatorString("(has targ + explore) moving towards attackLocation: " + attackLocation);
                            hasTarget = true;               
                        }
                    } else {
                        rc.setIndicatorString("(has targ + explore) no attacking location found moving to: " + targetLocation);
                        hasTarget = true;
                        exploreMode = true;
                    }

                    Navigation.Bug2.move(targetLocation);
                    return;
                }
            } else {
                rc.setIndicatorString("Moving to target location: " + targetLocation);
                Navigation.Bug2.move(targetLocation);

                if (rc.getLocation().distanceSquaredTo(targetLocation) <= 1) {
                    // hasTarget = false;
                    int x = rng.nextInt(rc.getMapWidth());
                    int y = rng.nextInt(rc.getMapHeight());
                    targetLocation = new MapLocation(x,y);
                    hasTarget = true;
                    exploreMode = true;

                    if (attackLocation.x == -1) 
                        attackLocation = getEnemyPaintZone(rc);
                    if (attackLocation.x >= 0 && attackLocation.y >= 0 && attackLocation.x < rc.getMapWidth() && attackLocation.y < rc.getMapHeight()) {
                        exploreMode = false;
                        if (rc.canAttack(attackLocation)) {
                            rc.setIndicatorString("(targ + no explore) Attacking location is " + attackLocation);
                            if (detectedSRP == false) {
                                rc.attack(attackLocation);
                            }
                            hasTarget = true;
                            if (rc.getNumberTowers() > TOWERTHRESHOLD) {
                                if (rng.nextDouble() < 0.7) {
                                    targetLocation = rc.getLocation().add(rc.getLocation().directionTo(attackLocation).opposite());
                                } else {
                                    targetLocation = rc.getLocation().add(rc.getLocation().directionTo(attackLocation));
                                }
                            } else {
                                if (calculateHealthPercentage(rc.senseRobotAtLocation(rc.getLocation())) > 30 && rc.getActionCooldownTurns() <= 1) {
                                    targetLocation = rc.getLocation().add(rc.getLocation().directionTo(attackLocation));
                                } else {
                                    targetLocation = rc.getLocation().add(rc.getLocation().directionTo(attackLocation).opposite());
                                }
                            }
                            attackLocation = new MapLocation(-1,-1);
                        } else {
                            rc.setIndicatorString("(targ + no explore) Moving towards attackLocation: " + attackLocation);
                            targetLocation = attackLocation;
                            hasTarget = true; 
                            Navigation.Bug2.move(targetLocation);
                            return;                       
                        }
                    } else {
                        rc.setIndicatorString("(targ + no explore) Moving to rand point: " + targetLocation);
                        Navigation.Bug2.move(targetLocation);
                        return;
                    }                   
                } else {
                    if (attackLocation.x == -1)
                        attackLocation = getEnemyPaintZone(rc);
                    if (attackLocation.x >= 0 && attackLocation.y >= 0 && attackLocation.x < rc.getMapWidth() && attackLocation.y < rc.getMapHeight()) {
                        exploreMode = false;
                        if (rc.canAttack(attackLocation)) {
                            rc.setIndicatorString("(targ + no explore -> diff) Attacking location is " + attackLocation);
                            if (detectedSRP == false) {
                                rc.attack(attackLocation);
                            }
                            hasTarget = true;
                            if (rc.getNumberTowers() > TOWERTHRESHOLD) {
                                if (rng.nextDouble() < 0.7) {
                                    targetLocation = rc.getLocation().add(rc.getLocation().directionTo(attackLocation).opposite());
                                } else {
                                    targetLocation = rc.getLocation().add(rc.getLocation().directionTo(attackLocation));
                                }
                            } else {
                                if (calculateHealthPercentage(rc.senseRobotAtLocation(rc.getLocation())) > 30 && rc.getActionCooldownTurns() <= 1) {
                                    targetLocation = rc.getLocation().add(rc.getLocation().directionTo(attackLocation));
                                } else {
                                    targetLocation = rc.getLocation().add(rc.getLocation().directionTo(attackLocation).opposite());
                                }
                            }
                            attackLocation = new MapLocation(-1,-1);
                        } else {
                            rc.setIndicatorString("(targ + no explore -> diff) Moving towards attackLocation: " + attackLocation);
                            targetLocation = attackLocation;
                            hasTarget = true;
                            return;                       
                        }
                    }
                }
            }
        } else {
            int x = rng.nextInt(rc.getMapWidth());
            int y = rng.nextInt(rc.getMapHeight());
            targetLocation = new MapLocation(x,y);
            hasTarget = true;
            exploreMode = true;
            
            if (attackLocation.x == -1)
                attackLocation = getEnemyPaintZone(rc);
            if (attackLocation.x >= 0 && attackLocation.y >= 0 && attackLocation.x < rc.getMapWidth() && attackLocation.y < rc.getMapHeight()) {
                exploreMode = false;
                if (rc.canAttack(attackLocation)) {
                    rc.setIndicatorString("(rand targ) Attacking location " + attackLocation);
                    if (detectedSRP == false) {
                        rc.attack(attackLocation);
                    }                    
                    hasTarget = true;
                    if (rc.getNumberTowers() > TOWERTHRESHOLD) {
                        if (rng.nextDouble() < 0.7) {
                            targetLocation = rc.getLocation().add(rc.getLocation().directionTo(attackLocation).opposite());
                        } else {
                            targetLocation = rc.getLocation().add(rc.getLocation().directionTo(attackLocation));
                        }
                    } else {
                        if (calculateHealthPercentage(rc.senseRobotAtLocation(rc.getLocation())) > 30 && rc.getActionCooldownTurns() <= 1) {
                            targetLocation = rc.getLocation().add(rc.getLocation().directionTo(attackLocation));
                        } else {
                            targetLocation = rc.getLocation().add(rc.getLocation().directionTo(attackLocation).opposite());
                        }
                    }
                    attackLocation = new MapLocation(-1,-1);
                } else {
                    rc.setIndicatorString("(rand targ) Moving towards attackLocation: " + attackLocation);
                    targetLocation = attackLocation;
                    hasTarget = true; 
                    Navigation.Bug2.move(targetLocation);
                    return;                       
                }
            } else {
                rc.setIndicatorString("(rand targ) Moving to rand point: " + targetLocation);
                Navigation.Bug2.move(targetLocation);
                return;
            }

        }

        
    }

    void runLowPaintSplasher() throws GameActionException {
        rc.setIndicatorString("need healing");
        boolean isPaintTower = false;
        MapLocation nearestAllyTower = getNearestAllyPaintTower();
        if (nearestAllyTower.x == -1) {
            isPaintTower = false;
            nearestAllyTower = getNearestAllyTower();
        }
        if (nearestAllyTower.x == -1){
            resetMessageFlag();
            // exploreMode = true;
            rc.setIndicatorString("tower is destroyed, go back to explore mode and try to re-build the tower");
            return;
        }                        

        //if i can get paint from nearestAllyTower
        int localPaintToTake = rc.getPaint() - rc.getType().paintCapacity;
        if (rc.canTransferPaint(nearestAllyTower, localPaintToTake)){
            rc.transferPaint(nearestAllyTower, localPaintToTake);
        }
        // else if (!rc.canSenseLocation(nearestAllyTower) && friendMopperFound){
        //     return;//stop here and wait for the mopper to give paint
        // }
        else{ 
            if (!isPaintTower) {
                Navigation.Bug2.move(nearestAllyTower);
                if (rc.canSendMessage(nearestAllyTower)){
                    rc.sendMessage(nearestAllyTower, encodeMessage(OptCode.EXPLORE));
                    rc.setIndicatorString("message sent to chip tower: " + encodeMessage(OptCode.EXPLORE));
                }
            } else {
                if (!lowPaintTower) {
                    int x = rng.nextInt(rc.getMapWidth());
                    int y = rng.nextInt(rc.getMapHeight());
                    targetLocation = new MapLocation(x,y);
                    hasTarget = true;
                    exploreMode = true;
                    rc.setIndicatorString("low paint tower, moving to random point: " + targetLocation);                    
                }
                Navigation.Bug2.move(targetLocation);
                rc.setIndicatorString("low paint, Moving to target location: " + targetLocation);
            }                     
            
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
