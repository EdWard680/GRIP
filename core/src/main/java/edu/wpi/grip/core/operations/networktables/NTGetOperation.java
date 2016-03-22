package edu.wpi.grip.core.operations.networktables;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Optional;
import java.util.function.Function;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Ordering;
import com.google.common.eventbus.EventBus;

import edu.wpi.first.wpilibj.networktables.NetworkTable;
import edu.wpi.first.wpilibj.tables.ITable;
import edu.wpi.first.wpilibj.tables.ITableListener;
import edu.wpi.grip.core.InputSocket;
import edu.wpi.grip.core.Operation;
import edu.wpi.grip.core.OutputSocket;
import edu.wpi.grip.core.SocketHint;
import edu.wpi.grip.core.SocketHints;
import edu.wpi.grip.core.Operation.Category;

/**
 * An operation that fetches any standard Network Tables value.
 */
public class NTGetOperation<T> implements Operation, ITableListener {
    
	private Class<T> type;
	T default_value; 
	
    private InputSocket<?> key_input = null;
    private OutputSocket<?> value_output = null;

    /**
     * Create a new fetch operation for a socket type that implements {@link NTPublishable} directly
     */
    @SuppressWarnings("unchecked")
    public NTGetOperation(Class<T> type, T default_value) {
    	this.type = checkNotNull(type);
    	this.default_value = checkNotNull(default_value);
    }

    @Override
    public String getName() {
        return "Get " + type.getSimpleName();
    }

    @Override
    public String getDescription() {
        return "Get a " + type.getSimpleName() + " to NetworkTables";
    }

    @Override
    public Category getCategory() {
        return Category.NETWORK;
    }

    @Override
    public Optional<InputStream> getIcon() {
        return Optional.of(getClass().getResourceAsStream("/edu/wpi/grip/ui/icons/first.png"));
    }

    @Override
    public InputSocket<?>[] createInputSockets(EventBus eventBus) {
        final InputSocket<?>[] sockets = new InputSocket[1];

        // Create a string input for the key used by NetworkTables
        key_input = sockets[0] = new InputSocket<>(eventBus,
                SocketHints.Inputs.createTextSocketHint("Subtable Name", "my" + type.getSimpleName()));
        
                
        return sockets;
    }

    @Override
    public OutputSocket<?>[] createOutputSockets(EventBus eventBus) {
    	final OutputSocket<?>[] sockets = new OutputSocket[1];

    	// Create an output for the actual object being fetched
        value_output = sockets[0] = new OutputSocket<>(eventBus,
                new SocketHint.Builder<>(type).identifier("Value").initialValue(default_value).build());

        return sockets;
    }

    @Override
    public void perform(InputSocket<?>[] inputs, OutputSocket<?>[] outputs) {

        final String subtableName = (String) key_input.getValue().get();

        if (subtableName.isEmpty()) {
            throw new IllegalArgumentException("Need key to fetch from NetworkTables");
        }

        // Get a subtable to put the values in.  Each NTPublishable has multiple properties that are published (such as
        // x, y, width, height, etc...), so they're grouped together in a subtable.
        final ITable subtable;
        synchronized (NetworkTable.class) {
            subtable = NetworkTable.getTable("GRIP");
            subtable.addTableListener(this);
        }
        
    }
    
    @SuppressWarnings("unchecked")
	@Override
    public void valueChanged(ITable source, String key, Object value, boolean isNew)
    {
		((OutputSocket<T>)value_output).setValue((T)value);
    }
}


