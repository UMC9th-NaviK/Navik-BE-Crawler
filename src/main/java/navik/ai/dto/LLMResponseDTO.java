package navik.ai.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import navik.ai.enums.AreaType;
import navik.ai.enums.CompanySize;
import navik.ai.enums.EducationLevel;
import navik.ai.enums.EmploymentType;
import navik.ai.enums.ExperienceType;
import navik.ai.enums.IndustryType;
import navik.ai.enums.JobType;
import navik.ai.enums.MajorType;

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
