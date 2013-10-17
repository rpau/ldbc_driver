package com.ldbc.driver.measurements;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.ldbc.driver.OperationResult;
import com.ldbc.driver.WorkloadException;
import com.ldbc.driver.measurements.metric.Metric;
import com.ldbc.driver.util.temporal.Duration;
import com.ldbc.driver.util.temporal.Time;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.matchers.JUnitMatchers.*;

public class WorkloadMetricsManagerTest
{

    @Test
    public void shouldReturnCorrectMeasurements() throws MetricsExporterException, WorkloadException
    {
        WorkloadMetricsManager workloadMeasurements = new WorkloadMetricsManager( TimeUnit.NANOSECONDS );

        OperationResult operationResult1 = new OperationResult( 1, "result one" );
        operationResult1.setOperationType( "type one" );
        operationResult1.setScheduledStartTime( Time.fromNano( 1 ) );
        operationResult1.setActualStartTime( Time.fromNano( 2 ) );
        operationResult1.setRunDuration( Duration.fromNano( 1 ) );

        OperationResult operationResult2 = new OperationResult( 2, "result two" );
        operationResult2.setOperationType( "type one" );
        operationResult2.setScheduledStartTime( Time.fromNano( 1 ) );
        operationResult2.setActualStartTime( Time.fromNano( 8 ) );
        operationResult2.setRunDuration( Duration.fromNano( 3 ) );

        OperationResult operationResult3 = new OperationResult( 2, "result three" );
        operationResult3.setOperationType( "type two" );
        operationResult3.setScheduledStartTime( Time.fromNano( 1 ) );
        operationResult3.setActualStartTime( Time.fromNano( 11 ) );
        operationResult3.setRunDuration( Duration.fromNano( 5 ) );

        workloadMeasurements.measure( operationResult1 );
        workloadMeasurements.measure( operationResult2 );
        workloadMeasurements.measure( operationResult3 );

        int metricsCount;

        metricsCount = 0;
        for ( Metric metric : workloadMeasurements.getRuntimes().getMetrics() )
        {
            metricsCount++;
        }
        assertThat( metricsCount, is( 2 ) );

        metricsCount = 0;
        for ( Metric metric : workloadMeasurements.getResultCodes().getMetrics() )
        {
            metricsCount++;
        }
        assertThat( metricsCount, is( 2 ) );

        metricsCount = 0;
        for ( Metric metric : workloadMeasurements.getStartTimeDelays().getMetrics() )
        {
            metricsCount++;
        }
        assertThat( metricsCount, is( 2 ) );

        // MetricsFormatter formatter = new SimpleMetricsFormatter();
        // MetricsExporter exporter = new OutputStreamMetricsExporter(
        // System.out );
        // exporter.export( formatter, workloadMeasurements.getAllMeasurements()
        // );
    }
}
