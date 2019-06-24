package podcast;

import org.springframework.context.ApplicationEvent;

import java.net.URI;

class ProductionFinishedEvent extends ApplicationEvent {

	ProductionFinishedEvent(URI finalArtifact) {
		super(finalArtifact);
	}

	@Override
	public URI getSource() {
		return (URI) super.getSource();
	}

}
