package com.foodfast.dto;

import com.foodfast.entity.ProductOptionSelectionType;

import java.util.List;

public class ReplaceProductOptionsRequest {
    public List<Group> groups;

    public static class Group {
        public String id;
        public String name;
        public ProductOptionSelectionType selectionType;
        public boolean required;
        public int minSelections;
        public int maxSelections;
        public int displayOrder;
        public boolean active = true;
        public List<Option> options;
    }

    public static class Option {
        public String id;
        public String name;
        public int priceDelta;
        public int displayOrder;
        public boolean active = true;
    }
}
