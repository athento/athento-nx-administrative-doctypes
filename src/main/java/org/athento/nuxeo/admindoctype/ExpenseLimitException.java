package org.athento.nuxeo.admindoctype;

/**
 * Expense limit exception.
 */
public class ExpenseLimitException extends Exception {

    private Expense limitExceeded;

    /**
     * Constructs a new exception with {@code null} as its detail message.
     * The cause is not initialized, and may subsequently be initialized by a
     * call to {@link #initCause}.
     */
    public ExpenseLimitException(Expense limitExceeded) {
        super();
        this.limitExceeded = limitExceeded;
    }

    /**
     * Constructs a new exception with the specified detail message.  The
     * cause is not initialized, and may subsequently be initialized by
     * a call to {@link #initCause}.
     *
     * @param message the detail message. The detail message is saved for
     *                later retrieval by the {@link #getMessage()} method.
     */
    public ExpenseLimitException(String message, Expense limitExceeded) {
        super(message);
        this.limitExceeded = limitExceeded;
    }

    /**
     * Constructs a new exception with the specified detail message and
     * cause.  <p>Note that the detail message associated with
     * {@code cause} is <i>not</i> automatically incorporated in
     * this exception's detail message.
     *
     * @param message the detail message (which is saved for later retrieval
     *                by the {@link #getMessage()} method).
     * @param cause   the cause (which is saved for later retrieval by the
     *                {@link #getCause()} method).  (A <tt>null</tt> value is
     *                permitted, and indicates that the cause is nonexistent or
     *                unknown.)
     * @since 1.4
     */
    public ExpenseLimitException(String message, Throwable cause, Expense limitExceeded) {
        super(message, cause);
        this.limitExceeded = limitExceeded;
    }

    public Expense getLimitExceeded() {
        return limitExceeded;
    }

    public void setLimitExceeded(Expense limitExceeded) {
        this.limitExceeded = limitExceeded;
    }
}
