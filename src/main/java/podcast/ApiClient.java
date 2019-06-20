package podcast;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.util.UUID;

@Log4j2
@Component
class ApiClient {

	private final String serverUrl;

	private final RestTemplate restTemplate;

	ApiClient(@Value("${podcast.api.url}") String serverUrl, RestTemplate template) {
		this.serverUrl = serverUrl;
		this.restTemplate = template;
	}

	public PublishResponse publishPackage(File archivePackage) {
		var headers = new HttpHeaders();
		headers.setContentType(MediaType.MULTIPART_FORM_DATA);
		var resource = new FileSystemResource(archivePackage);
		var body = new LinkedMultiValueMap<String, Object>();
		body.add("file", resource);
		var requestEntity = new HttpEntity<MultiValueMap<String, Object>>(body, headers);
		var uuid = UUID.randomUUID();
		var url = this.serverUrl + "?uid=" + uuid.toString();
		var response = restTemplate.postForEntity(url, requestEntity, String.class);
		var good = response.getStatusCode().is2xxSuccessful();
		return PublishResponse
			.builder()
			.published(good)
			.httpStatus(response.getStatusCode())
			.uid(uuid.toString())
			.build();
	}

	public static void main(String[] args) {
		var rt = new RestTemplateBuilder().build();
		ApiClient apiClient = new ApiClient(
			"http://localhost:8080/production?uid=" + UUID.randomUUID().toString(),
			rt);
		var file = new File("/Users/joshlong/Desktop/sample-package.zip");
		var sent = apiClient.publishPackage(file);
		log.info("published: " + sent);
	}

	@Data
	@Builder
	@AllArgsConstructor
	@NoArgsConstructor
	public static class PublishResponse {
		private String uid;
		private boolean published;
		private String errorMessage;
		private HttpStatus httpStatus;
	}
}
