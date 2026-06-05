package aidetector.core;

import java.util.List;

public interface Heuristic {

    enum Kind {
        AI,
        HUMAN
    }

    String id();

    String label();

    double weight();

    Kind kind();

    List<Finding> evaluate(SourceFile source);

    default double signedWeight() {
        return kind() == Kind.HUMAN ? -weight() : weight();
    }
}
