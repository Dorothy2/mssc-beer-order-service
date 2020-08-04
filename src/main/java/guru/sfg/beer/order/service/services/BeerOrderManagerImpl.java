package guru.sfg.beer.order.service.services;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.support.DefaultStateMachineContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import guru.sfg.beer.order.service.domain.BeerOrder;
import guru.sfg.beer.order.service.domain.BeerOrderEventEnum;
import guru.sfg.beer.order.service.domain.BeerOrderStatusEnum;
import guru.sfg.beer.order.service.repositories.BeerOrderRepository;
import guru.sfg.beer.order.service.sm.BeerOrderStateChangeInterceptor;
import guru.sfg.brewery.model.BeerOrderDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class BeerOrderManagerImpl implements BeerOrderManager {
	
	public static final String ORDER_ID_HEADER = "ORDER_ID_HEADER";
	private final StateMachineFactory<BeerOrderStatusEnum, BeerOrderEventEnum> stateMachineFactory;
	private final BeerOrderRepository beerOrderRepository;
	private final BeerOrderStateChangeInterceptor beerOrderStatusChangeInterceptor;
	

	@Transactional
	@Override
	public BeerOrder newBeerOrder(BeerOrder beerOrder) {
		// defensive coding
		beerOrder.setId(null);
		beerOrder.setOrderStatus(BeerOrderStatusEnum.NEW);
		BeerOrder savedBeerOrder = beerOrderRepository.saveAndFlush(beerOrder);
		sendBeerOrderEvent(savedBeerOrder, BeerOrderEventEnum.VALIDATE_ORDER);
		return beerOrder;
	}
	
	@Transactional
	@Override
	public void processValidationResult(UUID beerOrderId, Boolean isValid) {
		log.debug("Process validation result for beerOrderId: " + beerOrderId + " Valid? " + isValid);
		
		// BeerOrder beerOrder = beerOrderRepository.getOne(beerOrderId);
		Optional<BeerOrder> beerOrderOptional = beerOrderRepository.findById(beerOrderId);

		beerOrderOptional.ifPresentOrElse(beerOrder -> {
			if (isValid) {
				sendBeerOrderEvent(beerOrder, BeerOrderEventEnum.VALIDATION_PASSED);
				 //wait for status change
                awaitForStatus(beerOrderId, BeerOrderStatusEnum.VALIDATED);
				
				BeerOrder validatedOrder = beerOrderRepository.findById(beerOrderId).get();
				sendBeerOrderEvent(validatedOrder, BeerOrderEventEnum.ALLOCATE_ORDER);

			} else {
				sendBeerOrderEvent(beerOrder, BeerOrderEventEnum.VALIDATION_FAILED);
			}
		}, () -> log.error("Order Not found. Id: " + beerOrderId));
	}
	
	public void pickUpOrder(UUID orderId) {
		Optional<BeerOrder> beerOrderOptional = beerOrderRepository.findById(orderId);
		beerOrderOptional.ifPresentOrElse(beerOrder -> {
			sendBeerOrderEvent(beerOrder, BeerOrderEventEnum.BEER_ORDER_PICKED_UP);
	  	}, () -> log.error("Order Not found. Id: " + orderId));
	}
	
	public void cancelOrder(UUID orderId) {
		Optional<BeerOrder> beerOrderOptional = beerOrderRepository.findById(orderId);
		beerOrderOptional.ifPresentOrElse(beerOrder -> {
			sendBeerOrderEvent(beerOrder, BeerOrderEventEnum.CANCEL_ORDER);
	  	}, () -> log.error("Order Not found. Id: " + orderId));
	}
	
	private void sendBeerOrderEvent(BeerOrder beerOrder, BeerOrderEventEnum event) {
		StateMachine<BeerOrderStatusEnum, BeerOrderEventEnum> sm = build(beerOrder);
		
		Message msg = MessageBuilder.withPayload(event)
			.setHeader(ORDER_ID_HEADER, beerOrder.getId().toString())
			.build();
		
		sm.sendEvent(msg);
	}
	
	private StateMachine<BeerOrderStatusEnum, BeerOrderEventEnum> build(BeerOrder beerOrder) {
		StateMachine<BeerOrderStatusEnum, BeerOrderEventEnum> sm = stateMachineFactory.getStateMachine(beerOrder.getId());
		
		sm.stop();
		
		sm.getStateMachineAccessor()
			.doWithAllRegions(sma -> {
				sma.addStateMachineInterceptor(beerOrderStatusChangeInterceptor);
				sma.resetStateMachine(new DefaultStateMachineContext<>(beerOrder.getOrderStatus(), null, null, null));
			});
		
		sm.start();
		return sm;
	}

	@Override
	public void beerOrderAllocationPassed(BeerOrderDto beerOrderDto) {
		Optional<BeerOrder> beerOrderOptional = beerOrderRepository.findById(beerOrderDto.getId());
		beerOrderOptional.ifPresentOrElse(beerOrder -> {
			sendBeerOrderEvent(beerOrder, BeerOrderEventEnum.ALLOCATION_SUCCESS);
			 //wait for status change
            awaitForStatus(beerOrder.getId(), BeerOrderStatusEnum.ALLOCATED);
			updateAllocatedQty(beerOrderDto);
		}, () -> log.error("Order Not found. Id: " + beerOrderDto.getId()));
	}

	@Override
	public void beerOrderAllocationPendingInventory(BeerOrderDto beerOrderDto) {
		Optional<BeerOrder> beerOrderOptional = beerOrderRepository.findById(beerOrderDto.getId());
		beerOrderOptional.ifPresentOrElse(beerOrder -> {
			sendBeerOrderEvent(beerOrder, BeerOrderEventEnum.ALLOCATION_NO_INVENTORY);
			 //wait for status change
            awaitForStatus(beerOrder.getId(), BeerOrderStatusEnum.PENDING_INVENTORY);
			updateAllocatedQty(beerOrderDto);
		}, () -> log.error("Order Not found. Id: " + beerOrderDto.getId()));
	}

	@Override
	public void beerOrderAllocationFailed(BeerOrderDto beerOrderDto) {
		Optional<BeerOrder> beerOrderOptional = beerOrderRepository.findById(beerOrderDto.getId());
		beerOrderOptional.ifPresentOrElse(beerOrder -> {
			sendBeerOrderEvent(beerOrder, BeerOrderEventEnum.ALLOCATION_FAILED);
		}, () -> log.error("Order Not found. Id: " + beerOrderDto.getId()));

	}

	private void awaitForStatus(UUID beerOrderId, BeerOrderStatusEnum statusEnum) {

		AtomicBoolean found = new AtomicBoolean(false);
		AtomicInteger loopCount = new AtomicInteger(0);

		while (!found.get()) {
			if (loopCount.incrementAndGet() > 10) {
				found.set(true);
				log.debug("Loop Retries exceeded");
			}

			beerOrderRepository.findById(beerOrderId).ifPresentOrElse(beerOrder -> {
				if (beerOrder.getOrderStatus().equals(statusEnum)) {
					found.set(true);
					log.debug("Order Found");
				} else {
					log.debug("Order Status Not Equal. Expected: " + statusEnum.name() + " Found: "
							+ beerOrder.getOrderStatus().name());
				}
			}, () -> {
				log.debug("Order Id Not Found");
			});

			if (!found.get()) {
				try {
					log.debug("Sleeping for retry");
					Thread.sleep(100);
				} catch (Exception e) {
					// do nothing
				}
			}
		}
	}
	
	private void updateAllocatedQty(BeerOrderDto beerOrderDto) {
		Optional<BeerOrder> allocatedOrderOptional = beerOrderRepository.findById(beerOrderDto.getId());

		allocatedOrderOptional.ifPresentOrElse(allocatedOrder -> {
			allocatedOrder.getBeerOrderLines().forEach(beerOrderLine -> {
				beerOrderDto.getBeerOrderLines().forEach(beerOrderLineDto -> {
					if (beerOrderLine.getId().equals(beerOrderLineDto.getId())) {
						beerOrderLine.setQuantityAllocated(beerOrderLineDto.getQuantityAllocated());
					}
				});
			});

			beerOrderRepository.saveAndFlush(allocatedOrder);
		}, () -> log.error("Order Not found. Id: " + beerOrderDto.getId()));
	}

}
