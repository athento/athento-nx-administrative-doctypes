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
    private static final String TRAVEL_PARENT_CATEGORY = "travel";
    private static final String NONTRAVEL_PARENT_CATEGORY = "nonTravel";
    private static final String KMS_PARENT_CATEGORY = "kms";
    private static final String GOBACK_ID = "02";

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
                kmsRRGSecondary(doc, accumulated);
                kmsOthers(doc, accumulated);
                kmsRRGOffices(doc, accumulated);
                // Sum all expenses by category
                travelExpenses(doc, accumulated);
                noTravelExpenses(doc, accumulated);
                if (accumulated.isLimitExceeded()) {
                    Expense expenseErr = accumulated.getLimitExceededExpense();
                    // Get label for category
                    String categoryLabel = Utils.getVocabularyLabel(expenseErr.getCategory(), "expenselimits");
                    if (categoryLabel.contains(":")) {
                        categoryLabel = categoryLabel.split(":")[0];
                    }
                    String msg = Utils.getI18nLabel("label.expense.rrg",
                            new Object [] {expenseErr.getDate(), categoryLabel}, Locale.getDefault());
                    doc.setPropertyValue("administrative:lastMessage", msg);
                    doc.setPropertyValue("administrative:specialAuthorization", true);
                    FacesMessages.instance().add(StatusMessage.Severity.INFO, msg);
                } else {
                    String msg = Utils.getI18nLabel("label.expense.rrg.ok", null, Locale.getDefault());
                    doc.setPropertyValue("administrative:lastMessage", msg);
                    doc.setPropertyValue("administrative:specialAuthorization", false);
                }
                LOG.info("Accumu" + accumulated);
                // Calculate totals
                calcularTotales(doc, accumulated);
            }
        }
    }

    /**
     * Calcular totales.
     *
     * @param doc
     * @param accumulated
     */
    private void calcularTotales(DocumentModel doc, ExpenseList<Expense> accumulated) {
        double travelTotal = 0.0;
        double nonTravelTotal = 0.0;
        double kmsTotal = 0.0;
        double dietaCompletaNacionalConPernoctaTravel = 0.0;
        double dietaMediaNacionalConPernoctaTravel = 0.0;
        double dietaCompletaInternacionalConPernoctaTravel = 0.0;
        double dietaMediaInternacionalSinPernoctaTravel = 0.0;
        double invitacionTercerosNacionalTravel = 0.0;
        double invitacionTercerosInternacionalTravel = 0.0;
        double billetesTransporteTravel = 0.0;
        double peajeTravel = 0.0;
        double parkingTravel = 0.0;
        double taxisTravel = 0.0;
        double combustibleTravel = 0.0;
        double telefonoTravel = 0.0;
        double internetTravel = 0.0;
        double lavanderiaTravel = 0.0;
        double invitacionTercerosNacionalNonTravel = 0.0;
        double billetesTransporteNonTravel = 0.0;
        double peajeNonTravel = 0.0;
        double parkingNonTravel = 0.0;
        double taxisNonTravel = 0.0;
        double combustibleNonTravel = 0.0;
        double telefonoNonTravel = 0.0;
        double internetNonTravel = 0.0;
        double lavanderiaNonTravel = 0.0;
        double ownKms = 0.0;
        double expenseCompanyKmTotal = 0.0;
        for (Expense expense : accumulated) {
            double total = expense.getTotal();
            if (TRAVEL_PARENT_CATEGORY.equals(expense.getParentCategory())) {
                travelTotal += total;
                if ("001".equals(expense.getCategory())) {
                    dietaCompletaNacionalConPernoctaTravel += total;
                    doc.setPropertyValue("administrative:expenseDietaCompletaNacionalPernoctaTravelTotal", dietaCompletaNacionalConPernoctaTravel);
                }
                if ("002".equals(expense.getCategory())) {
                    dietaMediaNacionalConPernoctaTravel += total;
                    doc.setPropertyValue("administrative:expenseDietaMediaNacionalSinPernoctaTravelTotal", dietaMediaNacionalConPernoctaTravel);
                }
                if ("003".equals(expense.getCategory())) {
                    dietaCompletaInternacionalConPernoctaTravel += total;
                    doc.setPropertyValue("administrative:expenseDietaCompletaExtranjeroPernoctaTravelTotal", dietaCompletaInternacionalConPernoctaTravel);
                }
                if ("004".equals(expense.getCategory())) {
                    dietaMediaInternacionalSinPernoctaTravel += total;
                    doc.setPropertyValue("administrative:expenseDietaMediaExtranjeroSinPernoctaTravelTotal", dietaMediaInternacionalSinPernoctaTravel);
                }
                if ("102".equals(expense.getCategory())) {
                    invitacionTercerosNacionalTravel += total;
                    doc.setPropertyValue("administrative:expenseInvitacionTercersoNacionalTravelTotal", invitacionTercerosNacionalTravel);
                }
                if ("103".equals(expense.getCategory())) {
                    invitacionTercerosInternacionalTravel += total;
                    doc.setPropertyValue("administrative:expenseInvitacionTercersoInternacionalTravelTotal", invitacionTercerosInternacionalTravel);
                }
                if ("201".equals(expense.getCategory())) {
                    billetesTransporteTravel += total;
                    doc.setPropertyValue("administrative:expenseBilletesTransporteTravelTotal", billetesTransporteTravel);
                }
                if ("202".equals(expense.getCategory())) {
                    peajeTravel += total;
                    doc.setPropertyValue("administrative:expensePeajeTravelTotal", peajeTravel);
                }
                if ("203".equals(expense.getCategory())) {
                    parkingTravel += total;
                    doc.setPropertyValue("administrative:expenseParkingTravelTotal", parkingTravel);
                }
                if ("204".equals(expense.getCategory())) {
                    taxisTravel += total;
                    doc.setPropertyValue("administrative:expenseTaxisTravelTotal", taxisTravel);
                }
                if ("205".equals(expense.getCategory())) {
                    combustibleTravel += total;
                    doc.setPropertyValue("administrative:expenseCombustibleTravelTotal", combustibleTravel);
                }
                if ("206".equals(expense.getCategory())) {
                    telefonoTravel += total;
                    doc.setPropertyValue("administrative:expenseTelefonoTravelTotal", telefonoTravel);
                }
                if ("207".equals(expense.getCategory())) {
                    internetTravel += total;
                    doc.setPropertyValue("administrative:expenseInternetTravelTotal", internetTravel);
                }
                if ("208".equals(expense.getCategory())) {
                    lavanderiaTravel += total;
                    doc.setPropertyValue("administrative:expenseLavanderiaTravelTotal", lavanderiaTravel);
                }
            } else if (NONTRAVEL_PARENT_CATEGORY.equals(expense.getParentCategory())) {
                nonTravelTotal += total;
                if ("102".equals(expense.getCategory())) {
                    invitacionTercerosNacionalNonTravel += total;
                    doc.setPropertyValue("administrative:expenseInvitacionTercersoNacionalNonTravelTotal", invitacionTercerosNacionalNonTravel);
                }
                if ("201".equals(expense.getCategory())) {
                    billetesTransporteNonTravel += total;
                    doc.setPropertyValue("administrative:expenseBilletesTransporteNonTravelTotal", billetesTransporteNonTravel);
                }
                if ("202".equals(expense.getCategory())) {
                    peajeNonTravel += total;
                    doc.setPropertyValue("administrative:expensePeajeNonTravelTotal", peajeNonTravel);
                }
                if ("203".equals(expense.getCategory())) {
                    parkingNonTravel += total;
                    doc.setPropertyValue("administrative:expenseParkingNonTravelTotal", parkingNonTravel);
                }
                if ("204".equals(expense.getCategory())) {
                    taxisNonTravel += total;
                    doc.setPropertyValue("administrative:expenseTaxisNonTravelTotal", taxisNonTravel);
                }
                if ("205".equals(expense.getCategory())) {
                    combustibleNonTravel += total;
                    doc.setPropertyValue("administrative:expenseCombustibleNonTravelTotal", combustibleNonTravel);
                }
                if ("206".equals(expense.getCategory())) {
                    telefonoNonTravel += total;
                    doc.setPropertyValue("administrative:expenseTelefonoNonTravelTotal", telefonoNonTravel);
                }
                if ("207".equals(expense.getCategory())) {
                    internetNonTravel += total;
                    doc.setPropertyValue("administrative:expenseInternetNonTravelTotal", internetNonTravel);
                }
                if ("208".equals(expense.getCategory())) {
                    lavanderiaNonTravel += total;
                    doc.setPropertyValue("administrative:expenseLavanderiaNonTravelTotal", lavanderiaNonTravel);
                }
            } else if (KMS_PARENT_CATEGORY.equals(expense.getParentCategory())) {
                kmsTotal += total;
                if ("9001".equals(expense.getCategory())) {
                    ownKms += total;
                    doc.setPropertyValue("administrative:expenseOwnKmTotal", ownKms);
                } else {
                    expenseCompanyKmTotal += total;
                    doc.setPropertyValue("administrative:expenseCompanyKmTotal", expenseCompanyKmTotal);
                }
            }
        }
        doc.setPropertyValue("administrative:expenseTravelTotal", travelTotal);
        doc.setPropertyValue("administrative:expenseNonTravelTotal", nonTravelTotal);
        doc.setPropertyValue("administrative:expenseKmTotal", kmsTotal);
        doc.setPropertyValue("administrative:expenseTotal", (travelTotal + nonTravelTotal + kmsTotal));
    }

    /**
     * Calculate travel expenses.
     *
     * @param doc
     * @param accumulated
     * @throws ExpenseLimitException
     */
    private void travelExpenses(DocumentModel doc, ExpenseList<Expense> accumulated) {
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
                LOG.info("Expense total for travel for " + category + " with date " + date + " is " + expenseTotal);
                LOG.info("Gasto V " + date + ", " + limitForCategory + ", " + invitedNumber + ", " + expenseTotal);
                LOG.info("ACcum NV " + accumulated.isLimitExceeded());
                if (accumulated.hasExpense(date, category)) {
                    Expense currentExpense = accumulated.getExpense(date, category);
                    double subtotal = currentExpense.getTotal();
                    double accumulatedTotal = subtotal + expenseTotal;
                    if (limitForCategory > 0 && accumulatedTotal > limitForCategory) {
                        accumulated.setLimitExceeded(true);
                        accumulated.setLimitExceededExpense(currentExpense);
                    }
                    currentExpense = new Expense(date, category, (Double) expense.get("expense"));
                    currentExpense.setParentCategory(TRAVEL_PARENT_CATEGORY);
                    accumulated.add(currentExpense);
                } else {
                    Expense currentExpense = new Expense(date, category, (Double) expense.get("expense"));
                    currentExpense.setParentCategory(TRAVEL_PARENT_CATEGORY);
                    if (limitForCategory > 0 && expenseTotal > limitForCategory) {
                        accumulated.setLimitExceeded(true);
                        accumulated.setLimitExceededExpense(currentExpense);
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
     * @param accumulated
     * @throws ExpenseLimitException
     */
    private void noTravelExpenses(DocumentModel doc, ExpenseList<Expense> accumulated) {
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
                LOG.info("Gasto NV " + category + ", " + date + ", " + limitForCategory + ", " + invitedNumber + ", " + expenseTotal);
                LOG.info("ACcum NV " + accumulated.isLimitExceeded());
                if (accumulated.hasExpense(date, category)) {
                    LOG.info("Expense total for Non travel for category " + category + " with date " + date + " is " + expenseTotal);
                    Expense currentExpense = accumulated.getExpense(date, category);
                    double subtotal = currentExpense.getTotal();
                    double accumulatedTotal = subtotal + expenseTotal;
                    if (limitForCategory > 0 && accumulatedTotal > limitForCategory) {
                        accumulated.setLimitExceeded(true);
                        accumulated.setLimitExceededExpense(currentExpense);
                    }
                    currentExpense = new Expense(date, category, (Double) expense.get("expense"));
                    currentExpense.setParentCategory(NONTRAVEL_PARENT_CATEGORY);
                    accumulated.add(currentExpense);
                } else {
                    LOG.info("Non travel check " + expenseTotal + " > " + limitForCategory + " ?");
                    Expense currentExpense = new Expense(date, category, (Double) expense.get("expense"));
                    currentExpense.setParentCategory(NONTRAVEL_PARENT_CATEGORY);
                    if (limitForCategory > 0 && expenseTotal > limitForCategory) {
                        accumulated.setLimitExceeded(true);
                        accumulated.setLimitExceededExpense(currentExpense);
                    }
                    accumulated.add(currentExpense);
                }
            }
        }
    }

    /**
     * Calculate kms in others.
     *
     * @param doc
     * @param accumulated
     * @throws ExpenseLimitException
     */
    private void kmsOthers(DocumentModel doc, ExpenseList<Expense> accumulated) {
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
                LOG.info("Expense for others Km RS " + car + " with goBack: " + goBack + " with kms: " + kms);
                String carCostStr = Utils.getVocabularyLabel(car, "carkmcost");
                // Calculate cost
                double carCost = Double.valueOf(carCostStr);
                LOG.info("Car cost is " + carCost);
                double expenseTotal = kms * carCost;
                // Manage date
                String date = Utils.getStringDate(expenseDate);
                LOG.info("Expense total for Km others with date " + date + " is " + expenseTotal);
                Expense currentExpense = new Expense(date, car, expenseTotal);
                currentExpense.setParentCategory(KMS_PARENT_CATEGORY);
                // Add expense to accumulated
                accumulated.add(currentExpense);
                // Add totalCost to expense
                expense.put("expense", expenseTotal);
            }
        }
        doc.setPropertyValue("administrative:expenseKm", expenses);
    }

    /**
     * Calculate kms in secondary.
     *
     * @param doc
     * @param accumulated
     * @throws ExpenseLimitException
     */
    private void kmsRRGSecondary(DocumentModel doc, ExpenseList<Expense> accumulated) {
        ArrayList<Map<String, Serializable>> expenses = (ArrayList) doc.getPropertyValue("administrative:expenseKmSecondary");
        for (Map<String, Serializable> expense : expenses) {
            GregorianCalendar expenseDate = (GregorianCalendar) expense.get("expenseDate");
            if (expenseDate != null) {
                LOG.info("Secondary route: " + expense.get("route"));
                // Get category
                String car = (String) expense.get("category");
                String goBack = (String) expense.get("goback");
                String routeStr = Utils.getVocabularyLabel((String) expense.get("route"), "routescom_secondary");
                // Manage kms
                double kms = Double.valueOf(routeStr);
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
                Expense currentExpense = new Expense(date, car, expenseTotal);
                currentExpense.setParentCategory(KMS_PARENT_CATEGORY);
                // Add expense to accumulated
                accumulated.add(currentExpense);
                // Add totalCost to expense
                expense.put("km", kms);
                expense.put("expense", expenseTotal);
            }
        }
        doc.setPropertyValue("administrative:expenseKmSecondary", expenses);
    }

    /**
     * Calculate kms in RRG offices.
     *
     * @param doc
     * @param accumulated
     * @throws ExpenseLimitException
     */
    private void kmsRRGOffices(DocumentModel doc, ExpenseList<Expense> accumulated) {
        ArrayList<Map<String, Serializable>> expenses = (ArrayList) doc.getPropertyValue("administrative:expenseKmOffices");
        for (Map<String, Serializable> expense : expenses) {
            GregorianCalendar expenseDate = (GregorianCalendar) expense.get("expenseDate");
            if (expenseDate != null) {
                // Get category
                LOG.info("Offices route: " + expense.get("route"));
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
                Expense currentExpense = new Expense(date, car, expenseTotal);
                currentExpense.setParentCategory(KMS_PARENT_CATEGORY);
                // Add expense to accumulated
                accumulated.add(currentExpense);
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
