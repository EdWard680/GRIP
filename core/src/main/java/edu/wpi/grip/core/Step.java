package edu.wpi.grip.core;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import edu.wpi.grip.core.events.SocketChangedEvent;
import edu.wpi.grip.core.util.ExceptionWitness;

import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A step is an instance of an operation in a pipeline.  A step contains a list of input and output sockets, and it
 * runs the operation whenever one of the input sockets changes.
 */
@XStreamAlias(value = "grip:Step")
public class Step {
    private static final Logger logger = Logger.getLogger(Step.class.getName());
    private static final String MISSING_SOCKET_MESSAGE_END = " must have a value to run this step.";

    private final ExceptionWitness witness;

    private final Operation operation;
    private final InputSocket<?>[] inputSockets;
    private final OutputSocket<?>[] outputSockets;
    private final Optional<?> data;


    @Singleton
    public static class Factory {
        private final EventBus eventBus;
        private final ExceptionWitness.Factory exceptionWitnessFactory;

        @Inject
        public Factory(EventBus eventBus, ExceptionWitness.Factory exceptionWitnessFactory) {
            this.eventBus = eventBus;
            this.exceptionWitnessFactory = exceptionWitnessFactory;
        }

        public Step create(Operation operation) {
            checkNotNull(operation, "The operation can not be null");
            // Create the list of input and output sockets, and mark this step as their owner.
            final InputSocket<?>[] inputSockets = operation.createInputSockets(eventBus);
            final OutputSocket<?>[] outputSockets = operation.createOutputSockets(eventBus);

            final Step step = new Step(
                    operation,
                    inputSockets,
                    outputSockets,
                    operation.createData(),
                    exceptionWitnessFactory
            );
            eventBus.register(step);
            for (Socket<?> socket : inputSockets) {
                socket.setStep(Optional.of(step));
            }
            for (Socket<?> socket : outputSockets) {
                socket.setStep(Optional.of(step));
            }

            step.runPerformIfPossible();
            return step;
        }
    }

    /**
     * @param operation               The operation that is performed at this step.
     * @param inputSockets            The input sockets from the operation.
     * @param outputSockets           The output sockets provided by the operation.
     * @param data                    The data provided by the operation.
     * @param exceptionWitnessFactory A factory used to create an {@link ExceptionWitness}
     */
    Step(Operation operation,
         InputSocket<?>[] inputSockets,
         OutputSocket<?>[] outputSockets,
         Optional<?> data,
         ExceptionWitness.Factory exceptionWitnessFactory) {
        this.operation = operation;
        this.inputSockets = inputSockets;
        this.outputSockets = outputSockets;
        this.data = data;
        this.witness = exceptionWitnessFactory.create(this);
    }

    /**
     * @return The underlying <code>Operation</code> that this step performs
     */
    public Operation getOperation() {
        return this.operation;
    }

    /**
     * @return An array of <code>Socket</code>s that hold the inputs to this step
     */
    public InputSocket<?>[] getInputSockets() {
        return inputSockets;
    }

    /**
     * @return An array of <code>Socket</code>s that hold the outputs of this step
     */
    public OutputSocket<?>[] getOutputSockets() {
        return outputSockets;
    }

    /**
     * Resets all {@link OutputSocket OutputSockets} to their initial value.
     * Should only be used by {@link Step#runPerformIfPossible()}
     */
    private void resetOutputSockets() {
        for (OutputSocket<?> outputSocket : outputSockets) {
            outputSocket.resetValueToInitial();
        }
    }

    /**
     * The {@link Operation#perform} method should only be called if all {@link InputSocket#getValue()} are not empty.
     * If one input is invalid then the perform method will not run and all output sockets will be assigned to their
     * default values.
     */
    private synchronized void runPerformIfPossible() {
        for (InputSocket<?> inputSocket : inputSockets) {
            // If there is a socket that isn't present then we have a problem.
            if (!inputSocket.getValue().isPresent()) {
                witness.flagWarning(inputSocket.getSocketHint().getIdentifier() + MISSING_SOCKET_MESSAGE_END);
                resetOutputSockets();
                return;  /* Only run the perform method if all of the input sockets are present. */
            }
        }

        try {
            this.operation.perform(inputSockets, outputSockets, data);
        } catch (RuntimeException e) {
            // We do not want to catch all exceptions, only runtime exceptions.
            // This is especially important when it comes to InterruptedExceptions
            final String operationFailedMessage = "The " + operation.getName() + " operation did not perform correctly.";
            logger.log(Level.WARNING, operationFailedMessage, e);
            witness.flagException(e, operationFailedMessage);
            resetOutputSockets();
            return;
        }
        witness.clearException();
    }

    @Subscribe
    public void onInputSocketChanged(SocketChangedEvent e) {
        final Socket<?> socket = e.getSocket();

        // If this socket that changed is one of the inputs to this step, run the operation with the new value.
        if (socket.getStep().equals(Optional.of(this)) && socket.getDirection().equals(Socket.Direction.INPUT)) {
            runPerformIfPossible();
        }
    }
}
