package com.ldbc.driver;

import com.ldbc.driver.runtime.ConcurrentErrorReporter;
import com.ldbc.driver.runtime.coordination.CompletionTimeException;
import com.ldbc.driver.runtime.coordination.ConcurrentCompletionTimeService;
import com.ldbc.driver.runtime.metrics.ConcurrentMetricsService;
import com.ldbc.driver.runtime.metrics.MetricsCollectionException;
import com.ldbc.driver.runtime.scheduling.MultiCheck;
import com.ldbc.driver.runtime.scheduling.Spinner;
import com.ldbc.driver.runtime.scheduling.SpinnerCheck;
import com.ldbc.driver.temporal.DurationMeasurement;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

public abstract class OperationHandler<OPERATION_TYPE extends Operation<?>> implements Callable<OperationResult> {
    private Spinner spinner;
    private OPERATION_TYPE operation;
    private DbConnectionState dbConnectionState;
    private ConcurrentCompletionTimeService completionTimeService;
    private ConcurrentErrorReporter errorReporter;
    private ConcurrentMetricsService metricsService;
    private List<SpinnerCheck> checks = new ArrayList<SpinnerCheck>();

    private boolean initialized = false;

    public final void init(Spinner spinner,
                           Operation<?> operation,
                           ConcurrentCompletionTimeService completionTimeService,
                           ConcurrentErrorReporter errorReporter,
                           ConcurrentMetricsService metricsService) throws OperationException {
        if (initialized) {
            throw new OperationException(String.format("OperationHandler can not be initialized twice\n%s", toString()));
        }
        this.spinner = spinner;
        this.operation = (OPERATION_TYPE) operation;
        this.completionTimeService = completionTimeService;
        this.errorReporter = errorReporter;
        this.metricsService = metricsService;

        this.initialized = true;
    }

    public final OPERATION_TYPE operation() {
        return operation;
    }

    public final ConcurrentCompletionTimeService completionTimeService() {
        return completionTimeService;
    }

    public final void setDbConnectionState(DbConnectionState dbConnectionState) {
        this.dbConnectionState = dbConnectionState;
    }

    public final DbConnectionState dbConnectionState() {
        return dbConnectionState;
    }

    public final void addCheck(SpinnerCheck check) {
        checks.add(check);
    }

    /**
     * Internally calls the protected method executeOperation(operation)
     * and returns the associated OperationResult if execution was successful.
     * If execution is successful OperationResult metrics are also written to ConcurrentMetricsService.
     * If execution is unsuccessful the result is null, an error is written to ConcurrentErrorReporter,
     * and no metrics are written.
     *
     * @return an OperationResult if Operation execution was successful, otherwise null
     */
    @Override
    public OperationResult call() {
        if (false == initialized) {
            errorReporter.reportError(this, "Handler was executed before being initialized");
            return null;
        }
        try {
            spinner.waitForScheduledStartTime(operation, new MultiCheck(checks));
            DurationMeasurement durationMeasurement = DurationMeasurement.startMeasurementNow();
            OperationResult operationResult = executeOperation(operation);
            if (null == operationResult)
                throw new DbException(String.format("Handler returned null result:\n %s", toString()));
            operationResult.setRunDuration(durationMeasurement.durationUntilNow());
            operationResult.setActualStartTime(durationMeasurement.startTime());
            operationResult.setOperationType(operation.type());
            operationResult.setScheduledStartTime(operation.scheduledStartTime());
            completionTimeService.submitCompletedTime(operation.scheduledStartTime());
            metricsService.submitOperationResult(operationResult);
            return operationResult;
        } catch (DbException e) {
            String errMsg = String.format(
                    "Error encountered while executing query %s\n%s",
                    operation.toString(),
                    ConcurrentErrorReporter.stackTraceToString(e));
            errorReporter.reportError(this, errMsg);
        } catch (MetricsCollectionException e) {
            String errMsg = String.format(
                    "Error encountered while collecting metrics for query %s\n%s",
                    operation.toString(),
                    ConcurrentErrorReporter.stackTraceToString(e));
            errorReporter.reportError(this, errMsg);
        } catch (CompletionTimeException e) {
            String errMsg = String.format(
                    "Error encountered while submitting completed time for query %s\n%s",
                    operation.toString(),
                    ConcurrentErrorReporter.stackTraceToString(e));
            errorReporter.reportError(this, errMsg);
        } catch (Throwable e) {
            String errMsg = String.format(
                    "Unexpected error while executing query %s\n%s",
                    operation.toString(),
                    ConcurrentErrorReporter.stackTraceToString(e));
            errorReporter.reportError(this, errMsg);
        }

        return null;
    }

    protected abstract OperationResult executeOperation(OPERATION_TYPE operation) throws DbException;

    @Override
    public String toString() {
        return String.format("OperationHandler [type=%s, operation=%s]", getClass().getName(), operation);
    }
}