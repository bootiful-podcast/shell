package podcast;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
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

	public boolean publishPackage(File archivePackage) {
		var headers = new HttpHeaders();
		headers.setContentType(MediaType.MULTIPART_FORM_DATA);
		var resource = new FileSystemResource(archivePackage);
		var body = new LinkedMultiValueMap<String, Object>();
		body.add("file", resource);
		var requestEntity = new HttpEntity<MultiValueMap<String, Object>>(body, headers);
		var url = this.serverUrl + "?id=" + UUID.randomUUID().toString();
		var response = restTemplate.postForEntity(url, requestEntity, String.class);
		return response.getStatusCode().is2xxSuccessful();
	}

	public static void main(String[] args) {
		var rt = new RestTemplateBuilder().build();
		ApiClient apiClient = new ApiClient(
				"http://localhost:8080/production?id=" + UUID.randomUUID().toString(),
				rt);
		var file = new File("/Users/joshlong/Desktop/sample-package.zip");
		var sent = apiClient.publishPackage(file);
		log.info("sent: " + sent);
	}

}
