package podcast;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.shell.Availability;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellMethodAvailability;
import org.springframework.shell.standard.ShellOption;
import org.springframework.util.Assert;

import java.io.File;
import java.util.Arrays;
import java.util.UUID;

import static podcast.FileUtils.extensionFor;

@Log4j2
@ShellComponent
@RequiredArgsConstructor
class PodcastCommands {

	private static final String MEDIA_ARG = "--media";

	private static final String DISCOVERY_ARG = "--description";

	private final ApplicationEventPublisher publisher;

	private final ThreadLocal<Podcast> podcast = new ThreadLocal<>();

	private File intro, interview, archive;

	private final ApiClient apiClient;

	public Availability newPodcastAvailabilityCheck() {
		return !isPodcastStarted() ? Availability.available()
				: Availability.unavailable("you're already producing a new podcast");
	}

	public Availability addMediaAvailabilityCheck() {
		return isPodcastStarted() ? Availability.available()
				: Availability.unavailable("you need to start a new podcast");
	}

	@ShellMethodAvailability("newPodcastAvailabilityCheck")
	@ShellMethod(value = "new podcast")
	public void newPodcast(@ShellOption(DISCOVERY_ARG) String description) {
		this.podcast.set(new Podcast(description, UUID.randomUUID().toString()));
		this.publisher.publishEvent(new PodcastStartedEvent(getPodcast()));
	}

	@ShellMethodAvailability(value = "addMediaAvailabilityCheck")
	@ShellMethod(value = "add introduction media")
	public void addIntroductionMedia(@ShellOption(MEDIA_ARG) File file) {
		Assert.isTrue(isValidArtifact(file), "you must provide a valid artifact");
		this.intro = file;
	}

	@ShellMethodAvailability(value = "addMediaAvailabilityCheck")
	@ShellMethod(value = "add interview media")
	public void addInterviewMedia(@ShellOption(MEDIA_ARG) File file) {
		Assert.isTrue(isValidArtifact(file), "you must provide a valid artifact");
		this.interview = file;
	}

	private boolean isValidArtifact(File f) {
		return (f != null && Arrays.asList("wav", "mp3").contains(extensionFor(f)));
	}

	@ShellMethod(value = "publish", key = "publish")
	public void publishForProcessing() {
		// todo this is where we would publish the pacakge to the integration endpoint
		// todo make sure to send a checksum as well
		Assert.notNull(this.archive, "the archive must not be null");
		var publishResponse = apiClient.publishPackage(this.archive);
		Assert.isTrue(publishResponse.isPublished(),
				"could not publish the package archive");
	}

	@ShellMethod(value = "package", key = "package")
	public void createPackage() {
		var ext = extensionFor(this.intro);

		Assert.notNull(this.interview, "the interview file must be specified");
		Assert.notNull(this.intro, "the introduction file must be specified");

		var aPackage = this.getPodcast().addMedia(ext, this.intro, this.interview)
				.createPackage();

		publisher.publishEvent(new PackageCreatedEvent(aPackage));
	}

	@EventListener
	public void packageCreated(PackageCreatedEvent event) {
		this.archive = event.getSource();
		System.out.println("The podcast archive has been written to "
				+ event.getSource().getAbsolutePath());
	}

	private Podcast getPodcast() {
		return this.podcast.get();
	}

	private boolean isPodcastStarted() {
		return (getPodcast() != null);
	}

}
