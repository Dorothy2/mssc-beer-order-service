package guru.sfg.beer.order.service.sm;

import java.util.Optional;
import java.util.UUID;

import org.springframework.messaging.Message;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.state.State;
import org.springframework.statemachine.support.StateMachineInterceptorAdapter;
import org.springframework.statemachine.transition.Transition;
import org.springframework.stereotype.Component;

import guru.sfg.beer.order.service.domain.BeerOrder;
import guru.sfg.beer.order.service.domain.BeerOrderEventEnum;
import guru.sfg.beer.order.service.domain.BeerOrderStatusEnum;
import guru.sfg.beer.order.service.repositories.BeerOrderRepository;
import guru.sfg.beer.order.service.services.BeerOrderManagerImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class BeerOrderStatusChangeInterceptor extends StateMachineInterceptorAdapter<BeerOrderStatusEnum, BeerOrderEventEnum> {
	
	private final BeerOrderRepository beerOrderRepository;
	
	@Override
	public void preStateChange(State<BeerOrderStatusEnum, BeerOrderEventEnum> state, Message<BeerOrderEventEnum> message,
			Transition<BeerOrderStatusEnum, BeerOrderEventEnum> transition, StateMachine<BeerOrderStatusEnum, BeerOrderEventEnum> stateMachine ) {
	
//		Optional.ofNullable(message).ifPresent(msg -> {
//			Optional.ofNullable(UUID.class.cast(msg.getHeaders().getOrDefault(BeerOrderManagerImpl.BEER_ORDER_ID_HEADER, -1L)))
//		    .ifPresent(beerOrderId -> {
//		    	// fetch payment from db
//		    	BeerOrder beerOrder =  beerOrderRepository.getOne(beerOrderId);
//		    	System.out.println("State before: " + beerOrder.getOrderStatus());
//		    	System.out.println("State argument: " + state.getId());
//		    	beerOrder.setOrderStatus(state.getId());
//		    	System.out.println("State after: " + beerOrder.getOrderStatus());
//		    	// update payment status
//		    	beerOrderRepository.save(beerOrder);
//		    });
		
		Optional.ofNullable(message)
			.flatMap(msg -> Optional.ofNullable((String) msg.getHeaders().getOrDefault(BeerOrderManagerImpl.ORDER_ID_HEADER, " ")))
		    .ifPresent(orderId -> {
		    log.debug(String.format("Saving state for order id: %s Status %s", orderId, state.getId()));
	    	// fetch payment from db
	    	BeerOrder beerOrder =  beerOrderRepository.getOne(UUID.fromString(orderId));
	    	System.out.println("State before: " + beerOrder.getOrderStatus());
	    	System.out.println("State argument: " + state.getId());
	    	beerOrder.setOrderStatus(state.getId());
	    	System.out.println("State after: " + beerOrder.getOrderStatus());
	    	// update payment status
	    	// by default, Hibernate does a lazy write, saveAndFlush forces it to go to db right away
	    	beerOrderRepository.saveAndFlush(beerOrder);
	    });
		
		
		//});
	}

}
