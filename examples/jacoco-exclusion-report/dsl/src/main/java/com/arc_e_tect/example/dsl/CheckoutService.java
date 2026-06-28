package com.arc_e_tect.example.dsl;

public class CheckoutService {

    public String checkout(String cartId) {
        return "checked-out:" + cartId;
    }
}
