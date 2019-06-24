package podcast;

import org.springframework.context.ApplicationEvent;

class PodcastCreatedEvent extends ApplicationEvent {

	PodcastCreatedEvent(Podcast source) {
		super(source);
	}

	@Override
	public Podcast getSource() {
		return (Podcast) super.getSource();
	}

}
