package aidetector.signals;

import aidetector.core.Heuristic;

abstract class AbstractHeuristic implements Heuristic {

    private final String id;
    private final String label;
    private final double weight;
    private final Kind kind;

    AbstractHeuristic(String id, String label, double weight, Kind kind) {
        this.id = id;
        this.label = label;
        this.weight = weight;
        this.kind = kind;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public String label() {
        return label;
    }

    @Override
    public double weight() {
        return weight;
    }

    @Override
    public Kind kind() {
        return kind;
    }
}
