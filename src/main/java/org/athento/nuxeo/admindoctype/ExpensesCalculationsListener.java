package org.athento.nuxeo.admindoctype;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.faces.FacesMessages;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.jboss.seam.international.StatusMessage;

import java.io.Serializable;
import java.util.*;


/**
 * Expenses calculations listener.
 */
@Name("expensesCalculation")
@Scope(ScopeType.CONVERSATION)
public class ExpensesCalculationsListener implements EventListener {

    /** Log. */
    private static final Log LOG = LogFactory.getLog(ExpensesCalculationsListener.class);

    /** Document type name. */
    private static final String EXPENSE_TYPE = "Expenses";
    private static final String GOBACK_ID = "02";
    private static final String KM_LIMIT_ID = "209" ;

    /**
     * Manage event.
     *
     * @param event
     * @throws ClientException
     */
    @Override
    public void handleEvent(Event event) throws ClientException {
        if (event.getContext() instanceof DocumentEventContext) {
            // Check document type first
            DocumentEventContext ctxt = (DocumentEventContext) event.getContext();
            DocumentModel doc = ctxt.getSourceDocument();
            if (EXPENSE_TYPE.equals(doc.getType())) {
                if (LOG.isInfoEnabled()) {
                    LOG.info("Calculating expenses for " + doc.getRef());
                }
                ExpenseList<Expense> accumulated = new ExpenseList<>();
                // Calculate Kms
                kmsSecondary(doc);
                kmsRRGOffices(doc);
                try {
                    // Sum all expenses by category
                    travelExpenses(doc);
                    noTravelExpenses(doc);
                } catch (ExpenseLimitException e) {
                    LOG.info("Exponses exceeded with error: " + e.getMessage());
                    // Write error message
                    Expense expenseErr = e.getLimitExceeded();
                    // Get label for category
                    String categoryLabel = Utils.getVocabularyLabel(expenseErr.getCategory(), "expenselimits");
                    if (categoryLabel.contains(":")) {
                        categoryLabel = categoryLabel.split(":")[0];
                    }
                    FacesMessages.instance().add(StatusMessage.Severity.INFO, Utils.getI18nLabel("label.expense.rrg",
                            new Object [] {expenseErr.getDate(), categoryLabel}, Locale.getDefault()));
                }
                LOG.info(accumulated);
            }
        }
    }

    /**
     * Calculate travel expenses.
     *
     * @param doc
     * @throws ExpenseLimitException
     */
    private void travelExpenses(DocumentModel doc) throws ExpenseLimitException {
        ExpenseList<Expense> accumulated = new ExpenseList<>();
        List<Map<String, Serializable>> expenses = (List) doc.getPropertyValue("administrative:expenseTravel");
        for (Map<String, Serializable> expense : expenses) {
            GregorianCalendar expenseDate = (GregorianCalendar) expense.get("expenseDate");
            if (expenseDate != null) {
                // Get limit for this expense
                String category = (String) expense.get("category");
                double limitForCategory = getLimitForCategory(category);
                double invitedNumber = (Double) expense.get("invitedNumber");
                double expenseTotal = (Double) expense.get("expense") / invitedNumber;
                // Manage date
                String date = Utils.getStringDate(expenseDate);
                LOG.info("Expense total for travel with date " + date + " is " + expenseTotal);
                if (accumulated.hasExpense(date, category)) {
                    Expense currentExpense = accumulated.getExpense(date, category);
                    double subtotal = currentExpense.getTotal();
                    double accumulatedTotal = subtotal + expenseTotal;
                    if (accumulatedTotal > limitForCategory) {
                        throw new ExpenseLimitException("Expense travel limit exceeded for " + category + " with " + accumulatedTotal, currentExpense);
                    } else {
                        accumulated.add(currentExpense);
                    }
                } else {
                    Expense currentExpense = new Expense(date, category, expenseTotal);
                    if (expenseTotal > limitForCategory) {
                        throw new ExpenseLimitException("Expense travel limit exceeded for " + category + "with " + expenseTotal, currentExpense);
                    }
                    accumulated.add(currentExpense);
                }
            }
        }
    }

