package guru.sfg.beer.order.service;

import static com.github.jenspiegsa.wiremockextension.ManagedWireMockServer.with;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jms.core.JmsTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jenspiegsa.wiremockextension.WireMockExtension;
import com.github.tomakehurst.wiremock.WireMockServer;

import guru.sfg.beer.order.service.config.JmsConfig;
import guru.sfg.beer.order.service.domain.BeerOrder;
import guru.sfg.beer.order.service.domain.BeerOrderLine;
import guru.sfg.beer.order.service.domain.BeerOrderStatusEnum;
import guru.sfg.beer.order.service.domain.Customer;
import guru.sfg.beer.order.service.repositories.BeerOrderRepository;
import guru.sfg.beer.order.service.repositories.CustomerRepository;
import guru.sfg.beer.order.service.services.BeerOrderManager;
import guru.sfg.beer.order.service.services.BeerOrderService;
import guru.sfg.beer.order.service.services.beer.BeerServiceImpl;
import guru.sfg.brewery.model.BeerDto;
import guru.sfg.brewery.model.events.AllocationFailureEvent;

@ExtendWith(WireMockExtension.class)
@SpringBootTest
class BeerOrderManagerImpIT {
	
	private static final String BeerOrderValidationListener = null;

	@Autowired
	BeerOrderManager beerOrderManager;
	
	@Autowired
	BeerOrderRepository beerOrderRepository;
	
	@Autowired
	CustomerRepository customerRepository;
	
	@Autowired
	ObjectMapper objectMapper;
	
	@Autowired
	WireMockServer wireMockServer;
	
	@Autowired
	BeerOrderService beerOrderService;
	
	@Autowired
	JmsTemplate jmsTemplate;
	
	Customer testCustomer;
	
	UUID beerId = UUID.randomUUID();
	
	@TestConfiguration
	static class RestTemplateBuilderProvider {
	
		@Bean(destroyMethod = "stop")
		public WireMockServer wireMockServer() {
			WireMockServer server = with(wireMockConfig()
					.port(8083));
					// for additional logging; default is turned off
					// Provide an alternative notifier. The default logs to slf4j.
					//.notifier(new ConsoleNotifier(true)));
			server.start();
			return server;
		}
	}
	
	@BeforeEach
	void setUp() throws Exception {
		testCustomer = customerRepository.save(Customer.builder()
				.customerName("Test Customer")
				.build());
	}

	@Test
	void testNewToAllocated() throws InterruptedException, JsonProcessingException {
		
		BeerDto beerDto = BeerDto.builder().id(beerId).upc("12345").build();
				
		wireMockServer.stubFor(get(BeerServiceImpl.BEER_UPC_PATH_V1 + "12345")
			.willReturn(okJson(objectMapper.writeValueAsString(beerDto))));
		BeerOrder beerOrder = createBeerOrder();
		
		BeerOrder savedBeerOrder = beerOrderManager.newBeerOrder(beerOrder);
		
		await().untilAsserted(() -> {
			BeerOrder foundOrder = beerOrderRepository.findById(beerOrder.getId()).get();
			assertEquals(BeerOrderStatusEnum.ALLOCATED, foundOrder.getOrderStatus());
		});
		
		await().untilAsserted(() -> {
			BeerOrder foundOrder = beerOrderRepository.findById(beerOrder.getId()).get();
			BeerOrderLine line = foundOrder.getBeerOrderLines().iterator().next();
			assertEquals(line.getOrderQuantity(), line.getQuantityAllocated());
		});
			
		BeerOrder savedBeerOrder2 = beerOrderRepository.findById(savedBeerOrder.getId()).get();
		assertNotNull(savedBeerOrder2);
		assertEquals(BeerOrderStatusEnum.ALLOCATED, savedBeerOrder2.getOrderStatus());
	}
	
	@Test
	void testNewToPickedUp() throws InterruptedException, JsonProcessingException {
		
		BeerDto beerDto = BeerDto.builder().id(beerId).upc("12345").build();
				
		wireMockServer.stubFor(get(BeerServiceImpl.BEER_UPC_PATH_V1 + "12345")
			.willReturn(okJson(objectMapper.writeValueAsString(beerDto))));
		BeerOrder beerOrder = createBeerOrder();
		
		BeerOrder savedBeerOrder = beerOrderManager.newBeerOrder(beerOrder);
		
		await().untilAsserted(() -> {
			BeerOrder foundOrder = beerOrderRepository.findById(beerOrder.getId()).get();
			assertEquals(BeerOrderStatusEnum.ALLOCATED, foundOrder.getOrderStatus());
		});
		
		await().untilAsserted(() -> {
			BeerOrder foundOrder = beerOrderRepository.findById(beerOrder.getId()).get();
			BeerOrderLine line = foundOrder.getBeerOrderLines().iterator().next();
			assertEquals(line.getOrderQuantity(), line.getQuantityAllocated());
		});
		
		beerOrderService.pickupOrder(testCustomer.getId(), beerOrder.getId());
		await().untilAsserted(() -> {
			BeerOrder foundOrder = beerOrderRepository.findById(beerOrder.getId()).get();
			assertEquals(BeerOrderStatusEnum.PICKED_UP, foundOrder.getOrderStatus());
		});
			
		BeerOrder savedBeerOrder2 = beerOrderRepository.findById(savedBeerOrder.getId()).get();
		assertNotNull(savedBeerOrder2);
		assertEquals(BeerOrderStatusEnum.PICKED_UP, savedBeerOrder2.getOrderStatus());
	}
	
