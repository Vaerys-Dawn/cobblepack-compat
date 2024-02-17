package com.github.vareys.config;


import java.util.ArrayList;
import java.util.List;

public class PriorityConfig {
    public List<String> priorities = new ArrayList<>();

    public PriorityConfig() {
        if (priorities == null) priorities = new ArrayList<>();
    }

}
