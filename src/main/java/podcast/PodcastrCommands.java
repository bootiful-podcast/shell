package podcast;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;
import org.springframework.util.Assert;

import java.io.File;
import java.util.Arrays;
import java.util.UUID;

import static podcast.FileUtils.extensionFor;

@Log4j2
@ShellComponent
@RequiredArgsConstructor
class PodcastrCommands {

	private static final String DESCRIPTION_ARG = "--description";

	private static final String INTERVIEW_ARG = "--interview-media";

	private static final String INTRODUCTION_ARG = "--intro-media";

	private final ApplicationEventPublisher publisher;

	private final ApiClient apiClient;

	private boolean isValidMediaFile(File f) {
		return (f != null && Arrays.asList("wav", "mp3").contains(extensionFor(f)));
	}

	@ShellMethod(value = "produce", key = "produce")
	public void produce(@ShellOption(DESCRIPTION_ARG) String description,
			@ShellOption(INTERVIEW_ARG) File interview,
			@ShellOption(INTRODUCTION_ARG) File introduction) {

		var podcast = new Podcast(description, UUID.randomUUID().toString());

		this.publisher.publishEvent(new PodcastCreatedEvent(podcast));

		Assert.isTrue(this.isValidMediaFile(interview),
				"you must provide a valid artifact");
		Assert.isTrue(this.isValidMediaFile(introduction),
				"you must provide a valid artifact");

		var ext = extensionFor(introduction);
		var aPackage = podcast.addMedia(ext, introduction, interview).createPackage();

		publisher.publishEvent(new PackageCreatedEvent(aPackage));

		Assert.notNull(aPackage, "the archive must not be null");

		var uid = UUID.randomUUID().toString();
		var publishResponse = this.apiClient.publish(uid, aPackage);

		Assert.isTrue(publishResponse.isPublished(),
				"could not publish the package archive");

		publishResponse.checkProductionStatus().thenAccept(uri -> {
			log.info("the URI is " + uri.toString());
			publisher.publishEvent(new ProductionFinishedEvent(uri));
		});
		publisher.publishEvent(new ProductionStartedEvent(publishResponse));
	}

}
