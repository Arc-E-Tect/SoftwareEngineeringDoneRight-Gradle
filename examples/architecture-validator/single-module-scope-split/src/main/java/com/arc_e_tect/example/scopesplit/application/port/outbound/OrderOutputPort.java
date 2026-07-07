package com.arc_e_tect.example.scopesplit.application.port.outbound;

public interface OrderOutputPort {

    void persistOrder(String orderId);
}
