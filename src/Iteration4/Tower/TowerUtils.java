package Tower;

import battlecode.common.*;
import java.util.*;

public class TowerUtils extends TowerLogic {
    // Static variables for storing tower data
    private static MapLocation currentLocation; // Assuming this is updated somewhere else in your game logic
    private static PriorityQueue<PaintTower> paintTowers = new PriorityQueue<>(Comparator.comparingInt(t -> t.distanceSquaredTo(currentLocation)));

    // Class to represent a paint tower and its distance to the current robot location
    private static class PaintTower {
        MapLocation location;
        int distanceSquared;

        PaintTower(MapLocation location, int distanceSquared) {
            this.location = location;
            this.distanceSquared = distanceSquared;
        }

        // Method to calculate the distance from the robot's location to the tower
        public int distanceSquaredTo(MapLocation other) {
            return this.location.distanceSquaredTo(other);
        }
    }

    // Update the set of paint towers and update the closest one
    public static void updatePaintTowers(MapLocation newTowerLocation) {
        // Calculate the distance from the robot (or tower) to the new paint tower
        int distanceSquared = currentLocation.distanceSquaredTo(newTowerLocation);
        // Add the new tower to the priority queue
        paintTowers.add(new PaintTower(newTowerLocation, distanceSquared));

        // No need to manually sort, PriorityQueue keeps it sorted
    }

    // Get the closest paint tower from the sorted list
    public static MapLocation getClosestPaintTower() {
        if (!paintTowers.isEmpty()) {
            return paintTowers.peek().location;
        }
        return null;  // Return null if no towers are present
    }

    // Method to remove a destroyed paint tower from the list
    public static void removeDestroyedPaintTower(MapLocation destroyedTowerLocation) {
        // Create a temporary list to store the towers, then rebuild the priority queue
        List<PaintTower> newPaintTowers = new ArrayList<>();
        for (PaintTower tower : paintTowers) {
            if (!tower.location.equals(destroyedTowerLocation)) {
                newPaintTowers.add(tower);
            }
        }

        // Rebuild the PriorityQueue
        paintTowers.clear();
        paintTowers.addAll(newPaintTowers);
    }
}
