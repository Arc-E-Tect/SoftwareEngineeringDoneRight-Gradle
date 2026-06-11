package com.example.hexapp.port.in;

/**
 * Inbound port for placing an order. Lives correctly in the inbound ports package.
 */
public interface PlaceOrderPort {

    void placeOrder(String product);
}
