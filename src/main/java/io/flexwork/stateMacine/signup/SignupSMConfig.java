package io.flexwork.stateMacine.signup;

import io.flexwork.stateMacine.signup.actions.NewSignUpAction;
import io.flexwork.stateMacine.signup.actions.NewSignupVerificationAction;
import java.util.EnumSet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.EnumStateMachineConfigurerAdapter;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.config.builders.StateMachineConfigurationConfigurer;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;
import org.springframework.statemachine.data.jpa.JpaPersistingStateMachineInterceptor;
import org.springframework.statemachine.data.jpa.JpaStateMachineRepository;
import org.springframework.statemachine.guard.Guard;
import org.springframework.statemachine.persist.StateMachineRuntimePersister;
import org.springframework.statemachine.service.DefaultStateMachineService;
import org.springframework.statemachine.service.StateMachineService;

@Configuration
public class SignupSMConfig {

  @Configuration
  public static class StateMachinePersisterConfig {

    @Bean
    public StateMachineRuntimePersister<SignupStates, SignupEvents, String>
        stateMachineRuntimePersister(JpaStateMachineRepository jpaStateMachineRepository) {
      return new JpaPersistingStateMachineInterceptor<>(jpaStateMachineRepository);
    }
  }

  @Configuration
  @EnableStateMachineFactory
  public static class StateMachineConfig
      extends EnumStateMachineConfigurerAdapter<SignupStates, SignupEvents> {

    @Autowired private NewSignUpAction newSignUpAction;

    @Autowired private NewSignupVerificationAction newSignupVerificationAction;

    private StateMachineRuntimePersister<SignupStates, SignupEvents, String>
        stateMachineRuntimePersister;

    @Override
    public void configure(StateMachineConfigurationConfigurer<SignupStates, SignupEvents> config)
        throws Exception {
      config.withPersistence().runtimePersister(stateMachineRuntimePersister);
    }

    @Override
    public void configure(StateMachineStateConfigurer<SignupStates, SignupEvents> states)
        throws Exception {
      states
          .withStates()
          .initial(SignupStates.NEW_SIGNUP_USER)
          .states(EnumSet.allOf(SignupStates.class))
          .end(SignupStates.SIGNUP_COMPLETED);
    }

    @Override
    public void configure(StateMachineTransitionConfigurer<SignupStates, SignupEvents> transitions)
        throws Exception {
      transitions
          .withExternal()
          .source(SignupStates.NEW_SIGNUP_USER)
          .target(SignupStates.SIGNING_UP)
          .event(SignupEvents.NEW_SIGNUP)
          .guard(signupGuard())
          .action(newSignUpAction)
          .and()
          .withExternal()
          .source(SignupStates.SIGNING_UP)
          .target(SignupStates.SIGNUP_VERIFICATION)
          .event(SignupEvents.SIGNUP_VERIFICATION)
          .action(newSignupVerificationAction)
          .and()
          .withExternal()
          .source(SignupStates.SIGNUP_VERIFICATION)
          .target(SignupStates.SIGNUP_COMPLETED)
          .event(SignupEvents.SIGNUP_SUCCESS);
    }

    @Bean
    public Guard<SignupStates, SignupEvents> signupGuard() {
      return context -> {
        System.out.println("Signup Guard");
        return true;
      };
    }
  }

  @Configuration
  public static class ServiceConfig {

    @Bean
    public StateMachineService<SignupStates, SignupEvents> stateMachineService(
        StateMachineFactory<SignupStates, SignupEvents> stateMachineFactory,
        StateMachineRuntimePersister<SignupStates, SignupEvents, String>
            stateMachineRuntimePersister) {
      return new DefaultStateMachineService<>(stateMachineFactory, stateMachineRuntimePersister);
    }
  }
}
