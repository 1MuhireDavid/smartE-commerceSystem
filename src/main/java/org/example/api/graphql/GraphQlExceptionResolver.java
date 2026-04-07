package org.example.api.graphql;

import graphql.ErrorType;
import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * Maps exceptions thrown by GraphQL resolvers into structured GraphQL errors.
 *
 * <p>Without this, Spring GraphQL wraps every exception in a generic INTERNAL_ERROR
 * with a vague message. This resolver converts domain exceptions to meaningful
 * error types so clients can act on them.
 */
@Component
public class GraphQlExceptionResolver extends DataFetcherExceptionResolverAdapter {

    @Override
    protected GraphQLError resolveToSingleError(Throwable ex, DataFetchingEnvironment env) {
        if (ex instanceof ResponseStatusException rse) {
            ErrorType errorType = switch (rse.getStatusCode().value()) {
                case 400 -> ErrorType.ValidationError;
                case 404 -> ErrorType.DataFetchingException;   // closest GraphQL equivalent
                case 409 -> ErrorType.ValidationError;
                default  -> ErrorType.ExecutionAborted;
            };

            return GraphqlErrorBuilder.newError(env)
                    .errorType(errorType)
                    .message(rse.getReason() != null ? rse.getReason() : rse.getMessage())
                    .path(env.getExecutionStepInfo().getPath())
                    .location(env.getField().getSourceLocation())
                    .build();
        }

        // Let Spring GraphQL handle anything else as INTERNAL_ERROR
        return null;
    }
}
