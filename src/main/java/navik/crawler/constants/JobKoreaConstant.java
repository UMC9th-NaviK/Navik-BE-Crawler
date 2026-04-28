package navik.crawler.constants;

public class JobKoreaConstant {
	public static final String BASE_URL = "https://www.jobkorea.co.kr";
	public static final String RECRUITMENT_LIST_URL = "https://www.jobkorea.co.kr/recruit/joblist?menucode=duty";
	public static final String RECRUITMENT_DETAIL_URL_PATTERN = "https://www.jobkorea.co.kr/Recruit/GI_Read/(\\d+)";
	public static final String BLOOM_FILTER_NAME = "bf:jobkorea:post-id";
	public static final long BLOOM_FILTER_INSERTION_SIZE = 1000;
	public static final double BLOOM_FILTER_FPP = 0.05;
}
