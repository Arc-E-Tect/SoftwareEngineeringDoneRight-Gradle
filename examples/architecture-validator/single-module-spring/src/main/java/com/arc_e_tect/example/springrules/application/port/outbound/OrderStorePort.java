package com.arc_e_tect.example.springrules.application.port.outbound;

import com.arc_e_tect.example.springrules.domain.model.Order;

public interface OrderStorePort {

    void save(Order order);
}
