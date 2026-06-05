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

public final class HtmlReport implements ReportRenderer {

    private final double aiThreshold;
    private final double mixedThreshold;
    private final int evidenceLimit;

    public HtmlReport(double aiThreshold, double mixedThreshold, int evidenceLimit) {
        this.aiThreshold = aiThreshold;
        this.mixedThreshold = mixedThreshold;
        this.evidenceLimit = evidenceLimit;
    }

    @Override
    public String render(List<FileReport> reports) {
        StringBuilder out = new StringBuilder(64_000);
        out.append("<!doctype html>\n");
        out.append("<html lang=\"en\">\n");
        out.append("<head>\n");
        out.append("  <meta charset=\"utf-8\">\n");
        out.append("  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n");
        out.append("  <title>AI Code Detector Report</title>\n");
        out.append("  <style>\n");
        out.append(styles());
        out.append("  </style>\n");
        out.append("</head>\n");
        out.append("<body>\n");
        out.append("  <main class=\"shell\">\n");
        out.append("    <header class=\"page-header\">\n");
        out.append("      <div>\n");
        out.append("        <p class=\"eyebrow\">AI Code Detector</p>\n");
        out.append("        <h1>Code origin review</h1>\n");
        out.append("      </div>\n");
        out.append("      <p class=\"note\">Heuristic signals are review aids, not proof of authorship.</p>\n");
        out.append("    </header>\n");

        if (reports.isEmpty()) {
            out.append("    <section class=\"empty\">No supported source files were found.</section>\n");
        } else {
            renderSummary(out, reports);
            renderFilters(out);
            renderFiles(out, ranked(reports));
        }

        out.append("  </main>\n");
        out.append("  <script>\n");
        out.append(script());
        out.append("  </script>\n");
        out.append("</body>\n");
        out.append("</html>\n");
        return out.toString();
    }

    private void renderSummary(StringBuilder out, List<FileReport> reports) {
        double overall = weightedPercentage(reports);
        Map<Verdict.Classification, Integer> bands = new EnumMap<>(Verdict.Classification.class);
        for (FileReport report : reports) {
            bands.merge(report.verdict().classification(), 1, Integer::sum);
        }
        int high = bands.getOrDefault(Verdict.Classification.LIKELY_AI, 0);
        int review = bands.getOrDefault(Verdict.Classification.MIXED, 0);
        int low = bands.getOrDefault(Verdict.Classification.LIKELY_HUMAN, 0);

        out.append("    <section class=\"summary-grid\" aria-label=\"Summary\">\n");
        metric(out, "Overall", classificationLabel(classify(overall)), percent(overall) + " AI-leaning", riskClass(classify(overall)));
        metric(out, "Files", String.valueOf(reports.size()), "supported source files", "");
        metric(out, "High", String.valueOf(high), "requires review", "risk-high");
        metric(out, "Review", String.valueOf(review), "mixed signals", "risk-review");
        metric(out, "Low", String.valueOf(low), "low AI-likeness", "risk-low");
        out.append("    </section>\n");
    }

    private void metric(StringBuilder out, String label, String value, String caption, String riskClass) {
        out.append("      <article class=\"metric");
        if (!riskClass.isEmpty()) {
            out.append(' ').append(riskClass);
        }
        out.append("\">\n");
        out.append("        <span>").append(escape(label)).append("</span>\n");
        out.append("        <strong>").append(escape(value)).append("</strong>\n");
        out.append("        <small>").append(escape(caption)).append("</small>\n");
        out.append("      </article>\n");
    }

    private void renderFilters(StringBuilder out) {
        out.append("    <section class=\"toolbar\" aria-label=\"Filters\">\n");
        out.append("      <label>\n");
        out.append("        <span>Search</span>\n");
        out.append("        <input id=\"search\" type=\"search\" placeholder=\"File path or signal\">\n");
        out.append("      </label>\n");
        out.append("      <label>\n");
        out.append("        <span>Risk</span>\n");
        out.append("        <select id=\"risk\">\n");
        out.append("          <option value=\"all\">All</option>\n");
        out.append("          <option value=\"high\">High</option>\n");
        out.append("          <option value=\"review\">Review</option>\n");
        out.append("          <option value=\"low\">Low</option>\n");
        out.append("        </select>\n");
        out.append("      </label>\n");
        out.append("    </section>\n");
    }

