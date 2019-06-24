package podcast;

import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.net.URI;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Log4j2
class ApiClient {

	private final String serverUrl;

	private final RestTemplate restTemplate;

	private final Executor executor;

	ApiClient(String serverUrl, Executor executor, RestTemplate template) {
		this.serverUrl = serverUrl;
		this.restTemplate = template;
		this.executor = executor;
		log.info("initializing " + getClass().getName() + " with serverUrl '"
				+ this.serverUrl + "'");
	}

	public ProductionStatus publish(String uid, File archivePackage) {
		var headers = new HttpHeaders();
		headers.setContentType(MediaType.MULTIPART_FORM_DATA);
		var resource = new FileSystemResource(archivePackage);
		var body = new LinkedMultiValueMap<String, Object>();
		body.add("file", resource);
		var requestEntity = new HttpEntity<MultiValueMap<String, Object>>(body, headers);
		var url = this.serverUrl + "/podcasts/" + uid;
		var response = restTemplate.postForEntity(url, requestEntity, String.class);
		var good = response.getStatusCode().is2xxSuccessful();
		URI location = response.getHeaders().getLocation();
		Assert.notNull(location, "the location URI must be non-null");
		return new ProductionStatus(this.executor, this.restTemplate, null, good, uid,
				response.getStatusCode(),
				URI.create(this.serverUrl + location.getPath()));
	}

	public static void main(String[] args) {
		var maxThreads = Runtime.getRuntime().availableProcessors();
		var executor = Executors
				.newScheduledThreadPool(maxThreads >= 2 ? maxThreads / 2 : maxThreads);
		var uid = UUID.randomUUID().toString();
		var podcast = new Podcast(
				"Josh talks to Oleg Zhurakousky, lead of Spring Cloud Stream", uid);
		var ext = "mp3";
		var rootFile = new File("/Users/joshlong/Desktop/sample-podcast");
		var file = podcast.addMedia(ext, new File(rootFile, "oleg-intro.mp3"),
				new File(rootFile, "oleg-interview.mp3")).createPackage();
		var localUrl = "http://localhost:8080/";
		var productionUrl = "http://service-spontaneous-dingo.cfapps.io/";
		var rt = new RestTemplateBuilder().build();
		var apiClient = new ApiClient(localUrl, executor, rt);
		var sentResponse = apiClient.publish(uid, file);
		log.info("published: " + sentResponse);
		sentResponse.checkProductionStatus().thenAccept(uriOfProducedAsset -> log
				.info("the produced artifact URI is " + uriOfProducedAsset.toString()));

	}

	public static class ProductionStatus {

		private Executor executor;

		private RestTemplate template;

		ProductionStatus(Executor ex, RestTemplate rt, String errMsg, boolean published,
				String uid, HttpStatus status, URI statusUrl) {
			this.executor = ex;
			this.template = rt;
			this.uid = uid;
			Assert.notNull(this.executor, "the executor must be non-null");
			Assert.notNull(this.template,
					"the " + RestTemplate.class.getName() + " must be non-null");
			Assert.notNull(this.uid, "the UID must be non-null");
			this.errorMessage = errMsg;
			this.statusUrl = statusUrl;
			this.published = published;
			this.httpStatus = status;
		}

		public String getUid() {
			return uid;
		}

		public boolean isPublished() {
			return published;
		}

		public String getErrorMessage() {
			return errorMessage;
		}

		public HttpStatus getHttpStatus() {
			return httpStatus;
		}

		public URI getStatusUrl() {
			return statusUrl;
		}

		private String uid;

		private boolean published;

		private String errorMessage;

		private HttpStatus httpStatus;

		private URI statusUrl;

		public CompletableFuture<URI> checkProductionStatus() {
			Assert.notNull(this.executor, "the executor must not be null");
			return CompletableFuture.supplyAsync(this::doPollProductionStatus,
					this.executor);
		}

		@SneakyThrows
		private URI doPollProductionStatus() {
			// while (counter.incrementAndGet() < (maxWait / sleepInterval)) {
			while (true) {
				var result = this.template.getForEntity(this.statusUrl, Map.class);
				Assert.isTrue(result.getStatusCode().is2xxSuccessful(),
						"the HTTP request must return a valid 20x series HTTP status");
				var status = (Map<String, String>) result.getBody();
				// log.info("the status is " + status);
				var key = "media-url";
				if (status.containsKey(key)) {
					return URI.create(status.get(key));
				}
				else {
					var seconds = 10;
					Thread.sleep(seconds * 1000);
					log.debug("sleeping " + seconds
							+ "s while checking the requestProductionStatus of '"
							+ statusUrl + "'.");
				}
			}
			// return null;
		}

	}

}
