package Iteration3;

// Logger to keep track of the whole path finding process.
// flush is used at the end, after logging all the info for efficient query

public class Logger extends Globals {
    private static StringBuilder sb = new StringBuilder();

    public static void flush() {
        rc.setIndicatorString(sb.toString());
        sb = new StringBuilder();
    }

    public static void log(String message) {
        if (sb.length() > 0) {
            sb.append(", ");
        }

        sb.append(message);
    }
}