    private void renderFiles(StringBuilder out, List<FileReport> reports) {
        out.append("    <section class=\"files\" aria-label=\"Files\">\n");
        for (FileReport report : reports) {
            Verdict verdict = report.verdict();
            String risk = riskName(verdict.classification());
            List<SignalHit> hits = sortedByStrength(verdict.hits());
            out.append("      <details class=\"file-card\" data-risk=\"").append(risk)
                    .append("\" data-text=\"").append(escapeAttribute(searchText(report, hits))).append("\">\n");
            out.append("        <summary>\n");
            out.append("          <span class=\"path\">").append(escape(report.source().path())).append("</span>\n");
            out.append("          <span class=\"chips\">\n");
            out.append("            <span class=\"chip ").append(riskClass(verdict.classification())).append("\">")
                    .append(escape(riskLabel(verdict.classification()))).append("</span>\n");
            out.append("            <span class=\"chip neutral\">").append(percent(verdict.percentage())).append("</span>\n");
            out.append("            <span class=\"chip neutral\">").append(escape(verdict.confidence().label())).append("</span>\n");
            out.append("            <span class=\"chip neutral\">").append(report.source().lineCount()).append(" lines</span>\n");
            out.append("          </span>\n");
            out.append("        </summary>\n");

            if (hits.isEmpty()) {
                out.append("        <p class=\"no-signals\">No signals triggered.</p>\n");
            } else {
                out.append("        <div class=\"signals\">\n");
                for (SignalHit hit : hits) {
                    renderSignal(out, hit);
                }
                out.append("        </div>\n");
            }
            out.append("      </details>\n");
        }
        out.append("    </section>\n");
    }

    private void renderSignal(StringBuilder out, SignalHit hit) {
        out.append("          <section class=\"signal\">\n");
        out.append("            <header>\n");
        out.append("              <strong>").append(sign(hit.contribution())).append("</strong>\n");
        out.append("              <span>").append(escape(hit.heuristic().label())).append("</span>\n");
        out.append("            </header>\n");
        String hint = falsePositiveHint(hit.heuristic().id());
        if (hint != null) {
            out.append("            <p class=\"hint\">").append(escape(hint)).append("</p>\n");
        }
        out.append("            <ul>\n");
        for (Finding finding : limit(hit.findings(), evidenceLimit)) {
            out.append("              <li>");
            if (finding.hasLocation()) {
                out.append("<span>L").append(finding.line()).append("</span> ");
            }
            out.append(escape(finding.snippet())).append("</li>\n");
        }
        out.append("            </ul>\n");
        out.append("          </section>\n");
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

    private static List<FileReport> ranked(List<FileReport> reports) {
        List<FileReport> ranked = new ArrayList<>(reports);
        ranked.sort((a, b) -> Double.compare(b.verdict().percentage(), a.verdict().percentage()));
        return ranked;
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

    private static String searchText(FileReport report, List<SignalHit> hits) {
        StringBuilder text = new StringBuilder(report.source().path());
        for (SignalHit hit : hits) {
            text.append(' ').append(hit.heuristic().label()).append(' ').append(hit.heuristic().id());
        }
        return text.toString().toLowerCase(Locale.ROOT);
    }

    private static String riskName(Verdict.Classification classification) {
        return switch (classification) {
            case LIKELY_AI -> "high";
            case MIXED -> "review";
            case LIKELY_HUMAN -> "low";
        };
    }

    private static String riskLabel(Verdict.Classification classification) {
        return switch (classification) {
            case LIKELY_AI -> "High";
            case MIXED -> "Review";
            case LIKELY_HUMAN -> "Low";
        };
    }

    private static String classificationLabel(Verdict.Classification classification) {
        return switch (classification) {
            case LIKELY_AI -> "High AI-likeness";
            case MIXED -> "Review recommended";
            case LIKELY_HUMAN -> "Low AI-likeness";
        };
    }

    private static String riskClass(Verdict.Classification classification) {
        return switch (classification) {
            case LIKELY_AI -> "risk-high";
            case MIXED -> "risk-review";
            case LIKELY_HUMAN -> "risk-low";
        };
    }

    private static String falsePositiveHint(String id) {
        return switch (id) {
            case "formatting_too_clean" -> "Uniform formatting can come from an IDE formatter or Prettier.";
            case "generic_names" -> "Names like value, result, response, and item are normal in many codebases.";
            case "lack_of_comments" -> "Comment-free code is common when methods are short and names are clear.";
            case "repetitive_lines" -> "Repeated JSX tags, builders, annotations, and guard clauses can be normal.";
            case "over_structured" -> "Dense control flow can be expected in parsers, validators, and UI logic.";
            case "no_todo_fixme" -> "No TODO/FIXME markers is a weak signal in reviewed production code.";
            default -> null;
        };
    }

    private static String sign(double weight) {
        String formatted = String.format(Locale.ROOT, "%.1f", weight);
        return weight >= 0 ? "+" + formatted : formatted;
    }

    private static String percent(double value) {
        return Math.round(value) + "%";
    }

    private static String escape(String value) {
        StringBuilder out = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '&' -> out.append("&amp;");
                case '<' -> out.append("&lt;");
                case '>' -> out.append("&gt;");
                case '"' -> out.append("&quot;");
                case '\'' -> out.append("&#39;");
                default -> out.append(c);
            }
        }
        return out.toString();
    }

