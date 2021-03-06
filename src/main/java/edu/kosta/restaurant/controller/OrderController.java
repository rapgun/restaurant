package edu.kosta.restaurant.controller;

import edu.kosta.restaurant.domain.Order;
import edu.kosta.restaurant.domain.OrderDishes;
import edu.kosta.restaurant.dto.OrderDishesInsertRequest;
import edu.kosta.restaurant.dto.OrderDishesUpdateRequest;
import edu.kosta.restaurant.dto.OrderInsertRequest;
import edu.kosta.restaurant.dto.OrderUpdateRequest;
import edu.kosta.restaurant.mapper.OrderDishesMapper;
import edu.kosta.restaurant.mapper.OrderMapper;
import edu.kosta.restaurant.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@RestController
@RequestMapping("/orders")
public class OrderController {

    @Autowired
    OrderService orderService;

    @Autowired
    CookService cookService;

    @Autowired
    TabletService tabletService;

    @Autowired
    DishService dishService;

    @Autowired
    OrderDishesService orderDishesService;

    @GetMapping
    public List<Order> getList(@RequestParam(required = false, defaultValue = "false") Boolean current) {
        if (current) return orderService.findAllByStateIn();
        else return orderService.findAll();
    }

    @GetMapping("/{id}")
    public Optional<Order> getList(@PathVariable("id") long id) {
        return orderService.findById(id);
    }

    @PostMapping
    public Order setOrder(@Valid @RequestBody OrderInsertRequest request) {
        Order order = OrderMapper.INSTANCE.insertRequestToOrder(request);
        order.setState(Order.State.READY);

        Order result = orderService.saveOrder(order);
        List<OrderDishesInsertRequest> dishes = request.getDishes();

        for (OrderDishesInsertRequest orderDish : dishes) {
            OrderDishes orderDishes = OrderDishesMapper.INSTANCE.insertRequestToOrderDishes(orderDish);
            orderDishes.setOrders(result);
            orderDishesService.saveOrderDishes(orderDishes);
        }

        return result;
    }

    @PutMapping("/{id}")
    public Order setOrder(@PathVariable("id") long id, @Valid @RequestBody OrderUpdateRequest request) {
        Order order = orderService.findById(id).orElseThrow(NoSuchElementException::new);

        // cooks, tablets ?????? ???????????? ?????? ????????? ????????? ???????????? ???????????? ?????? ????????????
        // ??????, mapstruct??? enum type??? ???????????? ?????? ???????????? ?????????????????? ??????
        // ????????? ????????? ???????????? mapstruct??? ?????? ??????
        order.setCooks(cookService.findById(request.getCookId()).orElseThrow(NoSuchElementException::new));
        order.setTablets(tabletService.findById(request.getTabletId()).orElseThrow(NoSuchElementException::new));
        order.setState(request.getState());

        orderService.saveOrder(order);

        List<OrderDishesUpdateRequest> dishes = request.getDishes();

        // ????????????
        for (OrderDishes orderDishes : order.getOrderDishes()) {
            orderDishesService.deleteOrderDishes(orderDishes);
        }

        // ?????????
        for (OrderDishesUpdateRequest orderDish : dishes) {
            OrderDishes orderDishes = new OrderDishes();
            orderDishes.setOrders(order);
            orderDishes.setDishes(dishService.findById(orderDish.getDishId()).orElseThrow(NoSuchElementException::new));
            orderDishes.setQuantity(orderDish.getQuantity());
            orderDishesService.saveOrderDishes(orderDishes);
        }

        return order;
    }

    @PutMapping("/{id}/state")
    public Order setOrderState(@PathVariable("id") long id, @RequestBody Order request) {
        Order order = orderService.findById(id).orElseThrow(NoSuchElementException::new);
        order.setState(request.getState());

        orderService.saveOrder(order);

        return order;
    }

    @DeleteMapping("/{id}")
    public void deleteCook(@PathVariable("id") long id) {
        Order order = orderService.findById(id).orElseThrow(NoSuchElementException::new);
        orderService.deleteOrder(order);
    }
}
