package Iteration3;


import battlecode.common.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.ArrayList;


public class Robot {
    class OptCode {
        static final int NEEDPAINT = 0;
        static final int RUINFOUND = 1;
        static final int DAMAGEDPATTERN = 2;
        static final int PUTSTATE = 3;
        static final int MOVETOSPECIFICLOC = 4;
        static final int ENEMYTOWERFOUND = 5;
        
        static final int EXPLORE = 6;
        static final int ENGAGEMENT = 7;
    }   
    /**
     * Core robot class. Contains all necessary info for other classes and all high level instructions.
     */
    static final int LOWPAINTTRESHHOLD = 20;
    static final int PAINTTOTAKE = -20;
    static final int PAINTTOGIVE = 20;
    static final Random rng = new Random(6147);

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

    static RobotController rc;

    static Set<MapLocation> towersPos = new  LinkedHashSet<>();
    
    static boolean lowPaintFlag;
    static boolean friendMopperFound;
    static Set<MapLocation> ruinWithPatternDamaged;
    static boolean enemyTowerFound;
    
    static MapLocation emptyTile;
    Set<MapInfo> ruinsFound;
   
    static boolean stayPut;
    static MapLocation moveToTarget;


        
    public Robot(RobotController _rc) throws GameActionException {
        this.rc = _rc;
        stayPut = true;
    }

    //instructions run at the beginning of each turn
    void beginTurn() throws GameActionException {

    }

    //Instructions at the end of each turn
    void endTurn() throws GameActionException {
       
    }

    //Core turn method
    void runTurn() throws GameActionException {

    }

    void listenMessage(){
        Message[] messages = rc.readMessages(rc.getRoundNum());
        if (messages.length == 0)
            return;
        moveToTarget = new MapLocation(-1,-1);
        stayPut = false;

        for (Message m : messages) {
            int command = m.getBytes() >> 12;
            if (command == 1)
                stayPut = true;
            else if (command == 3){
                int y = m.getBytes() & 63;
                int x = (m.getBytes() >> 6) & 63;
                moveToTarget = new MapLocation(x,y);
            }
        }
    }

    

    int calculatePaintPercentage(RobotInfo robot) {
        return (int)(((double)robot.paintAmount / robot.type.paintCapacity) * 100);
    }

    int encodeMessage(int command, MapLocation targetLoc){
        int x = targetLoc != null ? targetLoc.x : 0; // Default x-coordinate if no target
        int y = targetLoc != null ? targetLoc.y : 0; // Default y-coordinate if no target

        // Encode coordinates and command into messageContent
        int messageContent = (x << 6) | y | (command << 12);
        return messageContent;
    }
}
