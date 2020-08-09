package guru.sfg.brewery.model.events;


import java.io.Serializable;

import guru.sfg.brewery.model.BeerOrderDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ValidateOrderRequest implements Serializable {
	
	private BeerOrderDto beerOrder;

	@Override
	public String toString() {
		return "ValidateOrderRequest [beerOrder=" + beerOrder + "]";
	}

}
