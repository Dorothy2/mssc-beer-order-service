package guru.sfg.beer.order.service.testcomponents;

import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import guru.sfg.beer.order.service.config.JmsConfig;
import guru.sfg.brewery.model.events.ValidateOrderRequest;
import guru.sfg.brewery.model.events.ValidateOrderResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Component
public class BeerOrderValidationListener {
	
	private final JmsTemplate jmsTemplate;
	
	public static final String FAIL_VALIDATION = "fail-validation";
	public static final String DO_NOT_VALIDATE = "do-not-validate";

	@JmsListener(destination = JmsConfig.VALIDATE_ORDER_QUEUE)
	public void listen(Message msg) {
		boolean isValid = true;

		ValidateOrderRequest request = (ValidateOrderRequest) msg.getPayload();

		if (request.getBeerOrder().getCustomerRef() != null
				&& (request.getBeerOrder().getCustomerRef().equals(DO_NOT_VALIDATE))) {
			;
			
		} else {
			if (request.getBeerOrder().getCustomerRef() != null
					&& request.getBeerOrder().getCustomerRef().equals(FAIL_VALIDATION)) {
				isValid = false;
			}
			jmsTemplate.convertAndSend(JmsConfig.VALIDATE_ORDER_RESPONSE_QUEUE,
					ValidateOrderResult.builder().isValid(isValid).orderId(request.getBeerOrder().getId()).build());
		}
	}
}