    /**
     * Calculate no travel expenses.
     *
     * @param doc
     * @throws ExpenseLimitException
     */
    private void noTravelExpenses(DocumentModel doc) throws ExpenseLimitException {
        ExpenseList<Expense> accumulated = new ExpenseList<>();
        List<Map<String, Serializable>> expenses = (List) doc.getPropertyValue("administrative:expenseNonTravel");
        for (Map<String, Serializable> expense : expenses) {
            GregorianCalendar expenseDate = (GregorianCalendar) expense.get("expenseDate");
            if (expenseDate != null) {
                // Get limit for this expense
                String category = (String) expense.get("category");
                double limitForCategory = getLimitForCategory(category);
                double invitedNumber = (Double) expense.get("invitedNumber");
                double expenseTotal = (Double) expense.get("expense") / invitedNumber;
                // Manage date
                String date = Utils.getStringDate(expenseDate);
                if (accumulated.hasExpense(date, category)) {
                    LOG.info("Expense total for Non travel with date " + date + " is " + expenseTotal);
                    Expense currentExpense = accumulated.getExpense(date, category);
                    double subtotal = currentExpense.getTotal();
                    double accumulatedTotal = subtotal + expenseTotal;
                    if (accumulatedTotal > limitForCategory) {
                        throw new ExpenseLimitException("Expense non travel limit exceeded for " + category + " with " + accumulatedTotal, currentExpense);
                    } else {
                        accumulated.add(currentExpense);
                    }
                } else {
                    Expense currentExpense = new Expense(date, category, expenseTotal);
                    if (expenseTotal > limitForCategory) {
                        throw new ExpenseLimitException("Expense travel limit exceeded with for " + category + " " + expenseTotal, currentExpense);
                    }
                    accumulated.add(currentExpense);
                }
            }
        }
    }

    /**
     * Calculate kms in secondary.
     *
     * @param doc
     * @throws ExpenseLimitException
     */
    private void kmsSecondary(DocumentModel doc) {
        ArrayList<Map<String, Serializable>> expenses = (ArrayList) doc.getPropertyValue("administrative:expenseKm");
        for (Map<String, Serializable> expense : expenses) {
            GregorianCalendar expenseDate = (GregorianCalendar) expense.get("expenseDate");
            if (expenseDate != null) {
                // Get category
                String car = (String) expense.get("category");
                String goBack = (String) expense.get("goback");
                // Manage kms
                double kms = (Double) expense.get("km");
                if (GOBACK_ID.equals(goBack)) {
                    kms = kms * 2;
                }
                LOG.info("Expense for secondary Km RS " + car + " with goBack: " + goBack + " with kms: " + kms);
                String carCostStr = Utils.getVocabularyLabel(car, "carkmcost");
                // Calculate cost
                double carCost = Double.valueOf(carCostStr);
                LOG.info("Car cost is " + carCost);
                double expenseTotal = kms * carCost;
                // Manage date
                String date = Utils.getStringDate(expenseDate);
                LOG.info("Expense total for Km red secundaria with date " + date + " is " + expenseTotal);
                // Add totalCost to expense
                expense.put("expense", expenseTotal);
            }
        }
        doc.setPropertyValue("administrative:expenseKm", expenses);
    }

    /**
     * Calculate kms in RRG offices.
     *
     * @param doc
     * @throws ExpenseLimitException
     */
    private void kmsRRGOffices(DocumentModel doc) {
        ArrayList<Map<String, Serializable>> expenses = (ArrayList) doc.getPropertyValue("administrative:expenseKmOffices");
        for (Map<String, Serializable> expense : expenses) {
            GregorianCalendar expenseDate = (GregorianCalendar) expense.get("expenseDate");
            if (expenseDate != null) {
                // Get category
                String car = (String) expense.get("category");
                String goBack = (String) expense.get("goback");
                String routeStr = Utils.getVocabularyLabel((String) expense.get("route"), "routescom");
                // Manage kms
                double kms = Double.valueOf(routeStr);
                if (GOBACK_ID.equals(goBack)) {
                    kms = kms * 2;
                }
                LOG.info("Expense for rrg centers Km RS " + car + " with goBack: " + goBack + " with kms: " + kms);
                String carCostStr = Utils.getVocabularyLabel(car, "carkmcost");
                // Calculate cost
                double carCost = Double.valueOf(carCostStr);
                LOG.info("Car cost is " + carCost);
                double expenseTotal = kms * carCost;
                // Manage date
                String date = Utils.getStringDate(expenseDate);
                LOG.info("Expense total for Km red secundaria with date " + date + " is " + expenseTotal);
                // Add totalCost to expense
                expense.put("km", kms);
                expense.put("expense", expenseTotal);
            }
        }
        doc.setPropertyValue("administrative:expenseKmOffices", expenses);
    }

    /**
     * Get limit from category.
     *
     * @param category
     * @return
     */
    private double getLimitForCategory(String category) {
        double limit = 0.0;
        String label = Utils.getVocabularyLabel(category, "expenselimits");
        if (label != null && label.contains(":")){
            String limitStr = label.split(":")[1];
            try {
                limit = Double.valueOf(limitStr);
            } catch (NumberFormatException e) {
                LOG.warn("Limit for " + label + " is wrong!", e);
            }
        }
        return limit;
    }



}
