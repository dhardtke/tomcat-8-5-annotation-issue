import javax.ws.rs.ApplicationPath;

@ApplicationPath("/")
public class ApplicationConfig extends ResourceConfig {
    /**
     * Configure the NablaExerciseHandlerService.
     */
    public ApplicationConfig() {
        register(VersioningWebService.class);
        register(JerseyMapperProvider.class);
        // register(JacksonFeature.class);
        register(AuthenticationFilter.class);

        // register(RightsFilter.class);
        register(GZIPWriterInterceptor.class);
        register(WebApplicationExceptionMapper.class);

        // where to find annotated web services
        packages("web");

        // allow dependency injection
        register(new NablaExerciseHandlerServicesBinder());
        register(new NablaEntityManagerBinder());

        // logging
        register(LoggingFeature.class);
    }
}
