package aidetector.core;

import java.util.List;

public record Verdict(double percentage, Classification classification, Confidence confidence, List<SignalHit> hits) {

    public enum Classification {
        LIKELY_AI("Likely AI-generated"),
        MIXED("Mixed signals"),
        LIKELY_HUMAN("Likely human-written");

        private final String label;

        Classification(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    public enum Confidence {
        LOW("low confidence"),
        MEDIUM("medium confidence"),
        HIGH("high confidence");

        private final String label;

        Confidence(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }
}