	@Test
	void testValidationPendingToCancelled() throws JsonProcessingException {
		BeerDto beerDto = BeerDto.builder().id(beerId).upc("12345").build();
		wireMockServer.stubFor(get(BeerServiceImpl.BEER_UPC_PATH_V1 + "12345")
				.willReturn(okJson(objectMapper.writeValueAsString(beerDto))));
		BeerOrder beerOrder = createBeerOrder();
		beerOrder.setCustomerRef(guru.sfg.beer.order.service.testcomponents.BeerOrderValidationListener.DO_NOT_VALIDATE);
		BeerOrder savedBeerOrder = beerOrderManager.newBeerOrder(beerOrder);
		
		BeerOrder savedBeerOrder2 = beerOrderRepository.findById(savedBeerOrder.getId()).get();
		assertNotNull(savedBeerOrder2);
		assertEquals(BeerOrderStatusEnum.VALIDATION_PENDING, savedBeerOrder2.getOrderStatus());
		
		beerOrderService.cancelOrder(beerOrder.getId());
		await().untilAsserted(() -> {
			BeerOrder foundOrder = beerOrderRepository.findById(beerOrder.getId()).get();
			assertEquals(BeerOrderStatusEnum.CANCELLED, foundOrder.getOrderStatus());
		});
	}
	
	@Test
	void testFailedValidation() throws JsonProcessingException {
		BeerDto beerDto = BeerDto.builder().id(beerId).upc("12345").build();
		
		wireMockServer.stubFor(get(BeerServiceImpl.BEER_UPC_PATH_V1 + "12345")
				.willReturn(okJson(objectMapper.writeValueAsString(beerDto))));
		BeerOrder beerOrder = createBeerOrder();
		beerOrder.setCustomerRef(guru.sfg.beer.order.service.testcomponents.BeerOrderAllocationListener.FAIL_ALLOCATION);
		BeerOrder savedBeerOrder = beerOrderManager.newBeerOrder(beerOrder);
		
		await().untilAsserted(() -> {
			BeerOrder faultyOrder = beerOrderRepository.findById(savedBeerOrder.getId()).get();
			assertEquals(BeerOrderStatusEnum.ALLOCATION_EXCEPTION, faultyOrder.getOrderStatus());
		});
		
		BeerOrder savedBeerOrder2 = beerOrderRepository.findById(beerOrder.getId()).get();
		assertEquals(BeerOrderStatusEnum.ALLOCATION_EXCEPTION, savedBeerOrder2.getOrderStatus() );
	}
	
	@Test
	void testPartialValidation() throws JsonProcessingException {
	BeerDto beerDto = BeerDto.builder().id(beerId).upc("12345").build();
		
		wireMockServer.stubFor(get(BeerServiceImpl.BEER_UPC_PATH_V1 + "12345")
				.willReturn(okJson(objectMapper.writeValueAsString(beerDto))));
		BeerOrder beerOrder = createBeerOrder();
		beerOrder.setCustomerRef(guru.sfg.beer.order.service.testcomponents.BeerOrderAllocationListener.PARTIAL_ALLOCATION);
		BeerOrder savedBeerOrder = beerOrderManager.newBeerOrder(beerOrder);
		
		await().untilAsserted(() -> {
			BeerOrder faultyOrder = beerOrderRepository.findById(savedBeerOrder.getId()).get();
			assertEquals(BeerOrderStatusEnum.PENDING_INVENTORY, faultyOrder.getOrderStatus());
		});
		
		BeerOrder savedBeerOrder2 = beerOrderRepository.findById(beerOrder.getId()).get();
		assertEquals(BeerOrderStatusEnum.PENDING_INVENTORY, savedBeerOrder2.getOrderStatus() );
	}
	
