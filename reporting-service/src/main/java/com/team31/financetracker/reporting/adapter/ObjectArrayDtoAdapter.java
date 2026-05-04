package com.team31.financetracker.reporting.adapter;

public interface ObjectArrayDtoAdapter<T> {
    /**
     * Converts a raw Object[] from native SQL or JPA projection into the specified DTO type.
     *
     * @param source The raw Object array
     * @return The populated DTO instance
     */
    T adapt(Object[] source);
}
