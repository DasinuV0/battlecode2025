package toddlerTourist;

import battlecode.common.*;


import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;


/**
 * RobotPlayer is the class that describes your main robot strategy.
 * The run() method inside this class is like your main function: this is what we'll call once your robot
 * is created!
 */
public class RobotPlayer {
    static final Random rng = new Random(6147);

    /** Array containing all the possible movement directions. */
    static final Direction[] directions = {
            Direction.NORTH,
            Direction.NORTHEAST,
            Direction.EAST,
            Direction.SOUTHEAST,
            Direction.SOUTH,
            Direction.SOUTHWEST,
            Direction.WEST,
            Direction.NORTHWEST,
    };

    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * It is like the main function for your robot. If this method returns, the robot dies!
     *
     * @param rc  The RobotController object. You use it to perform actions from this robot, and to get
     *            information on its current status. Essentially your portal to interacting with the world.
     **/
    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {
        Set<MapLocation> coords = new HashSet<>();
        int symm = -1;
        if(rc.getType().isTowerType() && rc.getRoundNum() == 1) {
            MapLocation nr = Symmetry.getStartingTowersCoord(rc);
            coords.add(nr);
        }
        // example when the tower receives a message with the other tower's coords we can start doing the symmetry computations
        coords.add(new MapLocation(0,0));
        if(coords.size() == 2) {
            //determine symmetry type
            MapLocation[] coordsArray = coords.toArray(new MapLocation[0]);
            MapLocation first = coordsArray[0];
            MapLocation second = coordsArray[1];
            symm = Symmetry.getSymmetryType(rc, coordsArray);
        }
        MapLocation[] ends = Symmetry.getLineEnds(rc, symm);
        

        Globals.init(rc); // Initialize globals for shared usage
        if (rc.getType().isTowerType()){
            while(true){
                try {
                    TowerLogic.run(rc);
                }
                catch (GameActionException e) {
                    // Oh no! It looks like we did something illegal in the Battlecode world. You should
                    // handle GameActionExceptions judiciously, in case unexpected events occur in the game
                    // world. Remember, uncaught exceptions cause your robot to explode!
                    // System.out.println("GameActionException");
                    e.printStackTrace();

                } catch (Exception e) {
                    // Oh no! It looks like our code tried to do something bad. This isn't a
                    // GameActionException, so it's more likely to be a bug in our code.
                    // System.out.println("Exception");
                    e.printStackTrace();

                } finally {
                    // Signify we've done everything we want to do, thereby ending our turn.
                    // This will make our code wait until the next turn, and then perform this loop again.
                    Clock.yield();
                }
            }
        }

        Robot rb = new Robot(rc);
        switch (rc.getType()){
            case SOLDIER: rb = new Soldier(rc); break;
            case MOPPER: rb = new Mopper(rc); break;
            case SPLASHER: rb = new Soldier(rc); break;
        }

        while(true){
            try {
                // if (rc.getRoundNum() == 2){
                //     runSoldier(rc);
                // } else {
                //     rb.beginTurn();
                //     rb.runTurn();
                //     rb.endTurn();
                // }
                // runSoldier(rc);
                rb.beginTurn();
                rb.runTurn();
                rb.endTurn();
            }
            catch (GameActionException e) {
                // Oh no! It looks like we did something illegal in the Battlecode world. You should
                // handle GameActionExceptions judiciously, in case unexpected events occur in the game
                // world. Remember, uncaught exceptions cause your robot to explode!
                // System.out.println("GameActionException");
                e.printStackTrace();

            } catch (Exception e) {
                // Oh no! It looks like our code tried to do something bad. This isn't a
                // GameActionException, so it's more likely to be a bug in our code.
                // System.out.println("Exception");
                e.printStackTrace();

            } finally {
                // Signify we've done everything we want to do, thereby ending our turn.
                // This will make our code wait until the next turn, and then perform this loop again.
                Clock.yield();
            }
        }
    }

    /**
     * Run a single turn for towers.
     * This code is wrapped inside the infinite loop in run(), so it is called once per turn.
     */
    public static void runTower(RobotController rc) throws GameActionException{
        Direction[] directions = Direction.allDirections();
        MapLocation myLocation = rc.getLocation();
        int soldiersSpawned = 0;

        for (Direction dir : directions) {
            if (soldiersSpawned >= 4) {
                break;
            }
            MapLocation spawnLocation = myLocation.add(dir);
            if (rc.canBuildRobot(UnitType.SOLDIER, spawnLocation)) {
                rc.buildRobot(UnitType.SOLDIER, spawnLocation);
                soldiersSpawned++;
            }
        }
        
        if (soldiersSpawned == 4) {
            MapLocation spawnLocation = myLocation.add(Direction.CENTER);
            if (rc.canBuildRobot(UnitType.MOPPER, spawnLocation)) {
                rc.buildRobot(UnitType.MOPPER, spawnLocation);
            }
        }

        // generate a random Unit
        if (soldiersSpawned > 4) {
            if (rng.nextInt(100) < 50) {
                if (rc.canBuildRobot(UnitType.SOLDIER, myLocation.add(Direction.CENTER))) {
                    rc.buildRobot(UnitType.SOLDIER, myLocation.add(Direction.CENTER));
                }
            } else {
                if (rc.canBuildRobot(UnitType.MOPPER, myLocation.add(Direction.CENTER))) {
                    rc.buildRobot(UnitType.MOPPER, myLocation.add(Direction.CENTER));
                }
            }
        }


    }

    // public static void runSoldier(RobotController rc) throws GameActionException{
    //     Team enemy = rc.getTeam().opponent();
        
    //     MapLocation myLocation = rc.getLocation();
    //     RobotInfo[] nearbyRobots = rc.senseNearbyRobots(-1, enemy);
        
    //     MapLocation closestTower = null;
    //     int closestDistance = Integer.MAX_VALUE;

    //     for (RobotInfo robot : nearbyRobots) {
    //         if (robot.getType().isTowerType()) {
    //             int distance = myLocation.distanceSquaredTo(robot.getLocation());
    //             if (distance < closestDistance) {
    //                 closestDistance = distance;
    //                 closestTower = robot.getLocation();
    //             }
    //         }
    //     }

    //     if (closestTower != null) {
    //         Direction dirToEnemy = myLocation.directionTo(closestTower);
    //         rc.setIndicatorString("Direction: " + dirToEnemy);
    //         System.out.println("Direction: " + dirToEnemy);
    //         // Move towards the enemy tower
    //         if (rc.canMove(dirToEnemy)) {
    //             rc.move(dirToEnemy);
    //         }

    //         // Attack the enemy tower if in range
    //         if (rc.canAttack(closestTower)) {
    //             rc.attack(closestTower);
    //         }
    //     }
    // }
}