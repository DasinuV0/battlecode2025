package Robot;

import battlecode.common.*;
import Navigation.Bug1.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.ArrayList;


public class Robot {
    class OptCode {
        static final int NEEDPAINT = 0;
        static final int PUTSTATE = 1;
        static final int DAMAGEDPATTERN = 2;
        static final int EXPLORE = 3;
        static final int GOTODEFEND = 4;
        static final int ATTACKTOWER = 5;
        static final int ENEMYTOWERFOUND = 5;
        static final int RUINFOUND = 7;
        
        // static final int MOVETOSPECIFICLOC = 6;
        // static final int ENGAGEMENT = 7;
    }   

    /**
     * Constants for Rush strategy
     */
    static int isSuicideRobot = 0;
    static int isDefenceMopper = 0;
    static int checkedEnemyTower = 0;

    /**
     * Core robot class. Contains all necessary info for other classes and all high level instructions.
     */
    static final int LOWPAINTTRESHHOLD = 20;
    static final int PAINTTOTAKE = -60;
    static final int PAINTTOGIVE = 60;
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

    static Set<MapLocation> paintTowersPos = new  LinkedHashSet<>();
    static Set<MapLocation> moneyTowersPos = new  LinkedHashSet<>();
    static Set<MapLocation> enemyTowersPos = new LinkedHashSet<>();
    
    //general flags
    static boolean lowPaintFlag;
    static boolean friendMopperFound;
    static Set<MapInfo> ruinsFound;
    static MapLocation emptyTile;
    static Set<MapLocation> ruinWithPatternDamaged;
   
   //messages flags (this will updated only when a new message is received)
    static boolean stayPut;
    static boolean exploreMode;
    static MapLocation targetLocation; 
    static boolean healMode;  //specific to mopper
    static boolean removePatterMode; //specific to mopper
    static boolean defendMode; //specific to mopper 
    static boolean attackMode; //specific to soldier 

        
    public Robot(RobotController _rc) throws GameActionException {
        this.rc = _rc;
        stayPut = true;
    }

    //instructions run at the beginning of each turn
    public void beginTurn() throws GameActionException {

    }

    //Instructions at the end of each turn
    public void endTurn() throws GameActionException {
       
    }

    //Core turn method
    public void runTurn() throws GameActionException {

    }

    void resetFlags(){
        lowPaintFlag = false;
        ruinsFound = new HashSet<>();
        emptyTile = new MapLocation(-1,-1);
        friendMopperFound = false;
    }
    
    void updateLowPaintFlag() throws GameActionException{
        //get the robotInfo of rc and then calculate the percetange of the remain paint 
        int remainPaint = calculatePaintPercentage(rc.senseRobotAtLocation(rc.getLocation()));
        //check if it is low paint
        if (remainPaint <= LOWPAINTTRESHHOLD)
            lowPaintFlag = true;
    }

    MapLocation getNearestAllyPaintTower() throws GameActionException{
        MapLocation nearestTower = new MapLocation(-1,-1);

        int minDist = 9999;
        for (Iterator<MapLocation> iterator = paintTowersPos.iterator(); iterator.hasNext();) {
            MapLocation pos =  iterator.next();
            //if pos is within the vision range and tower is destroyed
            if (rc.canSenseLocation(pos) && !towerExists(pos)){
                iterator.remove();
                return new MapLocation(-1,-1);
            }
            int currDist = pos.distanceSquaredTo(rc.getLocation());

            if (minDist > currDist){
                nearestTower = pos;
                minDist = currDist;
            }
        }
     
        return nearestTower;
    }

    MapLocation getNearestAllyMoneyTower() throws GameActionException{
        MapLocation nearestTower = new MapLocation(-1,-1);
        int minDist = 9999;
        for (Iterator<MapLocation> iterator = moneyTowersPos.iterator(); iterator.hasNext();) {
            MapLocation pos =  iterator.next();
            if (rc.canSenseLocation(pos) && !towerExists(pos)){
                iterator.remove();
                return new MapLocation(-1,-1);
            }
            int currDist = pos.distanceSquaredTo(rc.getLocation());

            if (minDist > currDist){
                nearestTower = pos;
                minDist = currDist;
            }
        }
     
        return nearestTower;
    }