    private static String escapeAttribute(String value) {
        return escape(value);
    }

    private static String styles() {
        return """
        :root {
          color-scheme: light;
          --bg: #f5f7fb;
          --panel: #ffffff;
          --line: #d8dee9;
          --text: #172033;
          --muted: #697386;
          --high: #b42318;
          --high-bg: #fff1f0;
          --review: #8a5a00;
          --review-bg: #fff7df;
          --low: #146c43;
          --low-bg: #edf9f2;
          --neutral-bg: #eef2f7;
        }
        * { box-sizing: border-box; }
        body {
          margin: 0;
          background: var(--bg);
          color: var(--text);
          font: 14px/1.5 Segoe UI, Roboto, Arial, sans-serif;
        }
        .shell {
          width: min(1180px, calc(100% - 32px));
          margin: 0 auto;
          padding: 28px 0 48px;
        }
        .page-header {
          display: flex;
          justify-content: space-between;
          gap: 24px;
          align-items: flex-end;
          margin-bottom: 20px;
        }
        .eyebrow {
          margin: 0 0 4px;
          color: var(--muted);
          font-size: 12px;
          font-weight: 700;
          letter-spacing: .08em;
          text-transform: uppercase;
        }
        h1 {
          margin: 0;
          font-size: 30px;
          line-height: 1.15;
          letter-spacing: 0;
        }
        .note {
          max-width: 360px;
          margin: 0;
          color: var(--muted);
          text-align: right;
        }
        .summary-grid {
          display: grid;
          grid-template-columns: repeat(5, minmax(0, 1fr));
          gap: 12px;
          margin-bottom: 16px;
        }
        .metric, .toolbar, .file-card, .empty {
          background: var(--panel);
          border: 1px solid var(--line);
          border-radius: 8px;
        }
        .metric {
          padding: 14px;
        }
        .metric span, .metric small {
          display: block;
          color: var(--muted);
        }
        .metric strong {
          display: block;
          margin: 3px 0;
          font-size: 22px;
          line-height: 1.2;
        }
        .toolbar {
          display: flex;
          gap: 12px;
          align-items: end;
          padding: 12px;
          margin-bottom: 16px;
        }
        .toolbar label {
          display: grid;
          gap: 5px;
          flex: 1;
        }
        .toolbar label:last-child {
          max-width: 180px;
        }
        .toolbar span {
          color: var(--muted);
          font-size: 12px;
          font-weight: 700;
        }
        input, select {
          width: 100%;
          border: 1px solid var(--line);
          border-radius: 6px;
          padding: 9px 10px;
          color: var(--text);
          background: #fff;
          font: inherit;
        }
        .files {
          display: grid;
          gap: 10px;
        }
        .file-card {
          overflow: hidden;
        }
        summary {
          display: flex;
          gap: 12px;
          justify-content: space-between;
          align-items: center;
          padding: 13px 14px;
          cursor: pointer;
          list-style: none;
        }
        summary::-webkit-details-marker { display: none; }
        .path {
          min-width: 0;
          overflow-wrap: anywhere;
          font-family: Consolas, SFMono-Regular, monospace;
          font-size: 13px;
        }
        .chips {
          display: flex;
          flex-wrap: wrap;
          gap: 6px;
          justify-content: flex-end;
          flex: 0 0 auto;
        }
        .chip {
          display: inline-flex;
          align-items: center;
          min-height: 24px;
          border-radius: 999px;
          padding: 2px 9px;
          font-size: 12px;
          font-weight: 700;
        }
        .risk-high { background: var(--high-bg); color: var(--high); }
        .risk-review { background: var(--review-bg); color: var(--review); }
        .risk-low { background: var(--low-bg); color: var(--low); }
        .neutral { background: var(--neutral-bg); color: var(--muted); }
        .signals {
          border-top: 1px solid var(--line);
          padding: 4px 14px 14px;
        }
        .signal {
          padding: 12px 0;
          border-bottom: 1px solid #edf0f5;
        }
        .signal:last-child {
          border-bottom: 0;
        }
        .signal header {
          display: flex;
          gap: 9px;
          align-items: baseline;
        }
        .signal header strong {
          font-family: Consolas, SFMono-Regular, monospace;
        }
        .hint {
          margin: 5px 0 7px;
          color: var(--muted);
        }
        ul {
          margin: 0;
          padding-left: 0;
          list-style: none;
        }
        li {
          margin-top: 4px;
          padding: 6px 8px;
          border-radius: 6px;
          background: #f8fafc;
          overflow-wrap: anywhere;
          font-family: Consolas, SFMono-Regular, monospace;
          font-size: 12px;
        }
        li span {
          color: var(--muted);
          font-weight: 700;
        }
        .no-signals {
          margin: 0;
          border-top: 1px solid var(--line);
          padding: 12px 14px 14px;
          color: var(--muted);
        }
        .empty {
          padding: 18px;
        }
        .hidden {
          display: none;
        }
        @media (max-width: 820px) {
          .page-header, summary, .toolbar {
            display: grid;
          }
          .note {
            text-align: left;
          }
          .summary-grid {
            grid-template-columns: repeat(2, minmax(0, 1fr));
          }
          .toolbar label:last-child {
            max-width: none;
          }
          .chips {
            justify-content: flex-start;
          }
        }
        """;
    }

    private static String script() {
        return """
        const search = document.getElementById('search');
        const risk = document.getElementById('risk');
        const cards = Array.from(document.querySelectorAll('.file-card'));

        function applyFilters() {
          const query = (search?.value || '').trim().toLowerCase();
          const selectedRisk = risk?.value || 'all';
          for (const card of cards) {
            const matchesText = !query || card.dataset.text.includes(query);
            const matchesRisk = selectedRisk === 'all' || card.dataset.risk === selectedRisk;
            card.classList.toggle('hidden', !(matchesText && matchesRisk));
          }
        }

        search?.addEventListener('input', applyFilters);
        risk?.addEventListener('change', applyFilters);
        """;
    }
}
