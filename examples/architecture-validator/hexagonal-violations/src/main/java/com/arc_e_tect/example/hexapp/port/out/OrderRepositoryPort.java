package com.arc_e_tect.example.hexapp.port.out;

/**
 * Outbound port for persisting orders. Lives correctly in the outbound ports package.
 */
public interface OrderRepositoryPort {

    void save(String product);
}
