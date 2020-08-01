package guru.sfg.beer.order.service.sm.actions;

import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.springframework.stereotype.Component;

import guru.sfg.beer.order.service.domain.BeerOrderEventEnum;
import guru.sfg.beer.order.service.domain.BeerOrderStatusEnum;
import guru.sfg.beer.order.service.services.BeerOrderManagerImpl;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class ValidationFailureAction implements Action<BeerOrderStatusEnum, BeerOrderEventEnum>{

	@Override
	public void execute(StateContext<BeerOrderStatusEnum, BeerOrderEventEnum> context) {
		String beerOrderId = (String) context.getMessage().getHeaders().get(BeerOrderManagerImpl.ORDER_ID_HEADER);
		log.error(String.format("Compensating Transaction -- Validation error for order id: %s", beerOrderId));
		
	}

}
