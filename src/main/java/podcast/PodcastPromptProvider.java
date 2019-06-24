package podcast;

import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;
import org.springframework.context.event.EventListener;
import org.springframework.shell.jline.PromptProvider;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicReference;

@Component
class PodcastPromptProvider implements PromptProvider {

	private final AtomicReference<String> status = new AtomicReference<>("");

	private final String introductionFileAddedMessage = "Introduction media added";

	private final String interviewFileAddedMessage = "An interview media file has been specified UID %s";

	private final String podcastProductionHasStartedMessage = "The podcast production process has started for UID %s";

	private final String podcastCreatedEventMessage = "A podcast has been initialized";

	private final String podcastProducedMessage = "A podcast has been produced at URI %s";

	@EventListener
	void handle(PodcastCreatedEvent event) {
		this.status.set(status(this.podcastCreatedEventMessage));

	}

	private String status(String msg) {
		System.out.println(msg);
		return msg;
	}

	@EventListener
	void handle(IntroductionFileEvent event) {
		this.status.set(status(this.introductionFileAddedMessage));
	}

	@EventListener
	void handle(InterviewFileEvent event) {
		this.status.set(status(this.interviewFileAddedMessage));
	}

	@EventListener
	void handle(ProductionStartedEvent pse) {
		this.status.set(status(this.podcastProductionHasStartedMessage));
	}

	@EventListener
	void handle(ProductionFinishedEvent event) {
		this.status.set(
				status(String.format(this.podcastProducedMessage, event.getSource())));
	}

	@Override
	public AttributedString getPrompt() {
		return new AttributedString(this.status.get(),
				AttributedStyle.DEFAULT.foreground(AttributedStyle.RED));
	}

}
