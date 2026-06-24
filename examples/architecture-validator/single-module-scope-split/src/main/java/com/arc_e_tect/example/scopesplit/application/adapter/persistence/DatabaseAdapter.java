package com.arc_e_tect.example.scopesplit.application.adapter.persistence;

import com.arc_e_tect.example.scopesplit.application.port.out.OrderOutputPort;

public class DatabaseAdapter implements OrderOutputPort {

    @Override
    public void persistOrder(String orderId) {
        // Example persistence implementation.
    }
}
