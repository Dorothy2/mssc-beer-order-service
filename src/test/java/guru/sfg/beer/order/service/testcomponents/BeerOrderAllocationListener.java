package guru.sfg.beer.order.service.testcomponents;

import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import guru.sfg.beer.order.service.config.JmsConfig;
import guru.sfg.brewery.model.events.AllocateOrderRequest;
import guru.sfg.brewery.model.events.AllocateOrderResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Component
public class BeerOrderAllocationListener {
	
	private final JmsTemplate jmsTemplate;

	@JmsListener(destination = JmsConfig.ALLOCATE_ORDER_QUEUE)
	public void listen(Message msg) {

		AllocateOrderRequest request = (AllocateOrderRequest) msg.getPayload();
		boolean pendingInventory = false;
		boolean allocationError = false;
		boolean sendResponse = true;

		request.getBeerOrderDto().getBeerOrderLines().forEach(beerOrderLineDto -> {
			beerOrderLineDto.setQuantityAllocated(beerOrderLineDto.getOrderQuantity());
		});

		jmsTemplate.convertAndSend(JmsConfig.ALLOCATE_ORDER_RESPONSE_QUEUE,
				AllocateOrderResult.builder().pendingInventory(pendingInventory).allocationError(allocationError)
						.beerOrderDto(request.getBeerOrderDto()).build());

	}
}
