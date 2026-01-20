package navik.llm.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import navik.llm.enums.AreaType;
import navik.llm.enums.CompanySize;
import navik.llm.enums.EducationLevel;
import navik.llm.enums.EmploymentType;
import navik.llm.enums.ExperienceType;
import navik.llm.enums.IndustryType;
import navik.llm.enums.JobType;
import navik.llm.enums.MajorType;

/**
 * Structured Output 관리
 */
public class LLMResponseDTO {

	@ToString
	@Getter
	@NoArgsConstructor
	public static class Recruitment {
		private String link;
		private String title;
		private String postId;
		private String companyName;
		private String companyLogo;
		private CompanySize companySize;
		private IndustryType industryType;
		private LocalDateTime startDate;
		private LocalDateTime endDate;
		private List<Position> positions;
		private String summary;

		@ToString
		@Getter
		@NoArgsConstructor
		public static class Position {
			private String name;
			private JobType jobType;
			private EmploymentType employmentType;
			private ExperienceType experienceType;
			private EducationLevel educationLevel;
			private AreaType areaType;
			private String detailAddress;
			private MajorType majorType;
			private List<String> kpis;
		}
	}
}
