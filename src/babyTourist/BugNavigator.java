package babyTourist;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;

// Bug Nav + marking tiles whenever possible
// Given a target, it will move to the target while simultaneously
// marking tiles that can be marked.
// Markers, which will state which color a space should be repainted to,
// and is visible to all allied robots.
// Markers do not have any other effect on the game.

public class BugNavigator extends Globals {
    private static MapLocation currentTarget;

    private static int minDistanceToTarget;
    private static boolean obstacleOnRight;
    private static MapLocation currentObstacle;
    private static FastSet visitedStates;

    public static void moveTo(MapLocation target) throws GameActionException {
        if (currentTarget == null || !currentTarget.equals(target)) {
            reset();
        }

        boolean hasOptions = false;
        for (int i = adjacentDirections.length; --i >= 0; ) {
            if (canMove(adjacentDirections[i])) {
                hasOptions = true;
                break;
            }
        }

        if (!hasOptions){
            //System.out.println("bot has no options, just return");
            return; // do nothing
        }


        MapLocation myLocation = rc.getLocation();
        // if the robot is closer to the target than before,
        // it resets the state and updates minDistanceToTarget.
        int distanceToTarget = distance1d(myLocation, target);
        if (distanceToTarget < minDistanceToTarget) {
            //System.out.println("A new minDistance is found, resetting to update");
            reset();
            minDistanceToTarget = distanceToTarget;
        }

        // if there is an obstacle and if that obstacle is now passable.
        // when it previously wasn't then reset because now we can potentially
        // consider a better direction to go to.
//        if (currentObstacle != null && rc.canSenseLocation(currentObstacle) && rc.sensePassability(currentObstacle)) {
//            reset();
//        }

        // check if previously visited the same state
        if (!visitedStates.add(getState(target))) {
            //System.out.println("state has been visited before, resetting");
            reset();
        }

        // update new target as long as the robot passes previous checks
        currentTarget = target;

        // no obstacle, see if can move diagonally to target
        if (currentObstacle == null) {
            Direction forward = myLocation.directionTo(target);
            if (canMove(forward)) {
                move(forward);
                return;
            }
            // otherwise, we set initial direction again for followWall function
            setInitialDirection();
        }

        followWall(true); // follow wall if cannot diagonally traverse to target
    }

    public static void reset() {
        currentTarget = null;
        minDistanceToTarget = Integer.MAX_VALUE;
        obstacleOnRight = true;
        currentObstacle = null;
        visitedStates = new FastSet();
    }

    private static void setInitialDirection() throws GameActionException {
        MapLocation myLocation = rc.getLocation();
        // direction to target
        Direction forward = myLocation.directionTo(currentTarget);

        // check in anticlockwise direction
        Direction left = forward.rotateLeft();
        for (int i = 8; --i >= 0; ) {
            MapLocation location = rc.adjacentLocation(left);
            if (rc.onTheMap(location) && rc.sensePassability(location)) {
                break; // position found
            }

            left = left.rotateLeft();
        }

        // check clockwise direction
        Direction right = forward.rotateRight();
        for (int i = 8; --i >= 0; ) {
            MapLocation location = rc.adjacentLocation(right);
            if (rc.onTheMap(location) && rc.sensePassability(location)) {
                break; // position found
            }

            right = right.rotateRight();
        }

        // out of the 2 first positions found, get the closer one from the target
        MapLocation leftLocation = rc.adjacentLocation(left);
        MapLocation rightLocation = rc.adjacentLocation(right);

        int leftDistance = distance1d(leftLocation, currentTarget);
        int rightDistance = distance1d(rightLocation, currentTarget);

        if (leftDistance < rightDistance) {
            obstacleOnRight = true;
        } else if (rightDistance < leftDistance) {
            obstacleOnRight = false;
        } else {
            obstacleOnRight = myLocation.distanceSquaredTo(leftLocation) < myLocation.distanceSquaredTo(rightLocation);
        }

        // change to the direction accordingly
        if (obstacleOnRight) {
            currentObstacle = rc.adjacentLocation(left.rotateRight());
        } else {
            currentObstacle = rc.adjacentLocation(right.rotateLeft());
        }
    }

    // follow the wall function when encountering one
    private static void followWall(boolean canRotate) throws GameActionException {
        Direction direction = rc.getLocation().directionTo(currentObstacle);

        for (int i = 8; --i >= 0; ) {
            direction = obstacleOnRight ? direction.rotateLeft() : direction.rotateRight();
            if (canMove(direction)) {
                //System.out.println("Moving with mark in " + direction);
                move(direction);
                return;
            }

            MapLocation location = rc.adjacentLocation(direction);
            if (canRotate && !rc.onTheMap(location)) {
                //System.out.println("can rotate and rc not on map");
                obstacleOnRight = !obstacleOnRight;
                followWall(false);
                return;
            }

            if (rc.onTheMap(location) && !rc.sensePassability(location)) {
                //System.out.println("rc on the map, but this location is an obstacle");
                currentObstacle = location;
            }
        }
    }


    private static char getState(MapLocation target) {
        MapLocation myLocation = rc.getLocation();
        Direction direction = myLocation.directionTo(currentObstacle != null ? currentObstacle : target);
        int rotation = obstacleOnRight ? 1 : 0;

        // 16 bits: xxxxxxyy yyyydddr
        // x = x-coordinate, y = y-coordinate, d = 8 cardinal directions, r = obstacle direction
        return (char) ((((myLocation.x << 6) | myLocation.y) << 4) | (direction.ordinal() << 1) | rotation);
    }

    // Manhattan distance calculation
    private static int distance1d(MapLocation a, MapLocation b) {
        return Math.max(Math.abs(a.x - b.x), Math.abs(a.y - b.y));
    }

    // can move in this direction and mark this tile
    private static boolean canMove(Direction direction) {
        return rc.canMove(direction);
    }

    // move in this direction and mark this tile
    private static void move(Direction direction) throws GameActionException {
        MapLocation fillLocation = rc.adjacentLocation(direction);

        rc.move(direction);

        Logger.log("bug " + direction);
    }
}