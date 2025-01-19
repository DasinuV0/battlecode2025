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

        static final int ATTACKSPLASHER = 8;
        static final int DEFENCESPLASHER = 9;
        static final int SENDPAINTZONE = 10;
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
    static final int LOWHEALTHTHRESHOLD = 20;

    /*
     * Game phases
     */
    static final int EARLY_GAME_TURNS = 200;

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

    MapLocation originPos; //this saves the position before lowpaintFlag == true
    MapLocation buildingTower; //this saves the position of the tower that we are currently building

    //general flags
    static boolean lowPaintFlag;
    static boolean friendMopperFound;
    static Set<MapInfo> ruinsFound;
    static MapLocation emptyTile;
    static Set<MapLocation> ruinWithPatternDamaged;
    static boolean isAttackSplasher;
    static boolean isDefenseSplasher;

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

    void updateLowHealthFlag() throws GameActionException{
        //get the robotInfo of rc and then calculate the percetange of the remain paint 
        int remainHealth = calculateHealthPercentage(rc.senseRobotAtLocation(rc.getLocation()));
        //check if it is low paint
        if (remainHealth <= LOWHEALTHTHRESHOLD)
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
            if (rc.getNumberTowers() % 2 == 1)
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
        if (rc.canSenseLocation(shouldBeMarked) && rc.senseMapInfo(shouldBeMarked).getMark() == PaintType.EMPTY && rc.canMarkTowerPattern(towerToBuild, targetLoc)){
            //DONE: which tower should the bot build?
            rc.markTowerPattern(towerToBuild, targetLoc);
        }
    }
    //override
    void tryToMarkPattern(MapLocation targetLoc) throws GameActionException{
        Direction dir = rc.getLocation().directionTo(targetLoc);

        // Mark the pattern we need to draw to build a tower here if we haven't already.
        MapLocation shouldBeMarked = targetLoc.subtract(dir);
        UnitType towerToBuild = getTowerToBuild();
        //if ruin is found, but pattern is not marked                    && just double check we can mark the tower pattern
        if (rc.canSenseLocation(shouldBeMarked) && rc.senseMapInfo(shouldBeMarked).getMark() == PaintType.EMPTY && rc.canMarkTowerPattern(towerToBuild, targetLoc)){
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
    //override  
    void tryToBuildTower(MapLocation targetLoc) throws GameActionException{
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

    int calculateHealthPercentage(RobotInfo robot) {
        double maxHealth = 0;
        if (robot.type == UnitType.SOLDIER) {
            maxHealth = 250;
        } else if (robot.type == UnitType.MOPPER) {
            maxHealth = 50;
        } else if (robot.type == UnitType.SPLASHER) {
            maxHealth = 150;
        }
        return (int)(((double)robot.health / maxHealth) * 100);
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
                buildingTower = new MapLocation(x,y);
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
            }else if (command == OptCode.ATTACKSPLASHER) {
                int y = m.getBytes() & 63;
                int x = (m.getBytes() >> 6) & 63;
                isAttackSplasher = true;
                targetLocation = new MapLocation(x,y);
            } else if (command == OptCode.DEFENCESPLASHER) {
                int y = m.getBytes() & 63;
                int x = (m.getBytes() >> 6) & 63;
                isDefenseSplasher = true;
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

    // 6031 bytecode
    public static MapLocation getEnemyPaintZone(RobotController rc) {
        MapInfo[] surrMapInfos = rc.senseNearbyMapInfos();
        int size = 9, center = 4;
        int[][] visionArea = new int[size][size];
        MapLocation bestLocation = null;

        for (MapInfo mapInfo : surrMapInfos) {
            MapLocation location = mapInfo.getMapLocation();
            int relativeX = location.x - rc.getLocation().x;
            int relativeY = location.y - rc.getLocation().y;
            int matrixX = center + relativeX;
            int matrixY = center + relativeY;

            if (mapInfo.getPaint() == PaintType.ENEMY_PRIMARY || mapInfo.getPaint() == PaintType.ENEMY_SECONDARY)
                visionArea[matrixY][matrixX] = 1;
        }

        int maxOverlap = 0;
        int overlapCount;

        overlapCount = visionArea[0][0] + visionArea[0][1] + visionArea[0][2] + visionArea[1][0] + visionArea[1][1] + visionArea[1][2] + visionArea[2][0] + visionArea[2][1] + visionArea[2][2];
        if (overlapCount > maxOverlap) {
            maxOverlap = overlapCount;
            bestLocation = new MapLocation(rc.getLocation().x - 3, rc.getLocation().y - 3);
        }

        overlapCount = visionArea[0][1] + visionArea[0][2] + visionArea[0][3] + visionArea[1][1] + visionArea[1][2] + visionArea[1][3] + visionArea[2][1] + visionArea[2][2] + visionArea[2][3];
        if (overlapCount > maxOverlap) {
            maxOverlap = overlapCount;
            bestLocation = new MapLocation(rc.getLocation().x - 2, rc.getLocation().y - 3);
        }

        overlapCount = visionArea[0][2] + visionArea[0][3] + visionArea[0][4] + visionArea[1][2] + visionArea[1][3] + visionArea[1][4] + visionArea[2][2] + visionArea[2][3] + visionArea[2][4];
        if (overlapCount > maxOverlap) {
            maxOverlap = overlapCount;
            bestLocation = new MapLocation(rc.getLocation().x - 1, rc.getLocation().y - 3);
        }

        overlapCount = visionArea[0][3] + visionArea[0][4] + visionArea[0][5] + visionArea[1][3] + visionArea[1][4] + visionArea[1][5] + visionArea[2][3] + visionArea[2][4] + visionArea[2][5];
        if (overlapCount > maxOverlap) {
            maxOverlap = overlapCount;
            bestLocation = new MapLocation(rc.getLocation().x, rc.getLocation().y - 3);
        }

        overlapCount = visionArea[0][4] + visionArea[0][5] + visionArea[0][6] + visionArea[1][4] + visionArea[1][5] + visionArea[1][6] + visionArea[2][4] + visionArea[2][5] + visionArea[2][6];
        if (overlapCount > maxOverlap) {
            maxOverlap = overlapCount;
            bestLocation = new MapLocation(rc.getLocation().x + 1, rc.getLocation().y - 3);
        }

        overlapCount = visionArea[0][5] + visionArea[0][6] + visionArea[0][7] + visionArea[1][5] + visionArea[1][6] + visionArea[1][7] + visionArea[2][5] + visionArea[2][6] + visionArea[2][7];
        if (overlapCount > maxOverlap) {
            maxOverlap = overlapCount;
            bestLocation = new MapLocation(rc.getLocation().x + 2, rc.getLocation().y - 3);
        }

        overlapCount = visionArea[0][6] + visionArea[0][7] + visionArea[0][8] + visionArea[1][6] + visionArea[1][7] + visionArea[1][8] + visionArea[2][6] + visionArea[2][7] + visionArea[2][8];
        if (overlapCount > maxOverlap) {
            maxOverlap = overlapCount;
            bestLocation = new MapLocation(rc.getLocation().x + 3, rc.getLocation().y - 3);
        }

        overlapCount = visionArea[1][0] + visionArea[1][1] + visionArea[1][2] + visionArea[2][0] + visionArea[2][1] + visionArea[2][2] + visionArea[3][0] + visionArea[3][1] + visionArea[3][2];
        if (overlapCount > maxOverlap) {
            maxOverlap = overlapCount;
            bestLocation = new MapLocation(rc.getLocation().x - 3, rc.getLocation().y - 2);
        }

        overlapCount = visionArea[1][1] + visionArea[1][2] + visionArea[1][3] + visionArea[2][1] + visionArea[2][2] + visionArea[2][3] + visionArea[3][1] + visionArea[3][2] + visionArea[3][3];
        if (overlapCount > maxOverlap) {
            maxOverlap = overlapCount;
            bestLocation = new MapLocation(rc.getLocation().x - 2, rc.getLocation().y - 2);
        }

        overlapCount = visionArea[1][2] + visionArea[1][3] + visionArea[1][4] + visionArea[2][2] + visionArea[2][3] + visionArea[2][4] + visionArea[3][2] + visionArea[3][3] + visionArea[3][4];
        if (overlapCount > maxOverlap) {
            maxOverlap = overlapCount;
            bestLocation = new MapLocation(rc.getLocation().x - 1, rc.getLocation().y - 2);
        }

        overlapCount = visionArea[1][3] + visionArea[1][4] + visionArea[1][5] + visionArea[2][3] + visionArea[2][4] + visionArea[2][5] + visionArea[3][3] + visionArea[3][4] + visionArea[3][5];
        if (overlapCount > maxOverlap) {
            maxOverlap = overlapCount;
            bestLocation = new MapLocation(rc.getLocation().x, rc.getLocation().y - 2);
        }

        overlapCount = visionArea[1][4] + visionArea[1][5] + visionArea[1][6] + visionArea[2][4] + visionArea[2][5] + visionArea[2][6] + visionArea[3][4] + visionArea[3][5] + visionArea[3][6];
        if (overlapCount > maxOverlap) {
            maxOverlap = overlapCount;
            bestLocation = new MapLocation(rc.getLocation().x + 1, rc.getLocation().y - 2);
        }

        overlapCount = visionArea[1][5] + visionArea[1][6] + visionArea[1][7] + visionArea[2][5] + visionArea[2][6] + visionArea[2][7] + visionArea[3][5] + visionArea[3][6] + visionArea[3][7];
        if (overlapCount > maxOverlap) {
            maxOverlap = overlapCount;
            bestLocation = new MapLocation(rc.getLocation().x + 2, rc.getLocation().y - 2);
        }

        overlapCount = visionArea[1][6] + visionArea[1][7] + visionArea[1][8] + visionArea[2][6] + visionArea[2][7] + visionArea[2][8] + visionArea[3][6] + visionArea[3][7] + visionArea[3][8];
        if (overlapCount > maxOverlap) {
            maxOverlap = overlapCount;
            bestLocation = new MapLocation(rc.getLocation().x + 3, rc.getLocation().y - 2);
        }

        overlapCount = visionArea[2][0] + visionArea[2][1] + visionArea[2][2] + visionArea[3][0] + visionArea[3][1] + visionArea[3][2] + visionArea[4][0] + visionArea[4][1] + visionArea[4][2];
        if (overlapCount > maxOverlap) {
            maxOverlap = overlapCount;
            bestLocation = new MapLocation(rc.getLocation().x - 3, rc.getLocation().y - 1);
        }

        overlapCount = visionArea[2][1] + visionArea[2][2] + visionArea[2][3] + visionArea[3][1] + visionArea[3][2] + visionArea[3][3] + visionArea[4][1] + visionArea[4][2] + visionArea[4][3];
        if (overlapCount > maxOverlap) {
            maxOverlap = overlapCount;
            bestLocation = new MapLocation(rc.getLocation().x - 2, rc.getLocation().y - 1);
        }

        overlapCount = visionArea[2][2] + visionArea[2][3] + visionArea[2][4] + visionArea[3][2] + visionArea[3][3] + visionArea[3][4] + visionArea[4][2] + visionArea[4][3] + visionArea[4][4];
        if (overlapCount > maxOverlap) {
            maxOverlap = overlapCount;
            bestLocation = new MapLocation(rc.getLocation().x - 1, rc.getLocation().y - 1);
        }

        overlapCount = visionArea[2][3] + visionArea[2][4] + visionArea[2][5] + visionArea[3][3] + visionArea[3][4] + visionArea[3][5] + visionArea[4][3] + visionArea[4][4] + visionArea[4][5];
        if (overlapCount > maxOverlap) {
            maxOverlap = overlapCount;
            bestLocation = new MapLocation(rc.getLocation().x, rc.getLocation().y - 1);
        }

        overlapCount = visionArea[2][4] + visionArea[2][5] + visionArea[2][6] + visionArea[3][4] + visionArea[3][5] + visionArea[3][6] + visionArea[4][4] + visionArea[4][5] + visionArea[4][6];
        if (overlapCount > maxOverlap) {
            maxOverlap = overlapCount;
            bestLocation = new MapLocation(rc.getLocation().x + 1, rc.getLocation().y - 1);
        }

        overlapCount = visionArea[2][5] + visionArea[2][6] + visionArea[2][7] + visionArea[3][5] + visionArea[3][6] + visionArea[3][7] + visionArea[4][5] + visionArea[4][6] + visionArea[4][7];
        if (overlapCount > maxOverlap) {
            maxOverlap = overlapCount;
            bestLocation = new MapLocation(rc.getLocation().x + 2, rc.getLocation().y - 1);
        }

        overlapCount = visionArea[2][6] + visionArea[2][7] + visionArea[2][8] + visionArea[3][6] + visionArea[3][7] + visionArea[3][8] + visionArea[4][6] + visionArea[4][7] + visionArea[4][8];
        if (overlapCount > maxOverlap) {
            maxOverlap = overlapCount;
            bestLocation = new MapLocation(rc.getLocation().x + 3, rc.getLocation().y - 1);
        }

        overlapCount = visionArea[3][0] + visionArea[3][1] + visionArea[3][2] + visionArea[4][0] + visionArea[4][1] + visionArea[4][2] + visionArea[5][0] + visionArea[5][1] + visionArea[5][2];
        if (overlapCount > maxOverlap) {
            maxOverlap = overlapCount;
            bestLocation = new MapLocation(rc.getLocation().x - 3, rc.getLocation().y);
        }

        overlapCount = visionArea[3][1] + visionArea[3][2] + visionArea[3][3] + visionArea[4][1] + visionArea[4][2] + visionArea[4][3] + visionArea[5][1] + visionArea[5][2] + visionArea[5][3];
        if (overlapCount > maxOverlap) {
            maxOverlap = overlapCount;
            bestLocation = new MapLocation(rc.getLocation().x - 2, rc.getLocation().y);
        }

        overlapCount = visionArea[3][2] + visionArea[3][3] + visionArea[3][4] + visionArea[4][2] + visionArea[4][3] + visionArea[4][4] + visionArea[5][2] + visionArea[5][3] + visionArea[5][4];
        if (overlapCount > maxOverlap) {
            maxOverlap = overlapCount;
            bestLocation = new MapLocation(rc.getLocation().x - 1, rc.getLocation().y);
        }

        overlapCount = visionArea[3][3] + visionArea[3][4] + visionArea[3][5] + visionArea[4][3] + visionArea[4][4] + visionArea[4][5] + visionArea[5][3] + visionArea[5][4] + visionArea[5][5];
        if (overlapCount > maxOverlap) {
            maxOverlap = overlapCount;
            bestLocation = new MapLocation(rc.getLocation().x, rc.getLocation().y);
        }

        overlapCount = visionArea[3][4] + visionArea[3][5] + visionArea[3][6] + visionArea[4][4] + visionArea[4][5] + visionArea[4][6] + visionArea[5][4] + visionArea[5][5] + visionArea[5][6];
        if (overlapCount > maxOverlap) {
            maxOverlap = overlapCount;
            bestLocation = new MapLocation(rc.getLocation().x + 1, rc.getLocation().y);
        }

        overlapCount = visionArea[3][5] + visionArea[3][6] + visionArea[3][7] + visionArea[4][5] + visionArea[4][6] + visionArea[4][7] + visionArea[5][5] + visionArea[5][6] + visionArea[5][7];
        if (overlapCount > maxOverlap) {
            maxOverlap = overlapCount;
            bestLocation = new MapLocation(rc.getLocation().x + 2, rc.getLocation().y);
        }

        overlapCount = visionArea[3][6] + visionArea[3][7] + visionArea[3][8] + visionArea[4][6] + visionArea[4][7] + visionArea[4][8] + visionArea[5][6] + visionArea[5][7] + visionArea[5][8];
        if (overlapCount > maxOverlap) {
            maxOverlap = overlapCount;
            bestLocation = new MapLocation(rc.getLocation().x + 3, rc.getLocation().y);
        }

        overlapCount = visionArea[4][0] + visionArea[4][1] + visionArea[4][2] + visionArea[5][0] + visionArea[5][1] + visionArea[5][2] + visionArea[6][0] + visionArea[6][1] + visionArea[6][2];
        if (overlapCount > maxOverlap) {
            maxOverlap = overlapCount;
            bestLocation = new MapLocation(rc.getLocation().x - 3, rc.getLocation().y + 1);
        }

        overlapCount = visionArea[4][1] + visionArea[4][2] + visionArea[4][3] + visionArea[5][1] + visionArea[5][2] + visionArea[5][3] + visionArea[6][1] + visionArea[6][2] + visionArea[6][3];
        if (overlapCount > maxOverlap) {
            maxOverlap = overlapCount;
            bestLocation = new MapLocation(rc.getLocation().x - 2, rc.getLocation().y + 1);
        }

        overlapCount = visionArea[4][2] + visionArea[4][3] + visionArea[4][4] + visionArea[5][2] + visionArea[5][3] + visionArea[5][4] + visionArea[6][2] + visionArea[6][3] + visionArea[6][4];
        if (overlapCount > maxOverlap) {
            maxOverlap = overlapCount;
            bestLocation = new MapLocation(rc.getLocation().x - 1, rc.getLocation().y + 1);
        }

        overlapCount = visionArea[4][3] + visionArea[4][4] + visionArea[4][5] + visionArea[5][3] + visionArea[5][4] + visionArea[5][5] + visionArea[6][3] + visionArea[6][4] + visionArea[6][5];
        if (overlapCount > maxOverlap) {
            maxOverlap = overlapCount;
            bestLocation = new MapLocation(rc.getLocation().x, rc.getLocation().y + 1);
        }

        overlapCount = visionArea[4][4] + visionArea[4][5] + visionArea[4][6] + visionArea[5][4] + visionArea[5][5] + visionArea[5][6] + visionArea[6][4] + visionArea[6][5] + visionArea[6][6];
        if (overlapCount > maxOverlap) {
            maxOverlap = overlapCount;
            bestLocation = new MapLocation(rc.getLocation().x + 1, rc.getLocation().y + 1);
        }

        overlapCount = visionArea[4][5] + visionArea[4][6] + visionArea[4][7] + visionArea[5][5] + visionArea[5][6] + visionArea[5][7] + visionArea[6][5] + visionArea[6][6] + visionArea[6][7];
        if (overlapCount > maxOverlap) {
            maxOverlap = overlapCount;
            bestLocation = new MapLocation(rc.getLocation().x + 2, rc.getLocation().y + 1);
        }

        overlapCount = visionArea[4][6] + visionArea[4][7] + visionArea[4][8] + visionArea[5][6] + visionArea[5][7] + visionArea[5][8] + visionArea[6][6] + visionArea[6][7] + visionArea[6][8];
        if (overlapCount > maxOverlap) {
            maxOverlap = overlapCount;
            bestLocation = new MapLocation(rc.getLocation().x + 3, rc.getLocation().y + 1);
        }

        overlapCount = visionArea[5][0] + visionArea[5][1] + visionArea[5][2] + visionArea[6][0] + visionArea[6][1] + visionArea[6][2] + visionArea[7][0] + visionArea[7][1] + visionArea[7][2];
        if (overlapCount > maxOverlap) {
            maxOverlap = overlapCount;
            bestLocation = new MapLocation(rc.getLocation().x - 3, rc.getLocation().y + 2);
        }

        overlapCount = visionArea[5][1] + visionArea[5][2] + visionArea[5][3] + visionArea[6][1] + visionArea[6][2] + visionArea[6][3] + visionArea[7][1] + visionArea[7][2] + visionArea[7][3];
        if (overlapCount > maxOverlap) {
            maxOverlap = overlapCount;
            bestLocation = new MapLocation(rc.getLocation().x - 2, rc.getLocation().y + 2);
        }

        overlapCount = visionArea[5][2] + visionArea[5][3] + visionArea[5][4] + visionArea[6][2] + visionArea[6][3] + visionArea[6][4] + visionArea[7][2] + visionArea[7][3] + visionArea[7][4];
        if (overlapCount > maxOverlap) {
            maxOverlap = overlapCount;
            bestLocation = new MapLocation(rc.getLocation().x - 1, rc.getLocation().y + 2);
        }

        overlapCount = visionArea[5][3] + visionArea[5][4] + visionArea[5][5] + visionArea[6][3] + visionArea[6][4] + visionArea[6][5] + visionArea[7][3] + visionArea[7][4] + visionArea[7][5];
        if (overlapCount > maxOverlap) {
            maxOverlap = overlapCount;
            bestLocation = new MapLocation(rc.getLocation().x, rc.getLocation().y + 2);
        }

        overlapCount = visionArea[5][4] + visionArea[5][5] + visionArea[5][6] + visionArea[6][4] + visionArea[6][5] + visionArea[6][6] + visionArea[7][4] + visionArea[7][5] + visionArea[7][6];
        if (overlapCount > maxOverlap) {
            maxOverlap = overlapCount;
            bestLocation = new MapLocation(rc.getLocation().x + 1, rc.getLocation().y + 2);
        }

        overlapCount = visionArea[5][5] + visionArea[5][6] + visionArea[5][7] + visionArea[6][5] + visionArea[6][6] + visionArea[6][7] + visionArea[7][5] + visionArea[7][6] + visionArea[7][7];
        if (overlapCount > maxOverlap) {
            maxOverlap = overlapCount;
            bestLocation = new MapLocation(rc.getLocation().x + 2, rc.getLocation().y + 2);
        }

        overlapCount = visionArea[5][6] + visionArea[5][7] + visionArea[5][8] + visionArea[6][6] + visionArea[6][7] + visionArea[6][8] + visionArea[7][6] + visionArea[7][7] + visionArea[7][8];
        if (overlapCount > maxOverlap) {
            maxOverlap = overlapCount;
            bestLocation = new MapLocation(rc.getLocation().x + 3, rc.getLocation().y + 2);
        }

        overlapCount = visionArea[6][0] + visionArea[6][1] + visionArea[6][2] + visionArea[7][0] + visionArea[7][1] + visionArea[7][2] + visionArea[8][0] + visionArea[8][1] + visionArea[8][2];
        if (overlapCount > maxOverlap) {
            maxOverlap = overlapCount;
            bestLocation = new MapLocation(rc.getLocation().x - 3, rc.getLocation().y + 3);
        }

        overlapCount = visionArea[6][1] + visionArea[6][2] + visionArea[6][3] + visionArea[7][1] + visionArea[7][2] + visionArea[7][3] + visionArea[8][1] + visionArea[8][2] + visionArea[8][3];
        if (overlapCount > maxOverlap) {
            maxOverlap = overlapCount;
            bestLocation = new MapLocation(rc.getLocation().x - 2, rc.getLocation().y + 3);
        }

        overlapCount = visionArea[6][2] + visionArea[6][3] + visionArea[6][4] + visionArea[7][2] + visionArea[7][3] + visionArea[7][4] + visionArea[8][2] + visionArea[8][3] + visionArea[8][4];
        if (overlapCount > maxOverlap) {
            maxOverlap = overlapCount;
            bestLocation = new MapLocation(rc.getLocation().x - 1, rc.getLocation().y + 3);
        }

        overlapCount = visionArea[6][3] + visionArea[6][4] + visionArea[6][5] + visionArea[7][3] + visionArea[7][4] + visionArea[7][5] + visionArea[8][3] + visionArea[8][4] + visionArea[8][5];
        if (overlapCount > maxOverlap) {
            maxOverlap = overlapCount;
            bestLocation = new MapLocation(rc.getLocation().x, rc.getLocation().y + 3);
        }

        overlapCount = visionArea[6][4] + visionArea[6][5] + visionArea[6][6] + visionArea[7][4] + visionArea[7][5] + visionArea[7][6] + visionArea[8][4] + visionArea[8][5] + visionArea[8][6];
        if (overlapCount > maxOverlap) {
            maxOverlap = overlapCount;
            bestLocation = new MapLocation(rc.getLocation().x + 1, rc.getLocation().y + 3);
        }

        overlapCount = visionArea[6][5] + visionArea[6][6] + visionArea[6][7] + visionArea[7][5] + visionArea[7][6] + visionArea[7][7] + visionArea[8][5] + visionArea[8][6] + visionArea[8][7];
        if (overlapCount > maxOverlap) {
            maxOverlap = overlapCount;
            bestLocation = new MapLocation(rc.getLocation().x + 2, rc.getLocation().y + 3);
        }

        overlapCount = visionArea[6][6] + visionArea[6][7] + visionArea[6][8] + visionArea[7][6] + visionArea[7][7] + visionArea[7][8] + visionArea[8][6] + visionArea[8][7] + visionArea[8][8];
        if (overlapCount > maxOverlap) {
            maxOverlap = overlapCount;
            bestLocation = new MapLocation(rc.getLocation().x + 3, rc.getLocation().y + 3);
        }

        return bestLocation != null ? bestLocation : new MapLocation(-1, -1);
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
