package navik.crawler.dto;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CrawledRecruitment {

	private String link;
	private String title;
	private String postId;
	private String companyName;
	private String companyLogo;
	private String companyInfo;
	private String qualification;
	private String timeInfo;
	private String outline;
	private String recruitmentDetail;

	public String toHtmlString() {
		return "\n<현재 시간>" + LocalDateTime.now() + "</현재 시간>\n"
			+ "\n<링크>" + link + "</링크>\n"
			+ "\n<제목>" + title + "</제목>\n"
			+ "\n<postId>" + postId + "</postId>\n"
			+ "\n<회사명>" + companyName + "</회사명>\n"
			+ "\n<회사 로고>" + companyLogo + "</회사 로고>\n"
			+ "\n<회사 정보>" + companyInfo + "</회사 정보>\n"
			+ "\n<지원 자격>" + qualification + "</지원 자격>\n"
			+ "\n<시간 정보>" + timeInfo + "</시간 정보>\n"
			+ "\n<모집 요강>" + outline + "</모집 요강>\n"
			+ "\n<채용 공고 상세>" + recruitmentDetail + "</채용 공고 상세>\n";
	}
}
