package aidetector.core;

import java.util.ArrayList;
import java.util.List;

public final class Analyzer {

    private static final double STRONG_WEIGHT = 1.3;

    private final List<Heuristic> heuristics;
    private final double aiThreshold;
    private final double mixedThreshold;

    public Analyzer(List<Heuristic> heuristics, double aiThreshold, double mixedThreshold) {
        this.heuristics = List.copyOf(heuristics);
        this.aiThreshold = aiThreshold;
        this.mixedThreshold = mixedThreshold;
    }

    public Verdict analyze(SourceFile source) {
        List<SignalHit> hits = new ArrayList<>();
        double matched = 0;
        double ceiling = 0;
        for (Heuristic heuristic : heuristics) {
            ceiling += heuristic.weight();
            List<Finding> findings = heuristic.evaluate(source);
            if (!findings.isEmpty()) {
                hits.add(new SignalHit(heuristic, findings));
                matched += heuristic.signedWeight();
            }
        }
        double percentage = ceiling == 0 ? 0 : clamp(matched / ceiling * 100.0, 0, 100);
        return new Verdict(percentage, classify(percentage), confidence(hits), hits);
    }

    private static Verdict.Confidence confidence(List<SignalHit> hits) {
        int strong = 0;
        for (SignalHit hit : hits) {
            Heuristic heuristic = hit.heuristic();
            if (heuristic.kind() == Heuristic.Kind.AI && heuristic.weight() >= STRONG_WEIGHT) {
                strong++;
            }
        }
        if (strong >= 3) {
            return Verdict.Confidence.HIGH;
        }
        if (strong == 2) {
            return Verdict.Confidence.MEDIUM;
        }
        return Verdict.Confidence.LOW;
    }

    public Verdict.Classification classify(double percentage) {
        if (percentage >= aiThreshold) {
            return Verdict.Classification.LIKELY_AI;
        }
        if (percentage >= mixedThreshold) {
            return Verdict.Classification.MIXED;
        }
        return Verdict.Classification.LIKELY_HUMAN;
    }

    private static double clamp(double value, double low, double high) {
        return Math.max(low, Math.min(high, value));
    }
}
