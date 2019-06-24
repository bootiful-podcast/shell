package podcast;

import lombok.extern.log4j.Log4j2;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;
import org.springframework.context.event.EventListener;
import org.springframework.shell.jline.PromptProvider;
import org.springframework.util.StringUtils;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;

@Log4j2
// @Component
@Deprecated
class OldPodcastPromptProvider implements PromptProvider {

	private Podcast podcast;

	private File introduction;

	private URI finalProductionArtifactURI;

	private File interview;

	private ApiClient.ProductionStatus productionStatus;

	@Override
	public AttributedString getPrompt() {

		String promptTerminal = " :>";

		if (podcast != null) {
			return new AttributedString(this.buildPromptForPodcast() + promptTerminal,
					AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW));
		}
		else {
			return new AttributedString("(no podcast created yet) " + promptTerminal,
					AttributedStyle.DEFAULT.foreground(AttributedStyle.RED));
		}
	}

	@EventListener
	public void handle(IntroductionFileEvent event) {
		this.introduction = event.getSource();
	}

	@EventListener
	public void handle(InterviewFileEvent event) {
		this.interview = event.getSource();
	}

	@EventListener
	public void handle(PodcastCreatedEvent event) {
		this.podcast = event.getSource();
	}

	@EventListener
	public void handle(ProductionStartedEvent pse) {
		this.productionStatus = pse.getSource();
	}

	@EventListener
	public void productionFinished(ProductionFinishedEvent event) {
		this.finalProductionArtifactURI = event.getSource();
		log.info("production has finished. "
				+ "The new artifact lives at the following URI "
				+ this.finalProductionArtifactURI);
	}

	private String buildPromptForPodcast() {
		String introductionLabel = "introduction: ", interviewLabel = "interview: ";
		String msg = "";
		if (podcast != null) {
			msg = podcast.getDescription() + " ";
			var list = new ArrayList<String>();

			addLabeledFileToPrompt(introductionLabel, list, this.introduction);
			addLabeledFileToPrompt(interviewLabel, list, this.interview);

			if (list.size() > 0) {
				var strings = list.toArray(new String[0]);
				var filesString = StringUtils.arrayToDelimitedString(strings, ", ");
				msg += "( " + filesString + " )";
			}
		}
		if (null != this.productionStatus) {
			msg = "Processing...";
		}

		if (null != this.finalProductionArtifactURI) {
			msg = "Finished! " + this.finalProductionArtifactURI.toString();
		}

		return msg;
	}

	private static void addLabeledFileToPrompt(String introductionLabel,
			ArrayList<String> list, File introduction) {
		if (null != introduction) {
			list.add(introductionLabel + introduction.getName());
		}
	}

}
