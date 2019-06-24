package podcast;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.shell.Availability;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellMethodAvailability;
import org.springframework.shell.standard.ShellOption;
import org.springframework.util.Assert;

import java.io.File;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static podcast.FileUtils.extensionFor;

@Log4j2
// @ShellComponent
@Deprecated
@RequiredArgsConstructor
class OldPodcastCommands {

	private static final String MEDIA_ARG = "--media";

	private static final String DESCRIPTION_ARG = "--description";

	private static final String INTERVIEW_ARG = "--interview-media";

	private static final String INTRODUCTION_ARG = "--intro-media";

	private final ApplicationEventPublisher publisher;

	private final ThreadLocal<Podcast> podcast = new ThreadLocal<>();

	private final AtomicBoolean packaged = new AtomicBoolean();

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
	public void newPodcast(@ShellOption(DESCRIPTION_ARG) String description) {
		this.podcast.set(new Podcast(description, UUID.randomUUID().toString()));
		this.publisher.publishEvent(new PodcastCreatedEvent(getPodcast()));
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
	public void publish() {
		if (!this.packaged.get()) {
			this.createPackage();
		}
		Assert.notNull(this.archive, "the archive must not be null");
		var uid = UUID.randomUUID().toString();
		var publishResponse = this.apiClient.publish(uid, this.archive);
		Assert.isTrue(publishResponse.isPublished(),
				"could not publish the package archive");
		publishResponse.checkProductionStatus().thenAccept(uri -> {
			log.info("the URI is " + uri.toString());
			publisher.publishEvent(new ProductionFinishedEvent(uri));
		});
		publisher.publishEvent(new ProductionStartedEvent(publishResponse));
	}

	@ShellMethod(value = "rush", key = "rush")
	public void rush(@ShellOption(DESCRIPTION_ARG) String description,
			@ShellOption(INTERVIEW_ARG) File interview,
			@ShellOption(INTRODUCTION_ARG) File introduction) {

		this.newPodcast(description);
		this.addInterviewMedia(interview);
		this.addIntroductionMedia(introduction);
		this.publish();
	}

	@ShellMethod(value = "package", key = "package")
	public void createPackage() {
		var ext = extensionFor(this.intro);
		Assert.notNull(this.interview, "the interview file must be specified");
		Assert.notNull(this.intro, "the introduction file must be specified");
		var aPackage = this.getPodcast()//
				.addMedia(ext, this.intro, this.interview)//
				.createPackage();

		publisher.publishEvent(new PackageCreatedEvent(aPackage));
	}

	@EventListener
	public void packageCreated(PackageCreatedEvent event) {
		this.archive = event.getSource();
		this.packaged.set(true);
		log.info("The podcast archive has been written to "
				+ event.getSource().getAbsolutePath());
	}

	private Podcast getPodcast() {
		return this.podcast.get();
	}

	private boolean isPodcastStarted() {
		return (getPodcast() != null);
	}

}
