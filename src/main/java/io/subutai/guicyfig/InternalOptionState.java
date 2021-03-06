package io.subutai.guicyfig;


import java.lang.reflect.Method;

import com.google.common.base.Preconditions;
import com.google.common.hash.HashCode;
import com.netflix.config.DynamicBooleanProperty;
import com.netflix.config.DynamicDoubleProperty;
import com.netflix.config.DynamicFloatProperty;
import com.netflix.config.DynamicIntProperty;
import com.netflix.config.DynamicLongProperty;
import com.netflix.config.DynamicStringProperty;
import com.netflix.config.PropertyWrapper;


/**
 * A module's configuration options.
 */
class InternalOptionState<V, T extends PropertyWrapper<V>> implements OptionState<V> {
    private final String key;
    private final T property;
    private V oldValue;
    private Option bypass;
    private Option override;
    private Method method;


    InternalOptionState( String key, T property, Method method ) {
        Preconditions.checkNotNull( key, "key cannot be null" );
        Preconditions.checkNotNull( property, "property cannot be null" );
        Preconditions.checkNotNull( method, "method cannot be null" );

        this.key = key;
        this.property = property;
        this.method = method;
        this.oldValue = extractValue();
    }


    private V extractValue() {
        if ( method.getReturnType().isEnum() ) {
            //noinspection unchecked
            return ( V ) EnumUtils.getEnumInstance( ( String ) property.getValue(), method.getReturnType() );
        }
        else {
            return property.getValue();
        }
    }


    Method getMethod() {
        return method;
    }


    V update() {
        V val = oldValue;
        oldValue = extractValue();
        return val;
    }


    @Override
    public String getKey() {
        return key;
    }


    @Override
    public V getValue() {
        return extractValue();
    }


    @Override
    public V getOldValue() {
        return oldValue;
    }


    @Override
    public V getOverrideValue() {
        if ( override == null || override.override() == null ) {
            return null;
        }

        //noinspection unchecked
        return ( V ) convertValue( override.override() );
    }


    @Override
    public V getBypassValue() {
        if ( bypass == null || bypass.override() == null ) {
            return null;
        }

        //noinspection unchecked
        return ( V ) convertValue( bypass.override() );
    }


    @Override
    public Option getOverride() {
        return override;
    }


    @Override
    public int hashCode() {
        return HashCode.fromString( key ).hashCode();
    }


    T getProperty() {
        return property;
    }


    void setBypass( Option value ) {
        this.bypass = value;
    }


    @Override
    public boolean isBypassed() {
        return bypass != null;
    }


    @Override
    public boolean isOverridden() {
        return override != null;
    }


    @Override
    public Option getBypass() {
        return bypass;
    }


    public void setOverride( Option override ) {
        this.override = override;
    }


    Object getEffectiveValue() {
        if ( bypass == null && override == null ) {
            return property.getValue();
        }

        if ( bypass == null ) {
            return getOverrideValue();
        }

        return getBypassValue();
    }


    Object convertValue( String value ) {
        if ( property instanceof DynamicStringProperty ) {
            if ( method.getReturnType().isEnum() ) {
                return EnumUtils.getEnumInstance( value, method.getReturnType() );
            }

            return value;
        }

        Preconditions.checkNotNull( value, "the value cannot be null if we're going to be parsing it" );

        if ( property instanceof DynamicIntProperty ) {
            return Integer.parseInt( value );
        }

        if ( property instanceof DynamicBooleanProperty ) {
            return Boolean.parseBoolean( value );
        }

        if ( property instanceof DynamicLongProperty ) {
            return Long.parseLong( value );
        }

        if ( property instanceof DynamicFloatProperty ) {
            return Float.parseFloat( value );
        }

        if ( property instanceof DynamicDoubleProperty ) {
            return Double.parseDouble( value );
        }

        throw new IllegalArgumentException( "Don't know how to convert the property: " + property.toString() );
    }
}
