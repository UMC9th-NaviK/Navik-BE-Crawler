package navik.crawler.ocr.constant;

import java.util.List;

public class NaverOCRConstant {
	public static final int MAX_PIXEL = 1960;
	public static final int MAX_FILE_SIZE = 50 * 1024 * 1024; // 50MB
	public static final String RECOMMENDED_VERSION = "V2";
	public static final String LANG_KOREAN = "ko";
	public static final List<String> SUPPORTED_EXTENSIONS = List.of("jpg", "jpeg", "png", "pdf", "tif", "tiff");
	public static final int TRASH_PIXEL_SIZE = 200;
}
