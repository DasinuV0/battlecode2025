package Iteration2;

import battlecode.common.*;

public class symmetry {
    public static MapLocation getStartingTowersCoord(RobotController rc) {
        return rc.getLocation();
    }
    public static int getSymmetryType(RobotController rc, MapLocation[] coordsArray) {
        MapLocation first = coordsArray[0];
        MapLocation second = coordsArray[1];

        int type = -1;
        if(first.x == second.x) {
            int mini = Math.min(first.y, second.y);
            int maxi = Math.max(first.y, second.y);
            if(rc.getMapHeight() - maxi == mini)
                type = 0; //vertical
        }
        else if(first.y == second.y) {
            int mini = Math.min(first.x, second.x);
            int maxi = Math.max(first.x, second.x);
            if(rc.getMapWidth() - maxi == mini)
                type = 1; //horizontal
        }
        else
            type = 2; //rotational
        return type;
    }
    public static MapLocation[] getLineEnds(RobotController rc, int symmetryType) {
        MapLocation[] points = new MapLocation[2];
        if(symmetryType == 0) { //vertical line
            points[0] = new MapLocation(rc.getMapWidth() / 2, rc.getMapHeight() - 1);
            points[1] = new MapLocation(rc.getMapWidth() / 2, 0);
        }
        if(symmetryType == 1) { //horizontal line
            points[0] = new MapLocation(0, rc.getMapHeight() / 2);
            points[1] = new MapLocation(rc.getMapWidth() - 1, rc.getMapHeight() / 2);
        }
        else { //diagonal line
            points[0] = new MapLocation(0, 0);
            points[1] = new MapLocation(rc.getMapWidth() - 1, rc.getMapHeight() - 1);
        }
        return points;
    }
    public static MapLocation getEnemyStartingTowerCoord(RobotController rc, MapLocation coord) {
        String coordKey = coord.x + "," + coord.y;
        switch(coordKey) {
            case "9,17":
                return new MapLocation(5, 2);
            case "5,2":
                return new MapLocation(9, 17);
            case "14,17":
                return new MapLocation(10, 2);
            case "10,2":
                return new MapLocation(14, 17);
            case "7,27":
                return new MapLocation(27, 27);
            case "27,27":
                return new MapLocation(7, 27);
            case "7,7":
                return new MapLocation(27, 7);
            case "27,7":
                return new MapLocation(7, 7);
            case "8,17":
                return new MapLocation(41, 17);
            case "41,17":
                return new MapLocation(8, 17);
            case "12,12":
                return new MapLocation(37, 12);
            case "37,12":
                return new MapLocation(12, 12);
            case "25,54":
                return new MapLocation(25, 4);
            case "25,4":
                return new MapLocation(25, 54);
            case "31,54":
                return new MapLocation(31, 4);
            case "31,4":
                return new MapLocation(31, 54);
            default:
                return null;
        }
    }
    public static MapLocation[] getLines(RobotController rc, int symmetryType) {
        MapLocation[] symm = getLineEnds(rc, symmetryType);
        MapLocation[] points = new MapLocation[6];
        if(symmetryType == 0) {
            points[0] = new MapLocation(rc.getMapWidth() / 4, rc.getMapHeight() - 1);
            points[1] = new MapLocation(rc.getMapWidth() / 4, 0);
            points[2] = symm[0];
            points[3] = symm[1];
            points[4] = new MapLocation((int)(rc.getMapWidth() * 1.5), rc.getMapHeight() - 1);
            points[5] = new MapLocation((int)(rc.getMapWidth() * 1.5), 0);
        }
        else if(symmetryType == 1) {
            points[0] = new MapLocation(0, rc.getMapHeight() / 4);
            points[1] = new MapLocation(rc.getMapWidth() - 1, rc.getMapHeight() / 4);
            points[2] = symm[0];
            points[3] = symm[1];
            points[4] = new MapLocation(0, (int)(rc.getMapHeight() * 1.5));
            points[5] = new MapLocation(rc.getMapWidth() - 1, (int)(rc.getMapHeight() * 1.5));
        }
        else {
            points[0] = new MapLocation(0, rc.getMapHeight() / 2);
            points[1] = new MapLocation(rc.getMapWidth() / 2, 0);
            points[2] = symm[0];
            points[3] = symm[1];
            points[4] = new MapLocation(rc.getMapWidth() / 2, rc.getMapHeight() - 1);
            points[5] = new MapLocation(rc.getMapWidth() - 1, rc.getMapHeight() / 2);
        }
        return points;
    }
    public static int getZone(RobotController rc, int symmetryType) {
        MapLocation loc = rc.getLocation();
        if(symmetryType == 0) {
            if(loc.x <= rc.getMapWidth() / 4)
                return 0;
            if(loc.x <= rc.getMapWidth() / 2)
                return 1;
            if(loc.x <= (int)(rc.getMapWidth() * 1.5))
                return 2;
            return 3;
        }
        if(symmetryType == 1) {
            if(loc.y <= rc.getMapHeight() / 4)
                return 0;
            if(loc.y <= rc.getMapHeight() / 2)
                return 1;
            if(loc.y <= (int)(rc.getMapHeight() * 1.5))
                return 2;
            return 3;
        }
        if(loc.x < rc.getMapWidth() / 2 && loc.y < rc.getMapHeight() / 2)
            return 0;
        int maxi = Math.max(rc.getMapWidth(), rc.getMapHeight());
        if(loc.x + loc.y <= maxi)
            return 1;
        if(loc.x <= rc.getMapWidth() / 2 || loc.y <= rc.getMapHeight() / 2)
            return 2;
        return 3;
    }
    public static int getRegion(RobotController rc, MapLocation coords) {
        int half_width = rc.getMapWidth() / 2, half_height = rc.getMapHeight() / 2;
        if(coords.x <= half_width && coords.y >= half_height)
            return 0;
        if(coords.x >= half_width && coords.y >= half_height)
            return 1;
        if(coords.x <= half_width && coords.y <= half_height)
            return 2;
        return 3;
    }
    public static int[] getAdjacentRegions(RobotController rc, MapLocation coords) {
        int regionType = getRegion(rc, coords);
        if(regionType == 0)
            return new int[]{1, 2};
        if(regionType == 1)
            return new int[]{0, 3};
        if(regionType == 2)
            return new int[]{0, 3};
        return new int[]{1, 2};
    }
}
