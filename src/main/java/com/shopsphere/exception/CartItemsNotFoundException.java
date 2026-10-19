package com.shopsphere.exception;

public class CartItemsNotFoundException extends RuntimeException {
    public CartItemsNotFoundException(String message) {
        super(message);
    }
}
