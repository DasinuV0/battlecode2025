package Navigation;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import static Util.Globals.*;

import java.util.HashSet;

public class Bug2 {
    static MapLocation prevDest;
    static HashSet<MapLocation> line = null;
    static boolean isTracing = false;
    static Direction wallDirection = null;
    static int obstacleDist = 0;

    public static void move(MapLocation dest) throws GameActionException{
        if (!dest.equals(prevDest)){
            prevDest = dest;
            line = createLine(dest, rc.getLocation());
        }

        if (!isTracing){
            Direction dir = rc.getLocation().directionTo(dest);
            if (rc.canMove(dir)) rc.move(dir);
            else {
                isTracing = true;
                wallDirection = dir;
                obstacleDist = rc.getLocation().distanceSquaredTo(dest);
            }
            return;
        }

        // on the line && is closer to dest -> stop tracing
        if (line.contains(rc.getLocation()) && rc.getLocation().distanceSquaredTo(dest) < obstacleDist) {
            isTracing = false;
            wallDirection = null;
            obstacleDist = 0;
            return;
        }

        if (rc.canMove(wallDirection)){
            rc.move(wallDirection);
            wallDirection = wallDirection.rotateRight();
            wallDirection = wallDirection.rotateRight();
            return;
        }

        // circumnavigate the wall
        for (int i = 0; i < 8; i++){
            wallDirection = wallDirection.rotateLeft();
            if (rc.canMove(wallDirection)){
                rc.move(wallDirection);
                wallDirection = wallDirection.rotateRight();
                wallDirection = wallDirection.rotateRight();
                break;
            }
        }
    }

    private static HashSet<MapLocation> createLine(MapLocation a, MapLocation b){
        HashSet<MapLocation> locs = new HashSet<>();
        int x = a.x, y = a.y;
        int dx = b.x - a.x, dy = b.y - a.y;
        int sx = (int) Math.signum(dx), sy = (int) Math.signum(dy);
        dx = Math.abs(dx);
        dy = Math.abs(dy);
        int d = Math.max(dx, dy);
        int r = d/2;

        if (dx > dy){
            for (int i = 0; i < d; i++){
                locs.add(new MapLocation(x, y));
                x += sx; r += dy;
                if (r >= dx){
                    locs.add(new MapLocation(x, y));
                    y += sy;
                    r -= dx;
                }
            }
        } else {
            for (int i = 0; i < d; i++){
                locs.add(new MapLocation(x, y));
                y += sy; r += dx;
                if (r >= dy){
                    locs.add(new MapLocation(x, y));
                    x += sx;
                    r -= dy;
                }
            }
        }
        locs.add(new MapLocation(x, y));
        return locs;
    }
}
