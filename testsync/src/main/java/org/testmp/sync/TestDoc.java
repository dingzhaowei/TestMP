/*
 * TestMP (Test Management Platform)
 * Copyright 2013 and beyond, Zhaowei Ding.
 *
 * TestMP is free software; you can redistribute it and/or modify it
 * under the terms of the MIT License (MIT).
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 */

package org.testmp.sync;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Target({ METHOD })
@Retention(RUNTIME)
public @interface TestDoc {

    /**
     * The test project to which this test belongs
     * 
     * @return
     */
    String project() default "";

    /**
     * The test name
     * 
     * @return
     */
    String name() default "";

    /**
     * The test description
     * 
     * @return
     */
    String description() default "";

    /**
     * The groups containing this test
     * 
     * @return
     */
    String[] groups() default {};

}
