package org.athento.nuxeo.admindoctype;

import java.util.ArrayList;

/**
 * ExpenseList class manage all expenses.
 */
public class ExpenseList<T extends Expense> extends ArrayList<T> {

    /**
     * Check exists an expense by date.
     *
     * @param date
     * @return
     */
    public boolean hasExpense(String date, String category) {
        for (T expense : this) {
            if (expense.getDate().equals(date) && expense.getCategory().equals(category)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get expense.
     *
     * @param date
     * @return
     */
    public Expense getExpense(String date, String category) {
        for (T expense : this) {
            if (expense.getDate().equals(date) && expense.getCategory().equals(category)) {
                return expense;
            }
        }
        return null;
    }
}
