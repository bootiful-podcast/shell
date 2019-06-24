package podcast;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@SpringBootApplication
public class PodcastShell {

	public static void main(String[] args) {
		SpringApplication.run(PodcastShell.class, args);
	}

	@Bean
	RestTemplate restTemplate(RestTemplateBuilder builder) {
		return builder.build();
	}

	@Bean
	Executor executor() {
		var maxThreads = Runtime.getRuntime().availableProcessors();
		return Executors
				.newScheduledThreadPool(maxThreads >= 2 ? maxThreads / 2 : maxThreads);
	}

	@Bean
	ApiClient apiClient(@Value("${podcast.api.url}") String url,
			RestTemplate restTemplate) {
		return new ApiClient(url, this.executor(), restTemplate);
	}

}
