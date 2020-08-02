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
	
	public static final String FAIL_ALLOCATION = "fail-allocation";
	public static final String PARTIAL_ALLOCATION = "partial-allocation";

	@JmsListener(destination = JmsConfig.ALLOCATE_ORDER_QUEUE)
	public void listen(Message msg) {

		AllocateOrderRequest request = (AllocateOrderRequest) msg.getPayload();
		boolean pendingInventory = false;
		boolean allocationError = false;
		boolean sendResponse = true;
		
		if(request.getBeerOrderDto().getCustomerRef() != null) {
			if(request.getBeerOrderDto().getCustomerRef().equals(FAIL_ALLOCATION)) {
				allocationError = true;
			} else if(request.getBeerOrderDto().getCustomerRef().equals(PARTIAL_ALLOCATION)) {
				pendingInventory = true;
			}
		}

		// trick to keep compiler happy
		final boolean finalPendingInventory = pendingInventory;
		if (!allocationError) {
			request.getBeerOrderDto().getBeerOrderLines().forEach(beerOrderLineDto -> {
				if(finalPendingInventory) {
					beerOrderLineDto.setQuantityAllocated(beerOrderLineDto.getOrderQuantity() - 1);
				} else {
					beerOrderLineDto.setQuantityAllocated(beerOrderLineDto.getOrderQuantity());
				}
			});
		}

		jmsTemplate.convertAndSend(JmsConfig.ALLOCATE_ORDER_RESPONSE_QUEUE,
				AllocateOrderResult.builder().pendingInventory(pendingInventory).allocationError(allocationError)
						.beerOrderDto(request.getBeerOrderDto()).build());

	}
}
