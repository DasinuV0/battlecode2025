package v1;

import battlecode.common.*;
import Iteration2.*;

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
            Globals.init(rc); // Initialize globals for shared usage
            if (rc.getType().isTowerType()){
            while(true){
                try {
                    runTower(rc);
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
        // Pick a direction to build in.
        Direction dir = directions[rng.nextInt(directions.length)];
        MapLocation nextLoc = rc.getLocation().add(dir);
        // Pick a random robot type to build.
        int robotType = 0;
        if (robotType == 0 && rc.canBuildRobot(UnitType.SOLDIER, nextLoc)){
            rc.buildRobot(UnitType.SOLDIER, nextLoc);
            // System.out.println("BUILT A SOLDIER");
        }
        else if (robotType == 1 && rc.canBuildRobot(UnitType.MOPPER, nextLoc)){
            rc.buildRobot(UnitType.MOPPER, nextLoc);
            // System.out.println("BUILT A MOPPER");
        }
        else if (robotType == 2 && rc.canBuildRobot(UnitType.SPLASHER, nextLoc)){
            rc.buildRobot(UnitType.SPLASHER, nextLoc);
            // System.out.println("BUILT A SPLASHER");
        }
    }
}
