package navik;

import java.time.LocalDateTime;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthCheckController {
	
	@GetMapping("/health")
	public LocalDateTime health() {
		return LocalDateTime.now();
	}
}
