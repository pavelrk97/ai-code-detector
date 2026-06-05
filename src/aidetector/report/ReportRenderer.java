package aidetector.report;

import aidetector.core.FileReport;

import java.util.List;

public interface ReportRenderer {

    String render(List<FileReport> reports);
}
