package guru.sfg.brewery.model.events;


import java.io.Serializable;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ValidateOrderResult implements Serializable {
	
	private UUID orderId;
	private Boolean isValid;

}