    MapLocation getNearestAllyTower() throws GameActionException{
        MapLocation nearestTower = new MapLocation(-1,-1);
        int minDist = 9999;
        for (Iterator<MapLocation> iterator = moneyTowersPos.iterator(); iterator.hasNext();) {
            MapLocation pos =  iterator.next();
            if (rc.canSenseLocation(pos) && !towerExists(pos)){
                iterator.remove();
                return new MapLocation(-1,-1);
            }
            int currDist = pos.distanceSquaredTo(rc.getLocation());

            if (minDist > currDist){

                nearestTower = pos;
                minDist = currDist;
            }
        }

        for (Iterator<MapLocation> iterator = paintTowersPos.iterator(); iterator.hasNext();) {
            MapLocation pos =  iterator.next();
            if (rc.canSenseLocation(pos) && !towerExists(pos)){
                iterator.remove();
                return new MapLocation(-1,-1);
            }
            int currDist = pos.distanceSquaredTo(rc.getLocation());

            if (minDist > currDist){
                nearestTower = pos;
                minDist = currDist;
            }
        }
        return nearestTower;
    }

    boolean towerExists(MapLocation tower) throws GameActionException {
        //if nearestAllyTower is within the vision range && if nearestAllyTower is not destroyed
        if (rc.canSenseRobotAtLocation(tower) == false){
            //.out.println("tower is destroyed at " + tower.x + " " + tower.y);
            return false;
        }

        return true;
    }

    //return true when we moved
    boolean tryToReachTargetLocation() throws GameActionException{
        if (targetLocation.x != (-1)){
            Navigation.Bug1.moveTo(targetLocation);
            if (rc.getLocation().distanceSquaredTo(targetLocation) <= 4 )
                targetLocation = new MapLocation(-1,-1);
            return true;
        }

        return false;
    }

    //override tryToReachTargetLocation, specify the distance to stop 
    boolean tryToReachTargetLocation(int dist) throws GameActionException{
        if (rc.getLocation().distanceSquaredTo(targetLocation) > dist){
            Navigation.Bug1.moveTo(targetLocation);
            //don't remove targetLocation
            // if (rc.getLocation().distanceSquaredTo(targetLocation) <= dist)
                // targetLocation = new MapLocation(-1,-1);
            return true;
        }

        return false;
    }

    UnitType getTowerToBuild() throws GameActionException{
        if (true){//TODO: if i'm in the region 0,1
            if (rc.getNumberTowers() % 2 == 0)
                return UnitType.LEVEL_ONE_MONEY_TOWER;
            else
                return UnitType.LEVEL_ONE_PAINT_TOWER;
        }
        //if i'm in the region 2
        return UnitType.LEVEL_ONE_DEFENSE_TOWER;    
        
    }

    void tryToMarkPattern(MapInfo curRuin) throws GameActionException{
        MapLocation targetLoc = curRuin.getMapLocation();
        Direction dir = rc.getLocation().directionTo(targetLoc);

        // Mark the pattern we need to draw to build a tower here if we haven't already.
        MapLocation shouldBeMarked = curRuin.getMapLocation().subtract(dir);
        UnitType towerToBuild = getTowerToBuild();
        //if ruin is found, but pattern is not marked                    && just double check we can mark the tower pattern
        if (rc.senseMapInfo(shouldBeMarked).getMark() == PaintType.EMPTY && rc.canMarkTowerPattern(towerToBuild, targetLoc)){
            //DONE: which tower should the bot build?
            rc.markTowerPattern(towerToBuild, targetLoc);
        }
    }

    void tryToBuildTower(MapInfo curRuin) throws GameActionException{
        MapLocation targetLoc = curRuin.getMapLocation();
        if (rc.canCompleteTowerPattern(UnitType.LEVEL_ONE_PAINT_TOWER, targetLoc)){
            rc.completeTowerPattern(UnitType.LEVEL_ONE_PAINT_TOWER, targetLoc);
            rc.setTimelineMarker("Tower built", 0, 255, 0);
        }
        if (rc.canCompleteTowerPattern(UnitType.LEVEL_ONE_DEFENSE_TOWER, targetLoc)){
            rc.completeTowerPattern(UnitType.LEVEL_ONE_DEFENSE_TOWER, targetLoc);
            rc.setTimelineMarker("Tower built", 0, 255, 0);
        }
        if (rc.canCompleteTowerPattern(UnitType.LEVEL_ONE_MONEY_TOWER, targetLoc)){
            rc.completeTowerPattern(UnitType.LEVEL_ONE_MONEY_TOWER, targetLoc);
            rc.setTimelineMarker("Tower built", 0, 255, 0);
        }
    }

