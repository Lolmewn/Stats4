package nl.lolmewn.stats;

import java.lang.annotation.*;

@Target(value = ElementType.TYPE)
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Statistic {

    String[] variables();

    String table();

}
