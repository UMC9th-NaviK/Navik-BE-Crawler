package navik.ocr.util;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import org.apache.commons.imaging.ImageInfo;
import org.apache.commons.imaging.Imaging;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import navik.ocr.dto.ImageMetadataDTO;

@Slf4j
@Component
public class ImageHelper {

	private final int TIMEOUT_MS = 5000;

	public ImageMetadataDTO getMetadata(String imageUrl) {

		if (imageUrl == null || imageUrl.isBlank()) {
			log.warn("[ImageHelper] 이미지 URL이 null이거나 비어있습니다.");
			return null;
		}
		
		if (imageUrl.startsWith("data:")) {
			log.warn("[ImageHelper] data URI 이미지는 처리하지 않습니다: {}", imageUrl);
			return null;
		}

		try {
			URL url = new URL(imageUrl);

			URLConnection connection = url.openConnection();
			connection.setConnectTimeout(TIMEOUT_MS);
			connection.setReadTimeout(TIMEOUT_MS);
			try (InputStream stream = connection.getInputStream()) {
				ImageInfo imageInfo = Imaging.getImageInfo(stream, "");

				return ImageMetadataDTO.builder()
					.fileSize(connection.getContentLengthLong())
					.width(imageInfo.getWidth())
					.height(imageInfo.getHeight())
					.extension(imageInfo.getFormat().getDefaultExtension().toLowerCase())
					.build();
			}
		} catch (Exception e) {
			log.error("[ImageHelper] 이미지 메타데이터 추출에 실패하였습니다: {}", e.getMessage());
			return null;
		}
	}
}
