
package guru.sfg.beer.order.service.services.beer;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.fasterxml.jackson.annotation.JsonFormat;

import guru.sfg.beer.order.service.domain.BeerOrder;
import guru.sfg.beer.order.service.domain.BeerOrderLine;
import guru.sfg.beer.order.service.web.mappers.BeerOrderLineMapper;
import guru.sfg.beer.order.service.web.mappers.BeerOrderMapper;
import guru.sfg.beer.order.service.web.model.BeerDto;
import guru.sfg.beer.order.service.web.model.BeerOrderLineDto;

@Disabled // utility for manual testing
@SpringBootTest
class BeerOrderMapperTest {
//	@Autowired
//BeerService beerService;
//	@Autowired
BeerOrderLineMapper mapper;
	
//	@Autowired
//	public void setBeerService(BeerService beerService) {
//		this.beerService = beerService;
//	}
	
	@Autowired
	public void setBeerOrderLineMapper(BeerOrderLineMapper mapper) {
		this.mapper = mapper;
	}
	
	public static final String BEER_1_UPC = "0631234200036";
    public static final String BEER_2_UPC = "0631234300019";
    public static final String BEER_3_UPC = "0083783375213";
    
    public static final UUID BEER_1_UUID = UUID.fromString("0a818933-087d-47f2-ad83-2f986ed087eb");
    public static final UUID BEER_2_UUID = UUID.fromString("a712d914-61ea-4623-8bd0-32c0f6545bfd");
    public static final UUID BEER_3_UUID = UUID.fromString("026cc3c8-3a0c-4083-a05b-e908048c1b08");
    


	@BeforeEach
	void setUp() throws Exception {
	}

	
	
//	@Test
//	@Disabled
//	void getBeerByUPCTest() {
//		Optional<BeerDto> dto = beerService.getBeerByUPC(BEER_2_UPC);
//		assertTrue(dto.isPresent(), "Beer service info not found.");
//
//		// BeerDto is coming from mssc-beer-service
//		// BeerInventoryBootstrap.java
//		System.out.println("BeerInfo: " + dto.toString());
//		BeerOrderLine line = createFakeOrderLine();
//		//public BeerOrderLineDto beerOrderLineToDto(BeerOrderLine line) {
//		BeerOrderLineDto orderLineDto = mapper.beerOrderLineToDto(line);
//        Optional<BeerDto> beerDtoOptional = beerService.getBeerByUPC(line.getUpc());
//
//        beerDtoOptional.ifPresent(beerDto -> {
//            orderLineDto.setBeerName(beerDto.getBeerName());
//            orderLineDto.setBeerStyle(beerDto.getBeerStyle());
//            orderLineDto.setPrice(beerDto.getPrice());
//            orderLineDto.setBeerId(beerDto.getId());
//            System.out.println("Updated order line DTO: " + orderLineDto.toString());
//        });
//	}
	
	@Test
	void mapperTest() {
		BeerDto dto = createFakeBeerDto();
		System.out.println("MapperTest BeerInfo: " + dto.toString());
		BeerOrderLine line = createFakeOrderLine();
		//public BeerOrderLineDto beerOrderLineToDto(BeerOrderLine line) {
		BeerOrderLineDto orderLineDto = mapper.beerOrderLineToDto(line);
		System.out.println("MapperTest Updated order line DTO: " + orderLineDto.toString());
 
	}
	
	private BeerOrderLine createFakeOrderLine() {
		UUID id = UUID.fromString("b9cbd370-4f4e-4798-a053-be38e0c4dd86");
		String upc = BEER_2_UPC;
		Long version = 0L;
		Timestamp createdDate = Timestamp.valueOf(LocalDateTime.now());
		Timestamp lastModifiedDate = createdDate;
		BeerOrder beerOrder = new BeerOrder();
		beerOrder.setId(UUID.randomUUID());
		UUID beerId = UUID.fromString("a712d914-61ea-4623-8bd0-32c0f6545bfd");
		Integer orderQuantity = 5;
		Integer quantityAllocated = 0;
		return (new BeerOrderLine(id, upc, version, createdDate, lastModifiedDate, beerOrder, beerId, orderQuantity,
				quantityAllocated));
	}
	
	private BeerDto createFakeBeerDto() {
		
		 BeerDto beerDto = BeerDto.builder()
	        		.beerName("Galaxy Cat")
	        		.beerStyle("PALE ALE")
	        		.createdDate(OffsetDateTime.now())
	        		.lastModifiedDate(OffsetDateTime.now())
	        		.price(new BigDecimal(7.99))
	        		.upc(BEER_2_UPC)
	        		.build();
		 return beerDto;
		
	}



}
