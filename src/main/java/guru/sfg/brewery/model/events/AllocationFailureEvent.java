package guru.sfg.brewery.model.events;


import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created by jt on 2/26/20.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AllocationFailureEvent {
    private UUID orderId;
}
