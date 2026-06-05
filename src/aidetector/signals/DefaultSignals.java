package aidetector.signals;

import aidetector.core.Heuristic;

import java.util.List;

public final class DefaultSignals {

    private DefaultSignals() {
    }

    public static List<Heuristic> all() {
        return List.of(
                new FormattingTooClean(),
                new GenericNames(),
                new MissingComments(),
                new RepetitiveLines(),
                new OverStructuredFlow(),
                new HumanComplexity(),
                new OverCommentingTrivial(),
                new ConversationalPreamble(),
                new ExcessiveTryCatch(),
                new DocBlocksEverywhere(),
                new DefensiveNullChecks(),
                new SymmetricHelperNames(),
                new MissingTaskMarkers());
    }
}
