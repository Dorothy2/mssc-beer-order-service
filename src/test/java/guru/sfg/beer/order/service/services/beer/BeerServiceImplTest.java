package guru.sfg.beer.order.service.services.beer;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import guru.sfg.brewery.model.BeerDto;

@Disabled // utility for manual testing
@SpringBootTest
class BeerServiceImplTest {
	
	BeerService beerService;
	
	@Autowired
	public void setBeerService(BeerService beerService) {
		this.beerService = beerService;
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

	@Test
	void getBeerByIdTest() {
		Optional<BeerDto> dto = beerService.getBeerById(BEER_1_UUID);
		assertTrue(dto.isPresent(), "Beer service info not found.");

		// BeerDto is coming from mssc-beer-service
		// BeerInventoryBootstrap.java
		System.out.println("BeerInfo: " + dto.toString());
	}
	
	@Test
	void getBeerByUPCTest() {
		Optional<BeerDto> dto = beerService.getBeerByUPC(BEER_2_UPC);
		assertTrue(dto.isPresent(), "Beer service info not found.");

		// BeerDto is coming from mssc-beer-service
		// BeerInventoryBootstrap.java
		System.out.println("BeerInfo: " + dto.toString());
	}


}
