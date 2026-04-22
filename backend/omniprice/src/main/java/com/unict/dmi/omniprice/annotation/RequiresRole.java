package com.unict.dmi.omniprice.annotation;

import java.lang.annotation.*;

/**
 * Annotation per il controllo dei ruoli via AOP (Reference Monitor pattern).
 * Applicata su metodi di controller che richiedono un ruolo specifico.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequiresRole {
    String[] value() default {"STANDARD"};
}