	@Test
	void testAllocationPendingToCancelled() throws JsonProcessingException {
		BeerDto beerDto = BeerDto.builder().id(beerId).upc("12345").build();
		wireMockServer.stubFor(get(BeerServiceImpl.BEER_UPC_PATH_V1 + "12345")
				.willReturn(okJson(objectMapper.writeValueAsString(beerDto))));
		BeerOrder beerOrder = createBeerOrder();
		beerOrder.setCustomerRef(guru.sfg.beer.order.service.testcomponents.BeerOrderAllocationListener.DO_NOT_ALLOCATE);
		BeerOrder savedBeerOrder = beerOrderManager.newBeerOrder(beerOrder);
		
		await().untilAsserted(() -> {
			BeerOrder savedBeerOrder2 = beerOrderRepository.findById(beerOrder.getId()).get();
			assertEquals(BeerOrderStatusEnum.ALLOCATION_PENDING, savedBeerOrder2.getOrderStatus());
		});
		
		beerOrderService.cancelOrder(savedBeerOrder.getId());
		await().untilAsserted(() -> {
			BeerOrder foundOrder = beerOrderRepository.findById(beerOrder.getId()).get();
			assertEquals(BeerOrderStatusEnum.CANCELLED, foundOrder.getOrderStatus());
		});
		
		savedBeerOrder = beerOrderRepository.findById(beerOrder.getId()).get();
		assertEquals(BeerOrderStatusEnum.CANCELLED, savedBeerOrder.getOrderStatus() );
	}
	
	@Test
	void testAllocatedToCancelled() throws JsonProcessingException {
		BeerDto beerDto = BeerDto.builder().id(beerId).upc("12345").build();
		
		wireMockServer.stubFor(get(BeerServiceImpl.BEER_UPC_PATH_V1 + "12345")
			.willReturn(okJson(objectMapper.writeValueAsString(beerDto))));
		BeerOrder beerOrder = createBeerOrder();
		
		BeerOrder savedBeerOrder = beerOrderManager.newBeerOrder(beerOrder);
		
		await().untilAsserted(() -> {
			BeerOrder foundOrder = beerOrderRepository.findById(beerOrder.getId()).get();
			assertEquals(BeerOrderStatusEnum.ALLOCATED, foundOrder.getOrderStatus());
		});
			
		BeerOrder savedBeerOrder2 = beerOrderRepository.findById(savedBeerOrder.getId()).get();
		beerOrderService.cancelOrder(savedBeerOrder2.getId());
		await().untilAsserted(() -> {
			BeerOrder foundOrder = beerOrderRepository.findById(beerOrder.getId()).get();
			assertEquals(BeerOrderStatusEnum.CANCELLED, foundOrder.getOrderStatus());
		});
		
		BeerOrder savedBeerOrder3 = beerOrderRepository.findById(savedBeerOrder2.getId()).get();
		assertEquals(BeerOrderStatusEnum.CANCELLED, savedBeerOrder3.getOrderStatus() );
	}
	
	@Test
	void testAllocationFailed() throws JsonProcessingException {
		BeerDto beerDto = BeerDto.builder().id(beerId).upc("12345").build();
		
		wireMockServer.stubFor(get(BeerServiceImpl.BEER_UPC_PATH_V1 + "12345")
				.willReturn(okJson(objectMapper.writeValueAsString(beerDto))));
		BeerOrder beerOrder = createBeerOrder();
		beerOrder.setCustomerRef(guru.sfg.beer.order.service.testcomponents.BeerOrderAllocationListener.FAIL_ALLOCATION);
		BeerOrder savedBeerOrder = beerOrderManager.newBeerOrder(beerOrder);
		System.out.println("Beer order id: " + savedBeerOrder.getId() + " status: " + savedBeerOrder.getCustomerRef());
		
		await().untilAsserted(() -> {
			BeerOrder faultyOrder = beerOrderRepository.findById(savedBeerOrder.getId()).get();
			assertEquals(BeerOrderStatusEnum.ALLOCATION_EXCEPTION, faultyOrder.getOrderStatus());
		});
				
        AllocationFailureEvent allocationFailureEvent = (AllocationFailureEvent) jmsTemplate.receiveAndConvert(JmsConfig.ALLOCATE_FAILURE_QUEUE);

        System.out.println("Beer order id from failure queue: " + allocationFailureEvent.getOrderId());
        assertNotNull(allocationFailureEvent);
        //assertThat(allocationFailureEvent.getOrderId()).isEqualTo(savedBeerOrder.getId());	
        assertEquals(allocationFailureEvent.getOrderId(), savedBeerOrder.getId());
   }
	
	public BeerOrder createBeerOrder() {
		BeerOrder beerOrder = BeerOrder.builder()
			.customer(testCustomer)
			.build();
		
		Set<BeerOrderLine> lines = new HashSet<>();
		lines.add(BeerOrderLine.builder()
			.beerId(beerOrder.getId())
			.orderQuantity(1)
			.upc("12345")
			.beerOrder(beerOrder)
			.build());
		
		beerOrder.setBeerOrderLines(lines);
		
		return beerOrder;
	}

}
