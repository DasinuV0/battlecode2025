package Navigation;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import static Util.Globals.*;
import java.util.HashSet;


public class Bug2 {
    static MapLocation prevDest = new MapLocation(-1, -1);
    static HashSet<MapLocation> line = null, visited = new HashSet<MapLocation>();;
    static boolean isTracing = false;
    static Direction wallDirection = null;
    static int obstacleDist = 0;

    public static void move(MapLocation dest) throws GameActionException{
        MapLocation curPos = rc.getLocation();
        if (curPos.equals(dest)) return;
        if (!dest.equals(prevDest) || visited.contains(curPos)){
            prevDest = dest;
            line = createLine(dest, rc.getLocation());
            reset();
        }

        for (MapLocation l: line){
            rc.setIndicatorDot(l, 255,0,0);
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
            }
            return;
        }

        // on the line && is closer to dest -> stop tracing
        if (line.contains(curPos) && curPos.distanceSquaredTo(dest) < obstacleDist) {
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

    private static void reset(){
        isTracing = false;
        wallDirection = null;
        obstacleDist = 0;
        visited.clear();
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

