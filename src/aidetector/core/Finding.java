package aidetector.core;

public record Finding(int line, String snippet) {

    public static Finding aggregate(String message) {
        return new Finding(0, message);
    }

    public boolean hasLocation() {
        return line > 0;
    }
}
