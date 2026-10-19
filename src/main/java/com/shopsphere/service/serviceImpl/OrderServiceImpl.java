package com.shopsphere.service.serviceImpl;

import com.shopsphere.constant.OrderStatus;
import com.shopsphere.constant.Role;
import com.shopsphere.dto.OrderResponseDto;
import com.shopsphere.dto.OrderStatusUpdateRequestDto;
import com.shopsphere.entity.*;
import com.shopsphere.exception.AccessDeniedException;
import com.shopsphere.exception.OrderNotFoundException;
import com.shopsphere.exception.OrderStatusException;
import com.shopsphere.mapper.CartMapper;
import com.shopsphere.mapper.OrderMapper;
import com.shopsphere.mapper.UserMapper;
import com.shopsphere.repository.*;
import com.shopsphere.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final UserMapper userMapper;
    private final CartMapper cartMapper;
    private final OrderMapper orderMapper;

    private void validateOrderStatus(OrderStatus currentStatus, OrderStatus newStatus){
        switch (currentStatus){
            case PLACED:
                if(newStatus!=OrderStatus.CONFIRMED
                        && newStatus!=OrderStatus.CANCELLED){
                    throw new OrderStatusException(
                            "Cannot change order status from"+currentStatus+" to "+newStatus);
                }
                break;
            case CONFIRMED:
                if(newStatus!=OrderStatus.SHIPPED
                        && newStatus!=OrderStatus.CANCELLED){
                    throw new OrderStatusException(
                            "Cannot change order status from"+currentStatus+" to "+newStatus);
                }
                break;
            case SHIPPED:
                if(newStatus!=OrderStatus.OUT_FOR_DELIVERY){
                    throw new OrderStatusException(
                            "Cannot change order status from"+currentStatus+" to "+newStatus);
                }
                break;
            case OUT_FOR_DELIVERY:
                if(newStatus!=OrderStatus.DELIVERED){
                    throw new OrderStatusException(
                            "Cannot change order status from"+currentStatus+" to "+newStatus);
                }
                break;
            case DELIVERED:
            case CANCELLED:
                throw new OrderStatusException(
                        "Order is already "+ currentStatus+".");
        }
    }
    @Override
    @Transactional
    public OrderResponseDto placeOrder() {
        User existingUser=userMapper.getCurrentUser();
        Cart userCart=cartMapper.getCartForUser(existingUser);
        Order newOrder=new Order();
        newOrder.setUser(existingUser);
        newOrder.setOrderStatus(OrderStatus.PLACED);
        newOrder.setOrderNumber(UUID.randomUUID().toString());
        newOrder.setTotalOrderAmount(userCart.getTotalAmount());
        for(CartItems cartItems:userCart.getCartItemsList()){
            OrderItems orderItems=new OrderItems();
            orderItems.setOrder(newOrder);
            orderItems.setProduct(cartItems.getProduct());
            orderItems.setQuantity(cartItems.getQuantity());
            orderItems.setPriceAtAdded(cartItems.getPriceAtAdded());
            newOrder.getOrderItemsList().add(orderItems);
            Product existingProduct=cartItems.getProduct();
            if(existingProduct.getStockQuantity()<cartItems.getQuantity()){
                throw new IllegalArgumentException("Only "+existingProduct.getStockQuantity()+" is available.");
            }
            existingProduct.setStockQuantity(existingProduct.getStockQuantity()- cartItems.getQuantity());
            productRepository.save(existingProduct);
        }
        orderRepository.save(newOrder);
        userCart.getCartItemsList().clear();
        cartMapper.calculateTotalCartAmount(userCart);
        cartRepository.save(userCart);
        return orderMapper.mapOrderToResponse(newOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponseDto> getUserOrders() {
        User existingUser=userMapper.getCurrentUser();
        List<Order> orderList=orderRepository.findByUser(existingUser);
        List<OrderResponseDto> orderResponseDtoList=new ArrayList<>();
        for(Order order:orderList){
            orderResponseDtoList.add(orderMapper.mapOrderToResponse(order));
        }
        return orderResponseDtoList;
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponseDto getOrderById(Long orderId) {
        User existingUser=userMapper.getCurrentUser();
        Order order=orderMapper.getOrder(orderId);
        if(existingUser.getRole()!= Role.ADMIN
                && !order.getUser().getUserId().equals(existingUser.getUserId())){
            throw new OrderNotFoundException("Order not found for Id: "+orderId);
        }
        return orderMapper.mapOrderToResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponseDto> getAllOrders() {
        List<Order> orderList =orderRepository.findAll();
        List<OrderResponseDto> orderResponseDtoList=new ArrayList<>();
        for(Order order:orderList){
            orderResponseDtoList.add(orderMapper.mapOrderToResponse(order));
        }
        return orderResponseDtoList;
    }

    @Override
    @Transactional
    public OrderResponseDto updateOrderStatus(Long orderId, OrderStatusUpdateRequestDto orderStatusUpdateRequestDto) {
        Order order=orderMapper.getOrder(orderId);
        validateOrderStatus(order.getOrderStatus(),orderStatusUpdateRequestDto.getOrderStatus());
        if(orderStatusUpdateRequestDto.getOrderStatus() == OrderStatus.CANCELLED){
            for(OrderItems orderItems: order.getOrderItemsList()){
                Product product=orderItems.getProduct();
                product.setStockQuantity(
                        product.getStockQuantity()+orderItems.getQuantity());
                productRepository.save(product);
            }
        }
        order.setOrderStatus(orderStatusUpdateRequestDto.getOrderStatus());
        return orderMapper.mapOrderToResponse(orderRepository.save(order));
    }

    @Override
    @Transactional
    public OrderResponseDto cancelOrder(Long orderId) {
        User user=userMapper.getCurrentUser();
        Order order=orderMapper.getOrder(orderId);
        if(!order.getUser().getUserId().equals(user.getUserId())){
            throw new AccessDeniedException("You are not authorized to cancel the order.");
        }
        if(order.getOrderStatus()!=OrderStatus.PLACED
                && order.getOrderStatus()!= OrderStatus.CANCELLED){
            throw new OrderStatusException(
                    "Order cannot be cancelled from "+order.getOrderStatus()+"status.");
        }
        if(order.getOrderStatus()!= OrderStatus.CANCELLED) {
            throw new OrderStatusException(
                    "Order is already in " + order.getOrderStatus() + "status.");
        }
        order.setOrderStatus(OrderStatus.CANCELLED);
        return orderMapper.mapOrderToResponse(orderRepository.save(order));
    }
}
