package com.shopsphere.service;

import com.shopsphere.dto.OrderResponseDto;
import com.shopsphere.dto.OrderStatusUpdateRequestDto;

import java.util.List;

public interface OrderService {
    OrderResponseDto placeOrder();
    List<OrderResponseDto> getUserOrders();
    OrderResponseDto getOrderById(Long orderId);
    List<OrderResponseDto> getAllOrders();
    OrderResponseDto updateOrderStatus(Long orderId, OrderStatusUpdateRequestDto orderStatusUpdateRequestDto);
    OrderResponseDto cancelOrder(Long orderId);
}
