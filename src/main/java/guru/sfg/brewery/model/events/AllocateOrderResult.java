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
public class AllocateOrderResult implements Serializable {
	
	private BeerOrderDto beerOrderDto;
	private Boolean allocationError = false;
	private Boolean pendingInventory = false;

}
