package fixtures;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.TYPE, ElementType.CONSTRUCTOR, ElementType.METHOD})
@interface GeneratedCodeExclusion {
}

@GeneratedCodeExclusion
public class CustomAnnotationFixture {

    private String value;

    @GeneratedCodeExclusion
    public CustomAnnotationFixture(String value) {
        this.value = value;
    }

    @GeneratedCodeExclusion
    public String excludedMethod() {
        return value;
    }
}