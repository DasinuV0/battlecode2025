package Iteration2;

import battlecode.common.*;

public class RobotInfoConverter{
    // 31 bits: _ppppppp Thhhhhhh UUUUyyyy yyxxxxxx
    // paint = p, T = team, health = h, U = unit type, y = y-coordinate, x = x-coordinate
    // paint = p and health = h are approximate, since it won't fit otherwise. ID is not sent.

    // ~ 69 byte code

    public static int encode(RobotInfo robotInfo){
        int mask = 0;
        mask += robotInfo.getLocation().x;
        mask += robotInfo.getLocation().y << 6;
        mask += robotInfo.getType().ordinal() << 12;
        int healthPercent = (100 * robotInfo.getHealth()) / robotInfo.getType().health;
        mask += healthPercent << 16;
        mask += robotInfo.getTeam().ordinal() << 23;
        int paintPercent = (100 * robotInfo.getPaintAmount()) / robotInfo.getType().paintCapacity;
        mask += paintPercent << 24;

        return mask;
    }

    // ~ 71 byte code

    public static RobotInfo decode(int mask){
        int locationMask = (1 << 6) - 1;
        int x = mask & locationMask;
        int y = (mask >> 6) & locationMask;
        UnitType unitType = UnitType.values()[(mask >> 12) & ((1 << 4) - 1)];
        int healthPercent = (mask >> 16) & ((1 << 7) - 1);
        Team team = Team.values()[(mask >> 23) & 1];
        int paintPercent = (mask >> 24) & ((1 << 7) - 1);
        return new RobotInfo(0, team, unitType, (int)Math.ceil (((unitType.health / 100.0) * healthPercent)),
                new MapLocation(x, y), (int)Math.ceil (((unitType.paintCapacity / 100.0) * paintPercent)));
    }

}