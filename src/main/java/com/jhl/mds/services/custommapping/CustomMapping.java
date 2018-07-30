package com.jhl.mds.services.custommapping;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.Map;

public class CustomMapping {

    private final ScriptEngine jsEngine;
    private final Bindings bindings;
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    public CustomMapping() {
        ScriptEngineManager manager = new ScriptEngineManager();
        this.jsEngine = manager.getEngineByName("nashorn");
        this.bindings = jsEngine.createBindings();
    }

    public synchronized String resolve(String input, Map<String, Object> data) throws ScriptException {
        bindings.clear();
        for (Map.Entry<String, Object> e : data.entrySet()) {
            bindings.put(e.getKey(), e.getValue());
        }

        input = "var json = JSON.stringify;" + input;

//        logger.info("Try to evaluation " + input);
        Object result = jsEngine.eval(input, bindings);
        return String.valueOf(result);
    }
}
