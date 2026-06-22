package es.brasatech.pinchito;

import es.brasatech.pinchito.model.*;
import org.springframework.aot.hint.*;
import org.thymeleaf.spring6.view.AbstractThymeleafView;
import org.thymeleaf.spring6.view.ThymeleafView;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;

public class PinchitoRuntimeHints implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        // Register Thymeleaf view classes for reflection
        hints.reflection().registerType(ThymeleafView.class, 
                MemberCategory.INVOKE_DECLARED_CONSTRUCTORS, 
                MemberCategory.INVOKE_DECLARED_METHODS);
                
        hints.reflection().registerType(AbstractThymeleafView.class, 
                MemberCategory.INVOKE_DECLARED_CONSTRUCTORS, 
                MemberCategory.INVOKE_DECLARED_METHODS);

        hints.reflection().registerType(ThymeleafViewResolver.class, 
                MemberCategory.INVOKE_DECLARED_CONSTRUCTORS, 
                MemberCategory.INVOKE_DECLARED_METHODS);

        // Register template and static resources
        hints.resources().registerPattern("static/**");
        hints.resources().registerPattern("templates/**");

        // Jackson 2 reflection support for native image
        hints.reflection().registerType(TypeReference.of("com.fasterxml.jackson.databind.json.JsonMapper"), hint -> {
            hint.withMembers(MemberCategory.INVOKE_PUBLIC_METHODS);
            hint.withMethod("builder", java.util.Collections.emptyList(), ExecutableMode.INVOKE);
        });
        hints.reflection().registerType(TypeReference.of("com.fasterxml.jackson.databind.ObjectMapper"), MemberCategory.INVOKE_PUBLIC_METHODS);
        hints.reflection().registerType(TypeReference.of("com.fasterxml.jackson.databind.cfg.MapperBuilder"), MemberCategory.INVOKE_PUBLIC_METHODS);
        hints.reflection().registerType(TypeReference.of("com.fasterxml.jackson.databind.json.JsonMapper$Builder"), MemberCategory.INVOKE_PUBLIC_METHODS);

        // Register Thymeleaf Mvc delegate for reflection
        hints.reflection().registerType(TypeReference.of("org.thymeleaf.spring6.expression.Mvc$Spring41MvcUriComponentsBuilderDelegate"), 
                MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                MemberCategory.INVOKE_DECLARED_METHODS,
                MemberCategory.INVOKE_PUBLIC_METHODS);

        // Register Model classes for reflection (Serialization/Deserialization)
        hints.reflection().registerType(Account.class, 
                MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                MemberCategory.INVOKE_DECLARED_METHODS,
                MemberCategory.DECLARED_FIELDS,
                MemberCategory.INVOKE_PUBLIC_METHODS);

        hints.reflection().registerType(Member.class, 
                MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                MemberCategory.INVOKE_DECLARED_METHODS,
                MemberCategory.DECLARED_FIELDS,
                MemberCategory.INVOKE_PUBLIC_METHODS);

        hints.reflection().registerType(Debit.class, 
                MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                MemberCategory.INVOKE_DECLARED_METHODS,
                MemberCategory.DECLARED_FIELDS,
                MemberCategory.INVOKE_PUBLIC_METHODS);

        hints.reflection().registerType(DebitKind.class, 
                MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                MemberCategory.INVOKE_DECLARED_METHODS,
                MemberCategory.DECLARED_FIELDS,
                MemberCategory.INVOKE_PUBLIC_METHODS);

        hints.reflection().registerType(PaymentRequest.class, 
                MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                MemberCategory.INVOKE_DECLARED_METHODS,
                MemberCategory.DECLARED_FIELDS,
                MemberCategory.INVOKE_PUBLIC_METHODS);

        hints.reflection().registerType(WebSocketEvent.class, 
                MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                MemberCategory.INVOKE_DECLARED_METHODS,
                MemberCategory.DECLARED_FIELDS,
                MemberCategory.INVOKE_PUBLIC_METHODS);
    }
}
