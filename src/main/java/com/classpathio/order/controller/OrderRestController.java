package com.classpathio.order.controller;

import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import com.classpathio.order.model.Order;
import com.classpathio.order.service.OrderService;
import lombok.RequiredArgsConstructor;


@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderRestController {
	
	private final OrderService orderService;
	
	@GetMapping
	public Set<Order> fetchAllOrders(){
		return this.orderService.fetchOrders();
	}
	
	@GetMapping("/{id}")
	public Order fetchOrderById(@PathVariable("id") long id) {
		return this.orderService.fetchOrderById(id);
	}
	
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public Order saveOrder(@RequestBody Order order) {
		return this.orderService.saveOrder(order);
	}
	
	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteOrderById(@PathVariable long id) {
		this.orderService.deleteOrderById(id);
	}

}
