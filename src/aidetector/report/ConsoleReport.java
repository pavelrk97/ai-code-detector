package aidetector.report;

import aidetector.core.FileReport;
import aidetector.core.Finding;
import aidetector.core.SignalHit;
import aidetector.core.Verdict;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ConsoleReport implements ReportRenderer {

    private final double aiThreshold;
    private final double mixedThreshold;
    private final int fileLimit;
    private final int evidenceLimit;

    public ConsoleReport(double aiThreshold, double mixedThreshold, int fileLimit, int evidenceLimit) {
        this.aiThreshold = aiThreshold;
        this.mixedThreshold = mixedThreshold;
        this.fileLimit = fileLimit;
        this.evidenceLimit = evidenceLimit;
    }

    @Override
    public String render(List<FileReport> reports) {
        if (reports.isEmpty()) {
            return "Nothing to analyze: no supported source files were found.";
        }
        StringBuilder out = new StringBuilder();
        if (reports.size() == 1) {
            renderFile(out, reports.get(0), true);
        } else {
            renderRepository(out, reports);
        }
        return out.toString().stripTrailing();
    }

    private void renderRepository(StringBuilder out, List<FileReport> reports) {
        double overall = weightedPercentage(reports);
        Map<Verdict.Classification, Integer> bands = new EnumMap<>(Verdict.Classification.class);
        for (FileReport report : reports) {
            bands.merge(report.verdict().classification(), 1, Integer::sum);
        }

        out.append("Scanned ").append(reports.size()).append(" files\n");
        out.append("Overall : ").append(classify(overall).label())
                .append("  (").append(percent(overall)).append(" AI-leaning)\n");
        out.append("Spread  : ")
                .append(bands.getOrDefault(Verdict.Classification.LIKELY_AI, 0)).append(" AI / ")
                .append(bands.getOrDefault(Verdict.Classification.MIXED, 0)).append(" mixed / ")
                .append(bands.getOrDefault(Verdict.Classification.LIKELY_HUMAN, 0)).append(" human\n\n");

        List<FileReport> ranked = new ArrayList<>(reports);
        ranked.sort((a, b) -> Double.compare(b.verdict().percentage(), a.verdict().percentage()));

        out.append("Most AI-leaning files:\n\n");
        int shown = 0;
        for (FileReport report : ranked) {
            if (shown >= fileLimit) {
                break;
            }
            renderFile(out, report, true);
            out.append('\n');
            shown++;
        }
    }

    private void renderFile(StringBuilder out, FileReport report, boolean withEvidence) {
        Verdict verdict = report.verdict();
        out.append(report.source().path()).append('\n');
        out.append("  Verdict : ").append(verdict.classification().label())
                .append("  (").append(percent(verdict.percentage())).append(" AI-leaning, ")
                .append(verdict.confidence().label()).append(")\n");
        out.append("  Lines   : ").append(report.source().lineCount()).append('\n');

        List<SignalHit> hits = sortedByStrength(verdict.hits());
        if (hits.isEmpty()) {
            out.append("  Signals : none triggered\n");
            return;
        }
        out.append("  Signals :\n");
        for (SignalHit hit : hits) {
            out.append("    ").append(sign(hit.contribution())).append("  ")
                    .append(hit.heuristic().label()).append('\n');
            if (withEvidence) {
                for (Finding finding : limit(hit.findings(), evidenceLimit)) {
                    out.append("          ").append(location(finding)).append(finding.snippet()).append('\n');
                }
            }
        }
    }

    private Verdict.Classification classify(double percentage) {
        if (percentage >= aiThreshold) {
            return Verdict.Classification.LIKELY_AI;
        }
        if (percentage >= mixedThreshold) {
            return Verdict.Classification.MIXED;
        }
        return Verdict.Classification.LIKELY_HUMAN;
    }

    private static double weightedPercentage(List<FileReport> reports) {
        double numerator = 0;
        double denominator = 0;
        for (FileReport report : reports) {
            int weight = Math.max(1, report.source().nonEmptyLineCount());
            numerator += report.verdict().percentage() * weight;
            denominator += weight;
        }
        return denominator == 0 ? 0 : numerator / denominator;
    }

    private static List<SignalHit> sortedByStrength(List<SignalHit> hits) {
        List<SignalHit> copy = new ArrayList<>(hits);
        copy.sort((a, b) -> Double.compare(Math.abs(b.contribution()), Math.abs(a.contribution())));
        return copy;
    }

    private static List<Finding> limit(List<Finding> findings, int max) {
        if (findings.size() <= max) {
            return findings;
        }
        return findings.subList(0, max);
    }

    private static String location(Finding finding) {
        return finding.hasLocation() ? "L" + finding.line() + ": " : "- ";
    }

    private static String sign(double weight) {
        String formatted = String.format(Locale.ROOT, "%.1f", weight);
        return weight >= 0 ? "+" + formatted : formatted;
    }

    private static String percent(double value) {
        return Math.round(value) + "%";
    }
}
