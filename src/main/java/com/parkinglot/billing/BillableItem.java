package com.parkinglot.billing;

import java.util.List;

public interface BillableItem {
    double getCost();
    String getDescription();
    List<String> getItemizedLines();
}
