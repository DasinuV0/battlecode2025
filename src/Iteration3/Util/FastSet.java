package oldUtil;

import battlecode.common.MapLocation;

// visited set, but in string form as an optimization

public class FastSet {
    private StringBuilder values = new StringBuilder();

    public boolean add(char value) {
        String str = String.valueOf(value);
        if (values.indexOf(str) == -1) {
            values.append(str);
            return true;
        }

        return false;
    }

    public boolean contains(char value) {
        return values.indexOf(String.valueOf(value)) > -1;
    }

    public boolean add(MapLocation location) {
        return add(encodeLocation(location));
    }

    public boolean contains(MapLocation location) {
        return contains(encodeLocation(location));
    }

    private char encodeLocation(MapLocation location) {
        return (char) ((location.x << 6) | location.y);
    }
}