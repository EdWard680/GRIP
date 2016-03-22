package edu.wpi.grip.core.operations.networktables;

import java.util.function.Function;

/**
 * An adapter to allow numbers to be published from GRIP sockets into NetworkTables
 *
 * @see NTPublishOperation#NTPublishOperation(Class, Class, Function)
 */
public class NTNumber implements NTPublishable {

    private double number;

    public NTNumber() {
    	this(0.0);
    }
    
    public NTNumber(Number number) {
        this.number = number.doubleValue();
    }

    @NTValue(weight = 0)
    public double getValue() {
        return number;
    }
    
    public Number getNumber() {
    	return number;
    }
    
    @NTValue(weight = 1)
    public void setValue(double val) {
    	number = val;
    }
}
