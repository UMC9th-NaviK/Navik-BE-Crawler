package navik.growth.analysis.dto;

import java.util.List;
import navik.growth.analysis.dto.AnalysisResponse.GrowthAnalysisResponse.KpiDelta;

/** Serializable checkpoint before any embedding request is made. */
public record AnalysisDraft(String title, String content, List<KpiDelta> kpis, List<String> abilities) { }
