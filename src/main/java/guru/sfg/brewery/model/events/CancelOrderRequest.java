package guru.sfg.brewery.model.events;


import java.io.Serializable;
import java.util.UUID;

import guru.sfg.brewery.model.BeerOrderDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CancelOrderRequest implements Serializable {
	
	private BeerOrderDto beerOrderDto;
	private UUID orderId;

}
