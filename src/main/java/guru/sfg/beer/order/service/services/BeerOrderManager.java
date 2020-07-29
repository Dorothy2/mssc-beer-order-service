package guru.sfg.beer.order.service.services;

import guru.sfg.beer.order.service.domain.BeerOrder;

interface BeerOrderManager {
	
	BeerOrder newBeerOrder(BeerOrder beerOrder);
}
