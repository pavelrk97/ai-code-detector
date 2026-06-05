package aidetector.core;

import java.util.List;

public record SignalHit(Heuristic heuristic, List<Finding> findings) {

    public double contribution() {
        return heuristic.signedWeight();
    }
}
