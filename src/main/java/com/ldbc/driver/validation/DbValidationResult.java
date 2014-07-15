package com.ldbc.driver.validation;

import com.google.common.collect.Lists;
import com.ldbc.driver.Db;
import com.ldbc.driver.Operation;
import com.ldbc.driver.util.Tuple;

import java.util.*;

public class DbValidationResult {
    private final Db db;
    private final Set<Class> missingHandlersForOperationTypes;
    private final List<Tuple.Tuple2<Operation<?>, String>> unableToExecuteOperations;
    private final List<Tuple.Tuple3<Operation<?>, Object, Object>> incorrectResultsForOperations;
    private final Map<Class, Integer> successfullyExecutedOperationsPerOperationType;

    DbValidationResult(Db db) {
        this.db = db;
        this.missingHandlersForOperationTypes = new HashSet<>();
        this.unableToExecuteOperations = new ArrayList<>();
        this.incorrectResultsForOperations = new ArrayList<>();
        this.successfullyExecutedOperationsPerOperationType = new HashMap<>();
    }

    void reportMissingHandlerForOperation(Operation<?> operation) {
        missingHandlersForOperationTypes.add(operation.getClass());
    }

    void reportUnableToExecuteOperation(Operation<?> operation, String errorMessage) {
        unableToExecuteOperations.add(Tuple.<Operation<?>, String>tuple2(operation, errorMessage));
    }

    void reportIncorrectResultForOperation(Operation<?> operation, Object expectedResult, Object actualResult) {
        incorrectResultsForOperations.add(Tuple.<Operation<?>, Object, Object>tuple3(operation, expectedResult, actualResult));
    }

    void reportSuccessfulExecution(Operation<?> operation) {
        if (false == successfullyExecutedOperationsPerOperationType.containsKey(operation.getClass()))
            successfullyExecutedOperationsPerOperationType.put(operation.getClass(), 0);
        int successfullyExecutedOperationsForOperationType = successfullyExecutedOperationsPerOperationType.get(operation.getClass());
        successfullyExecutedOperationsForOperationType++;
        successfullyExecutedOperationsPerOperationType.put(operation.getClass(), successfullyExecutedOperationsForOperationType);
    }

    public boolean isSuccessful() {
        return missingHandlersForOperationTypes.isEmpty() && unableToExecuteOperations.isEmpty() && incorrectResultsForOperations.isEmpty();
    }

    public String resultMessage() {
        int padRightDistance = 10;
        StringBuilder sb = new StringBuilder();
        sb.append("Validation Result: ").append((isSuccessful()) ? "PASS" : "FAIL").append("\n");
        sb.append("Database: ").append(db.getClass().getName()).append("\n");
        sb.append("  ***\n");
        sb.append("  Successfully executed ").append(successfullyExecutedOperationsPerOperationType.size()).append(" operation types:\n");
        for (Class successfullyExecutedOperationsForType : sort(successfullyExecutedOperationsPerOperationType.keySet()))
            sb.append("    ").
                    append(String.format("%1$-" + padRightDistance + "s", successfullyExecutedOperationsPerOperationType.get(successfullyExecutedOperationsForType) + " of")).
                    append(successfullyExecutedOperationsForType.getSimpleName()).
                    append("\n");
        sb.append("  ***\n");
        sb.append("  Missing handler implementations for ").append(missingHandlersForOperationTypes.size()).append(" operation types:\n");
        for (Class operationType : sort(missingHandlersForOperationTypes))
            sb.append("    ").append(String.format("%1$-" + padRightDistance + "s", operationType.getName())).append("\n");
        sb.append("  ***\n");
        sb.append("  Unable to execute ").append(unableToExecuteOperations.size()).append(" operations:\n");
        for (Tuple.Tuple2<Operation<?>, String> failedOperationExecution : unableToExecuteOperations)
            sb.
                    append("    Operation: ").append(failedOperationExecution._1().toString()).append("\n").
                    append("               ").append(failedOperationExecution._2()).append("\n");
        sb.append("  ***\n");
        sb.append("  Incorrect results for ").append(incorrectResultsForOperations.size()).append(" operations:\n");
        for (Tuple.Tuple3<Operation<?>, Object, Object> incorrectResult : incorrectResultsForOperations)
            sb.
                    append("    Operation:        ").append(incorrectResult._1().toString()).append("\n").
                    append("    Expected Result:  ").append(incorrectResult._2().toString()).append("\n").
                    append("    Actual Result  :  ").append(incorrectResult._3().toString()).append("\n");
        sb.append("  ***\n");
        return sb.toString();
    }

    private <T> List<T> sort(Iterable<T> unsorted) {
        List<T> sorted = Lists.newArrayList(unsorted);
        Collections.sort(sorted, new DefaultComparator<T>());
        return sorted;
    }

    private static class DefaultComparator<T> implements Comparator<T> {
        @Override
        public int compare(T o1, T o2) {
            if (o1 instanceof Comparable)
                return ((Comparable) o1).compareTo(o2);
            else
                return o1.toString().compareTo(o2.toString());
        }
    }

}