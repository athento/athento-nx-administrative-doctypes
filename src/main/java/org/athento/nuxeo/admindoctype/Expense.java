package org.athento.nuxeo.admindoctype;

import java.io.Serializable;

/**
 * Expense class to define an expense.
 */
public class Expense implements Serializable {


    String parentCategory;
    String date;
    String category;
    double total = 0.0;

    public Expense(String date, String category, double total) {
        this.date = date;
        this.category = category;
        this.total = total;
    }

    public String getParentCategory() {
        return parentCategory;
    }

    public void setParentCategory(String parentCategory) {
        this.parentCategory = parentCategory;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }

    @Override
    public String toString() {
        return "Expense{" +
                "date='" + date + '\'' +
                ", category='" + category + '\'' +
                ", total=" + total +
                '}';
    }
}
