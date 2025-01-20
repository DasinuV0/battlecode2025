package Navigation;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;

import static Util.Globals.*;
import java.util.HashSet;


public class Bug2 {
    static MapLocation prevDest = new MapLocation(-1, -1);
    static MapLocation lineStart = new MapLocation(-1,-1);
    static MapLocation lineEnd = new MapLocation(-1,-1);
    static HashSet<MapLocation> visited = new HashSet<>();
    static boolean isTracing = false;
    static Direction wallDirection = null;
    static int obstacleDist = 0, dx, dy, sx, sy;
    static double m;

    public static void move(MapLocation dest) throws GameActionException{
        MapLocation curPos = rc.getLocation();
        if (curPos.equals(dest)) return;
        if (!dest.equals(prevDest) || visited.contains(curPos)){
            prevDest = dest;
            setLine(dest, rc.getLocation());
            reset();
        }

        if (!isTracing){
            Direction dir = curPos.directionTo(dest);
            if (rc.canMove(dir)) {
                visited.add(curPos);
                rc.move(dir);
            }
            else {
                isTracing = true;
                wallDirection = dir;
                obstacleDist = curPos.distanceSquaredTo(dest);
                visited.clear();
            }
            return;
        }

        // on the line && is closer to dest -> stop tracing
        if (isOnLine(curPos) && curPos.distanceSquaredTo(dest) < obstacleDist) {
            reset();
            return;
        }

        if (rc.canMove(wallDirection)){
            visited.add(curPos);
            rc.move(wallDirection);
            wallDirection = wallDirection.rotateRight();
            wallDirection = wallDirection.rotateRight();
            return;
        }

        // circumnavigate the wall
        for (int i = 0; i < 8; i++){
            wallDirection = wallDirection.rotateLeft();
            if (rc.canMove(wallDirection)){
                visited.add(curPos);
                rc.move(wallDirection);
                wallDirection = wallDirection.rotateRight();
                wallDirection = wallDirection.rotateRight();
                break;
            }
        }
    }

    private static boolean isOnLine(MapLocation loc){
        int diffx = loc.x - lineStart.x;
        int diffy = loc.y - lineStart.y;
        if (sx != (int) Math.signum(diffx) || Math.abs(diffx) > Math.abs(dx)) return false;
        if (sy != (int) Math.signum(diffy) || Math.abs(diffy) > Math.abs(dy)) return false;

        if (lineStart.x == lineEnd.x) return true;

        int step = Math.abs(diffx);
        double y_float = lineStart.y + Math.abs(m) * step * sy;
        int y = (int) (Math.ceil(y_float)-1);
        return Math.abs(loc.y - y) <= 3;
    }

    private static void reset(){
        isTracing = false;
        wallDirection = null;
        obstacleDist = 0;
        visited.clear();
    }

    private static void setLine(MapLocation a, MapLocation b){
        lineStart = a; lineEnd = b;
        dx = lineEnd.x - lineStart.x;
        dy = lineEnd.y - lineStart.y;
        sx = (int) Math.signum(dx);
        sy = (int) Math.signum(dy);
        if (a.x != b.x) m = (double)(lineEnd.y - lineStart.y) / (lineEnd.x - lineStart.x);
    }
}
