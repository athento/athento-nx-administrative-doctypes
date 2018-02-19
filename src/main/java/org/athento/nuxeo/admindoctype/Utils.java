package org.athento.nuxeo.admindoctype;

import org.nuxeo.common.utils.i18n.I18NUtils;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.runtime.api.Framework;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.Map;

/**
 * Utils.
 */
public final class Utils {

    /**
     * Run operation.
     *
     * @param operationId
     * @param input
     * @param params
     * @param session
     * @return
     * @throws Exception
     */
    public static Object runOperation(String operationId, Object input,
                                      Map<String, Object> params, CoreSession session) throws Exception {
        AutomationService automationManager = Framework
                .getLocalService(AutomationService.class);
        // Input setting
        OperationContext ctx = new OperationContext(session);
        ctx.setInput(input);
        Object o = null;
        // Setting parameters of the chain
        try {
            // Run Automation service
            o = automationManager.run(ctx, operationId, params);
        } catch (Exception e) {
            throw e;
        }
        return o;
    }

    /**
     * Transform date to XMLGregorianCalendar.
     *
     * @param date
     * @return
     * @throws DatatypeConfigurationException
     */
    public static XMLGregorianCalendar transformDateToXmlGregorianCalendar(Date date) {
        GregorianCalendar c = new GregorianCalendar();
        c.setTime(date);
        try {
            return DatatypeFactory.newInstance().newXMLGregorianCalendar(c);
        } catch (DatatypeConfigurationException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Get string date from gregorian calendar.
     *
     * @param gc
     * @return
     */
    public static String getStringDate(GregorianCalendar gc) {
        return new SimpleDateFormat("dd-MM-yyyy").format(gc.getTime());
    }

    /**
     * Get vocabulary label.
     *
     * @param entryId
     * @param vocabulary
     * @return
     */
    public static String getVocabularyLabel(String entryId, String vocabulary) {
        DirectoryService directoryManager = Framework.getService(DirectoryService.class);
        Session session = null;
        try {
            session = directoryManager.open(vocabulary);
            DocumentModel entry = session.getEntry(entryId);
            if (entry != null) {
                if (entry.hasSchema("xvocabulary")) {
                    return (String) entry.getPropertyValue("xvocabulary:label");
                } else {
                    return (String) entry.getPropertyValue("vocabulary:label");
                }
            }
            return "";
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }

    /**
     * Get i18n message.
     *
     * @param label
     * @param params
     * @param locale
     * @return
     */
    public static String getI18nLabel(String label, Object [] params, Locale locale) {
        if (label == null) {
            label = "";
        }
        return I18NUtils.getMessageString("messages", label, params, locale);
    }
}

