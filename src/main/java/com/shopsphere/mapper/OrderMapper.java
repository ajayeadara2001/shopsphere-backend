package com.shopsphere.mapper;

import com.shopsphere.dto.OrderItemsResponseDto;
import com.shopsphere.dto.OrderResponseDto;
import com.shopsphere.entity.Order;
import com.shopsphere.entity.OrderItems;
import com.shopsphere.exception.OrderNotFoundException;
import com.shopsphere.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class OrderMapper {
    private final OrderRepository orderRepository;
    public OrderItemsResponseDto mapOrderItemsToResponse(OrderItems orderItems){
        return new OrderItemsResponseDto(
                orderItems.getProduct().getProductId(),
                orderItems.getProduct().getProductName(),
                orderItems.getQuantity(),
                orderItems.getPriceAtAdded()
        );
    }

    public OrderResponseDto mapOrderToResponse(Order order){
        List<OrderItemsResponseDto> orderItemsResponseDtoList=order
                .getOrderItemsList()
                .stream()
                .map(this::mapOrderItemsToResponse)
                .toList();
        return new OrderResponseDto(
                order.getOrderId(),
                order.getOrderNumber(),
                order.getTotalOrderAmount(),
                order.getOrderStatus(),
                orderItemsResponseDtoList,
                order.getOrderedAt()
        );
    }

    public Order getOrder(Long orderId){
    return orderRepository.findById(orderId)
            .orElseThrow(()-> new OrderNotFoundException("Order not found for Id: "+orderId));
    }

}
