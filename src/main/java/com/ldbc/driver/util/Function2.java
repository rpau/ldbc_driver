package com.ldbc.driver.util;

public interface Function2<INPUT1, INPUT2, RETURN> {
    RETURN apply(INPUT1 input1, INPUT2 input2);
}
