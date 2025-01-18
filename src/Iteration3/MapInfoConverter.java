// package Iteration3;

// import battlecode.common.MapInfo;
// import battlecode.common.MapLocation;
// import battlecode.common.PaintType;


// public class MapInfoConverter{
//     // 21 bit used out of 32: ________ ___mmmpp pRWPyyyy yyxxxxxx
//     // m = mark value, p = paint value, R = bool for hasRuin()
//     // W = bool for isWall(), P = bool for isPassable()
//     // x = x-coordinate, y = y-coordinate

//     // ~ 55 byte code
//     public static int encode(MapInfo mapInfo) {
//         int mask = 0;
//         mask += mapInfo.getMapLocation().x;
//         mask += (mapInfo.getMapLocation().y << 6);

//         if(mapInfo.isPassable()) mask += (1<<12);
//         if(mapInfo.isWall()) mask += (1<<13);
//         if(mapInfo.hasRuin()) mask += (1<<14);

//         mask += (mapInfo.getPaint().ordinal() << 15);
//         mask += (mapInfo.getMark().ordinal() << 18);

//         return mask;
//     }

    // ~ 62 byte code
    // public static MapInfo decode(int mask) {
    //     int submask = (1 << 6) - 1;
    //     int x = mask & submask;
    //     int y = (mask >> 6) & submask;
    //     boolean isPassable = (mask & (1 << 12)) != 0;
    //     boolean isWall = (mask & (1 << 13)) != 0;
    //     boolean hasRuin = (mask & (1 << 14)) != 0;
    //     PaintType paint = PaintType.values()[(mask >> 15) & ((1 << 3) - 1)];
    //     PaintType mark = PaintType.values()[(mask >> 18) & ((1 << 3) - 1)];
    //     return new MapInfo(new MapLocation(x, y), isPassable, isWall, paint, mark, hasRuin);
    // }
// }