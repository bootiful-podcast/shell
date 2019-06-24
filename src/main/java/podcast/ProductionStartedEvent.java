package podcast;

import org.springframework.context.ApplicationEvent;

class ProductionStartedEvent extends ApplicationEvent {

	@Override
	public ApiClient.ProductionStatus getSource() {
		return ApiClient.ProductionStatus.class.cast(super.getSource());
	}

	public ProductionStartedEvent(ApiClient.ProductionStatus ps) {
		super(ps);
	}

}