    void tryToRebuildTower() throws GameActionException{
        for (MapInfo curRuin : ruinsFound)
            tryToBuildTower(curRuin);

        //if none of them is built, go closer to one of them
        // if (ruinsFound.size() > 0){
        //     Iterator<MapInfo> iterator = ruinsFound.iterator();
        //     MapInfo curr =  iterator.next();
        //     Navigation.Bug1.msoveTos(curr.getMapLocation());
        // }
    }

    //default color is primaryColor
    boolean tryToPaintAtLoc(MapLocation loc, PaintType paintType) throws GameActionException{
        if (rc.canSenseLocation(loc) && rc.senseMapInfo(loc).getPaint() == paintType && rc.canAttack(loc)){
            rc.attack(loc);
            return true;
        }
        return false;
    }
    //overloading: call tryToPaintAtLoc(loc, true) to useSecondaryColor (assuming that loc is either neutral or ally tile)
    void tryToPaintAtLoc(MapLocation loc, boolean useSecondaryColor) throws GameActionException{
        if (rc.canSenseLocation(loc) && rc.canAttack(loc))
            rc.attack(loc, useSecondaryColor);
    }

    int calculatePaintPercentage(RobotInfo robot) {
        return (int)(((double)robot.paintAmount / robot.type.paintCapacity) * 100);
    }

    void resetMessageFlag(){
        stayPut = false;
        exploreMode = false;
        targetLocation = new MapLocation(-1,-1);
        healMode = false;  
        removePatterMode = false;
        defendMode = false;
        attackMode = false;
    }

    void listenMessage(){
        Message[] messages = rc.readMessages(rc.getRoundNum());
        //if no message received in this turn, don't change any message flags
        if (messages.length == 0)
            return;

        resetMessageFlag();

        for (Message m : messages) {
            int command = m.getBytes() >> 12;
            //.out.println("command " + command + " received");

            if (command == OptCode.PUTSTATE){
                stayPut = true;
            }
            else if (command == OptCode.EXPLORE){
                int y = m.getBytes() & 63;
                int x = (m.getBytes() >> 6) & 63;
                exploreMode = true;
                targetLocation = new MapLocation(x,y);
            }else if (command == OptCode.DAMAGEDPATTERN){
                int y = m.getBytes() & 63;
                int x = (m.getBytes() >> 6) & 63;
                removePatterMode = true;
                targetLocation = new MapLocation(x,y); 
            }
            else if (command == OptCode.GOTODEFEND){
                int y = m.getBytes() & 63;
                int x = (m.getBytes() >> 6) & 63;
                defendMode = true;
                targetLocation = new MapLocation(x,y);   
            }else if (command == OptCode.ATTACKTOWER){
                int y = m.getBytes() & 63;
                int x = (m.getBytes() >> 6) & 63;
                attackMode = true;
                targetLocation = new MapLocation(x,y);   
            }
        }
    }

    int encodeMessage(int command, MapLocation targetLoc){
        int x = targetLoc != null ? targetLoc.x : 0; // Default x-coordinate if no target
        int y = targetLoc != null ? targetLoc.y : 0; // Default y-coordinate if no target

        // Encode coordinates and command into messageContent
        int messageContent = (x << 6) | y | (command << 12);
        return messageContent;
    }

    boolean isPaintTower(UnitType tower){
        return tower == UnitType.LEVEL_ONE_PAINT_TOWER || tower == UnitType.LEVEL_TWO_PAINT_TOWER || tower == UnitType.LEVEL_THREE_PAINT_TOWER;
    }

    boolean isMoneyTower(UnitType tower){
        return tower == UnitType.LEVEL_ONE_MONEY_TOWER || tower == UnitType.LEVEL_TWO_MONEY_TOWER || tower == UnitType.LEVEL_THREE_MONEY_TOWER;
    }

    boolean isDefendTower(UnitType tower){
        return tower == UnitType.LEVEL_ONE_DEFENSE_TOWER || tower == UnitType.LEVEL_TWO_DEFENSE_TOWER || tower == UnitType.LEVEL_THREE_DEFENSE_TOWER;
    }
}
