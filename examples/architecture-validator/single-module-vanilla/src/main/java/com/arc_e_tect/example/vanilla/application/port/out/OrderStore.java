package com.arc_e_tect.example.vanilla.application.port.out;

import com.arc_e_tect.example.vanilla.application.domain.model.Order;

public interface OrderStore {

    void save(Order order);
}